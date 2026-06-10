import { Body, Controller, Get, Param, Post } from '@nestjs/common';
import { SessionsService } from './sessions.service.js';

@Controller()
export class SessionsController {
  constructor(private readonly sessionsService: SessionsService) {}

  @Get('clinics/:clinicId/sessions')
  listByClinic(@Param('clinicId') clinicId: string) {
    return this.sessionsService.findByClinic(clinicId);
  }

  @Get('sessions/:id')
  getById(@Param('id') id: string) {
    return this.sessionsService.findById(id);
  }

  @Post('sessions/:id/verify')
  verify(@Param('id') id: string, @Body() body: unknown) {
    return this.sessionsService.verify(id, body);
  }
}
