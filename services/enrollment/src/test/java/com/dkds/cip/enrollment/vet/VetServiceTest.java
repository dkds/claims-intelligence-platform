package com.dkds.cip.enrollment.vet;

import com.dkds.cip.enrollment.clinic.ClinicService;
import com.dkds.cip.enrollment.common.event.EnrollmentEventPublisher;
import com.dkds.cip.enrollment.common.exception.ResourceNotFoundException;
import com.dkds.cip.enrollment.vet.dto.RegisterVetRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VetServiceTest {

    @Mock
    VetRepository repository;
    @Mock
    ClinicService clinicService;
    @Mock
    EnrollmentEventPublisher eventPublisher;
    @InjectMocks
    VetService service;

    @Test
    void register_setsStatusPending() {
        var clinicId = UUID.randomUUID();
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var vet = service.register(clinicId, new RegisterVetRequest("Jane", "Doe", "j@vet.com", "LIC-001"));

        assertThat(vet.getStatus()).isEqualTo(VetStatus.PENDING);
        assertThat(vet.getClinicId()).isEqualTo(clinicId);
    }

    @Test
    void approve_pendingVet_setsApproved() {
        var vet = pendingVet();
        when(repository.findById(vet.getId())).thenReturn(Optional.of(vet));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.approve(vet.getId());

        assertThat(result.getStatus()).isEqualTo(VetStatus.APPROVED);
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void approve_alreadyApproved_throwsIllegalState() {
        var vet = pendingVet();
        vet.setStatus(VetStatus.APPROVED);
        when(repository.findById(vet.getId())).thenReturn(Optional.of(vet));

        assertThatThrownBy(() -> service.approve(vet.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APPROVED");
    }

    @Test
    void reject_pendingVet_setsRejectedWithReason() {
        var vet = pendingVet();
        when(repository.findById(vet.getId())).thenReturn(Optional.of(vet));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.reject(vet.getId(), "License expired");

        assertThat(result.getStatus()).isEqualTo(VetStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("License expired");
    }

    @Test
    void reject_alreadyRejected_throwsIllegalState() {
        var vet = pendingVet();
        vet.setStatus(VetStatus.REJECTED);
        when(repository.findById(vet.getId())).thenReturn(Optional.of(vet));

        assertThatThrownBy(() -> service.reject(vet.getId(), "reason"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REJECTED");
    }

    @Test
    void getById_notFound_throwsResourceNotFound() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Vet pendingVet() {
        var vet = new Vet();
        vet.setId(UUID.randomUUID());
        vet.setClinicId(UUID.randomUUID());
        vet.setFirstName("Jane");
        vet.setLastName("Doe");
        vet.setLicenseNumber("LIC-001");
        vet.setStatus(VetStatus.PENDING);
        vet.setRegisteredAt(Instant.now());
        return vet;
    }
}
