package com.dkds.cip.enrollment.clinic;

import com.dkds.cip.enrollment.clinic.dto.RegisterClinicRequest;
import com.dkds.cip.enrollment.clinic.dto.UpdateClinicRequest;
import com.dkds.cip.enrollment.common.event.EnrollmentEventPublisher;
import com.dkds.cip.enrollment.common.event.payload.ClinicRegisteredPayload;
import com.dkds.cip.enrollment.common.event.payload.ClinicUpdatedPayload;
import com.dkds.cip.enrollment.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicService {

    private final ClinicRepository repository;
    private final EnrollmentEventPublisher eventPublisher;

    @Transactional
    public Clinic register(RegisterClinicRequest req) {
        var clinic = new Clinic();
        clinic.setName(req.name());
        clinic.setAddressLine1(req.addressLine1());
        clinic.setAddressLine2(req.addressLine2());
        clinic.setCity(req.city());
        clinic.setPostcode(req.postcode());
        clinic.setCountryCode(req.countryCode());
        clinic.setContactEmail(req.contactEmail());
        clinic.setContactPhone(req.contactPhone());
        clinic.setRegisteredAt(Instant.now());
        var saved = repository.save(clinic);
        eventPublisher.publish("clinic.updated", "clinic", saved.getId(), saved.getId(),
                new ClinicRegisteredPayload(saved.getId(), saved.getName(), saved.getStatus().name()));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Clinic> listAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Clinic getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic not found: " + id));
    }

    @Transactional(readOnly = true)
    public void throwIfNotExists(UUID id) {
        getById(id);
    }

    @Transactional
    public Clinic update(UUID id, UpdateClinicRequest req) {
        var clinic = getById(id);
        if (req.name() != null) clinic.setName(req.name());
        if (req.addressLine1() != null) clinic.setAddressLine1(req.addressLine1());
        if (req.addressLine2() != null) clinic.setAddressLine2(req.addressLine2());
        if (req.city() != null) clinic.setCity(req.city());
        if (req.postcode() != null) clinic.setPostcode(req.postcode());
        if (req.countryCode() != null) clinic.setCountryCode(req.countryCode());
        if (req.contactEmail() != null) clinic.setContactEmail(req.contactEmail());
        if (req.contactPhone() != null) clinic.setContactPhone(req.contactPhone());
        clinic.setUpdatedAt(Instant.now());
        var saved = repository.save(clinic);
        eventPublisher.publish("clinic.updated", "clinic", saved.getId(), saved.getId(),
                new ClinicUpdatedPayload(saved.getId(), saved.getName(), saved.getStatus().name()));
        return saved;
    }

    @Transactional
    public void deactivate(UUID id) {
        var clinic = getById(id);
        clinic.setStatus(ClinicStatus.SUSPENDED);
        clinic.setUpdatedAt(Instant.now());
        var saved = repository.save(clinic);
        eventPublisher.publish("clinic.updated", "clinic", saved.getId(), saved.getId(),
                new ClinicUpdatedPayload(saved.getId(), saved.getName(), saved.getStatus().name()));
    }
}
