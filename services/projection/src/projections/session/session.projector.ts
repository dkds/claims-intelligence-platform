import { Injectable, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { EventEnvelope } from '../../common/event-envelope.interface.js';
import { Session } from './session.schema.js';

@Injectable()
export class SessionProjector {
  private readonly logger = new Logger(SessionProjector.name);

  constructor(
    @InjectModel(Session.name) private readonly model: Model<Session>,
  ) {}

  async handle(envelope: EventEnvelope): Promise<void> {
    const { eventType, aggregateId, payload } = envelope;
    const p = payload as Record<string, unknown>;
    const filter = { _id: aggregateId };

    if (eventType === 'session.logged') {
      await this.model.findOneAndUpdate(
        filter,
        { $set: { _id: aggregateId, ...(payload as object) } },
        { upsert: true, new: true },
      );
    } else if (eventType === 'session.verified') {
      await this.model.findOneAndUpdate(
        filter,
        {
          $set: {
            status: 'VERIFIED',
            lines: p['lines'] ?? [],
            verifiedBy: p['verifiedBy'],
            verifiedAt: p['verifiedAt'],
          },
        },
        { upsert: true },
      );
    } else {
      return;
    }

    this.logger.debug(`Projected ${eventType} for session ${aggregateId}`);
  }
}
