package com.dkds.cip.claims.claim;

import com.dkds.cip.claims.adjudication.AdjudicationDecision;
import com.dkds.cip.claims.adjudication.RulesEngine;
import com.dkds.cip.claims.claim.dto.ManualClaimLineRequest;
import com.dkds.cip.claims.claim.dto.SubmitManualClaimRequest;
import com.dkds.cip.claims.common.exception.ResourceNotFoundException;
import com.dkds.cip.claims.fraud.FraudScoredPayload;
import com.dkds.cip.claims.masterdata.catalogue.LocalCatalogueItem;
import com.dkds.cip.claims.masterdata.catalogue.LocalCatalogueItemRepository;
import com.dkds.cip.claims.masterdata.clinic.LocalClinicRepository;
import com.dkds.cip.claims.masterdata.clinic.LocalClinicStatus;
import com.dkds.cip.claims.masterdata.pet.LocalPetRepository;
import com.dkds.cip.claims.masterdata.pet.LocalPetStatus;
import com.dkds.cip.claims.masterdata.policy.LocalPolicyRepository;
import com.dkds.cip.claims.masterdata.policy.LocalPolicyStatus;
import com.dkds.cip.claims.masterdata.session.SessionLinePayload;
import com.dkds.cip.claims.masterdata.session.SessionVerifiedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final LocalClinicRepository clinicRepository;
    private final LocalPetRepository petRepository;
    private final LocalPolicyRepository policyRepository;
    private final LocalCatalogueItemRepository catalogueRepository;
    private final RulesEngine rulesEngine;
    private final ClaimEventPublisher eventPublisher;

    @Transactional
    public Claim submitManualClaim(UUID clinicId, SubmitManualClaimRequest req) {
        var clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic not found: " + clinicId));
        if (clinic.getStatus() != LocalClinicStatus.ACTIVE) {
            throw new IllegalStateException("Clinic " + clinicId + " is not ACTIVE");
        }

        var pet = petRepository.findById(req.petId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found: " + req.petId()));
        if (pet.getStatus() != LocalPetStatus.ACTIVE) {
            throw new IllegalStateException("Pet " + req.petId() + " is not ACTIVE");
        }
        if (!pet.getClinicId().equals(clinicId)) {
            throw new IllegalStateException("Pet " + req.petId() + " does not belong to clinic " + clinicId);
        }

        var policy = policyRepository.findById(req.policyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + req.policyId()));
        if (policy.getStatus() != LocalPolicyStatus.ACTIVE) {
            throw new IllegalStateException("Policy " + req.policyId() + " is not ACTIVE");
        }
        if (!policy.getPetId().equals(req.petId())) {
            throw new IllegalStateException("Policy " + req.policyId() + " does not belong to pet " + req.petId());
        }

        var claim = new Claim();
        claim.setClinicId(clinicId);
        claim.setPetId(req.petId());
        claim.setPolicyId(req.policyId());
        claim.setOrigin(ClaimOrigin.MANUAL);
        claim.setSubmittedBy(req.submittedBy());
        claim.setStatus(ClaimStatus.ASSEMBLED);
        claim.setCreatedAt(Instant.now());

        var totalRequested = BigDecimal.ZERO;
        for (var lineReq : req.lines()) {
            var line = new ClaimLine();
            line.setClaim(claim);
            line.setProcedureCode(lineReq.procedureCode());
            line.setQuantity(lineReq.quantity());
            line.setRequestedAmount(lineReq.requestedAmount());
            claim.getLines().add(line);
            totalRequested = totalRequested.add(lineReq.requestedAmount());
        }
        claim.setTotalRequested(totalRequested);

        addTransition(claim, null, ClaimStatus.ASSEMBLED, "auto", null);

        var saved = claimRepository.save(claim);
        eventPublisher.publishAssembled(saved);

        return routeToReview(saved, List.of("Manual claims require adjuster review"));
    }

    @Transactional
    public Claim assembleFromSession(SessionVerifiedPayload payload) {
        var pet = petRepository.findById(payload.petId()).orElse(null);
        if (pet == null || pet.getStatus() != LocalPetStatus.ACTIVE) {
            log.warn("Skipping session.verified {} — pet {} not found or inactive",
                    payload.sessionId(), payload.petId());
            return null;
        }

        var policies = policyRepository.findByPetIdAndStatus(payload.petId(), LocalPolicyStatus.ACTIVE);
        if (policies.isEmpty()) {
            log.warn("Skipping session.verified {} — no active policy for pet {}",
                    payload.sessionId(), payload.petId());
            return null;
        }
        var policy = policies.get(0);

        var procedureCodes = payload.lines().stream()
                .map(SessionLinePayload::procedureCode)
                .collect(java.util.stream.Collectors.toSet());
        var catalogueMap = buildCatalogueMap(procedureCodes);

        var claim = new Claim();
        claim.setClinicId(payload.clinicId());
        claim.setPetId(payload.petId());
        claim.setPolicyId(policy.getId());
        claim.setOrigin(ClaimOrigin.SESSION);
        claim.setSourceSessionId(payload.sessionId());
        claim.setSubmittedBy(payload.verifiedBy());
        claim.setStatus(ClaimStatus.ASSEMBLED);
        claim.setCreatedAt(Instant.now());

        var totalRequested = BigDecimal.ZERO;
        for (var linePayload : payload.lines()) {
            var line = new ClaimLine();
            line.setClaim(claim);
            line.setProcedureCode(linePayload.procedureCode());
            line.setQuantity(linePayload.quantity());
            var catalogueItem = catalogueMap.get(linePayload.procedureCode());
            var requestedAmount = catalogueItem != null
                    ? catalogueItem.getReimbursementRate().multiply(BigDecimal.valueOf(linePayload.quantity()))
                    : BigDecimal.ZERO;
            line.setRequestedAmount(requestedAmount);
            claim.getLines().add(line);
            totalRequested = totalRequested.add(requestedAmount);
        }
        claim.setTotalRequested(totalRequested);

        addTransition(claim, null, ClaimStatus.ASSEMBLED, "auto", null);
        var saved = claimRepository.save(claim);
        eventPublisher.publishAssembled(saved);

        return saved;
    }

    @Transactional
    public void handleFraudScored(FraudScoredPayload payload) {
        var claim = claimRepository.findById(payload.claimId()).orElse(null);
        if (claim == null) {
            log.warn("Skipping claim.fraud-scored {} — claim not found", payload.claimId());
            return;
        }
        if (claim.getStatus() != ClaimStatus.ASSEMBLED || claim.getOrigin() != ClaimOrigin.SESSION) {
            log.debug("Ignoring claim.fraud-scored for claim {} — status={} origin={}",
                    claim.getId(), claim.getStatus(), claim.getOrigin());
            return;
        }

        if ("low".equals(payload.riskLevel())) {
            var policy = policyRepository.findById(claim.getPolicyId());
            var procedureCodes = claim.getLines().stream()
                    .map(ClaimLine::getProcedureCode)
                    .collect(Collectors.toSet());
            var catalogueMap = buildCatalogueMap(procedureCodes);
            adjudicate(claim, policy, catalogueMap);
        } else {
            routeToReview(claim, List.of("Fraud risk level: " + payload.riskLevel()));
        }
    }

    @Transactional(readOnly = true)
    public Claim getById(UUID id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Claim> listByClinic(UUID clinicId, Optional<ClaimStatus> status) {
        return status.map(s -> claimRepository.findByClinicIdAndStatus(clinicId, s))
                .orElseGet(() -> claimRepository.findByClinicId(clinicId));
    }

    Claim adjudicate(Claim claim,
                     Optional<com.dkds.cip.claims.masterdata.policy.LocalPolicy> policy,
                     Map<String, LocalCatalogueItem> catalogue) {
        var result = rulesEngine.adjudicate(claim.getLines(), policy, catalogue);

        switch (result.decision()) {
            case APPROVED, PARTIALLY_APPROVED -> {
                var decision = result.decision() == AdjudicationDecision.APPROVED
                        ? AdjudicationDecision.APPROVED : AdjudicationDecision.PARTIALLY_APPROVED;
                addTransition(claim, ClaimStatus.ASSEMBLED, ClaimStatus.ADJUDICATED, "auto",
                        String.join("; ", result.reasons()));
                claim.setStatus(ClaimStatus.ADJUDICATED);
                claim.setAdjudicationDecision(decision);
                claim.setApprovedAmount(result.totalApproved());
                claim.setUpdatedAt(Instant.now());
                var saved = claimRepository.save(claim);
                eventPublisher.publishAdjudicated(saved, result.reasons());

                addTransition(saved, ClaimStatus.ADJUDICATED, ClaimStatus.READY_FOR_SUBMISSION, "auto", null);
                saved.setStatus(ClaimStatus.READY_FOR_SUBMISSION);
                saved.setUpdatedAt(Instant.now());
                var ready = claimRepository.save(saved);
                eventPublisher.publishReadyForSubmission(ready);
                return ready;
            }
            case REJECTED -> {
                addTransition(claim, ClaimStatus.ASSEMBLED, ClaimStatus.REJECTED, "auto",
                        String.join("; ", result.reasons()));
                claim.setStatus(ClaimStatus.REJECTED);
                claim.setUpdatedAt(Instant.now());
                var saved = claimRepository.save(claim);
                eventPublisher.publishRejected(saved, result.reasons());
                return saved;
            }
        }
        return claim;
    }

    private Claim routeToReview(Claim claim, List<String> reasons) {
        addTransition(claim, ClaimStatus.ASSEMBLED, ClaimStatus.PENDING_REVIEW, "auto",
                String.join("; ", reasons));
        claim.setStatus(ClaimStatus.PENDING_REVIEW);
        claim.setUpdatedAt(Instant.now());
        var saved = claimRepository.save(claim);
        eventPublisher.publishRoutedToReview(saved, reasons);
        return saved;
    }

    private Map<String, LocalCatalogueItem> buildCatalogueMap(java.util.Set<String> codes) {
        return codes.stream()
                .flatMap(code -> catalogueRepository.findByCode(code).stream())
                .collect(Collectors.toMap(LocalCatalogueItem::getCode, item -> item));
    }

    private void addTransition(Claim claim, ClaimStatus from, ClaimStatus to, String actor, String reason) {
        var t = new ClaimTransition();
        t.setClaim(claim);
        t.setFromStatus(from);
        t.setToStatus(to);
        t.setActor(actor);
        t.setReason(reason);
        t.setOccurredAt(Instant.now());
        claim.getTransitions().add(t);
    }
}
