package com.dkds.cip.enrollment.pet;

import com.dkds.cip.enrollment.pet.dto.EnrolPetRequest;
import com.dkds.cip.enrollment.pet.dto.PetResponse;
import com.dkds.cip.enrollment.pet.dto.UpdatePetRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PetController {

    private final PetService service;

    @PostMapping("/clinics/{clinicId}/pets")
    @ResponseStatus(HttpStatus.CREATED)
    public PetResponse enrol(@PathVariable UUID clinicId,
                             @Valid @RequestBody EnrolPetRequest req) {
        return PetResponse.from(service.enrol(clinicId, req));
    }

    @GetMapping("/clinics/{clinicId}/pets")
    public List<PetResponse> listByClinic(@PathVariable UUID clinicId) {
        return service.listByClinic(clinicId).stream().map(PetResponse::from).toList();
    }

    @GetMapping("/pets/{id}")
    public PetResponse getById(@PathVariable UUID id) {
        return PetResponse.from(service.getById(id));
    }

    @PutMapping("/pets/{id}")
    public PetResponse update(@PathVariable UUID id, @RequestBody UpdatePetRequest req) {
        return PetResponse.from(service.update(id, req));
    }
}
