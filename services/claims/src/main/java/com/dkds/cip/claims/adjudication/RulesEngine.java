package com.dkds.cip.claims.adjudication;

import com.dkds.cip.claims.claim.ClaimLine;
import com.dkds.cip.claims.masterdata.catalogue.LocalCatalogueItem;
import com.dkds.cip.claims.masterdata.policy.LocalPolicy;
import com.dkds.cip.claims.masterdata.policy.LocalPolicyStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RulesEngine {

    /**
     * Adjudicates a set of claim lines against a policy and catalogue.
     * Sets approvedAmount on each line as a side effect, then returns the overall decision.
     */
    public AdjudicationResult adjudicate(
            List<ClaimLine> lines,
            Optional<LocalPolicy> policy,
            Map<String, LocalCatalogueItem> catalogue) {

        if (policy.isEmpty() || policy.get().getStatus() != LocalPolicyStatus.ACTIVE) {
            lines.forEach(l -> l.setApprovedAmount(BigDecimal.ZERO));
            return AdjudicationResult.rejected(List.of("No active policy found"));
        }

        var coverageRate = switch (policy.get().getCoverageType()) {
            case BASIC -> new BigDecimal("0.60");
            case STANDARD -> new BigDecimal("0.80");
            case PREMIUM -> BigDecimal.ONE;
        };

        var reasons = new ArrayList<String>();
        var totalApproved = BigDecimal.ZERO;
        var totalRequested = BigDecimal.ZERO;

        for (var line : lines) {
            totalRequested = totalRequested.add(line.getRequestedAmount());
            var item = catalogue.get(line.getProcedureCode());
            if (item == null || !item.isActive()) {
                line.setApprovedAmount(BigDecimal.ZERO);
                reasons.add("Procedure not covered: " + line.getProcedureCode());
            } else {
                // Reimburse at the catalogue rate, capped by what the clinic requested
                var catalogueMax = item.getReimbursementRate()
                        .multiply(BigDecimal.valueOf(line.getQuantity()));
                var applicable = line.getRequestedAmount().min(catalogueMax);
                var approved = applicable.multiply(coverageRate).setScale(2, RoundingMode.HALF_UP);
                line.setApprovedAmount(approved);
                totalApproved = totalApproved.add(approved);
            }
        }

        if (totalApproved.compareTo(BigDecimal.ZERO) == 0) {
            return AdjudicationResult.rejected(reasons.isEmpty()
                    ? List.of("No coverable procedures") : reasons);
        }
        if (totalApproved.compareTo(totalRequested) < 0) {
            return AdjudicationResult.partiallyApproved(totalApproved, reasons);
        }
        return AdjudicationResult.approved(totalApproved);
    }
}
