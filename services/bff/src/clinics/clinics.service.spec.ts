import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { Test, TestingModule } from '@nestjs/testing';
import { getModelToken } from '@nestjs/mongoose';
import { ConfigService } from '@nestjs/config';
import { HttpService } from '@nestjs/axios';
import { AxiosError } from 'axios';
import { of, throwError } from 'rxjs';
import { ClinicsService } from './clinics.service.js';
import { BffClinic } from '../common/schemas/clinic.schema.js';
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

describe('ClinicsService', () => {
  let service: ClinicsService;
  let find: jest.Mock;
  let httpPost: jest.Mock;

  beforeEach(async () => {
    find = jest.fn().mockReturnValue(leanExec([{ _id: 'clinic-1' }]));
    httpPost = jest.fn();

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        ClinicsService,
        { provide: getModelToken(BffClinic.name), useValue: { find } },
        { provide: HttpService, useValue: { post: httpPost } },
        {
          provide: ConfigService,
          useValue: {
            getOrThrow: jest.fn().mockReturnValue('http://enrollment'),
          },
        },
      ],
    }).compile();

    service = module.get(ClinicsService);
  });

  describe('findAll', () => {
    it('returns all clinics', async () => {
      await expect(service.findAll()).resolves.toEqual([{ _id: 'clinic-1' }]);
      expect(find).toHaveBeenCalledWith();
    });
  });

  describe('create', () => {
    it('returns the created clinic on success', async () => {
      httpPost.mockReturnValue(
        of({ data: { _id: 'clinic-2', name: 'Riverside Vets' } }),
      );

      await expect(service.create({ name: 'Riverside Vets' })).resolves.toEqual(
        {
          _id: 'clinic-2',
          name: 'Riverside Vets',
        },
      );
      expect(httpPost).toHaveBeenCalledWith('http://enrollment/clinics', {
        name: 'Riverside Vets',
      });
    });

    it('maps a downstream validation error to an HttpException', async () => {
      httpPost.mockReturnValue(
        throwError(() => axiosError(422, { code: 'VALIDATION_FAILED' })),
      );

      await expect(service.create({})).rejects.toMatchObject({
        response: {
          code: 'VALIDATION_FAILED',
          message: 'Validation failed.',
          status: 422,
        },
      });
    });
  });
});
