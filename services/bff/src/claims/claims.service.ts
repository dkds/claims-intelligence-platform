import { HttpService } from '@nestjs/axios';
import { Injectable, NotFoundException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { InjectModel } from '@nestjs/mongoose';
import { AxiosError } from 'axios';
import { Model } from 'mongoose';
import { firstValueFrom } from 'rxjs';
import { toHttpException } from '../common/errors/http-exception.mapper.js';
import { BffClaim, ClaimDocument } from '../common/schemas/claim.schema.js';

@Injectable()
export class ClaimsService {
  private readonly claimsUrl: string;

  constructor(
    private readonly config: ConfigService,
    @InjectModel(BffClaim.name) private readonly model: Model<ClaimDocument>,
    private readonly http: HttpService,
  ) {
    this.claimsUrl = config.getOrThrow<string>('CLAIMS_URL');
  }

  async findByClinic(
    clinicId: string,
    status?: string,
  ): Promise<ClaimDocument[]> {
    const filter: Record<string, string> = { clinicId };
    if (status) filter['status'] = status;
    return this.model.find(filter).lean().exec();
  }

  async findById(id: string): Promise<ClaimDocument> {
    const doc = await this.model.findById(id).lean().exec();
    if (!doc)
      throw new NotFoundException({
        code: 'CLAIM_NOT_FOUND',
        message: 'Claim not found.',
        status: 404,
      });
    return doc;
  }

  async createManual(clinicId: string, body: unknown): Promise<unknown> {
    try {
      const res = await firstValueFrom(
        this.http.post(
          `${this.claimsUrl}/clinics/${clinicId}/claims`,
          body,
        ),
      );
      return res.data;
    } catch (err) {
      throw toHttpException(err as AxiosError);
    }
  }

  async approve(id: string, body: unknown): Promise<unknown> {
    try {
      const res = await firstValueFrom(
        this.http.post(`${this.claimsUrl}/claims/${id}/approve`, body),
      );
      return res.data;
    } catch (err) {
      throw toHttpException(err as AxiosError);
    }
  }

  async reject(id: string, body: unknown): Promise<unknown> {
    try {
      const res = await firstValueFrom(
        this.http.post(`${this.claimsUrl}/claims/${id}/reject`, body),
      );
      return res.data;
    } catch (err) {
      throw toHttpException(err as AxiosError);
    }
  }
}
