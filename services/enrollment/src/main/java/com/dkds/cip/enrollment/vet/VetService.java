package com.dkds.cip.enrollment.vet;

import com.dkds.cip.enrollment.clinic.ClinicService;
import com.dkds.cip.enrollment.common.event.EnrollmentEventPublisher;
import com.dkds.cip.enrollment.common.event.payload.VetApprovedPayload;
import com.dkds.cip.enrollment.common.event.payload.VetRegisteredPayload;
import com.dkds.cip.enrollment.common.event.payload.VetRejectedPayload;
import com.dkds.cip.enrollment.common.exception.ResourceNotFoundException;
import com.dkds.cip.enrollment.vet.dto.RegisterVetRequest;
import com.dkds.cip.enrollment.vet.dto.UpdateVetRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VetService {

    private final VetRepository repository;
    private final ClinicService clinicService;
    private final EnrollmentEventPublisher eventPublisher;

    @Transactional
    public Vet register(UUID clinicId, RegisterVetRequest req) {
        clinicService.throwIfNotExists(clinicId);
        var vet = new Vet();
        vet.setClinicId(clinicId);
        vet.setFirstName(req.firstName());
        vet.setLastName(req.lastName());
        vet.setEmail(req.email());
        vet.setLicenseNumber(req.licenseNumber());
        vet.setRegisteredAt(Instant.now());
        var saved = repository.save(vet);
        eventPublisher.publish("vet.registered", "vet", saved.getId(), clinicId,
                new VetRegisteredPayload(saved.getId(), clinicId,
                        saved.getFirstName(), saved.getLastName(), saved.getStatus().name()));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Vet> listByClinic(UUID clinicId) {
        clinicService.throwIfNotExists(clinicId);
        return repository.findByClinicId(clinicId);
    }

    @Transactional(readOnly = true)
    public Vet getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vet not found: " + id));
    }

    @Transactional
    public Vet update(UUID id, UpdateVetRequest req) {
        var vet = getById(id);
        if (req.firstName() != null) vet.setFirstName(req.firstName());
        if (req.lastName() != null) vet.setLastName(req.lastName());
        if (req.email() != null) vet.setEmail(req.email());
        if (req.licenseNumber() != null) vet.setLicenseNumber(req.licenseNumber());
        vet.setUpdatedAt(Instant.now());
        return repository.save(vet);
    }

    @Transactional
    public Vet approve(UUID id) {
        var vet = getById(id);
        if (vet.getStatus() != VetStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot approve vet in status " + vet.getStatus() + "; must be PENDING");
        }
        vet.setStatus(VetStatus.APPROVED);
        vet.setUpdatedAt(Instant.now());
        var saved = repository.save(vet);
        eventPublisher.publish("vet.approved", "vet", saved.getId(), saved.getClinicId(),
                new VetApprovedPayload(saved.getId(), saved.getClinicId()));
        return saved;
    }

    @Transactional
    public Vet reject(UUID id, String reason) {
        var vet = getById(id);
        if (vet.getStatus() != VetStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot reject vet in status " + vet.getStatus() + "; must be PENDING");
        }
        vet.setStatus(VetStatus.REJECTED);
        vet.setRejectionReason(reason);
        vet.setUpdatedAt(Instant.now());
        var saved = repository.save(vet);
        eventPublisher.publish("vet.rejected", "vet", saved.getId(), saved.getClinicId(),
                new VetRejectedPayload(saved.getId(), saved.getClinicId(), reason));
        return saved;
    }
}
