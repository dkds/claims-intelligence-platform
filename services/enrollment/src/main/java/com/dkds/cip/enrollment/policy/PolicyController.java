package com.dkds.cip.enrollment.policy;

import com.dkds.cip.enrollment.policy.dto.AssignPolicyRequest;
import com.dkds.cip.enrollment.policy.dto.PolicyResponse;
import com.dkds.cip.enrollment.policy.dto.UpdatePolicyRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService service;

    @PostMapping("/pets/{petId}/policies")
    @ResponseStatus(HttpStatus.CREATED)
    public PolicyResponse assign(@PathVariable UUID petId,
                                 @Valid @RequestBody AssignPolicyRequest req) {
        return PolicyResponse.from(service.assign(petId, req));
    }

    @GetMapping("/pets/{petId}/policies")
    public List<PolicyResponse> listByPet(@PathVariable UUID petId) {
        return service.listByPet(petId).stream().map(PolicyResponse::from).toList();
    }

    @GetMapping("/policies/{id}")
    public PolicyResponse getById(@PathVariable UUID id) {
        return PolicyResponse.from(service.getById(id));
    }

    @PutMapping("/policies/{id}")
    public PolicyResponse update(@PathVariable UUID id, @RequestBody UpdatePolicyRequest req) {
        return PolicyResponse.from(service.update(id, req));
    }
}
