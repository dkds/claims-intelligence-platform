import { Module } from '@nestjs/common';
import { HttpModule } from '@nestjs/axios';
import { MongooseModule } from '@nestjs/mongoose';
import { BffClaim, BffClaimSchema } from '../common/schemas/claim.schema.js';
import { ClaimsService } from './claims.service.js';
import { ClaimsController } from './claims.controller.js';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: BffClaim.name, schema: BffClaimSchema },
    ]),
    HttpModule.register({ timeout: 5000 }),
  ],
  providers: [ClaimsService],
  controllers: [ClaimsController],
})
export class ClaimsModule {}
