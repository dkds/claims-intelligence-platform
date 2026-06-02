package com.dkds.cip.claims.claim;

import com.dkds.cip.claims.claim.dto.ClaimResponse;
import com.dkds.cip.claims.claim.dto.SubmitManualClaimRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService service;

    @PostMapping("/clinics/{clinicId}/claims")
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimResponse submitManualClaim(@PathVariable UUID clinicId,
                                           @Valid @RequestBody SubmitManualClaimRequest req) {
        return ClaimResponse.from(service.submitManualClaim(clinicId, req));
    }

    @GetMapping("/claims/{claimId}")
    public ClaimResponse getById(@PathVariable UUID claimId) {
        return ClaimResponse.from(service.getById(claimId));
    }

    @GetMapping("/clinics/{clinicId}/claims")
    public List<ClaimResponse> listByClinic(@PathVariable UUID clinicId,
                                            @RequestParam(required = false) ClaimStatus status) {
        return service.listByClinic(clinicId, Optional.ofNullable(status))
                .stream().map(ClaimResponse::from).toList();
    }
}
