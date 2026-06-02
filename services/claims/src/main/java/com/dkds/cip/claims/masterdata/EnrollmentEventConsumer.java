package com.dkds.cip.claims.masterdata;

import com.dkds.cip.claims.masterdata.catalogue.CatalogueUpdatedPayload;
import com.dkds.cip.claims.masterdata.catalogue.LocalCatalogueItem;
import com.dkds.cip.claims.masterdata.catalogue.LocalCatalogueItemRepository;
import com.dkds.cip.claims.masterdata.clinic.ClinicUpdatedPayload;
import com.dkds.cip.claims.masterdata.clinic.LocalClinic;
import com.dkds.cip.claims.masterdata.clinic.LocalClinicRepository;
import com.dkds.cip.claims.masterdata.clinic.LocalClinicStatus;
import com.dkds.cip.claims.masterdata.pet.LocalPet;
import com.dkds.cip.claims.masterdata.pet.LocalPetRepository;
import com.dkds.cip.claims.masterdata.pet.LocalPetStatus;
import com.dkds.cip.claims.masterdata.pet.PetEnrolledPayload;
import com.dkds.cip.claims.masterdata.policy.CoverageType;
import com.dkds.cip.claims.masterdata.policy.LocalPolicy;
import com.dkds.cip.claims.masterdata.policy.LocalPolicyRepository;
import com.dkds.cip.claims.masterdata.policy.LocalPolicyStatus;
import com.dkds.cip.claims.masterdata.policy.PolicyAssignedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollmentEventConsumer {

    private final JsonMapper jsonMapper;
    private final LocalClinicRepository clinicRepo;
    private final LocalPetRepository petRepo;
    private final LocalPolicyRepository policyRepo;
    private final LocalCatalogueItemRepository catalogueRepo;

    @KafkaListener(topics = "${cip.kafka.topics.enrollment}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consume(String message) {
        try {
            var envelope = jsonMapper.readTree(message);
            var eventType = envelope.get("eventType").asString();
            var payload = envelope.get("payload");
            switch (eventType) {
                case "clinic.updated" -> applyClinicUpdated(payload);
                case "pet.enrolled" -> applyPetEnrolled(payload);
                case "policy.assigned" -> applyPolicyAssigned(payload);
                case "catalogue.updated" -> applyCatalogueUpdated(payload);
                case "vet.registered", "vet.approved", "vet.rejected" ->
                        log.debug("Ignoring vet event in claims service: {}", eventType);
                default -> log.debug("Ignoring unknown enrollment event: {}", eventType);
            }
        } catch (Exception e) {
            // Phase 3: log and advance offset; Phase 7 will add DLT routing
            log.error("Failed to process enrollment event: {}", e.getMessage(), e);
        }
    }

    private void applyClinicUpdated(JsonNode payload) {
        var p = jsonMapper.treeToValue(payload, ClinicUpdatedPayload.class);
        var clinic = clinicRepo.findById(p.clinicId()).orElseGet(() -> {
            var c = new LocalClinic();
            c.setId(p.clinicId());
            return c;
        });
        clinic.setName(p.name());
        clinic.setStatus(LocalClinicStatus.valueOf(p.status()));
        clinic.setUpdatedAt(Instant.now());
        clinicRepo.save(clinic);
        log.debug("Upserted local_clinic {}", p.clinicId());
    }

    private void applyPetEnrolled(JsonNode payload) {
        var p = jsonMapper.treeToValue(payload, PetEnrolledPayload.class);
        if (!clinicRepo.existsById(p.clinicId())) {
            log.warn("Skipping pet.enrolled for pet {} — clinic {} not in local store",
                    p.petId(), p.clinicId());
            return;
        }
        var pet = petRepo.findById(p.petId()).orElseGet(() -> {
            var v = new LocalPet();
            v.setId(p.petId());
            return v;
        });
        pet.setClinicId(p.clinicId());
        pet.setOwnerId(p.ownerId());
        pet.setName(p.name());
        pet.setStatus(LocalPetStatus.valueOf(p.status()));
        pet.setUpdatedAt(Instant.now());
        petRepo.save(pet);
        log.debug("Upserted local_pet {}", p.petId());
    }

    private void applyPolicyAssigned(JsonNode payload) {
        var p = jsonMapper.treeToValue(payload, PolicyAssignedPayload.class);
        if (!petRepo.existsById(p.petId())) {
            log.warn("Skipping policy.assigned for policy {} — pet {} not in local store",
                    p.policyId(), p.petId());
            return;
        }
        var policy = policyRepo.findById(p.policyId()).orElseGet(() -> {
            var v = new LocalPolicy();
            v.setId(p.policyId());
            return v;
        });
        policy.setPetId(p.petId());
        policy.setCoverageType(CoverageType.valueOf(p.coverageType()));
        policy.setStartDate(p.startDate());
        policy.setEndDate(p.endDate());
        policy.setStatus(LocalPolicyStatus.valueOf(p.status()));
        policy.setUpdatedAt(Instant.now());
        policyRepo.save(policy);
        log.debug("Upserted local_policy {}", p.policyId());
    }

    private void applyCatalogueUpdated(JsonNode payload) {
        var p = jsonMapper.treeToValue(payload, CatalogueUpdatedPayload.class);
        var item = catalogueRepo.findById(p.itemId()).orElseGet(() -> {
            var v = new LocalCatalogueItem();
            v.setId(p.itemId());
            return v;
        });
        item.setCode(p.code());
        item.setDescription(p.description());
        item.setReimbursementRate(p.reimbursementRate());
        item.setActive(p.active());
        item.setUpdatedAt(Instant.now());
        catalogueRepo.save(item);
        log.debug("Upserted local_catalogue_item {} ({})", p.itemId(), p.code());
    }
}
