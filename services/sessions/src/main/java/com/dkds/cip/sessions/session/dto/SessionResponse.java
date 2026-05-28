package com.dkds.cip.sessions.session.dto;

import com.dkds.cip.sessions.session.Session;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        UUID clinicId,
        UUID petId,
        UUID vetId,
        String status,
        Instant loggedAt,
        Instant verifiedAt,
        UUID verifiedBy,
        List<SessionLineResponse> lines
) {
    public static SessionResponse from(Session session) {
        return new SessionResponse(
                session.getId(),
                session.getClinicId(),
                session.getPetId(),
                session.getVetId(),
                session.getStatus().name(),
                session.getLoggedAt(),
                session.getVerifiedAt(),
                session.getVerifiedBy(),
                session.getLines().stream().map(SessionLineResponse::from).toList()
        );
    }
}
