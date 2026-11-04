import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import * as bcrypt from 'bcryptjs';
import { MoreThan, Repository } from 'typeorm';
import { toCatalogDto } from '../content/content.module';
import {
  AdminUser, Child, Content, Expert, Family, Feedback, ForumPost, ForumReport, ForumTopic, License, Organization, Payment, UsageEvent,
} from '../entities';
import { LiveGateway } from '../live/live.gateway';
import {
  AdminUserInput, ContentInput, ExpertInput, ExpertReplyInput, LicenseInput, OrganizationInput,
} from './admin.dto';

@Injectable()
export class AdminService {
  constructor(
    @InjectRepository(Content) private readonly content: Repository<Content>,
    @InjectRepository(Family) private readonly families: Repository<Family>,
    @InjectRepository(Child) private readonly children: Repository<Child>,
    @InjectRepository(Feedback) private readonly feedback: Repository<Feedback>,
    @InjectRepository(License) private readonly licenses: Repository<License>,
    @InjectRepository(Organization) private readonly organizations: Repository<Organization>,
    @InjectRepository(Payment) private readonly payments: Repository<Payment>,
    @InjectRepository(ForumTopic) private readonly topics: Repository<ForumTopic>,
    @InjectRepository(ForumPost) private readonly posts: Repository<ForumPost>,
    @InjectRepository(ForumReport) private readonly reports: Repository<ForumReport>,
    @InjectRepository(Expert) private readonly experts: Repository<Expert>,
    @InjectRepository(UsageEvent) private readonly usage: Repository<UsageEvent>,
    @InjectRepository(AdminUser) private readonly admins: Repository<AdminUser>,
    private readonly live: LiveGateway,
  ) {}

  // ---------- Dashboard ----------
  async stats() {
    const dayAgo = new Date(Date.now() - 24 * 3600 * 1000);
    const [families, children, content, published, openReports, activeLicenses, pendingExperts, revenue, usageByCategory] = await Promise.all([
      this.families.count(),
      this.children.count(),
      this.content.count(),
      this.content.count({ where: { published: true } }),
      this.reports.count({ where: { status: 'OPEN' } }),
      this.licenses.count({ where: { active: true, endsAt: MoreThan(new Date()) } }),
      this.experts.count({ where: { verified: false } }),
      this.payments.createQueryBuilder('p').select('COALESCE(SUM(p.amountRials), 0)', 'total').where("p.status = 'PAID'").getRawOne<{ total: string }>(),
      this.usage.createQueryBuilder('u')
        .select('u.category', 'category')
        .addSelect('COUNT(*)', 'heartbeats')
        .where('u.createdAt > :since', { since: dayAgo })
        .groupBy('u.category')
        .getRawMany<{ category: string; heartbeats: string }>(),
    ]);
    return {
      families, children, content, published, openReports, activeLicenses, pendingExperts,
      revenueRials: revenue?.total ?? '0',
      liveFamilies: this.live.connectedFamilies(),
      // Each heartbeat represents 30 seconds of usage on the device.
      usageMinutesLast24h: usageByCategory.map((u) => ({ category: u.category, minutes: Math.round(Number(u.heartbeats) / 2) })),
    };
  }

  // ---------- Content CMS ----------
  listContent(type?: string) {
    return this.content.find({ where: type ? { type: type as Content['type'] } : {}, order: { updatedAt: 'DESC' }, take: 500 });
  }

  async getContent(id: string) {
    const c = await this.content.findOne({ where: { id } });
    if (!c) throw new NotFoundException();
    return c;
  }

  createContent(input: ContentInput) {
    if (input.minAge > input.maxAge) throw new BadRequestException('حداقل سن نباید از حداکثر سن بیشتر باشد');
    return this.content.save(this.content.create({ ...input, mediaUrl: input.mediaUrl ?? null }));
  }

  async updateContent(id: string, input: ContentInput) {
    const c = await this.getContent(id);
    if (input.minAge > input.maxAge) throw new BadRequestException('حداقل سن نباید از حداکثر سن بیشتر باشد');
    Object.assign(c, input, { mediaUrl: input.mediaUrl ?? null });
    return this.content.save(c);
  }

  async deleteContent(id: string) {
    await this.content.delete(id);
  }

  async previewCatalogItem(id: string) {
    return toCatalogDto(await this.getContent(id));
  }

  // ---------- Feedback moderation ----------
  pendingFeedback() {
    return this.feedback.find({ where: { approved: false }, relations: { content: true }, order: { createdAt: 'DESC' }, take: 200 });
  }

  async setFeedbackApproval(id: string, approved: boolean) {
    if (approved) await this.feedback.update(id, { approved: true });
    else await this.feedback.delete(id);
  }

