package com.dkds.cip.sessions.masterdata.clinic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LocalClinicRepository extends JpaRepository<LocalClinic, UUID> {
}
