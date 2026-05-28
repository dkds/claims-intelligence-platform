package com.dkds.cip.sessions.session;

import com.dkds.cip.sessions.session.dto.LogSessionRequest;
import com.dkds.cip.sessions.session.dto.SessionResponse;
import com.dkds.cip.sessions.session.dto.VerifySessionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SessionController {

    private final SessionService service;

    @PostMapping("/clinics/{clinicId}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse log(@PathVariable UUID clinicId,
                               @Valid @RequestBody LogSessionRequest req) {
        return SessionResponse.from(service.log(clinicId, req));
    }

    @GetMapping("/clinics/{clinicId}/sessions")
    public List<SessionResponse> listByClinic(@PathVariable UUID clinicId) {
        return service.listByClinic(clinicId).stream().map(SessionResponse::from).toList();
    }

    @GetMapping("/sessions/{id}")
    public SessionResponse getById(@PathVariable UUID id) {
        return SessionResponse.from(service.getById(id));
    }

    @PostMapping("/sessions/{id}/verify")
    public SessionResponse verify(@PathVariable UUID id,
                                  @Valid @RequestBody VerifySessionRequest req) {
        return SessionResponse.from(service.verify(id, req.verifiedBy()));
    }

    @PostMapping("/sessions/{id}/cancel")
    public SessionResponse cancel(@PathVariable UUID id) {
        return SessionResponse.from(service.cancel(id));
    }
}
