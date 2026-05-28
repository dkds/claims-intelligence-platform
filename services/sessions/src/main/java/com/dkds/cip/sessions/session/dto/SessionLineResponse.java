package com.dkds.cip.sessions.session.dto;

import com.dkds.cip.sessions.session.SessionLine;

import java.util.UUID;

public record SessionLineResponse(
        UUID id,
        String procedureCode,
        int quantity,
        String notes
) {
    public static SessionLineResponse from(SessionLine line) {
        return new SessionLineResponse(
                line.getId(),
                line.getProcedureCode(),
                line.getQuantity(),
                line.getNotes()
        );
    }
}
