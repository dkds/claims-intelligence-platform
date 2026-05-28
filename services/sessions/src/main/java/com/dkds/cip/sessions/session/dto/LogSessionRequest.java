package com.dkds.cip.sessions.session.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record LogSessionRequest(
        @NotNull UUID vetId,
        @NotNull UUID petId,
        @NotNull @Size(min = 1) List<@Valid SessionLineRequest> lines
) {
}
