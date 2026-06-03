package event

import "time"

type FraudScoredPayload struct {
	ClaimID      string    `json:"claimId"`
	Score        float64   `json:"score"`
	RiskLevel    string    `json:"riskLevel"`
	Flags        []string  `json:"flags"`
	ModelVersion string    `json:"modelVersion"`
	ScoredAt     time.Time `json:"scoredAt"`
}
