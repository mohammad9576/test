import { Body, Controller, Get, HttpCode, Injectable, Module, NotFoundException, Param, ParseUUIDPipe, Post, UseGuards } from '@nestjs/common';
import { InjectRepository, TypeOrmModule } from '@nestjs/typeorm';
import { IsString, Length } from 'class-validator';
import { Repository } from 'typeorm';
import { CurrentFamily, FamilyAuthGuard, FamilyPrincipal } from '../common/auth';
import { Family, ForumPost, ForumReport, ForumTopic } from '../entities';

export class CreateTopicDto {
  @IsString() @Length(3, 120) title: string;
  @IsString() @Length(3, 2000) body: string;
  @IsString() @Length(1, 30) category: string;
}

export class CreatePostDto {
  @IsString() @Length(1, 2000) body: string;
}

export class ReportDto {
  @IsString() @Length(1, 200) reason: string;
}

function postDto(p: ForumPost) {
  return { id: p.id, body: p.body, authorName: p.authorName, authorRole: p.authorRole, createdAt: p.createdAt.toISOString() };
}

@Injectable()
export class ForumService {
  constructor(
    @InjectRepository(ForumTopic) private readonly topics: Repository<ForumTopic>,
    @InjectRepository(ForumPost) private readonly posts: Repository<ForumPost>,
    @InjectRepository(ForumReport) private readonly reports: Repository<ForumReport>,
    @InjectRepository(Family) private readonly families: Repository<Family>,
  ) {}

  private async displayName(familyId: string): Promise<string> {
    const f = await this.families.findOne({ where: { id: familyId } });
    const name = f?.motherName || f?.fatherName;
    return name ? `والد (${name})` : 'والد';
  }

  async list() {
    const topics = await this.topics.find({ where: { hidden: false }, relations: { posts: true }, order: { createdAt: 'DESC' }, take: 100 });
    return topics.map((t) => ({
      id: t.id,
      title: t.title,
      body: t.body,
      authorName: t.authorName,
      authorRole: t.authorRole,
      category: t.category,
      createdAt: t.createdAt.toISOString(),
      posts: t.posts.filter((p) => !p.hidden).sort((a, b) => a.createdAt.getTime() - b.createdAt.getTime()).map(postDto),
    }));
  }

  async createTopic(familyId: string, dto: CreateTopicDto) {
    const topic = await this.topics.save(this.topics.create({
      ...dto, authorName: await this.displayName(familyId), authorRole: 'PARENT', family: { id: familyId } as Family,
    }));
    return { id: topic.id, title: topic.title, body: topic.body, authorName: topic.authorName, authorRole: topic.authorRole, category: topic.category, createdAt: topic.createdAt.toISOString(), posts: [] };
  }

  async reply(familyId: string, topicId: string, dto: CreatePostDto) {
    const topic = await this.topics.findOne({ where: { id: topicId, hidden: false } });
    if (!topic) throw new NotFoundException('تاپیک یافت نشد');
    const post = await this.posts.save(this.posts.create({
      topic, body: dto.body, authorName: await this.displayName(familyId), authorRole: 'PARENT', family: { id: familyId } as Family,
    }));
    return postDto(post);
  }

  async report(familyId: string, postId: string, dto: ReportDto) {
    const post = await this.posts.findOne({ where: { id: postId } });
    if (!post) throw new NotFoundException();
    await this.reports.save(this.reports.create({ post, reporter: { id: familyId } as Family, reason: dto.reason }));
  }
}

@Controller('forum')
@UseGuards(FamilyAuthGuard)
export class ForumController {
  constructor(private readonly forum: ForumService) {}

  @Get('topics')
  list() {
    return this.forum.list();
  }

  @Post('topics')
  create(@CurrentFamily() p: FamilyPrincipal, @Body() dto: CreateTopicDto) {
    return this.forum.createTopic(p.familyId, dto);
  }

  @Post('topics/:id/posts')
  reply(@CurrentFamily() p: FamilyPrincipal, @Param('id', ParseUUIDPipe) id: string, @Body() dto: CreatePostDto) {
    return this.forum.reply(p.familyId, id, dto);
  }

  @Post('posts/:id/report')
  @HttpCode(204)
  report(@CurrentFamily() p: FamilyPrincipal, @Param('id', ParseUUIDPipe) id: string, @Body() dto: ReportDto) {
    return this.forum.report(p.familyId, id, dto);
  }
}

@Module({
  imports: [TypeOrmModule.forFeature([ForumTopic, ForumPost, ForumReport, Family])],
  controllers: [ForumController],
  providers: [ForumService],
})
export class ForumModule {}
