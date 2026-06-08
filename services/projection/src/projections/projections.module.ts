import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { Claim, ClaimSchema } from './claim/claim.schema.js';
import { ClaimProjector } from './claim/claim.projector.js';
import { Clinic, ClinicSchema } from './clinic/clinic.schema.js';
import { ClinicProjector } from './clinic/clinic.projector.js';
import { Pet, PetSchema } from './pet/pet.schema.js';
import { PetProjector } from './pet/pet.projector.js';
import { Session, SessionSchema } from './session/session.schema.js';
import { SessionProjector } from './session/session.projector.js';
import { Vet, VetSchema } from './vet/vet.schema.js';
import { VetProjector } from './vet/vet.projector.js';
import { KafkaConsumerService } from '../kafka/kafka-consumer.service.js';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: Clinic.name, schema: ClinicSchema },
      { name: Pet.name, schema: PetSchema },
      { name: Vet.name, schema: VetSchema },
      { name: Session.name, schema: SessionSchema },
      { name: Claim.name, schema: ClaimSchema },
    ]),
  ],
  providers: [
    ClinicProjector,
    PetProjector,
    VetProjector,
    SessionProjector,
    ClaimProjector,
    KafkaConsumerService,
  ],
})
export class ProjectionsModule {}
