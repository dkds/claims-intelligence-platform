import { HttpService } from '@nestjs/axios';
import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { InjectModel } from '@nestjs/mongoose';
import { AxiosError } from 'axios';
import { Model } from 'mongoose';
import { firstValueFrom } from 'rxjs';
import { toHttpException } from '../common/errors/http-exception.mapper.js';
import { BffVet, VetDocument } from '../common/schemas/vet.schema.js';

@Injectable()
export class VetsService {
  private readonly enrollmentUrl: string;

  constructor(
    private readonly config: ConfigService,
    @InjectModel(BffVet.name)
    private readonly model: Model<VetDocument>,
    private readonly http: HttpService,
  ) {
    this.enrollmentUrl = config.getOrThrow<string>('ENROLLMENT_URL');
  }

  async findByClinic(clinicId: string): Promise<VetDocument[]> {
    return this.model.find({ clinicId }).lean().exec();
  }

  async create(clinicId: string, body: unknown): Promise<unknown> {
    try {
      const res = await firstValueFrom(
        this.http.post(`${this.enrollmentUrl}/clinics/${clinicId}/vets`, body),
      );
      return res.data;
    } catch (err) {
      throw toHttpException(err as AxiosError);
    }
  }

  async approve(id: string): Promise<unknown> {
    try {
      const res = await firstValueFrom(
        this.http.post(`${this.enrollmentUrl}/vets/${id}/approve`, {}),
      );
      return res.data;
    } catch (err) {
      throw toHttpException(err as AxiosError);
    }
  }

  async reject(id: string, body: unknown): Promise<unknown> {
    try {
      const res = await firstValueFrom(
        this.http.post(`${this.enrollmentUrl}/vets/${id}/reject`, body),
      );
      return res.data;
    } catch (err) {
      throw toHttpException(err as AxiosError);
    }
  }
}
