import { Injectable, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { EventEnvelope } from '../../common/event-envelope.interface.js';
import { Claim, ClaimDocument } from './claim.schema.js';

@Injectable()
export class ClaimProjector {
  private readonly logger = new Logger(ClaimProjector.name);

  constructor(
    @InjectModel(Claim.name) private readonly model: Model<ClaimDocument>,
  ) {}

  async handle(envelope: EventEnvelope): Promise<void> {
    const { eventType, aggregateId, payload } = envelope;
    const p = payload as Record<string, unknown>;
    const filter = { _id: aggregateId };

    if (eventType === 'claim.assembled') {
      await this.model.findOneAndUpdate(
        { filter },
        { $set: { _id: aggregateId, ...(payload as object) } },
        { upsert: true, new: true },
      );
    } else if (eventType === 'claim.fraud-scored') {
      await this.model.findOneAndUpdate(
        { filter },
        {
          $set: {
            fraud: {
              score: p['score'],
              riskLevel: p['riskLevel'],
              flags: p['flags'] ?? [],
              modelVersion: p['modelVersion'],
              scoredAt: p['scoredAt'],
            },
          },
        },
        { upsert: true },
      );
    } else if (eventType === 'claim.adjudicated') {
      await this.model.findOneAndUpdate(
        { filter },
        {
          $set: {
            status: 'ADJUDICATED',
            decision: p['decision'],
            approvedAmount: p['approvedAmount'],
            adjudicatedBy: p['adjudicatedBy'],
            updatedAt: p['updatedAt'],
          },
        },
        { upsert: true },
      );
    } else if (eventType === 'claim.rejected') {
      await this.model.findOneAndUpdate(
        { filter },
        {
          $set: {
            status: 'REJECTED',
            reasons: p['reasons'] ?? [],
            updatedAt: p['updatedAt'],
          },
        },
        { upsert: true },
      );
    } else if (eventType === 'claim.ready-for-submission') {
      await this.model.findOneAndUpdate(
        { filter },
        { $set: { status: 'READY_FOR_SUBMISSION', updatedAt: p['updatedAt'] } },
        { upsert: true },
      );
    } else {
      return;
    }

    this.logger.debug(`Projected ${eventType} for claim ${aggregateId}`);
  }
}
