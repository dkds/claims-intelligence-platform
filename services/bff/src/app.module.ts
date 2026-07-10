import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { MongooseModule } from '@nestjs/mongoose';
import { ClaimsModule } from './claims/claims.module.js';
import { SessionsModule } from './sessions/sessions.module.js';
import { ClinicsModule } from './clinics/clinics.module.js';
import { VetsModule } from './vets/vets.module.js';
import { HealthController } from './health/health.controller.js';

@Module({
  imports: [
    ConfigModule.forRoot({ envFilePath: '.env', isGlobal: true }),
    MongooseModule.forRootAsync({
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        uri: config.getOrThrow<string>('MONGODB_URI'),
      }),
    }),
    ClaimsModule,
    SessionsModule,
    ClinicsModule,
    VetsModule,
  ],
  controllers: [HealthController],
})
export class AppModule {}
