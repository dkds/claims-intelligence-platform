package com.dkds.cip.claims.masterdata.clinic;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "local_clinic")
@Getter
@Setter
public class LocalClinic {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocalClinicStatus status = LocalClinicStatus.ACTIVE;

    private Instant updatedAt;
}
