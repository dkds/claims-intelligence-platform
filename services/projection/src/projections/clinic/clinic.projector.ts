import { Injectable, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { EventEnvelope } from '../../common/event-envelope.interface.js';
import { Clinic } from './clinic.schema.js';

@Injectable()
export class ClinicProjector {
  private readonly logger = new Logger(ClinicProjector.name);

  constructor(
    @InjectModel(Clinic.name) private readonly model: Model<Clinic>,
  ) {}

  async handle(envelope: EventEnvelope): Promise<void> {
    const { eventType, aggregateId, payload } = envelope;

    if (eventType === 'clinic.updated') {
      await this.model.findOneAndUpdate(
        { _id: aggregateId },
        { $set: { _id: aggregateId, ...(payload as object) } },
        { upsert: true, new: true },
      );
      this.logger.debug(`Projected ${eventType} for clinic ${aggregateId}`);
    }
  }
}
