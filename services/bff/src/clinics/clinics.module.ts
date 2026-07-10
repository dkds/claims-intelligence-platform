import { HttpModule } from '@nestjs/axios';
import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { BffClinic, BffClinicSchema } from '../common/schemas/clinic.schema.js';
import { ClinicsController } from './clinics.controller.js';
import { ClinicsService } from './clinics.service.js';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: BffClinic.name, schema: BffClinicSchema },
    ]),
    HttpModule.register({ timeout: 5000 }),
  ],
  providers: [ClinicsService],
  controllers: [ClinicsController],
})
export class ClinicsModule {}
