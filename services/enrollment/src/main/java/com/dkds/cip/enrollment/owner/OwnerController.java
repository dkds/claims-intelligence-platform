package com.dkds.cip.enrollment.owner;

import com.dkds.cip.enrollment.owner.dto.OwnerResponse;
import com.dkds.cip.enrollment.owner.dto.RegisterOwnerRequest;
import com.dkds.cip.enrollment.owner.dto.UpdateOwnerRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService service;

    @PostMapping("/clinics/{clinicId}/owners")
    @ResponseStatus(HttpStatus.CREATED)
    public OwnerResponse register(@PathVariable UUID clinicId,
                                  @Valid @RequestBody RegisterOwnerRequest req) {
        return OwnerResponse.from(service.register(clinicId, req));
    }

    @GetMapping("/clinics/{clinicId}/owners")
    public List<OwnerResponse> listByClinic(@PathVariable UUID clinicId) {
        return service.listByClinic(clinicId).stream().map(OwnerResponse::from).toList();
    }

    @GetMapping("/owners/{id}")
    public OwnerResponse getById(@PathVariable UUID id) {
        return OwnerResponse.from(service.getById(id));
    }

    @PutMapping("/owners/{id}")
    public OwnerResponse update(@PathVariable UUID id, @RequestBody UpdateOwnerRequest req) {
        return OwnerResponse.from(service.update(id, req));
    }
}
