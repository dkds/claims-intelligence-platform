package event

type ClaimLine struct {
	ProcedureCode string  `json:"procedureCode"`
	Quantity      int     `json:"quantity"`
	Amount        float64 `json:"amount"`
}

type ClaimAssembledPayload struct {
	ClaimID         string      `json:"claimId"`
	ClinicID        string      `json:"clinicId"`
	PetID           string      `json:"petId"`
	Origin          string      `json:"origin"`
	SourceSessionID *string     `json:"sourceSessionId,omitempty"`
	PolicyID        string      `json:"policyId"`
	Lines           []ClaimLine `json:"lines"`
	TotalRequested  float64     `json:"totalRequested"`
	SubmittedBy     string      `json:"submittedBy"`
}
