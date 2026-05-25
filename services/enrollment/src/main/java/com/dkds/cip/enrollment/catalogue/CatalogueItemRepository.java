package com.dkds.cip.enrollment.catalogue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CatalogueItemRepository extends JpaRepository<CatalogueItem, UUID> {
    List<CatalogueItem> findByActiveTrue();
}
