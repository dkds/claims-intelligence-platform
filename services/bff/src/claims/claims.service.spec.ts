import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { Test, TestingModule } from '@nestjs/testing';
import { getModelToken } from '@nestjs/mongoose';
import { ConfigService } from '@nestjs/config';
import { HttpService } from '@nestjs/axios';
import { NotFoundException } from '@nestjs/common';
import { AxiosError } from 'axios';
import { of, throwError } from 'rxjs';
import { ClaimsService } from './claims.service.js';
import { BffClaim } from '../common/schemas/claim.schema.js';
import { leanExec } from '../common/testing/mongo-query.mock.js';

function axiosError(
  status: number,
  data?: Record<string, unknown>,
): AxiosError {
  return {
    isAxiosError: true,
    name: 'AxiosError',
    message: 'Request failed',
    toJSON: () => ({}),
    response: {
      status,
      data,
      statusText: '',
      headers: {},
      config: {} as never,
    },
  };
}

describe('ClaimsService', () => {
  let service: ClaimsService;
  let find: jest.Mock;
  let findById: jest.Mock;
  let httpPost: jest.Mock;

  beforeEach(async () => {
    find = jest.fn().mockReturnValue(leanExec([{ _id: 'claim-1' }]));
    findById = jest.fn().mockReturnValue(leanExec({ _id: 'claim-1' }));
    httpPost = jest.fn();

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        ClaimsService,
        { provide: getModelToken(BffClaim.name), useValue: { find, findById } },
        { provide: HttpService, useValue: { post: httpPost } },
        {
          provide: ConfigService,
          useValue: { getOrThrow: jest.fn().mockReturnValue('http://claims') },
        },
      ],
    }).compile();

    service = module.get(ClaimsService);
  });

  describe('findByClinic', () => {
    it('filters by clinicId only when no status is given', async () => {
      await service.findByClinic('clinic-1');
      expect(find).toHaveBeenCalledWith({ clinicId: 'clinic-1' });
    });

    it('filters by clinicId and status when given', async () => {
      await service.findByClinic('clinic-1', 'PENDING_REVIEW');
      expect(find).toHaveBeenCalledWith({
        clinicId: 'clinic-1',
        status: 'PENDING_REVIEW',
      });
    });
  });

  describe('findById', () => {
    it('returns the claim when found', async () => {
      await expect(service.findById('claim-1')).resolves.toEqual({
        _id: 'claim-1',
      });
    });

    it('throws a 404 NotFoundException when the claim does not exist', async () => {
      findById.mockReturnValue(leanExec(null));

      await expect(service.findById('missing')).rejects.toMatchObject({
        response: {
          code: 'CLAIM_NOT_FOUND',
          message: 'Claim not found.',
          status: 404,
        },
      });
      await expect(service.findById('missing')).rejects.toBeInstanceOf(
        NotFoundException,
      );
    });
  });

  describe('createManual', () => {
    it('returns the created claim on success', async () => {
      httpPost.mockReturnValue(of({ data: { _id: 'claim-2' } }));

      await expect(
        service.createManual('clinic-1', { petId: 'pet-1' }),
      ).resolves.toEqual({ _id: 'claim-2' });
      expect(httpPost).toHaveBeenCalledWith(
        'http://claims/clinics/clinic-1/claims',
        { petId: 'pet-1' },
      );
    });

    it('maps a downstream policy-expired error to a friendly HttpException', async () => {
      httpPost.mockReturnValue(
        throwError(() => axiosError(422, { code: 'POLICY_EXPIRED' })),
      );

      await expect(service.createManual('clinic-1', {})).rejects.toMatchObject({
        response: {
          code: 'POLICY_EXPIRED',
          message: 'The policy has expired and cannot be used for new claims.',
          status: 422,
        },
      });
    });
  });

  describe('approve', () => {
    it('returns the adjudication result on success', async () => {
      httpPost.mockReturnValue(of({ data: { status: 'APPROVED' } }));

      await expect(
        service.approve('claim-1', { approvedBy: 'adj-1' }),
      ).resolves.toEqual({ status: 'APPROVED' });
    });

    it('maps a downstream error to an HttpException', async () => {
      httpPost.mockReturnValue(
        throwError(() => axiosError(404, { code: 'CLAIM_NOT_FOUND' })),
      );

      await expect(service.approve('missing', {})).rejects.toMatchObject({
        response: { code: 'CLAIM_NOT_FOUND', status: 404 },
      });
    });
  });

  describe('reject', () => {
    it('returns the adjudication result on success', async () => {
      httpPost.mockReturnValue(of({ data: { status: 'REJECTED' } }));

      await expect(
        service.reject('claim-1', { rejectedBy: 'adj-1' }),
      ).resolves.toEqual({ status: 'REJECTED' });
    });

    it('falls back to a status-based message for an unrecognised error code', async () => {
      httpPost.mockReturnValue(
        throwError(() => axiosError(500, { code: 'SOME_UNMAPPED_CODE' })),
      );

      await expect(service.reject('claim-1', {})).rejects.toMatchObject({
        response: {
          message: 'An unexpected server error occurred.',
          status: 500,
        },
      });
    });
  });
});
