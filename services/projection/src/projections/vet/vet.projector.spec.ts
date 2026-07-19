import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { Test, TestingModule } from '@nestjs/testing';
import { getModelToken } from '@nestjs/mongoose';
import { VetProjector } from './vet.projector.js';
import { Vet } from './vet.schema.js';
import { EventEnvelope } from '../../common/event-envelope.interface.js';
import { resolvedMock } from '../../common/testing/jest-mock.util.js';

describe('VetProjector', () => {
  let projector: VetProjector;
  let findOneAndUpdate: jest.Mock;

  beforeEach(async () => {
    findOneAndUpdate = resolvedMock(undefined);

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        VetProjector,
        {
          provide: getModelToken(Vet.name),
          useValue: { findOneAndUpdate },
        },
      ],
    }).compile();

    projector = module.get(VetProjector);
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
      aggregateType: 'vet',
      aggregateId: 'vet-1',
      correlationId: 'corr-1',
      causationId: '',
      payload,
    };
  }

  it('projects vet.registered to an upsert of the vet document', async () => {
    await projector.handle(
      envelope('vet.registered', {
        clinicId: 'clinic-1',
        firstName: 'Jo',
        status: 'PENDING',
      }),
    );

    expect(findOneAndUpdate).toHaveBeenCalledWith(
      { _id: 'vet-1' },
      {
        $set: {
          _id: 'vet-1',
          clinicId: 'clinic-1',
          firstName: 'Jo',
          status: 'PENDING',
        },
      },
      { upsert: true, new: true },
    );
  });

  it('projects vet.approved to an APPROVED status update', async () => {
    await projector.handle(envelope('vet.approved', { approvedBy: 'admin-1' }));

    expect(findOneAndUpdate).toHaveBeenCalledWith(
      { _id: 'vet-1' },
      { $set: { status: 'APPROVED', approvedBy: 'admin-1' } },
      { upsert: true },
    );
  });

  it('projects vet.rejected to a REJECTED status update with the rejection reason', async () => {
    await projector.handle(
      envelope('vet.rejected', { rejectionReason: 'Unverified license' }),
    );

    expect(findOneAndUpdate).toHaveBeenCalledWith(
      { _id: 'vet-1' },
      { $set: { status: 'REJECTED', rejectionReason: 'Unverified license' } },
      { upsert: true },
    );
  });

  it('ignores unknown event types', async () => {
    await projector.handle(envelope('vet.something-else', {}));

    expect(findOneAndUpdate).not.toHaveBeenCalled();
  });
});
