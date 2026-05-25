package com.dkds.cip.enrollment.vet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VetRepository extends JpaRepository<Vet, UUID> {
    List<Vet> findByClinicId(UUID clinicId);
}
