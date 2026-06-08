import { Injectable, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { EventEnvelope } from '../../common/event-envelope.interface.js';
import { Pet, PetDocument } from './pet.schema.js';

@Injectable()
export class PetProjector {
  private readonly logger = new Logger(PetProjector.name);

  constructor(
    @InjectModel(Pet.name) private readonly model: Model<PetDocument>,
  ) {}

  async handle(envelope: EventEnvelope): Promise<void> {
    const { eventType, aggregateId, payload } = envelope;

    if (eventType === 'pet.enrolled') {
      await this.model.findOneAndUpdate(
        { filter: { _id: aggregateId } },
        { $set: { _id: aggregateId, ...(payload as object) } },
        { upsert: true, new: true },
      );
      this.logger.debug(`Projected ${eventType} for pet ${aggregateId}`);
    }
  }
}
