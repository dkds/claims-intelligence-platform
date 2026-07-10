import { HttpModule } from '@nestjs/axios';
import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { BffVet, BffVetSchema } from '../common/schemas/vet.schema.js';
import { VetsController } from './vets.controller.js';
import { VetsService } from './vets.service.js';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: BffVet.name, schema: BffVetSchema }]),
    HttpModule.register({ timeout: 5000 }),
  ],
  providers: [VetsService],
  controllers: [VetsController],
})
export class VetsModule {}
