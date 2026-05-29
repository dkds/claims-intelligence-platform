package com.dkds.cip.enrollment.catalogue;

import com.dkds.cip.enrollment.catalogue.dto.CreateCatalogueItemRequest;
import com.dkds.cip.enrollment.catalogue.dto.UpdateCatalogueItemRequest;
import com.dkds.cip.enrollment.common.event.EnrollmentEventPublisher;
import com.dkds.cip.enrollment.common.event.payload.CatalogueUpdatedPayload;
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
    private final EnrollmentEventPublisher eventPublisher;

    @Transactional
    public CatalogueItem create(CreateCatalogueItemRequest req) {
        var item = new CatalogueItem();
        item.setCode(req.code().toUpperCase());
        item.setDescription(req.description());
        item.setReimbursementRate(req.reimbursementRate());
        item.setCreatedAt(Instant.now());
        var saved = repository.save(item);
        eventPublisher.publish("catalogue.updated", "catalogue-item", saved.getId(), null,
                new CatalogueUpdatedPayload(saved.getId(), saved.getCode(), saved.getDescription(),
                        saved.getReimbursementRate(), saved.isActive()));
        return saved;
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
        var saved = repository.save(item);
        eventPublisher.publish("catalogue.updated", "catalogue-item", saved.getId(), null,
                new CatalogueUpdatedPayload(saved.getId(), saved.getCode(), saved.getDescription(),
                        saved.getReimbursementRate(), saved.isActive()));
        return saved;
    }

    @Transactional
    public void deactivate(UUID id) {
        var item = getById(id);
        item.setActive(false);
        item.setUpdatedAt(Instant.now());
        var saved = repository.save(item);
        eventPublisher.publish("catalogue.updated", "catalogue-item", saved.getId(), null,
                new CatalogueUpdatedPayload(saved.getId(), saved.getCode(), saved.getDescription(),
                        saved.getReimbursementRate(), saved.isActive()));
    }
}
