package com.dkds.cip.sessions.masterdata.vet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LocalVetRepository extends JpaRepository<LocalVet, UUID> {
    List<LocalVet> findByClinicId(UUID clinicId);
}
