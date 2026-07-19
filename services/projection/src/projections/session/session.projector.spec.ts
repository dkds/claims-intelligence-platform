import { Test, TestingModule } from '@nestjs/testing';
import { getModelToken } from '@nestjs/mongoose';
import { SessionProjector } from './session.projector.js';
import { Session } from './session.schema.js';
import { EventEnvelope } from '../../common/event-envelope.interface.js';

describe('SessionProjector', () => {
  let projector: SessionProjector;
  let findOneAndUpdate: jest.Mock;

  beforeEach(async () => {
    findOneAndUpdate = jest.fn().mockResolvedValue(undefined);

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        SessionProjector,
        {
          provide: getModelToken(Session.name),
          useValue: { findOneAndUpdate },
        },
      ],
    }).compile();

    projector = module.get(SessionProjector);
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
      producer: 'sessions',
      tenantId: 'clinic-1',
      aggregateType: 'session',
      aggregateId: 'session-1',
      correlationId: 'corr-1',
      causationId: '',
      payload,
    };
  }

  it('projects session.logged to an upsert of the session document', async () => {
    await projector.handle(
      envelope('session.logged', {
        clinicId: 'clinic-1',
        petId: 'pet-1',
        status: 'LOGGED',
      }),
    );

    expect(findOneAndUpdate).toHaveBeenCalledWith(
      { _id: 'session-1' },
      {
        $set: {
          _id: 'session-1',
          clinicId: 'clinic-1',
          petId: 'pet-1',
          status: 'LOGGED',
        },
      },
      { upsert: true, new: true },
    );
  });

  it('projects session.verified to a VERIFIED status update', async () => {
    await projector.handle(
      envelope('session.verified', {
        lines: [
          {
            lineId: 'l1',
            description: 'Checkup',
            amount: 40,
            category: 'exam',
          },
        ],
        verifiedBy: 'mgr-1',
        verifiedAt: '2026-06-01T10:05:00Z',
      }),
    );

    expect(findOneAndUpdate).toHaveBeenCalledWith(
      { _id: 'session-1' },
      {
        $set: {
          status: 'VERIFIED',
          lines: [
            {
              lineId: 'l1',
              description: 'Checkup',
              amount: 40,
              category: 'exam',
            },
          ],
          verifiedBy: 'mgr-1',
          verifiedAt: '2026-06-01T10:05:00Z',
        },
      },
      { upsert: true },
    );
  });

  it('defaults lines to an empty array when absent on session.verified', async () => {
    await projector.handle(
      envelope('session.verified', {
        verifiedBy: 'mgr-1',
        verifiedAt: '2026-06-01T10:05:00Z',
      }),
    );

    expect(findOneAndUpdate).toHaveBeenCalledWith(
      { _id: 'session-1' },
      {
        $set: {
          status: 'VERIFIED',
          lines: [],
          verifiedBy: 'mgr-1',
          verifiedAt: '2026-06-01T10:05:00Z',
        },
      },
      { upsert: true },
    );
  });

  it('ignores unknown event types', async () => {
    await projector.handle(envelope('session.something-else', {}));

    expect(findOneAndUpdate).not.toHaveBeenCalled();
  });
});
