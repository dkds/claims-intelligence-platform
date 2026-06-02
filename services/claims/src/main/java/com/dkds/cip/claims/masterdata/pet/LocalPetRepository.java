package com.dkds.cip.claims.masterdata.pet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LocalPetRepository extends JpaRepository<LocalPet, UUID> {
}
