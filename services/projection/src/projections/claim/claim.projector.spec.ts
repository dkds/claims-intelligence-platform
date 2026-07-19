import { Test, TestingModule } from '@nestjs/testing';
import { getModelToken } from '@nestjs/mongoose';
import { ClaimProjector } from './claim.projector.js';
import { Claim } from './claim.schema.js';
import { EventEnvelope } from '../../common/event-envelope.interface.js';

describe('ClaimProjector', () => {
  let projector: ClaimProjector;
  let findOneAndUpdate: jest.Mock;

  beforeEach(async () => {
    findOneAndUpdate = jest.fn().mockResolvedValue(undefined);

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        ClaimProjector,
        {
          provide: getModelToken(Claim.name),
          useValue: { findOneAndUpdate },
        },
      ],
    }).compile();

    projector = module.get(ClaimProjector);
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
      producer: 'claims-service',
      tenantId: 'clinic-1',
      aggregateType: 'claim',
      aggregateId: 'claim-77421',
      correlationId: 'corr-1',
      causationId: '',
      payload,
    };
  }

  it('projects claim.routed-to-review to a PENDING_REVIEW status update', async () => {
    await projector.handle(
      envelope('claim.routed-to-review', {
        claimId: 'claim-77421',
        reasons: ['Fraud risk level: high'],
        routedBy: 'auto',
        origin: 'session',
        updatedAt: '2026-06-01T10:00:31Z',
      }),
    );

    expect(findOneAndUpdate).toHaveBeenCalledWith(
      { _id: 'claim-77421' },
      {
        $set: {
          status: 'PENDING_REVIEW',
          reasons: ['Fraud risk level: high'],
          updatedAt: '2026-06-01T10:00:31Z',
        },
      },
      { upsert: true },
    );
  });

  it('defaults reasons to an empty array when absent', async () => {
    await projector.handle(
      envelope('claim.routed-to-review', {
        claimId: 'claim-77421',
        updatedAt: '2026-06-01T10:00:31Z',
      }),
    );

    expect(findOneAndUpdate).toHaveBeenCalledWith(
      { _id: 'claim-77421' },
      {
        $set: {
          status: 'PENDING_REVIEW',
          reasons: [],
          updatedAt: '2026-06-01T10:00:31Z',
        },
      },
      { upsert: true },
    );
  });

  it('ignores unknown event types', async () => {
    await projector.handle(envelope('claim.something-else', {}));

    expect(findOneAndUpdate).not.toHaveBeenCalled();
  });
});
