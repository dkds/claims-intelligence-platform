package com.dkds.cip.sessions.session.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VerifySessionRequest(@NotNull UUID verifiedBy) {
}
