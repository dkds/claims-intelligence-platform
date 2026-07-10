import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

export type VetDocument = BffVet & Document;

@Schema({ _id: false, collection: 'vets', timestamps: false })
export class BffVet {
  @Prop({ type: String, required: true }) _id: string = '';
  @Prop({ type: String }) clinicId: string = '';
  @Prop({ type: String }) firstName: string = '';
  @Prop({ type: String }) lastName: string = '';
  @Prop({ type: String }) status: string = '';
  @Prop({ type: String }) rejectionReason?: string;
}

export const BffVetSchema = SchemaFactory.createForClass(BffVet);
