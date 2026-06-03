package config

import "os"

type Config struct {
	KafkaBrokers string
	ClaimsGroup  string
	ClaimsTopic  string
	FraudTopic   string
}

func Load() Config {
	return Config{
		KafkaBrokers: getEnv("KAFKA_BROKERS", "redpanda:9092"),
		ClaimsGroup:  getEnv("KAFKA_GROUP", "fraud-detection"),
		ClaimsTopic:  getEnv("KAFKA_CLAIMS_TOPIC", "cip.claims.v1"),
		FraudTopic:   getEnv("KAFKA_FRAUD_TOPIC", "cip.fraud.v1"),
	}
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
