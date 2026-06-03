package event

import "time"

type Envelope[P any] struct {
	EventID       string    `json:"eventId"`
	EventType     string    `json:"eventType"`
	EventVersion  int       `json:"eventVersion"`
	OccurredAt    time.Time `json:"occurredAt"`
	Producer      string    `json:"producer"`
	TenantID      string    `json:"tenantId"`
	AggregateType string    `json:"aggregateType"`
	AggregateID   string    `json:"aggregateId"`
	CorrelationID string    `json:"correlationId"`
	CausationID   *string   `json:"causationId"`
	Payload       P         `json:"payload"`
}
