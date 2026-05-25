package com.dkds.cip.enrollment.owner;

import com.dkds.cip.enrollment.clinic.ClinicService;
import com.dkds.cip.enrollment.common.exception.ResourceNotFoundException;
import com.dkds.cip.enrollment.owner.dto.RegisterOwnerRequest;
import com.dkds.cip.enrollment.owner.dto.UpdateOwnerRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OwnerService {

    private final OwnerRepository repository;
    private final ClinicService clinicService;

    @Transactional
    public Owner register(UUID clinicId, RegisterOwnerRequest req) {
        clinicService.throwIfNotExists(clinicId);
        var owner = new Owner();
        owner.setClinicId(clinicId);
        owner.setFirstName(req.firstName());
        owner.setLastName(req.lastName());
        owner.setEmail(req.email());
        owner.setPhone(req.phone());
        owner.setCreatedAt(Instant.now());
        return repository.save(owner);
    }

    @Transactional(readOnly = true)
    public List<Owner> listByClinic(UUID clinicId) {
        clinicService.throwIfNotExists(clinicId);
        return repository.findByClinicId(clinicId);
    }

    @Transactional(readOnly = true)
    public Owner getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found: " + id));
    }

    @Transactional(readOnly = true)
    public void throwIfNotExists(UUID id) {
        getById(id);
    }

    @Transactional
    public Owner update(UUID id, UpdateOwnerRequest req) {
        var owner = getById(id);
        if (req.firstName() != null) owner.setFirstName(req.firstName());
        if (req.lastName() != null) owner.setLastName(req.lastName());
        if (req.email() != null) owner.setEmail(req.email());
        if (req.phone() != null) owner.setPhone(req.phone());
        return repository.save(owner);
    }
}
