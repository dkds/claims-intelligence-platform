package com.dkds.cip.enrollment.vet;

import com.dkds.cip.enrollment.vet.dto.RegisterVetRequest;
import com.dkds.cip.enrollment.vet.dto.RejectVetRequest;
import com.dkds.cip.enrollment.vet.dto.UpdateVetRequest;
import com.dkds.cip.enrollment.vet.dto.VetResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class VetController {

    private final VetService service;

    @PostMapping("/clinics/{clinicId}/vets")
    @ResponseStatus(HttpStatus.CREATED)
    public VetResponse register(@PathVariable UUID clinicId,
                                @Valid @RequestBody RegisterVetRequest req) {
        return VetResponse.from(service.register(clinicId, req));
    }

    @GetMapping("/clinics/{clinicId}/vets")
    public List<VetResponse> listByClinic(@PathVariable UUID clinicId) {
        return service.listByClinic(clinicId).stream().map(VetResponse::from).toList();
    }

    @GetMapping("/vets/{id}")
    public VetResponse getById(@PathVariable UUID id) {
        return VetResponse.from(service.getById(id));
    }

    @PutMapping("/vets/{id}")
    public VetResponse update(@PathVariable UUID id, @RequestBody UpdateVetRequest req) {
        return VetResponse.from(service.update(id, req));
    }

    @PostMapping("/vets/{id}/approve")
    public VetResponse approve(@PathVariable UUID id) {
        return VetResponse.from(service.approve(id));
    }

    @PostMapping("/vets/{id}/reject")
    public VetResponse reject(@PathVariable UUID id,
                              @Valid @RequestBody RejectVetRequest req) {
        return VetResponse.from(service.reject(id, req.reason()));
    }
}
