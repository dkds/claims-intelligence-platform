package scoring

import (
	"testing"

	"github.com/dkds/cip/fraud-detection/internal/event"
)

func TestScore_sessionLowValue_isLowRisk(t *testing.T) {
	result := Score(event.ClaimAssembledPayload{
		Origin:         "session",
		TotalRequested: 120.00,
		Lines:          []event.ClaimLine{{ProcedureCode: "CONSULT", Quantity: 1, Amount: 120.00}},
	})
	if result.RiskLevel != "low" {
		t.Errorf("expected low, got %s", result.RiskLevel)
	}
	if len(result.Flags) != 0 {
		t.Errorf("expected no flags, got %v", result.Flags)
	}
}

func TestScore_manualOrigin_addsFlag(t *testing.T) {
	result := Score(event.ClaimAssembledPayload{
		Origin:         "manual",
		TotalRequested: 100.00,
		Lines:          []event.ClaimLine{{ProcedureCode: "CONSULT", Quantity: 1, Amount: 100.00}},
	})
	if !contains(result.Flags, "manual-origin") {
		t.Errorf("expected manual-origin flag, got %v", result.Flags)
	}
	// score = 0.25, still low
	if result.RiskLevel != "low" {
		t.Errorf("expected low risk, got %s", result.RiskLevel)
	}
}

func TestScore_highValue_isMediumRisk(t *testing.T) {
	result := Score(event.ClaimAssembledPayload{
		Origin:         "session",
		TotalRequested: 6000.00,
		Lines:          []event.ClaimLine{{ProcedureCode: "SURGERY", Quantity: 1, Amount: 6000.00}},
	})
	if result.RiskLevel != "medium" {
		t.Errorf("expected medium, got %s", result.RiskLevel)
	}
}

func TestScore_manualHighValue_isHighRisk(t *testing.T) {
	result := Score(event.ClaimAssembledPayload{
		Origin:         "manual",
		TotalRequested: 6000.00,
		Lines:          []event.ClaimLine{{ProcedureCode: "SURGERY", Quantity: 1, Amount: 6000.00}},
	})
	if result.RiskLevel != "high" {
		t.Errorf("expected high, got %s", result.RiskLevel)
	}
}

func TestScore_manyLines_raisesScore(t *testing.T) {
	lines := make([]event.ClaimLine, 11)
	for i := range lines {
		lines[i] = event.ClaimLine{ProcedureCode: "CONSULT", Quantity: 1, Amount: 50.00}
	}
	result := Score(event.ClaimAssembledPayload{
		Origin:         "session",
		TotalRequested: 550.00,
		Lines:          lines,
	})
	if !contains(result.Flags, "many-lines") {
		t.Errorf("expected many-lines flag, got %v", result.Flags)
	}
}

func TestScore_cappedAtOne(t *testing.T) {
	lines := make([]event.ClaimLine, 11)
	for i := range lines {
		lines[i] = event.ClaimLine{ProcedureCode: "SURGERY", Quantity: 1, Amount: 600.00}
	}
	result := Score(event.ClaimAssembledPayload{
		Origin:         "manual",
		TotalRequested: 6600.00,
		Lines:          lines,
	})
	if result.Score > 1.0 {
		t.Errorf("score should be capped at 1.0, got %f", result.Score)
	}
}

func contains(s []string, v string) bool {
	for _, x := range s {
		if x == v {
			return true
		}
	}
	return false
}
