package com.dkds.cip.enrollment.owner;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OwnerRepository extends JpaRepository<Owner, UUID> {
    List<Owner> findByClinicId(UUID clinicId);
}
