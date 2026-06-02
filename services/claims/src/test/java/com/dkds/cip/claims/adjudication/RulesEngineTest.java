package com.dkds.cip.claims.adjudication;

import com.dkds.cip.claims.claim.ClaimLine;
import com.dkds.cip.claims.masterdata.catalogue.LocalCatalogueItem;
import com.dkds.cip.claims.masterdata.policy.CoverageType;
import com.dkds.cip.claims.masterdata.policy.LocalPolicy;
import com.dkds.cip.claims.masterdata.policy.LocalPolicyStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RulesEngineTest {

    private final RulesEngine engine = new RulesEngine();

    @Test
    void adjudicate_standardPolicy_approvesAt80Percent() {
        var lines = List.of(line("CONSULT", 1, "100.00"));
        var policy = activePolicy(CoverageType.STANDARD);
        var catalogue = Map.of("CONSULT", catalogueItem("CONSULT", "100.00", true));

        var result = engine.adjudicate(lines, Optional.of(policy), catalogue);

        assertThat(result.decision()).isEqualTo(AdjudicationDecision.PARTIALLY_APPROVED);
        assertThat(result.totalApproved()).isEqualByComparingTo("80.00");
        assertThat(lines.get(0).getApprovedAmount()).isEqualByComparingTo("80.00");
    }

    @Test
    void adjudicate_premiumPolicy_approvesAt100Percent() {
        var lines = List.of(line("CONSULT", 1, "100.00"));
        var policy = activePolicy(CoverageType.PREMIUM);
        var catalogue = Map.of("CONSULT", catalogueItem("CONSULT", "100.00", true));

        var result = engine.adjudicate(lines, Optional.of(policy), catalogue);

        assertThat(result.decision()).isEqualTo(AdjudicationDecision.APPROVED);
        assertThat(result.totalApproved()).isEqualByComparingTo("100.00");
    }

    @Test
    void adjudicate_basicPolicy_approvesAt60Percent() {
        var lines = List.of(line("CONSULT", 1, "100.00"));
        var policy = activePolicy(CoverageType.BASIC);
        var catalogue = Map.of("CONSULT", catalogueItem("CONSULT", "100.00", true));

        var result = engine.adjudicate(lines, Optional.of(policy), catalogue);

        assertThat(result.decision()).isEqualTo(AdjudicationDecision.PARTIALLY_APPROVED);
        assertThat(result.totalApproved()).isEqualByComparingTo("60.00");
    }

    @Test
    void adjudicate_requestedLessThanCatalogueRate_capsAtRequested() {
        var lines = List.of(line("CONSULT", 1, "50.00"));
        var policy = activePolicy(CoverageType.PREMIUM);
        var catalogue = Map.of("CONSULT", catalogueItem("CONSULT", "100.00", true));

        var result = engine.adjudicate(lines, Optional.of(policy), catalogue);

        assertThat(result.decision()).isEqualTo(AdjudicationDecision.APPROVED);
        assertThat(result.totalApproved()).isEqualByComparingTo("50.00");
    }

    @Test
    void adjudicate_unknownProcedureCode_rejects() {
        var lines = List.of(line("UNKNOWN", 1, "100.00"));
        var policy = activePolicy(CoverageType.STANDARD);

        var result = engine.adjudicate(lines, Optional.of(policy), Map.of());

        assertThat(result.decision()).isEqualTo(AdjudicationDecision.REJECTED);
        assertThat(result.totalApproved()).isEqualByComparingTo("0.00");
        assertThat(result.reasons()).anyMatch(r -> r.contains("UNKNOWN"));
    }

    @Test
    void adjudicate_inactiveCatalogueItem_rejects() {
        var lines = List.of(line("CONSULT", 1, "100.00"));
        var policy = activePolicy(CoverageType.PREMIUM);
        var catalogue = Map.of("CONSULT", catalogueItem("CONSULT", "100.00", false));

        var result = engine.adjudicate(lines, Optional.of(policy), catalogue);

        assertThat(result.decision()).isEqualTo(AdjudicationDecision.REJECTED);
    }

    @Test
    void adjudicate_noPolicy_rejects() {
        var lines = List.of(line("CONSULT", 1, "100.00"));

        var result = engine.adjudicate(lines, Optional.empty(), Map.of());

        assertThat(result.decision()).isEqualTo(AdjudicationDecision.REJECTED);
        assertThat(result.reasons()).anyMatch(r -> r.contains("No active policy"));
    }

    @Test
    void adjudicate_expiredPolicy_rejects() {
        var lines = List.of(line("CONSULT", 1, "100.00"));
        var policy = activePolicy(CoverageType.STANDARD);
        policy.setStatus(LocalPolicyStatus.EXPIRED);

        var result = engine.adjudicate(lines, Optional.of(policy), Map.of());

        assertThat(result.decision()).isEqualTo(AdjudicationDecision.REJECTED);
    }

    @Test
    void adjudicate_mixedLines_partiallyApproves() {
        var lines = List.of(
                line("CONSULT", 1, "100.00"),
                line("UNKNOWN", 1, "50.00")
        );
        var policy = activePolicy(CoverageType.PREMIUM);
        var catalogue = Map.of("CONSULT", catalogueItem("CONSULT", "100.00", true));

        var result = engine.adjudicate(lines, Optional.of(policy), catalogue);

        assertThat(result.decision()).isEqualTo(AdjudicationDecision.PARTIALLY_APPROVED);
        assertThat(result.totalApproved()).isEqualByComparingTo("100.00");
    }

    @Test
    void adjudicate_multipleQuantity_computesCorrectly() {
        var lines = List.of(line("CONSULT", 3, "240.00"));
        var policy = activePolicy(CoverageType.PREMIUM);
        var catalogue = Map.of("CONSULT", catalogueItem("CONSULT", "80.00", true));

        var result = engine.adjudicate(lines, Optional.of(policy), catalogue);

        // catalogue max = 80 * 3 = 240, requested = 240, premium = 100% → 240
        assertThat(result.decision()).isEqualTo(AdjudicationDecision.APPROVED);
        assertThat(result.totalApproved()).isEqualByComparingTo("240.00");
    }

    private ClaimLine line(String code, int quantity, String amount) {
        var l = new ClaimLine();
        l.setProcedureCode(code);
        l.setQuantity(quantity);
        l.setRequestedAmount(new BigDecimal(amount));
        return l;
    }

    private LocalPolicy activePolicy(CoverageType type) {
        var p = new LocalPolicy();
        p.setId(UUID.randomUUID());
        p.setPetId(UUID.randomUUID());
        p.setCoverageType(type);
        p.setStartDate(LocalDate.now().minusDays(30));
        p.setEndDate(LocalDate.now().plusDays(335));
        p.setStatus(LocalPolicyStatus.ACTIVE);
        return p;
    }

    private LocalCatalogueItem catalogueItem(String code, String rate, boolean active) {
        var item = new LocalCatalogueItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setDescription("desc");
        item.setReimbursementRate(new BigDecimal(rate));
        item.setActive(active);
        return item;
    }
}
