import { HttpService } from '@nestjs/axios';
import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { InjectModel } from '@nestjs/mongoose';
import { AxiosError } from 'axios';
import { Model } from 'mongoose';
import { firstValueFrom } from 'rxjs';
import { toHttpException } from '../common/errors/http-exception.mapper.js';
import { BffClinic, ClinicDocument } from '../common/schemas/clinic.schema.js';

@Injectable()
export class ClinicsService {
  private readonly enrollmentUrl: string;

  constructor(
    private readonly config: ConfigService,
    @InjectModel(BffClinic.name)
    private readonly model: Model<ClinicDocument>,
    private readonly http: HttpService,
  ) {
    this.enrollmentUrl = config.getOrThrow<string>('ENROLLMENT_URL');
  }

  async findAll(): Promise<ClinicDocument[]> {
    return this.model.find().lean().exec();
  }

  async create(body: unknown): Promise<unknown> {
    try {
      const res = await firstValueFrom(
        this.http.post(`${this.enrollmentUrl}/clinics`, body),
      );
      return res.data;
    } catch (err) {
      throw toHttpException(err as AxiosError);
    }
  }
}
