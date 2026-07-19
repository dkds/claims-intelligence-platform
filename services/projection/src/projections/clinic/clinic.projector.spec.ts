import { Test, TestingModule } from '@nestjs/testing';
import { getModelToken } from '@nestjs/mongoose';
import { ClinicProjector } from './clinic.projector.js';
import { Clinic } from './clinic.schema.js';
import { EventEnvelope } from '../../common/event-envelope.interface.js';

describe('ClinicProjector', () => {
  let projector: ClinicProjector;
  let findOneAndUpdate: jest.Mock;

  beforeEach(async () => {
    findOneAndUpdate = jest.fn().mockResolvedValue(undefined);

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        ClinicProjector,
        {
          provide: getModelToken(Clinic.name),
          useValue: { findOneAndUpdate },
        },
      ],
    }).compile();

    projector = module.get(ClinicProjector);
  });

  function envelope(
    eventType: string,
    payload: Record<string, unknown>,
  ): EventEnvelope {
    return {
      eventId: 'evt-1',
      eventType,
      eventVersion: 1,
      occurredAt: '2026-06-01T10:00:00Z',
      producer: 'enrollment-policy',
      tenantId: 'clinic-1',
      aggregateType: 'clinic',
      aggregateId: 'clinic-1',
      correlationId: 'corr-1',
      causationId: '',
      payload,
    };
  }

  it('projects clinic.updated to an upsert of the clinic document', async () => {
    await projector.handle(
      envelope('clinic.updated', { name: 'Riverside Vets', status: 'ACTIVE' }),
    );

    expect(findOneAndUpdate).toHaveBeenCalledWith(
      { _id: 'clinic-1' },
      { $set: { _id: 'clinic-1', name: 'Riverside Vets', status: 'ACTIVE' } },
      { upsert: true, new: true },
    );
  });

  it('ignores unknown event types', async () => {
    await projector.handle(envelope('clinic.something-else', {}));

    expect(findOneAndUpdate).not.toHaveBeenCalled();
  });
});
