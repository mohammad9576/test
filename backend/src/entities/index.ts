import {
  Column,
  CreateDateColumn,
  Entity,
  Index,
  ManyToOne,
  OneToMany,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
} from 'typeorm';

export type ContentType = 'VIDEO' | 'BOOK' | 'MUSIC' | 'LEARNING' | 'EXPLORE' | 'ARTICLE' | 'WEEKEND_IDEA';
export type AuthorRole = 'PARENT' | 'PSYCHOLOGIST' | 'COUNSELOR' | 'PEDIATRICIAN' | 'MEDIA_EXPERT';
export type AdminRole = 'SUPER_ADMIN' | 'CONTENT_EDITOR' | 'MODERATOR' | 'FINANCE';
export type LicenseKind = 'FAMILY' | 'SCHOOL' | 'ORGANIZATION';

@Entity('organizations')
export class Organization {
  @PrimaryGeneratedColumn('uuid') id: string;
  @Column() name: string;
  @Column({ type: 'varchar', length: 20 }) kind: 'SCHOOL' | 'ORGANIZATION';
  @Column({ nullable: true }) contactPhone?: string;
  @Column({ nullable: true }) city?: string;
  @CreateDateColumn() createdAt: Date;
}

@Entity('families')
export class Family {
  @PrimaryGeneratedColumn('uuid') id: string;
  @Index({ unique: true }) @Column() phone: string;
  @Column({ default: '' }) familyName: string;
  @Column({ default: '' }) fatherName: string;
  @Column({ default: '' }) motherName: string;
  @ManyToOne(() => Organization, { nullable: true, onDelete: 'SET NULL' }) organization?: Organization | null;
  @OneToMany(() => Child, (c) => c.family, { cascade: true }) children: Child[];
  @Column({ type: 'timestamptz', nullable: true }) lastSyncAt?: Date | null;
  @CreateDateColumn() createdAt: Date;
}

@Entity('children')
@Index(['family', 'localId'], { unique: true })
export class Child {
  @PrimaryGeneratedColumn('uuid') id: string;
  @ManyToOne(() => Family, (f) => f.children, { onDelete: 'CASCADE' }) family: Family;
  @Column({ type: 'bigint' }) localId: string;
  @Column() name: string;
  @Column({ type: 'date' }) birthDate: string;
  @Column() gender: string;
  @Column() avatar: string;
  @Column({ type: 'text', array: true, default: '{}' }) interests: string[];
  @Column({ default: '' }) talents: string;
  @Column({ default: '' }) abilities: string;
  @Column({ default: 0 }) stars: number;
  @Column({ default: 60 }) dailyLimitMinutes: number;
  @Column({ default: true }) bedtimeEnabled: boolean;
  @Column({ default: 1260 }) bedtimeStartMinutes: number;
  @Column({ default: 420 }) bedtimeEndMinutes: number;
  @Column({ default: 2 }) maxViolence: number;
  @Column({ default: 2 }) maxFear: number;
  @Column({ default: 1 }) maxAbnormality: number;
  @UpdateDateColumn() updatedAt: Date;
}

@Entity('content')
export class Content {
  @PrimaryGeneratedColumn('uuid') id: string;
  @Index() @Column({ type: 'varchar', length: 20 }) type: ContentType;
  @Column() title: string;
  @Column({ type: 'text', default: '' }) description: string;
  @Column({ type: 'text', default: '' }) body: string;
  @Column({ default: '' }) category: string;
  @Column({ default: '✨' }) emoji: string;
  @Column({ type: 'bigint', default: '4290372811' }) colorStart: string;
  @Column({ type: 'bigint', default: '4284463498' }) colorEnd: string;
  @Column({ type: 'varchar', nullable: true }) mediaUrl: string | null;
  @Column({ default: 0 }) durationMinutes: number;
  // The five Kodomo tags + two positive indicators used by the radar chart.
  @Column({ default: 3 }) minAge: number;
  @Column({ default: 18 }) maxAge: number;
  @Column({ default: 1 }) violence: number;
  @Column({ default: 1 }) fear: number;
  @Column({ default: 3 }) educational: number;
  @Column({ default: 1 }) abnormality: number;
  @Column({ default: 3 }) creativity: number;
  @Column({ default: 3 }) familyValues: number;
  @Column({ type: 'text', array: true, default: '{}' }) positives: string[];
  @Column({ type: 'text', array: true, default: '{}' }) negatives: string[];
  @Column({ type: 'text', default: '' }) expertReview: string;
  @Column({ default: '' }) expertName: string;
  @Column({ type: 'text', array: true, default: '{}' }) interests: string[];
  @Column({ default: '' }) author: string;
  @Column({ type: 'jsonb', default: () => "'[]'" }) pages: { index: number; text: string; emoji: string }[];
  @Column({ default: false }) published: boolean;
  @CreateDateColumn() createdAt: Date;
  @Index() @UpdateDateColumn() updatedAt: Date;
}

@Entity('feedback')
export class Feedback {
  @PrimaryGeneratedColumn('uuid') id: string;
  @ManyToOne(() => Content, { onDelete: 'CASCADE' }) content: Content;
  @ManyToOne(() => Family, { onDelete: 'CASCADE' }) family: Family;
  @Column() liked: boolean;
  @Column({ type: 'text', default: '' }) comment: string;
  @Column() childAge: number;
  @Column({ default: false }) approved: boolean;
  @CreateDateColumn() createdAt: Date;
}

@Entity('experts')
export class Expert {
  @PrimaryGeneratedColumn('uuid') id: string;
  @Column() fullName: string;
  @Index({ unique: true }) @Column() phone: string;
  @Column({ type: 'varchar', length: 20 }) role: AuthorRole;
  @Column({ default: '' }) licenseNumber: string;
  @Column({ default: false }) verified: boolean;
  @CreateDateColumn() createdAt: Date;
}

