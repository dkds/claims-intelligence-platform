import {
  Injectable,
  Logger,
  OnModuleDestroy,
  OnModuleInit,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Kafka, Consumer } from 'kafkajs';
import { ClaimProjector } from '../projections/claim/claim.projector.js';
import { ClinicProjector } from '../projections/clinic/clinic.projector.js';
import { PetProjector } from '../projections/pet/pet.projector.js';
import { SessionProjector } from '../projections/session/session.projector.js';
import { VetProjector } from '../projections/vet/vet.projector.js';
import { EventEnvelope } from '../common/event-envelope.interface.js';

const TOPICS = [
  'cip.enrollment.v1',
  'cip.sessions.v1',
  'cip.claims.v1',
  'cip.fraud.v1',
] as const;
type Topic = (typeof TOPICS)[number];

@Injectable()
export class KafkaConsumerService implements OnModuleInit, OnModuleDestroy {
  private readonly logger = new Logger(KafkaConsumerService.name);
  private consumer: Consumer;

  constructor(
    private readonly config: ConfigService,
    private readonly clinicProjector: ClinicProjector,
    private readonly petProjector: PetProjector,
    private readonly vetProjector: VetProjector,
    private readonly sessionProjector: SessionProjector,
    private readonly claimProjector: ClaimProjector,
  ) {
    const kafka = new Kafka({
      clientId: 'projection',
      brokers: config.getOrThrow<string>('KAFKA_BROKERS').split(','),
    });
    this.consumer = kafka.consumer({ groupId: 'cip-projection' });
  }

  async onModuleInit(): Promise<void> {
    await this.consumer.connect();
    await this.consumer.subscribe({ topics: [...TOPICS], fromBeginning: true });
    await this.consumer.run({
      eachMessage: async ({ topic, message }) => {
        if (!message.value) return;
        try {
          const envelope = JSON.parse(
            message.value.toString(),
          ) as EventEnvelope;
          await this.dispatch(topic as Topic, envelope);
        } catch (err) {
          this.logger.error(
            `Failed to process message on ${topic}: ${(err as Error).message}`,
          );
        }
      },
    });
    this.logger.log(`Subscribed to topics: ${TOPICS.join(', ')}`);
  }

  async onModuleDestroy(): Promise<void> {
    this.logger.log('Disconnecting Kafka consumer...');
    try {
      await this.consumer.disconnect();
      this.logger.log('Kafka consumer disconnected');
    } catch (err) {
      this.logger.error(
        `Error disconnecting Kafka consumer: ${(err as Error).message}`,
      );
    }
  }

  private async dispatch(topic: Topic, envelope: EventEnvelope): Promise<void> {
    switch (topic) {
      case 'cip.enrollment.v1':
        if (envelope.aggregateType === 'Clinic')
          await this.clinicProjector.handle(envelope);
        else if (envelope.aggregateType === 'Pet')
          await this.petProjector.handle(envelope);
        else if (envelope.aggregateType === 'Vet')
          await this.vetProjector.handle(envelope);
        break;
      case 'cip.sessions.v1':
        await this.sessionProjector.handle(envelope);
        break;
      case 'cip.claims.v1':
        await this.claimProjector.handle(envelope);
        break;
      case 'cip.fraud.v1':
        await this.claimProjector.handle(envelope);
        break;
    }
  }
}
