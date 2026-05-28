package com.dkds.cip.sessions.session;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "session")
@Getter
@Setter
public class Session {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID clinicId;

    @Column(nullable = false)
    private UUID petId;

    @Column(nullable = false)
    private UUID vetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.LOGGED;

    @Column(nullable = false, updatable = false)
    private Instant loggedAt;

    private Instant verifiedAt;

    private UUID verifiedBy;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<SessionLine> lines = new ArrayList<>();
}
