import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

export type ClinicDocument = BffClinic & Document;

@Schema({ _id: false, collection: 'clinics', timestamps: false })
export class BffClinic {
  @Prop({ type: String, required: true }) _id: string = '';
  @Prop({ type: String }) name: string = '';
  @Prop({ type: String }) contactEmail: string = '';
  @Prop({ type: String }) status: string = '';
  @Prop({ type: String }) updatedAt: string = '';
}

export const BffClinicSchema = SchemaFactory.createForClass(BffClinic);
