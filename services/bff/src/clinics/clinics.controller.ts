import { Body, Controller, Get, Post } from '@nestjs/common';
import { ClinicsService } from './clinics.service.js';

@Controller()
export class ClinicsController {
  constructor(private readonly clinicsService: ClinicsService) {}

  @Get('clinics')
  listAll() {
    return this.clinicsService.findAll();
  }

  @Post('clinics')
  create(@Body() body: unknown) {
    return this.clinicsService.create(body);
  }
}
