import { Test, TestingModule } from '@nestjs/testing';
import { getModelToken } from '@nestjs/mongoose';
import { ConfigService } from '@nestjs/config';
import { HttpService } from '@nestjs/axios';
import { NotFoundException } from '@nestjs/common';
import { AxiosError } from 'axios';
import { of, throwError } from 'rxjs';
import { SessionsService } from './sessions.service.js';
import { BffSession } from '../common/schemas/session.schema.js';

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

describe('SessionsService', () => {
  let service: SessionsService;
  let find: jest.Mock;
  let findById: jest.Mock;
  let httpPost: jest.Mock;

  beforeEach(async () => {
    find = jest
      .fn()
      .mockReturnValue({
        lean: () => ({
          exec: jest.fn().mockResolvedValue([{ _id: 'session-1' }]),
        }),
      });
    findById = jest
      .fn()
      .mockReturnValue({
        lean: () => ({
          exec: jest
            .fn()
            .mockResolvedValue({ _id: 'session-1', status: 'LOGGED' }),
        }),
      });
    httpPost = jest.fn();

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        SessionsService,
        {
          provide: getModelToken(BffSession.name),
          useValue: { find, findById },
        },
        { provide: HttpService, useValue: { post: httpPost } },
        {
          provide: ConfigService,
          useValue: {
            getOrThrow: jest.fn().mockReturnValue('http://sessions'),
          },
        },
      ],
    }).compile();

    service = module.get(SessionsService);
  });

  describe('findByClinic', () => {
    it('filters sessions by clinicId', async () => {
      await service.findByClinic('clinic-1');
      expect(find).toHaveBeenCalledWith({ clinicId: 'clinic-1' });
    });
  });

  describe('findById', () => {
    it('returns the session when found', async () => {
      await expect(service.findById('session-1')).resolves.toEqual({
        _id: 'session-1',
        status: 'LOGGED',
      });
    });

    it('throws a 404 NotFoundException when the session does not exist', async () => {
      findById.mockReturnValue({
        lean: () => ({ exec: jest.fn().mockResolvedValue(null) }),
      });

      await expect(service.findById('missing')).rejects.toBeInstanceOf(
        NotFoundException,
      );
      await expect(service.findById('missing')).rejects.toMatchObject({
        response: {
          code: 'SESSION_NOT_FOUND',
          message: 'Session not found.',
          status: 404,
        },
      });
    });
  });

  describe('verify', () => {
    it('returns the verified session on success', async () => {
      httpPost.mockReturnValue(
        of({ data: { _id: 'session-1', status: 'VERIFIED' } }),
      );

      await expect(
        service.verify('session-1', { verifiedBy: 'mgr-1' }),
      ).resolves.toEqual({
        _id: 'session-1',
        status: 'VERIFIED',
      });
      expect(httpPost).toHaveBeenCalledWith(
        'http://sessions/sessions/session-1/verify',
        { verifiedBy: 'mgr-1' },
      );
    });

    it('maps an already-verified conflict to an HttpException', async () => {
      httpPost.mockReturnValue(
        throwError(() => axiosError(409, { code: 'SESSION_ALREADY_VERIFIED' })),
      );

      await expect(service.verify('session-1', {})).rejects.toMatchObject({
        response: {
          code: 'SESSION_ALREADY_VERIFIED',
          message: 'This session has already been verified.',
          status: 409,
        },
      });
    });
  });
});
