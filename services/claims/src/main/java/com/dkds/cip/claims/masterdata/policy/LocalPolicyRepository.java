package com.dkds.cip.claims.masterdata.policy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LocalPolicyRepository extends JpaRepository<LocalPolicy, UUID> {
    List<LocalPolicy> findByPetIdAndStatus(UUID petId, LocalPolicyStatus status);
}
