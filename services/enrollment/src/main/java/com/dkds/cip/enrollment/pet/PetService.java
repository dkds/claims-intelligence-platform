package com.dkds.cip.enrollment.pet;

import com.dkds.cip.enrollment.clinic.ClinicService;
import com.dkds.cip.enrollment.common.exception.ResourceNotFoundException;
import com.dkds.cip.enrollment.owner.OwnerService;
import com.dkds.cip.enrollment.pet.dto.EnrolPetRequest;
import com.dkds.cip.enrollment.pet.dto.UpdatePetRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository repository;
    private final ClinicService clinicService;
    private final OwnerService ownerService;

    @Transactional
    public Pet enrol(UUID clinicId, EnrolPetRequest req) {
        clinicService.throwIfNotExists(clinicId);
        ownerService.throwIfNotExists(req.ownerId());
        var pet = new Pet();
        pet.setClinicId(clinicId);
        pet.setOwnerId(req.ownerId());
        pet.setName(req.name());
        pet.setSpecies(req.species());
        pet.setBreed(req.breed());
        pet.setDateOfBirth(req.dateOfBirth());
        pet.setMicrochipNumber(req.microchipNumber());
        pet.setEnrolledAt(Instant.now());
        return repository.save(pet);
    }

    @Transactional(readOnly = true)
    public List<Pet> listByClinic(UUID clinicId) {
        clinicService.throwIfNotExists(clinicId);
        return repository.findByClinicId(clinicId);
    }

    @Transactional(readOnly = true)
    public Pet getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found: " + id));
    }

    @Transactional(readOnly = true)
    public void throwIfNotExists(UUID id) {
        getById(id);
    }

    @Transactional
    public Pet update(UUID id, UpdatePetRequest req) {
        var pet = getById(id);
        if (req.name() != null) pet.setName(req.name());
        if (req.species() != null) pet.setSpecies(req.species());
        if (req.breed() != null) pet.setBreed(req.breed());
        if (req.dateOfBirth() != null) pet.setDateOfBirth(req.dateOfBirth());
        if (req.microchipNumber() != null) pet.setMicrochipNumber(req.microchipNumber());
        return repository.save(pet);
    }
}
