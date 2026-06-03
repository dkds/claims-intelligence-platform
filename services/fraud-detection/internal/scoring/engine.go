package scoring

import (
	"fmt"

	"github.com/dkds/cip/fraud-detection/internal/event"
)

const ModelVersion = "rules-2026.04"

type Result struct {
	Score     float64
	RiskLevel string
	Flags     []string
}

// Score evaluates a claim.assembled payload against the rules engine.
func Score(claim event.ClaimAssembledPayload) Result {
	var flags []string
	score := 0.0

	// Manual claims carry a higher base risk than session-derived ones.
	if claim.Origin == "manual" {
		score += 0.25
		flags = append(flags, "manual-origin")
	}

	if claim.TotalRequested > 5000 {
		score += 0.35
		flags = append(flags, fmt.Sprintf("high-value:%.2f", claim.TotalRequested))
	}

	if len(claim.Lines) > 10 {
		score += 0.20
		flags = append(flags, "many-lines")
	}

	return Result{
		Score:     min(score, 1.0),
		RiskLevel: riskLevel(score),
		Flags:     flags,
	}
}

func riskLevel(score float64) string {
	switch {
	case score >= 0.6:
		return "high"
	case score >= 0.3:
		return "medium"
	default:
		return "low"
	}
}
