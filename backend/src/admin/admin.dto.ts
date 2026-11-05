import { Type } from 'class-transformer';
import {
  ArrayMaxSize, IsArray, IsBoolean, IsDateString, IsIn, IsInt, IsNumberString, IsOptional, IsString, IsUUID, Length, Max, Min, ValidateNested,
} from 'class-validator';

const CONTENT_TYPES = ['VIDEO', 'BOOK', 'MUSIC', 'LEARNING', 'EXPLORE', 'ARTICLE', 'WEEKEND_IDEA'] as const;
const ROLES = ['PSYCHOLOGIST', 'COUNSELOR', 'PEDIATRICIAN', 'MEDIA_EXPERT'] as const;

export class ContentPageInput {
  @IsInt() @Min(0) index: number;
  @IsString() @Length(1, 2000) text: string;
  @IsString() @Length(0, 16) emoji: string;
}

export class ContentInput {
  @IsIn(CONTENT_TYPES) type: (typeof CONTENT_TYPES)[number];
  @IsString() @Length(1, 160) title: string;
  @IsString() @Length(0, 2000) description: string;
  @IsString() @Length(0, 20000) body: string;
  @IsString() @Length(0, 60) category: string;
  @IsString() @Length(1, 16) emoji: string;
  @IsNumberString() colorStart: string;
  @IsNumberString() colorEnd: string;
  @IsOptional() @IsString() mediaUrl?: string | null;
  @IsInt() @Min(0) durationMinutes: number;
  @IsInt() @Min(0) @Max(18) minAge: number;
  @IsInt() @Min(0) @Max(18) maxAge: number;
  @IsInt() @Min(1) @Max(5) violence: number;
  @IsInt() @Min(1) @Max(5) fear: number;
  @IsInt() @Min(1) @Max(5) educational: number;
  @IsInt() @Min(1) @Max(5) abnormality: number;
  @IsInt() @Min(1) @Max(5) creativity: number;
  @IsInt() @Min(1) @Max(5) familyValues: number;
  @IsArray() @IsString({ each: true }) @ArrayMaxSize(10) positives: string[];
  @IsArray() @IsString({ each: true }) @ArrayMaxSize(10) negatives: string[];
  @IsString() @Length(0, 4000) expertReview: string;
  @IsString() @Length(0, 120) expertName: string;
  @IsArray() @IsString({ each: true }) @ArrayMaxSize(9) interests: string[];
  @IsString() @Length(0, 120) author: string;
  @IsArray() @ValidateNested({ each: true }) @Type(() => ContentPageInput) @ArrayMaxSize(200) pages: ContentPageInput[];
  @IsBoolean() published: boolean;
}

export class UploadRequest {
  @IsString() @Length(1, 200) fileName: string;
}

export class LicenseInput {
  @IsIn(['FAMILY', 'SCHOOL', 'ORGANIZATION']) kind: 'FAMILY' | 'SCHOOL' | 'ORGANIZATION';
  @IsOptional() @IsUUID() familyId?: string;
  @IsOptional() @IsUUID() organizationId?: string;
  @IsInt() @Min(1) @Max(100000) seats: number;
  @IsDateString() startsAt: string;
  @IsDateString() endsAt: string;
  @IsNumberString() priceRials: string;
}

export class OrganizationInput {
  @IsString() @Length(2, 120) name: string;
  @IsIn(['SCHOOL', 'ORGANIZATION']) kind: 'SCHOOL' | 'ORGANIZATION';
  @IsOptional() @IsString() contactPhone?: string;
  @IsOptional() @IsString() city?: string;
}

export class ExpertInput {
  @IsString() @Length(3, 80) fullName: string;
  @IsString() @Length(10, 16) phone: string;
  @IsIn(ROLES) role: (typeof ROLES)[number];
  @IsString() @Length(0, 40) licenseNumber: string;
}

export class ExpertReplyInput {
  @IsUUID() expertId: string;
  @IsString() @Length(1, 4000) body: string;
}

export class ReportDecision {
  @IsIn(['DISMISSED', 'ACTIONED']) status: 'DISMISSED' | 'ACTIONED';
}

export class AdminUserInput {
  @IsString() @Length(3, 120) email: string;
  @IsString() @Length(8, 128) password: string;
  @IsString() @Length(2, 80) fullName: string;
  @IsIn(['SUPER_ADMIN', 'CONTENT_EDITOR', 'MODERATOR', 'FINANCE']) role: 'SUPER_ADMIN' | 'CONTENT_EDITOR' | 'MODERATOR' | 'FINANCE';
}

export class ReviewRequestDecision {
  @IsIn(['PENDING', 'IN_REVIEW', 'DONE', 'REJECTED']) status: 'PENDING' | 'IN_REVIEW' | 'DONE' | 'REJECTED';
  @IsString() @Length(0, 1000) adminNote: string;
}
