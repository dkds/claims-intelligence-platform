package com.dkds.cip.sessions.session;

import com.dkds.cip.sessions.common.exception.ResourceNotFoundException;
import com.dkds.cip.sessions.masterdata.catalogue.LocalCatalogueItem;
import com.dkds.cip.sessions.masterdata.catalogue.LocalCatalogueItemRepository;
import com.dkds.cip.sessions.masterdata.pet.LocalPetRepository;
import com.dkds.cip.sessions.masterdata.pet.LocalPetStatus;
import com.dkds.cip.sessions.masterdata.policy.LocalPolicyRepository;
import com.dkds.cip.sessions.masterdata.policy.LocalPolicyStatus;
import com.dkds.cip.sessions.masterdata.vet.LocalVetRepository;
import com.dkds.cip.sessions.masterdata.vet.LocalVetStatus;
import com.dkds.cip.sessions.session.dto.LogSessionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final LocalVetRepository vetRepository;
    private final LocalPetRepository petRepository;
    private final LocalPolicyRepository policyRepository;
    private final LocalCatalogueItemRepository catalogueRepository;
    private final SessionEventPublisher eventPublisher;

    @Transactional
    public Session log(UUID clinicId, LogSessionRequest req) {
        var vet = vetRepository.findById(req.vetId())
                .orElseThrow(() -> new ResourceNotFoundException("Vet not found: " + req.vetId()));
        if (vet.getStatus() != LocalVetStatus.APPROVED) {
            throw new IllegalStateException("Vet " + req.vetId() + " is not APPROVED");
        }
        if (!vet.getClinicId().equals(clinicId)) {
            throw new IllegalStateException("Vet " + req.vetId() + " does not belong to clinic " + clinicId);
        }

        var pet = petRepository.findById(req.petId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found: " + req.petId()));
        if (pet.getStatus() != LocalPetStatus.ACTIVE) {
            throw new IllegalStateException("Pet " + req.petId() + " is not ACTIVE");
        }
        if (!pet.getClinicId().equals(clinicId)) {
            throw new IllegalStateException("Pet " + req.petId() + " does not belong to clinic " + clinicId);
        }

        var activePolicies = policyRepository.findByPetIdAndStatus(req.petId(), LocalPolicyStatus.ACTIVE);
        if (activePolicies.isEmpty()) {
            throw new IllegalStateException("Pet " + req.petId() + " has no ACTIVE policy");
        }

        for (var lineReq : req.lines()) {
            boolean valid = catalogueRepository.findByCode(lineReq.procedureCode())
                    .map(LocalCatalogueItem::isActive)
                    .orElse(false);
            if (!valid) {
                throw new IllegalStateException("Procedure code not found or inactive: " + lineReq.procedureCode());
            }
        }

        var session = new Session();
        session.setClinicId(clinicId);
        session.setPetId(req.petId());
        session.setVetId(req.vetId());
        session.setLoggedAt(Instant.now());

        for (var lineReq : req.lines()) {
            var line = new SessionLine();
            line.setProcedureCode(lineReq.procedureCode());
            line.setQuantity(lineReq.quantity());
            line.setNotes(lineReq.notes());
            line.setSession(session);
            session.getLines().add(line);
        }

        var saved = sessionRepository.save(session);
        eventPublisher.publishSessionLogged(saved);
        return saved;
    }

    @Transactional
    public Session verify(UUID id, UUID verifiedBy) {
        var session = getById(id);
        if (session.getStatus() != SessionStatus.LOGGED) {
            throw new IllegalStateException(
                    "Cannot verify session in status " + session.getStatus() + "; must be LOGGED");
        }
        session.setStatus(SessionStatus.VERIFIED);
        session.setVerifiedAt(Instant.now());
        session.setVerifiedBy(verifiedBy);
        var saved = sessionRepository.save(session);
        eventPublisher.publishSessionVerified(saved);
        return saved;
    }

    @Transactional
    public Session cancel(UUID id) {
        var session = getById(id);
        if (session.getStatus() == SessionStatus.VERIFIED) {
            throw new IllegalStateException("Cannot cancel a VERIFIED session");
        }
        session.setStatus(SessionStatus.CANCELLED);
        return sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public Session getById(UUID id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Session> listByClinic(UUID clinicId) {
        return sessionRepository.findByClinicId(clinicId);
    }
}
