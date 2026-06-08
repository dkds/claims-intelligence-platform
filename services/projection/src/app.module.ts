import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { ProjectionsModule } from './projections/projections.module.js';
import { HealthController } from './health/health.controller.js';

@Module({
  imports: [
    MongooseModule.forRoot(
      process.env['MONGODB_URI'] ?? 'mongodb://mongo:27017/cip_projection',
    ),
    ProjectionsModule,
  ],
  controllers: [HealthController],
})
export class AppModule {}
