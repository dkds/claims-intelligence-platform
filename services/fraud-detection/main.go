package main

import (
	"context"
	"log/slog"
	"os"
	"os/signal"
	"strings"
	"syscall"
	"time"

	"github.com/google/uuid"

	"github.com/dkds/cip/fraud-detection/internal/config"
	"github.com/dkds/cip/fraud-detection/internal/event"
	"github.com/dkds/cip/fraud-detection/internal/kafka"
	"github.com/dkds/cip/fraud-detection/internal/scoring"
)

func main() {
	ctx, cancel := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer cancel()

	logger := slog.New(slog.NewJSONHandler(os.Stdout, nil))
	slog.SetDefault(logger)

	cfg := config.Load()
	brokers := strings.Split(cfg.KafkaBrokers, ",")

	producer, err := kafka.NewProducer(brokers, cfg.FraudTopic)
	if err != nil {
		slog.Error("failed to create producer", "err", err)
		os.Exit(1)
	}
	defer producer.Close()

	slog.Info("fraud service starting", "brokers", cfg.KafkaBrokers, "topic", cfg.ClaimsTopic)

	err = kafka.RunConsumer(ctx, brokers, cfg.ClaimsTopic, cfg.ClaimsGroup, func(ctx context.Context, env event.Envelope[event.ClaimAssembledPayload]) error {
		claim := env.Payload
		result := scoring.Score(claim)

		slog.Info("scored claim",
			"claimId", claim.ClaimID,
			"score", result.Score,
			"riskLevel", result.RiskLevel,
			"flags", result.Flags,
		)

		flags := result.Flags
		if flags == nil {
			flags = []string{}
		}

		causationID := env.EventID
		scored := event.Envelope[event.FraudScoredPayload]{
			EventID:       uuid.New().String(),
			EventType:     "claim.fraud-scored",
			EventVersion:  1,
			OccurredAt:    time.Now().UTC(),
			Producer:      "fraud-detection",
			TenantID:      env.TenantID,
			AggregateType: "claim",
			AggregateID:   env.AggregateID,
			CorrelationID: env.CorrelationID,
			CausationID:   &causationID,
			Payload: event.FraudScoredPayload{
				ClaimID:      claim.ClaimID,
				Score:        result.Score,
				RiskLevel:    result.RiskLevel,
				Flags:        flags,
				ModelVersion: scoring.ModelVersion,
				ScoredAt:     time.Now().UTC(),
			},
		}

		return producer.Publish(ctx, claim.ClaimID, scored)
	})
	if err != nil {
		slog.Error("consumer error", "err", err)
		os.Exit(1)
	}

	slog.Info("fraud service stopped")
}

