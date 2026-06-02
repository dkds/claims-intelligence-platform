package com.dkds.cip.claims.claim;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {
    List<Claim> findByClinicId(UUID clinicId);

    List<Claim> findByClinicIdAndStatus(UUID clinicId, ClaimStatus status);

    List<Claim> findByPetId(UUID petId);
}
