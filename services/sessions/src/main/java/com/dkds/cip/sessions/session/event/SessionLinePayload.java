package com.dkds.cip.sessions.session.event;

public record SessionLinePayload(String procedureCode, int quantity, String notes) {
}
