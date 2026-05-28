package com.dkds.cip.sessions.masterdata.pet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LocalPetRepository extends JpaRepository<LocalPet, UUID> {
    List<LocalPet> findByClinicId(UUID clinicId);
}
