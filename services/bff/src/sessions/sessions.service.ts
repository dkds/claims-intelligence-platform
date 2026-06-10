import { HttpService } from '@nestjs/axios';
import { Injectable, NotFoundException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { InjectModel } from '@nestjs/mongoose';
import { AxiosError } from 'axios';
import { Model } from 'mongoose';
import { firstValueFrom } from 'rxjs';
import { toHttpException } from '../common/errors/http-exception.mapper.js';
import {
  BffSession,
  SessionDocument,
} from '../common/schemas/session.schema.js';

@Injectable()
export class SessionsService {
  private readonly sessionsUrl: string;

  constructor(
    private readonly config: ConfigService,
    @InjectModel(BffSession.name)
    private readonly model: Model<SessionDocument>,
    private readonly http: HttpService,
  ) {
    this.sessionsUrl = config.getOrThrow<string>('SESSIONS_URL');
  }

  async findByClinic(clinicId: string): Promise<SessionDocument[]> {
    return this.model.find({ clinicId }).lean().exec();
  }

  async findById(id: string): Promise<SessionDocument> {
    const doc = await this.model.findById(id).lean().exec();
    if (!doc)
      throw new NotFoundException({
        code: 'SESSION_NOT_FOUND',
        message: 'Session not found.',
        status: 404,
      });
    return doc;
  }

  async verify(sessionId: string, body: unknown): Promise<unknown> {
    try {
      const res = await firstValueFrom(
        this.http.post(
          `${this.sessionsUrl}/api/sessions/${sessionId}/verify`,
          body,
        ),
      );
      return res.data;
    } catch (err) {
      throw toHttpException(err as AxiosError);
    }
  }
}
