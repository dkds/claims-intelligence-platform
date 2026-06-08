import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

@Schema({ _id: false })
class SessionLine {
  @Prop({ type: String })
  lineId: string = '';

  @Prop({ type: String })
  description: string = '';

  @Prop({ type: Number })
  amount: number = 0;

  @Prop({ type: String })
  category: string = '';
}

const SessionLineSchema = SchemaFactory.createForClass(SessionLine);

export type SessionDocument = Session & Document;

@Schema({ _id: false, collection: 'sessions', timestamps: false })
export class Session {
  @Prop({ type: String, required: true })
  _id: string = '';

  @Prop({ type: String })
  petId: string = '';

  @Prop({ type: String })
  vetId: string = '';

  @Prop({ type: String })
  clinicId: string = '';

  @Prop({ type: String })
  status: string = '';

  @Prop({ type: [SessionLineSchema], default: [] })
  lines: SessionLine[] = [];

  @Prop({ type: String })
  loggedAt: string = '';

  @Prop({ type: String })
  verifiedBy?: string;

  @Prop({ type: String })
  verifiedAt?: string;
}

export const SessionSchema = SchemaFactory.createForClass(Session);
