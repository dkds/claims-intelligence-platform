package kafka

import (
	"context"
	"encoding/json"
	"fmt"
	"log/slog"

	"github.com/twmb/franz-go/pkg/kgo"

	"github.com/dkds/cip/fraud-detection/internal/event"
)

type Handler func(ctx context.Context, env event.Envelope[event.ClaimAssembledPayload]) error

func RunConsumer(ctx context.Context, brokers []string, topic, group string, handler Handler) error {
	cl, err := kgo.NewClient(
		kgo.SeedBrokers(brokers...),
		kgo.ConsumerGroup(group),
		kgo.ConsumeTopics(topic),
	)
	if err != nil {
		return fmt.Errorf("create consumer: %w", err)
	}
	defer cl.Close()

	for {
		fetches := cl.PollFetches(ctx)
		if ctx.Err() != nil {
			return nil
		}
		fetches.EachError(func(t string, p int32, err error) {
			slog.Error("fetch error", "topic", t, "partition", p, "err", err)
		})
		fetches.EachRecord(func(r *kgo.Record) {
			var env event.Envelope[event.ClaimAssembledPayload]
			if err := json.Unmarshal(r.Value, &env); err != nil {
				slog.Error("unmarshal error", "err", err)
				return
			}
			if env.EventType != "claim.assembled" {
				return
			}
			if err := handler(ctx, env); err != nil {
				slog.Error("handler error", "eventId", env.EventID, "err", err)
			}
		})
	}
}
