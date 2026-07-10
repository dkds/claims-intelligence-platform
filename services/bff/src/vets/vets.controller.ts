import { Body, Controller, Get, Param, Post } from '@nestjs/common';
import { VetsService } from './vets.service.js';

@Controller()
export class VetsController {
  constructor(private readonly vetsService: VetsService) {}

  @Get('clinics/:clinicId/vets')
  listByClinic(@Param('clinicId') clinicId: string) {
    return this.vetsService.findByClinic(clinicId);
  }

  @Post('clinics/:clinicId/vets')
  create(@Param('clinicId') clinicId: string, @Body() body: unknown) {
    return this.vetsService.create(clinicId, body);
  }

  @Post('vets/:id/approve')
  approve(@Param('id') id: string) {
    return this.vetsService.approve(id);
  }

  @Post('vets/:id/reject')
  reject(@Param('id') id: string, @Body() body: unknown) {
    return this.vetsService.reject(id, body);
  }
}
