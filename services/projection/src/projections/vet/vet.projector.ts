import { Injectable, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { EventEnvelope } from '../../common/event-envelope.interface.js';
import { Vet, VetDocument } from './vet.schema.js';

@Injectable()
export class VetProjector {
  private readonly logger = new Logger(VetProjector.name);

  constructor(
    @InjectModel(Vet.name) private readonly model: Model<VetDocument>,
  ) {}

  async handle(envelope: EventEnvelope): Promise<void> {
    const { eventType, aggregateId, payload } = envelope;
    const filter = { _id: aggregateId };

    if (eventType === 'vet.registered') {
      await this.model.findOneAndUpdate(
        { filter },
        { $set: { _id: aggregateId, ...(payload as object) } },
        { upsert: true, new: true },
      );
    } else if (eventType === 'vet.approved') {
      await this.model.findOneAndUpdate(
        { filter },
        { $set: { status: 'APPROVED', ...(payload as object) } },
        { upsert: true },
      );
    } else if (eventType === 'vet.rejected') {
      const p = payload as Record<string, unknown>;
      await this.model.findOneAndUpdate(
        { filter },
        { $set: { status: 'REJECTED', rejectionReason: p['rejectionReason'] } },
        { upsert: true },
      );
    } else {
      return;
    }

    this.logger.debug(`Projected ${eventType} for vet ${aggregateId}`);
  }
}
