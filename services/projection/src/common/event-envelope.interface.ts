export interface EventEnvelope<T = unknown> {
  eventId: string;
  eventType: string;
  eventVersion: number;
  occurredAt: string;
  producer: string;
  tenantId: string;
  aggregateType: string;
  aggregateId: string;
  correlationId: string;
  causationId: string;
  payload: T;
}
