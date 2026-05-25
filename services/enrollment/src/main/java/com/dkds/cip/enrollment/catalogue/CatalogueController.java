package com.dkds.cip.enrollment.catalogue;

import com.dkds.cip.enrollment.catalogue.dto.CatalogueItemResponse;
import com.dkds.cip.enrollment.catalogue.dto.CreateCatalogueItemRequest;
import com.dkds.cip.enrollment.catalogue.dto.UpdateCatalogueItemRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/catalogue")
@RequiredArgsConstructor
public class CatalogueController {

    private final CatalogueService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogueItemResponse create(@Valid @RequestBody CreateCatalogueItemRequest req) {
        return CatalogueItemResponse.from(service.create(req));
    }

    @GetMapping
    public List<CatalogueItemResponse> listActive() {
        return service.listActive().stream().map(CatalogueItemResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CatalogueItemResponse getById(@PathVariable UUID id) {
        return CatalogueItemResponse.from(service.getById(id));
    }

    @PutMapping("/{id}")
    public CatalogueItemResponse update(@PathVariable UUID id,
                                        @RequestBody UpdateCatalogueItemRequest req) {
        return CatalogueItemResponse.from(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id) {
        service.deactivate(id);
    }
}
