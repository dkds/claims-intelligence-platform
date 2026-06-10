import { Body, Controller, Get, Param, Post, Query } from '@nestjs/common';
import { ClaimsService } from './claims.service.js';

@Controller()
export class ClaimsController {
  constructor(private readonly claimsService: ClaimsService) {}

  @Get('clinics/:clinicId/claims')
  listByClinic(
    @Param('clinicId') clinicId: string,
    @Query('status') status?: string,
  ) {
    return this.claimsService.findByClinic(clinicId, status);
  }

  @Get('claims/:id')
  getById(@Param('id') id: string) {
    return this.claimsService.findById(id);
  }

  @Post('clinics/:clinicId/claims')
  create(@Param('clinicId') clinicId: string, @Body() body: unknown) {
    return this.claimsService.createManual(clinicId, body);
  }
}
