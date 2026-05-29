package com.dkds.cip.enrollment.policy;

import com.dkds.cip.enrollment.common.event.EnrollmentEventPublisher;
import com.dkds.cip.enrollment.common.event.payload.PolicyAssignedPayload;
import com.dkds.cip.enrollment.common.exception.ResourceNotFoundException;
import com.dkds.cip.enrollment.pet.PetService;
import com.dkds.cip.enrollment.policy.dto.AssignPolicyRequest;
import com.dkds.cip.enrollment.policy.dto.UpdatePolicyRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository repository;
    private final PetService petService;
    private final EnrollmentEventPublisher eventPublisher;

    @Transactional
    public Policy assign(UUID petId, AssignPolicyRequest req) {
        var pet = petService.getById(petId);
        var policy = new Policy();
        policy.setPetId(petId);
        policy.setCoverageType(req.coverageType());
        policy.setStartDate(req.startDate());
        policy.setEndDate(req.endDate());
        policy.setCreatedAt(Instant.now());
        var saved = repository.save(policy);
        eventPublisher.publish("policy.assigned", "policy", saved.getId(), pet.getClinicId(),
                new PolicyAssignedPayload(saved.getId(), petId, saved.getCoverageType().name(),
                        saved.getStartDate(), saved.getEndDate(), saved.getStatus().name()));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Policy> listByPet(UUID petId) {
        petService.throwIfNotExists(petId);
        return repository.findByPetId(petId);
    }

    @Transactional(readOnly = true)
    public Policy getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + id));
    }

    @Transactional
    public Policy update(UUID id, UpdatePolicyRequest req) {
        var policy = getById(id);
        if (req.endDate() != null) policy.setEndDate(req.endDate());
        if (req.status() != null) policy.setStatus(req.status());
        policy.setUpdatedAt(Instant.now());
        return repository.save(policy);
    }
}
