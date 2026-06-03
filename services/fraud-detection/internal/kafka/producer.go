package kafka

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/twmb/franz-go/pkg/kgo"

	"github.com/dkds/cip/fraud-detection/internal/event"
)

type Producer struct {
	client *kgo.Client
	topic  string
}

func NewProducer(brokers []string, topic string) (*Producer, error) {
	cl, err := kgo.NewClient(kgo.SeedBrokers(brokers...))
	if err != nil {
		return nil, fmt.Errorf("create producer: %w", err)
	}
	return &Producer{client: cl, topic: topic}, nil
}

func (p *Producer) Publish(ctx context.Context, claimID string, env event.Envelope[event.FraudScoredPayload]) error {
	b, err := json.Marshal(env)
	if err != nil {
		return fmt.Errorf("marshal event: %w", err)
	}
	results := p.client.ProduceSync(ctx, &kgo.Record{
		Topic: p.topic,
		Key:   []byte(claimID),
		Value: b,
	})
	return results.FirstErr()
}

func (p *Producer) Close() {
	p.client.Close()
}
