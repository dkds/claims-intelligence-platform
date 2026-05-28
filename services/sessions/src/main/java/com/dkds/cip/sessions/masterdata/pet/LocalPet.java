package com.dkds.cip.sessions.masterdata.pet;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "local_pet")
@Getter
@Setter
public class LocalPet {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID clinicId;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocalPetStatus status = LocalPetStatus.ACTIVE;

    private Instant updatedAt;
}
