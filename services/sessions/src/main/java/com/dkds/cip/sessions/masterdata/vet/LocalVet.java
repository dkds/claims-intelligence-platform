package com.dkds.cip.sessions.masterdata.vet;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "local_vet")
@Getter
@Setter
public class LocalVet {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID clinicId;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocalVetStatus status = LocalVetStatus.PENDING;

    private Instant updatedAt;
}
