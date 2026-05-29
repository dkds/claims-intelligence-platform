package com.dkds.cip.sessions.session;

import com.dkds.cip.sessions.common.exception.ResourceNotFoundException;
import com.dkds.cip.sessions.masterdata.catalogue.LocalCatalogueItem;
import com.dkds.cip.sessions.masterdata.catalogue.LocalCatalogueItemRepository;
import com.dkds.cip.sessions.masterdata.pet.LocalPet;
import com.dkds.cip.sessions.masterdata.pet.LocalPetRepository;
import com.dkds.cip.sessions.masterdata.pet.LocalPetStatus;
import com.dkds.cip.sessions.masterdata.policy.LocalPolicy;
import com.dkds.cip.sessions.masterdata.policy.LocalPolicyRepository;
import com.dkds.cip.sessions.masterdata.policy.LocalPolicyStatus;
import com.dkds.cip.sessions.masterdata.vet.LocalVet;
import com.dkds.cip.sessions.masterdata.vet.LocalVetRepository;
import com.dkds.cip.sessions.masterdata.vet.LocalVetStatus;
import com.dkds.cip.sessions.session.dto.LogSessionRequest;
import com.dkds.cip.sessions.session.dto.SessionLineRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    SessionRepository sessionRepository;
    @Mock
    LocalVetRepository vetRepository;
    @Mock
    LocalPetRepository petRepository;
    @Mock
    LocalPolicyRepository policyRepository;
    @Mock
    LocalCatalogueItemRepository catalogueRepository;
    @Mock
    SessionEventPublisher eventPublisher;

    @InjectMocks
    SessionService service;

    @Test
    void logSession_pendingVet_throwsIllegalState() {
        var clinicId = UUID.randomUUID();
        var vet = vet(clinicId, LocalVetStatus.PENDING);
        when(vetRepository.findById(vet.getId())).thenReturn(Optional.of(vet));

        var req = new LogSessionRequest(vet.getId(), UUID.randomUUID(), List.of(line("CONSULT")));

        assertThatThrownBy(() -> service.log(clinicId, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not APPROVED");
    }

    @Test
    void logSession_vetWrongClinic_throwsIllegalState() {
        var clinicId = UUID.randomUUID();
        var vet = vet(UUID.randomUUID(), LocalVetStatus.APPROVED);
        when(vetRepository.findById(vet.getId())).thenReturn(Optional.of(vet));

        var req = new LogSessionRequest(vet.getId(), UUID.randomUUID(), List.of(line("CONSULT")));

        assertThatThrownBy(() -> service.log(clinicId, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not belong to clinic");
    }

    @Test
    void logSession_inactivePet_throwsIllegalState() {
        var clinicId = UUID.randomUUID();
        var vet = vet(clinicId, LocalVetStatus.APPROVED);
        var pet = pet(clinicId, LocalPetStatus.INACTIVE);
        when(vetRepository.findById(vet.getId())).thenReturn(Optional.of(vet));
        when(petRepository.findById(pet.getId())).thenReturn(Optional.of(pet));

        var req = new LogSessionRequest(vet.getId(), pet.getId(), List.of(line("CONSULT")));

        assertThatThrownBy(() -> service.log(clinicId, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not ACTIVE");
    }

    @Test
    void logSession_noActivePolicy_throwsIllegalState() {
        var clinicId = UUID.randomUUID();
        var vet = vet(clinicId, LocalVetStatus.APPROVED);
        var pet = pet(clinicId, LocalPetStatus.ACTIVE);
        when(vetRepository.findById(vet.getId())).thenReturn(Optional.of(vet));
        when(petRepository.findById(pet.getId())).thenReturn(Optional.of(pet));
        when(policyRepository.findByPetIdAndStatus(pet.getId(), LocalPolicyStatus.ACTIVE))
                .thenReturn(List.of());

        var req = new LogSessionRequest(vet.getId(), pet.getId(), List.of(line("CONSULT")));

        assertThatThrownBy(() -> service.log(clinicId, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no ACTIVE policy");
    }

    @Test
    void logSession_unknownProcedureCode_throwsIllegalState() {
        var clinicId = UUID.randomUUID();
        var vet = vet(clinicId, LocalVetStatus.APPROVED);
        var pet = pet(clinicId, LocalPetStatus.ACTIVE);
        when(vetRepository.findById(vet.getId())).thenReturn(Optional.of(vet));
        when(petRepository.findById(pet.getId())).thenReturn(Optional.of(pet));
        when(policyRepository.findByPetIdAndStatus(pet.getId(), LocalPolicyStatus.ACTIVE))
                .thenReturn(List.of(new LocalPolicy()));
        when(catalogueRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        var req = new LogSessionRequest(vet.getId(), pet.getId(), List.of(line("UNKNOWN")));

        assertThatThrownBy(() -> service.log(clinicId, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found or inactive");
    }

    @Test
    void logSession_validData_savesSession() {
        var clinicId = UUID.randomUUID();
        var vet = vet(clinicId, LocalVetStatus.APPROVED);
        var pet = pet(clinicId, LocalPetStatus.ACTIVE);
        var catalogueItem = catalogueItem("CONSULT", true);
        when(vetRepository.findById(vet.getId())).thenReturn(Optional.of(vet));
        when(petRepository.findById(pet.getId())).thenReturn(Optional.of(pet));
        when(policyRepository.findByPetIdAndStatus(pet.getId(), LocalPolicyStatus.ACTIVE))
                .thenReturn(List.of(new LocalPolicy()));
        when(catalogueRepository.findByCode("CONSULT")).thenReturn(Optional.of(catalogueItem));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new LogSessionRequest(vet.getId(), pet.getId(), List.of(line("CONSULT")));
        var result = service.log(clinicId, req);

        assertThat(result.getStatus()).isEqualTo(SessionStatus.LOGGED);
        assertThat(result.getClinicId()).isEqualTo(clinicId);
        assertThat(result.getLines()).hasSize(1);
    }

    @Test
    void verifySession_loggedSession_setsVerified() {
        var session = loggedSession();
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.verify(session.getId(), UUID.randomUUID());

        assertThat(result.getStatus()).isEqualTo(SessionStatus.VERIFIED);
        assertThat(result.getVerifiedAt()).isNotNull();
    }

    @Test
    void verifySession_alreadyVerified_throwsIllegalState() {
        var session = loggedSession();
        session.setStatus(SessionStatus.VERIFIED);
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.verify(session.getId(), UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be LOGGED");
    }

    @Test
    void cancelSession_loggedSession_setsCancelled() {
        var session = loggedSession();
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.cancel(session.getId());

        assertThat(result.getStatus()).isEqualTo(SessionStatus.CANCELLED);
    }

    @Test
    void cancelSession_verifiedSession_throwsIllegalState() {
        var session = loggedSession();
        session.setStatus(SessionStatus.VERIFIED);
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.cancel(session.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel a VERIFIED session");
    }

    @Test
    void getById_notFound_throwsResourceNotFoundException() {
        var id = UUID.randomUUID();
        when(sessionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private LocalVet vet(UUID clinicId, LocalVetStatus status) {
        var v = new LocalVet();
        v.setId(UUID.randomUUID());
        v.setClinicId(clinicId);
        v.setFirstName("Jane");
        v.setLastName("Doe");
        v.setStatus(status);
        return v;
    }

    private LocalPet pet(UUID clinicId, LocalPetStatus status) {
        var p = new LocalPet();
        p.setId(UUID.randomUUID());
        p.setClinicId(clinicId);
        p.setOwnerId(UUID.randomUUID());
        p.setName("Rex");
        p.setStatus(status);
        return p;
    }

    private LocalCatalogueItem catalogueItem(String code, boolean active) {
        var item = new LocalCatalogueItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setDescription("desc");
        item.setActive(active);
        return item;
    }

    private SessionLineRequest line(String code) {
        return new SessionLineRequest(code, 1, null);
    }

    private Session loggedSession() {
        var s = new Session();
        s.setId(UUID.randomUUID());
        s.setStatus(SessionStatus.LOGGED);
        return s;
    }
}
