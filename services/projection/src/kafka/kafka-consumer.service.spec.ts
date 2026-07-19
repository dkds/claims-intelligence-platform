import { Test, TestingModule } from '@nestjs/testing';
import { ConfigService } from '@nestjs/config';
import { KafkaConsumerService } from './kafka-consumer.service.js';
import { ClinicProjector } from '../projections/clinic/clinic.projector.js';
import { PetProjector } from '../projections/pet/pet.projector.js';
import { VetProjector } from '../projections/vet/vet.projector.js';
import { SessionProjector } from '../projections/session/session.projector.js';
import { ClaimProjector } from '../projections/claim/claim.projector.js';
import { EventEnvelope } from '../common/event-envelope.interface.js';

const mockConsumer = {
  connect: jest.fn().mockResolvedValue(undefined),
  subscribe: jest.fn().mockResolvedValue(undefined),
  run: jest.fn().mockResolvedValue(undefined),
  disconnect: jest.fn().mockResolvedValue(undefined),
};

jest.mock('kafkajs', () => ({
  Kafka: jest.fn().mockImplementation(() => ({
    consumer: jest.fn().mockReturnValue(mockConsumer),
  })),
}));

describe('KafkaConsumerService', () => {
  let service: KafkaConsumerService;
  let clinicProjector: { handle: jest.Mock };
  let petProjector: { handle: jest.Mock };
  let vetProjector: { handle: jest.Mock };
  let sessionProjector: { handle: jest.Mock };
  let claimProjector: { handle: jest.Mock };

  beforeEach(async () => {
    jest.clearAllMocks();
    clinicProjector = { handle: jest.fn().mockResolvedValue(undefined) };
    petProjector = { handle: jest.fn().mockResolvedValue(undefined) };
    vetProjector = { handle: jest.fn().mockResolvedValue(undefined) };
    sessionProjector = { handle: jest.fn().mockResolvedValue(undefined) };
    claimProjector = { handle: jest.fn().mockResolvedValue(undefined) };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        KafkaConsumerService,
        {
          provide: ConfigService,
          useValue: { getOrThrow: jest.fn().mockReturnValue('broker:9092') },
        },
        { provide: ClinicProjector, useValue: clinicProjector },
        { provide: PetProjector, useValue: petProjector },
        { provide: VetProjector, useValue: vetProjector },
        { provide: SessionProjector, useValue: sessionProjector },
        { provide: ClaimProjector, useValue: claimProjector },
      ],
    }).compile();

    service = module.get(KafkaConsumerService);
  });

  function envelope(aggregateType: string): EventEnvelope {
    return {
      eventId: 'evt-1',
      eventType: 'x',
      eventVersion: 1,
      occurredAt: '2026-06-01T10:00:00Z',
      producer: 'enrollment-policy',
      tenantId: 'clinic-1',
      aggregateType,
      aggregateId: 'agg-1',
      correlationId: 'corr-1',
      causationId: '',
      payload: {},
    };
  }

  describe('dispatch', () => {
    it('routes cip.enrollment.v1 clinic events to the clinic projector', async () => {
      await (service as unknown as { dispatch: Function }).dispatch(
        'cip.enrollment.v1',
        envelope('clinic'),
      );

      expect(clinicProjector.handle).toHaveBeenCalled();
      expect(petProjector.handle).not.toHaveBeenCalled();
      expect(vetProjector.handle).not.toHaveBeenCalled();
    });

    it('routes cip.enrollment.v1 pet events to the pet projector', async () => {
      await (service as unknown as { dispatch: Function }).dispatch(
        'cip.enrollment.v1',
        envelope('pet'),
      );

      expect(petProjector.handle).toHaveBeenCalled();
    });

    it('routes cip.enrollment.v1 vet events to the vet projector', async () => {
      await (service as unknown as { dispatch: Function }).dispatch(
        'cip.enrollment.v1',
        envelope('vet'),
      );

      expect(vetProjector.handle).toHaveBeenCalled();
    });

    it('routes to no projector for an unrecognised aggregateType on cip.enrollment.v1', async () => {
      // Regression guard for the earlier capitalisation bug (aggregateType 'Clinic' vs 'clinic')
      // that silently dropped every clinic/vet/pet event until Phase 6f surfaced it.
      await (service as unknown as { dispatch: Function }).dispatch(
        'cip.enrollment.v1',
        envelope('Clinic'),
      );

      expect(clinicProjector.handle).not.toHaveBeenCalled();
      expect(petProjector.handle).not.toHaveBeenCalled();
      expect(vetProjector.handle).not.toHaveBeenCalled();
    });

    it('routes cip.sessions.v1 to the session projector', async () => {
      await (service as unknown as { dispatch: Function }).dispatch(
        'cip.sessions.v1',
        envelope('session'),
      );

      expect(sessionProjector.handle).toHaveBeenCalled();
    });

    it('routes cip.claims.v1 to the claim projector', async () => {
      await (service as unknown as { dispatch: Function }).dispatch(
        'cip.claims.v1',
        envelope('claim'),
      );

      expect(claimProjector.handle).toHaveBeenCalled();
    });

    it('routes cip.fraud.v1 to the claim projector', async () => {
      await (service as unknown as { dispatch: Function }).dispatch(
        'cip.fraud.v1',
        envelope('claim'),
      );

      expect(claimProjector.handle).toHaveBeenCalled();
    });
  });

  describe('onModuleInit', () => {
    it('connects and subscribes to all topics', async () => {
      await service.onModuleInit();

      expect(mockConsumer.connect).toHaveBeenCalled();
      expect(mockConsumer.subscribe).toHaveBeenCalledWith({
        topics: [
          'cip.enrollment.v1',
          'cip.sessions.v1',
          'cip.claims.v1',
          'cip.fraud.v1',
        ],
        fromBeginning: true,
      });
    });

    it('dispatches a well-formed message to the right projector', async () => {
      await service.onModuleInit();
      const eachMessage = mockConsumer.run.mock.calls[0][0].eachMessage;

      await eachMessage({
        topic: 'cip.claims.v1',
        message: { value: Buffer.from(JSON.stringify(envelope('claim'))) },
      });

      expect(claimProjector.handle).toHaveBeenCalled();
    });

    it('catches malformed message JSON without throwing', async () => {
      await service.onModuleInit();
      const eachMessage = mockConsumer.run.mock.calls[0][0].eachMessage;

      await expect(
        eachMessage({
          topic: 'cip.claims.v1',
          message: { value: Buffer.from('not-json') },
        }),
      ).resolves.toBeUndefined();
      expect(claimProjector.handle).not.toHaveBeenCalled();
    });

    it('ignores messages with no value', async () => {
      await service.onModuleInit();
      const eachMessage = mockConsumer.run.mock.calls[0][0].eachMessage;

      await eachMessage({ topic: 'cip.claims.v1', message: { value: null } });

      expect(claimProjector.handle).not.toHaveBeenCalled();
    });
  });
});
