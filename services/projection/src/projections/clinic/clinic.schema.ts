import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

export type ClinicDocument = Clinic & Document;

@Schema({ _id: false, collection: 'clinics', timestamps: false })
export class Clinic {
  @Prop({ type: String, required: true })
  _id: string = '';

  @Prop({ type: String })
  name: string = '';

  @Prop({ type: String })
  contactEmail: string = '';

  @Prop({ type: String })
  status: string = '';

  @Prop({ type: String })
  updatedAt: string = '';
}

export const ClinicSchema = SchemaFactory.createForClass(Clinic);
