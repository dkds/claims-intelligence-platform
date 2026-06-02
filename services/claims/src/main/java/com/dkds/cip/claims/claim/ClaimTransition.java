package com.dkds.cip.claims.claim;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "claim_transition")
@Getter
@Setter
public class ClaimTransition {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Enumerated(EnumType.STRING)
    private ClaimStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus toStatus;

    private String actor;

    private String reason;

    @Column(nullable = false)
    private Instant occurredAt;
}
