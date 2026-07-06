import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

@Schema({ _id: false })
class ClaimLine {
  @Prop({ type: String }) procedureCode: string = '';
  @Prop({ type: Number }) quantity: number = 0;
  @Prop({ type: Number }) requestedAmount: number = 0;
}

const ClaimLineSchema = SchemaFactory.createForClass(ClaimLine);

@Schema({ _id: false })
class FraudResult {
  @Prop({ type: Number }) score: number = 0;
  @Prop({ type: String }) riskLevel: string = '';
  @Prop({ type: [String], default: [] }) flags: string[] = [];
  @Prop({ type: String }) modelVersion: string = '';
  @Prop({ type: String }) scoredAt: string = '';
}

const FraudResultSchema = SchemaFactory.createForClass(FraudResult);

export type ClaimDocument = BffClaim & Document;

@Schema({ _id: false, collection: 'claims', timestamps: false })
export class BffClaim {
  @Prop({ type: String, required: true }) _id: string = '';
  @Prop({ type: String }) clinicId: string = '';
  @Prop({ type: String }) petId: string = '';
  @Prop({ type: String }) policyId: string = '';
  @Prop({ type: String }) origin: string = '';
  @Prop({ type: String }) sourceSessionId?: string;
  @Prop({ type: String }) status: string = '';
  @Prop({ type: String }) submittedBy: string = '';
  @Prop({ type: Number }) totalRequested: number = 0;
  @Prop({ type: [ClaimLineSchema], default: [] }) lines: ClaimLine[] = [];
  @Prop({ type: FraudResultSchema }) fraud?: FraudResult;
  @Prop({ type: String }) decision?: string;
  @Prop({ type: Number }) approvedAmount?: number;
  @Prop({ type: [String] }) reasons?: string[];
  @Prop({ type: String }) adjudicatedBy?: string;
  @Prop({ type: String }) assembledAt: string = '';
  @Prop({ type: String }) updatedAt: string = '';
  @Prop({ type: String }) correlationId: string = '';
}

export const BffClaimSchema = SchemaFactory.createForClass(BffClaim);
