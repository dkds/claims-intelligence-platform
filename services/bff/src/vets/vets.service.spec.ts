import { Test, TestingModule } from '@nestjs/testing';
import { getModelToken } from '@nestjs/mongoose';
import { ConfigService } from '@nestjs/config';
import { HttpService } from '@nestjs/axios';
import { AxiosError } from 'axios';
import { of, throwError } from 'rxjs';
import { VetsService } from './vets.service.js';
import { BffVet } from '../common/schemas/vet.schema.js';

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
  } as AxiosError;
}

describe('VetsService', () => {
  let service: VetsService;
  let find: jest.Mock;
  let httpPost: jest.Mock;

  beforeEach(async () => {
    find = jest
      .fn()
      .mockReturnValue({
        lean: () => ({ exec: jest.fn().mockResolvedValue([{ _id: 'vet-1' }]) }),
      });
    httpPost = jest.fn();

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        VetsService,
        { provide: getModelToken(BffVet.name), useValue: { find } },
        { provide: HttpService, useValue: { post: httpPost } },
        {
          provide: ConfigService,
          useValue: {
            getOrThrow: jest.fn().mockReturnValue('http://enrollment'),
          },
        },
      ],
    }).compile();

    service = module.get(VetsService);
  });

  describe('findByClinic', () => {
    it('filters vets by clinicId', async () => {
      await service.findByClinic('clinic-1');
      expect(find).toHaveBeenCalledWith({ clinicId: 'clinic-1' });
    });
  });

  describe('create', () => {
    it('returns the created vet on success', async () => {
      httpPost.mockReturnValue(
        of({ data: { _id: 'vet-2', status: 'PENDING' } }),
      );

      await expect(
        service.create('clinic-1', { firstName: 'Jo' }),
      ).resolves.toEqual({
        _id: 'vet-2',
        status: 'PENDING',
      });
      expect(httpPost).toHaveBeenCalledWith(
        'http://enrollment/clinics/clinic-1/vets',
        { firstName: 'Jo' },
      );
    });

    it('maps a downstream error to an HttpException', async () => {
      httpPost.mockReturnValue(
        throwError(() => axiosError(422, { code: 'VALIDATION_FAILED' })),
      );

      await expect(service.create('clinic-1', {})).rejects.toMatchObject({
        response: { code: 'VALIDATION_FAILED', status: 422 },
      });
    });
  });

  describe('approve', () => {
    it('returns the approved vet on success', async () => {
      httpPost.mockReturnValue(
        of({ data: { _id: 'vet-1', status: 'APPROVED' } }),
      );

      await expect(service.approve('vet-1')).resolves.toEqual({
        _id: 'vet-1',
        status: 'APPROVED',
      });
      expect(httpPost).toHaveBeenCalledWith(
        'http://enrollment/vets/vet-1/approve',
        {},
      );
    });

    it('maps a downstream error to an HttpException', async () => {
      httpPost.mockReturnValue(
        throwError(() => axiosError(404, { code: 'VET_NOT_FOUND' })),
      );

      await expect(service.approve('missing')).rejects.toMatchObject({
        response: { code: 'VET_NOT_FOUND', status: 404 },
      });
    });
  });

  describe('reject', () => {
    it('returns the rejected vet on success', async () => {
      httpPost.mockReturnValue(
        of({ data: { _id: 'vet-1', status: 'REJECTED' } }),
      );

      await expect(
        service.reject('vet-1', { reason: 'Unverified license' }),
      ).resolves.toEqual({
        _id: 'vet-1',
        status: 'REJECTED',
      });
    });

    it('falls back to a status-based message for an unrecognised error code', async () => {
      httpPost.mockReturnValue(
        throwError(() => axiosError(500, { code: 'SOME_UNMAPPED_CODE' })),
      );

      await expect(service.reject('vet-1', {})).rejects.toMatchObject({
        response: {
          message: 'An unexpected server error occurred.',
          status: 500,
        },
      });
    });
  });
});
