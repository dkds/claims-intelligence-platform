import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { Test, TestingModule } from '@nestjs/testing';
import { getModelToken } from '@nestjs/mongoose';
import { PetProjector } from './pet.projector.js';
import { Pet } from './pet.schema.js';
import { EventEnvelope } from '../../common/event-envelope.interface.js';
import { resolvedMock } from '../../common/testing/jest-mock.util.js';

describe('PetProjector', () => {
  let projector: PetProjector;
  let findOneAndUpdate: jest.Mock;

  beforeEach(async () => {
    findOneAndUpdate = resolvedMock(undefined);

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        PetProjector,
        {
          provide: getModelToken(Pet.name),
          useValue: { findOneAndUpdate },
        },
      ],
    }).compile();

    projector = module.get(PetProjector);
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
      aggregateType: 'pet',
      aggregateId: 'pet-1',
      correlationId: 'corr-1',
      causationId: '',
      payload,
    };
  }

  it('projects pet.enrolled to an upsert of the pet document', async () => {
    await projector.handle(
      envelope('pet.enrolled', {
        clinicId: 'clinic-1',
        name: 'Biscuit',
        species: 'dog',
      }),
    );

    expect(findOneAndUpdate).toHaveBeenCalledWith(
      { _id: 'pet-1' },
      {
        $set: {
          _id: 'pet-1',
          clinicId: 'clinic-1',
          name: 'Biscuit',
          species: 'dog',
        },
      },
      { upsert: true, new: true },
    );
  });

  it('ignores unknown event types', async () => {
    await projector.handle(envelope('pet.something-else', {}));

    expect(findOneAndUpdate).not.toHaveBeenCalled();
  });
});
