package com.dkds.cip.claims.masterdata.catalogue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LocalCatalogueItemRepository extends JpaRepository<LocalCatalogueItem, UUID> {
    Optional<LocalCatalogueItem> findByCode(String code);
}