  // ---------- Families, organizations & licenses ----------
  listFamilies() {
    return this.families.find({ relations: { children: true, organization: true }, order: { createdAt: 'DESC' }, take: 500 });
  }

  listOrganizations() {
    return this.organizations.find({ order: { createdAt: 'DESC' } });
  }

  createOrganization(input: OrganizationInput) {
    return this.organizations.save(this.organizations.create(input));
  }

  listLicenses() {
    return this.licenses.find({ relations: { family: true, organization: true }, order: { createdAt: 'DESC' }, take: 500 });
  }

  async createLicense(input: LicenseInput) {
    if (input.kind === 'FAMILY' && !input.familyId) throw new BadRequestException('برای لایسنس خانوادگی، خانواده الزامی است');
    if (input.kind !== 'FAMILY' && !input.organizationId) throw new BadRequestException('برای لایسنس سازمانی، سازمان الزامی است');
    const license = await this.licenses.save(this.licenses.create({
      kind: input.kind,
      family: input.familyId ? ({ id: input.familyId } as Family) : null,
      organization: input.organizationId ? ({ id: input.organizationId } as Organization) : null,
      seats: input.seats,
      startsAt: new Date(input.startsAt),
      endsAt: new Date(input.endsAt),
      priceRials: input.priceRials,
    }));
    if (BigInt(input.priceRials) > 0n) {
      await this.payments.save(this.payments.create({ license, amountRials: input.priceRials, gatewayRef: `manual-${license.id.slice(0, 8)}` }));
    }
    return license;
  }

  async setLicenseActive(id: string, active: boolean) {
    await this.licenses.update(id, { active });
  }

  async financialReport() {
    const rows = await this.payments.createQueryBuilder('p')
      .select("to_char(date_trunc('month', p.createdAt), 'YYYY-MM')", 'month')
      .addSelect("COALESCE(SUM(CASE WHEN p.status = 'PAID' THEN p.amountRials ELSE 0 END), 0)", 'paid')
      .addSelect("COALESCE(SUM(CASE WHEN p.status = 'REFUNDED' THEN p.amountRials ELSE 0 END), 0)", 'refunded')
      .addSelect('COUNT(*)', 'transactions')
      .groupBy('month')
      .orderBy('month', 'DESC')
      .limit(24)
      .getRawMany<{ month: string; paid: string; refunded: string; transactions: string }>();
    return rows;
  }

  // ---------- Forum moderation & experts ----------
  openReports() {
    return this.reports.find({ where: { status: 'OPEN' }, relations: { post: { topic: true } }, order: { createdAt: 'DESC' }, take: 200 });
  }

  async decideReport(id: string, status: 'DISMISSED' | 'ACTIONED') {
    const report = await this.reports.findOne({ where: { id }, relations: { post: true } });
    if (!report) throw new NotFoundException();
    report.status = status;
    await this.reports.save(report);
    if (status === 'ACTIONED') await this.posts.update(report.post.id, { hidden: true });
  }

  listTopics() {
    return this.topics.find({ relations: { posts: true }, order: { createdAt: 'DESC' }, take: 200 });
  }

  async setTopicHidden(id: string, hidden: boolean) {
    await this.topics.update(id, { hidden });
  }

  listExperts() {
    return this.experts.find({ order: { createdAt: 'DESC' } });
  }

  createExpert(input: ExpertInput) {
    return this.experts.save(this.experts.create(input));
  }

  async verifyExpert(id: string, verified: boolean) {
    await this.experts.update(id, { verified });
  }

  async expertReply(topicId: string, input: ExpertReplyInput) {
    const [topic, expert] = await Promise.all([
      this.topics.findOne({ where: { id: topicId } }),
      this.experts.findOne({ where: { id: input.expertId } }),
    ]);
    if (!topic || !expert) throw new NotFoundException();
    if (!expert.verified) throw new BadRequestException('کارشناس هنوز تأیید نشده است');
    return this.posts.save(this.posts.create({ topic, expert, body: input.body, authorName: expert.fullName, authorRole: expert.role }));
  }

  // ---------- Admin users ----------
  listAdmins() {
    return this.admins.find({ select: { id: true, email: true, fullName: true, role: true, createdAt: true } });
  }

  async createAdmin(input: AdminUserInput) {
    const saved = await this.admins.save(this.admins.create({
      email: input.email.toLowerCase(), fullName: input.fullName, role: input.role, passwordHash: await bcrypt.hash(input.password, 12),
    }));
    return { id: saved.id, email: saved.email, fullName: saved.fullName, role: saved.role };
  }
}
