package com.dkds.cip.enrollment.clinic;

import com.dkds.cip.enrollment.clinic.dto.ClinicResponse;
import com.dkds.cip.enrollment.clinic.dto.RegisterClinicRequest;
import com.dkds.cip.enrollment.clinic.dto.UpdateClinicRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clinics")
@RequiredArgsConstructor
public class ClinicController {

    private final ClinicService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClinicResponse register(@Valid @RequestBody RegisterClinicRequest req) {
        return ClinicResponse.from(service.register(req));
    }

    @GetMapping
    public List<ClinicResponse> listAll() {
        return service.listAll().stream().map(ClinicResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ClinicResponse getById(@PathVariable UUID id) {
        return ClinicResponse.from(service.getById(id));
    }

    @PutMapping("/{id}")
    public ClinicResponse update(@PathVariable UUID id, @RequestBody UpdateClinicRequest req) {
        return ClinicResponse.from(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id) {
        service.deactivate(id);
    }
}
