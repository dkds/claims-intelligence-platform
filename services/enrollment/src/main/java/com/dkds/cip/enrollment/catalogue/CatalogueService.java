package com.dkds.cip.enrollment.catalogue;

import com.dkds.cip.enrollment.catalogue.dto.CreateCatalogueItemRequest;
import com.dkds.cip.enrollment.catalogue.dto.UpdateCatalogueItemRequest;
import com.dkds.cip.enrollment.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CatalogueService {

    private final CatalogueItemRepository repository;

    @Transactional
    public CatalogueItem create(CreateCatalogueItemRequest req) {
        var item = new CatalogueItem();
        item.setCode(req.code().toUpperCase());
        item.setDescription(req.description());
        item.setReimbursementRate(req.reimbursementRate());
        item.setCreatedAt(Instant.now());
        return repository.save(item);
    }

    @Transactional(readOnly = true)
    public List<CatalogueItem> listActive() {
        return repository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public CatalogueItem getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catalogue item not found: " + id));
    }

    @Transactional
    public CatalogueItem update(UUID id, UpdateCatalogueItemRequest req) {
        var item = getById(id);
        if (req.description() != null) item.setDescription(req.description());
        if (req.reimbursementRate() != null) item.setReimbursementRate(req.reimbursementRate());
        item.setUpdatedAt(Instant.now());
        return repository.save(item);
    }

    @Transactional
    public void deactivate(UUID id) {
        var item = getById(id);
        item.setActive(false);
        item.setUpdatedAt(Instant.now());
        repository.save(item);
    }
}
