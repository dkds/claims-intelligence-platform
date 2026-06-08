import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

export type PetDocument = Pet & Document;

@Schema({ _id: false, collection: 'pets', timestamps: false })
export class Pet {
  @Prop({ type: String, required: true })
  _id: string = '';

  @Prop({ type: String })
  clinicId: string = '';

  @Prop({ type: String })
  ownerId: string = '';

  @Prop({ type: String })
  name: string = '';

  @Prop({ type: String })
  species: string = '';

  @Prop({ type: String })
  breed?: string;

  @Prop({ type: String })
  status: string = '';
}

export const PetSchema = SchemaFactory.createForClass(Pet);