@Entity('forum_topics')
export class ForumTopic {
  @PrimaryGeneratedColumn('uuid') id: string;
  @Column() title: string;
  @Column({ type: 'text' }) body: string;
  @Column() authorName: string;
  @Column({ type: 'varchar', length: 20, default: 'PARENT' }) authorRole: AuthorRole;
  @ManyToOne(() => Family, { nullable: true, onDelete: 'SET NULL' }) family?: Family | null;
  @Column({ default: 'عمومی' }) category: string;
  @Column({ default: false }) hidden: boolean;
  @OneToMany(() => ForumPost, (p) => p.topic) posts: ForumPost[];
  @CreateDateColumn() createdAt: Date;
}

@Entity('forum_posts')
export class ForumPost {
  @PrimaryGeneratedColumn('uuid') id: string;
  @ManyToOne(() => ForumTopic, (t) => t.posts, { onDelete: 'CASCADE' }) topic: ForumTopic;
  @Column({ type: 'text' }) body: string;
  @Column() authorName: string;
  @Column({ type: 'varchar', length: 20, default: 'PARENT' }) authorRole: AuthorRole;
  @ManyToOne(() => Family, { nullable: true, onDelete: 'SET NULL' }) family?: Family | null;
  @ManyToOne(() => Expert, { nullable: true, onDelete: 'SET NULL' }) expert?: Expert | null;
  @Column({ default: false }) hidden: boolean;
  @CreateDateColumn() createdAt: Date;
}

@Entity('forum_reports')
export class ForumReport {
  @PrimaryGeneratedColumn('uuid') id: string;
  @ManyToOne(() => ForumPost, { onDelete: 'CASCADE' }) post: ForumPost;
  @ManyToOne(() => Family, { nullable: true, onDelete: 'SET NULL' }) reporter?: Family | null;
  @Column({ default: '' }) reason: string;
  @Column({ type: 'varchar', length: 20, default: 'OPEN' }) status: 'OPEN' | 'DISMISSED' | 'ACTIONED';
  @CreateDateColumn() createdAt: Date;
}

@Entity('licenses')
export class License {
  @PrimaryGeneratedColumn('uuid') id: string;
  @Column({ type: 'varchar', length: 20 }) kind: LicenseKind;
  @ManyToOne(() => Family, { nullable: true, onDelete: 'CASCADE' }) family?: Family | null;
  @ManyToOne(() => Organization, { nullable: true, onDelete: 'CASCADE' }) organization?: Organization | null;
  @Column({ default: 1 }) seats: number;
  @Column({ type: 'timestamptz' }) startsAt: Date;
  @Column({ type: 'timestamptz' }) endsAt: Date;
  @Column({ type: 'bigint', default: '0' }) priceRials: string;
  @Column({ default: true }) active: boolean;
  @CreateDateColumn() createdAt: Date;
}

@Entity('payments')
export class Payment {
  @PrimaryGeneratedColumn('uuid') id: string;
  @ManyToOne(() => License, { onDelete: 'SET NULL', nullable: true }) license?: License | null;
  @Column({ type: 'bigint' }) amountRials: string;
  @Column({ default: '' }) gatewayRef: string;
  @Column({ type: 'varchar', length: 20, default: 'PAID' }) status: 'PAID' | 'REFUNDED' | 'FAILED';
  @CreateDateColumn() createdAt: Date;
}

@Entity('usage_events')
@Index(['family', 'createdAt'])
export class UsageEvent {
  @PrimaryGeneratedColumn('increment', { type: 'bigint' }) id: string;
  @ManyToOne(() => Family, { onDelete: 'CASCADE' }) family: Family;
  @Column({ type: 'bigint' }) childLocalId: string;
  @Column() childName: string;
  @Column() category: string;
  @Column({ default: '' }) title: string;
  @Column({ type: 'int' }) usedSecondsToday: number;
  @Column({ type: 'int' }) limitSeconds: number;
  @CreateDateColumn() createdAt: Date;
}

@Entity('admin_users')
export class AdminUser {
  @PrimaryGeneratedColumn('uuid') id: string;
  @Index({ unique: true }) @Column() email: string;
  @Column() passwordHash: string;
  @Column() fullName: string;
  @Column({ type: 'varchar', length: 20 }) role: AdminRole;
  @CreateDateColumn() createdAt: Date;
}

export type ReviewRequestStatus = 'PENDING' | 'IN_REVIEW' | 'DONE' | 'REJECTED';

/** A title a parent approved for their child although no expert (kodoumo) evaluation existed yet. */
@Entity('review_requests')
export class ReviewRequest {
  @PrimaryGeneratedColumn('uuid') id: string;
  @Column() title: string;
  @Column({ type: 'varchar', length: 30, default: 'VIDEO' }) kind: string;
  @Column({ type: 'text', default: '' }) url: string;
  @Column({ type: 'text', default: '' }) note: string;
  @Column({ default: 0 }) childAge: number;
  @Column({ default: '' }) deviceId: string;
  @Column({ default: 1 }) requests: number;
  @Column({ type: 'varchar', length: 20, default: 'PENDING' }) status: ReviewRequestStatus;
  @Column({ type: 'text', default: '' }) adminNote: string;
  @CreateDateColumn() createdAt: Date;
  @UpdateDateColumn() updatedAt: Date;
}

export const ALL_ENTITIES = [
  Organization, Family, Child, Content, Feedback, Expert, ForumTopic, ForumPost, ForumReport, License, Payment, UsageEvent, AdminUser, ReviewRequest,
];
