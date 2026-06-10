import { HttpModule } from '@nestjs/axios';
import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import {
  BffSession,
  BffSessionSchema,
} from '../common/schemas/session.schema.js';
import { SessionsController } from './sessions.controller.js';
import { SessionsService } from './sessions.service.js';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: BffSession.name, schema: BffSessionSchema },
    ]),
    HttpModule.register({ timeout: 5000 }),
  ],
  providers: [SessionsService],
  controllers: [SessionsController],
})
export class SessionsModule {}
