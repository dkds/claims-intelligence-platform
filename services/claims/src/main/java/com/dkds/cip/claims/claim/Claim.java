package com.dkds.cip.claims.claim;

import com.dkds.cip.claims.adjudication.AdjudicationDecision;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "claim")
@Getter
@Setter
public class Claim {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID clinicId;

    @Column(nullable = false)
    private UUID petId;

    @Column(nullable = false)
    private UUID policyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimOrigin origin;

    private UUID sourceSessionId;

    @Column(nullable = false)
    private UUID submittedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status = ClaimStatus.ASSEMBLED;

    @Enumerated(EnumType.STRING)
    private AdjudicationDecision adjudicationDecision;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRequested;

    @Column(precision = 12, scale = 2)
    private BigDecimal approvedAmount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ClaimLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("occurredAt ASC")
    private List<ClaimTransition> transitions = new ArrayList<>();
}
