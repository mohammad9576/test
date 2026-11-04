import { Body, Controller, Get, HttpCode, Injectable, Module, NotFoundException, Post, Query, UseGuards } from '@nestjs/common';
import { InjectRepository, TypeOrmModule } from '@nestjs/typeorm';
import { IsBoolean, IsInt, IsString, Length, Max, Min } from 'class-validator';
import { MoreThan, Repository } from 'typeorm';
import { CurrentFamily, FamilyAuthGuard, FamilyPrincipal } from '../common/auth';
import { Content, Family, Feedback } from '../entities';

export class FeedbackDto {
  @IsString() contentId: string;
  @IsBoolean() liked: boolean;
  @IsString() @Length(0, 280) comment: string;
  @IsInt() @Min(0) @Max(18) childAge: number;
}

/** Maps a content row to the exact JSON contract consumed by the Android `ContentDto`. */
export function toCatalogDto(c: Content, likes = 0, dislikes = 0) {
  return {
    id: c.id,
    type: c.type,
    title: c.title,
    description: c.description,
    body: c.body,
    category: c.category,
    emoji: c.emoji,
    colorStart: Number(c.colorStart),
    colorEnd: Number(c.colorEnd),
    mediaUrl: c.mediaUrl,
    durationMinutes: c.durationMinutes,
    rating: {
      minAge: c.minAge, maxAge: c.maxAge, violence: c.violence, fear: c.fear, educational: c.educational,
      abnormality: c.abnormality, creativity: c.creativity, familyValues: c.familyValues,
    },
    positives: c.positives,
    negatives: c.negatives,
    expertReview: c.expertReview,
    expertName: c.expertName,
    peerLikes: likes,
    peerDislikes: dislikes,
    interests: c.interests,
    author: c.author,
    pages: c.pages,
  };
}

@Injectable()
export class ContentService {
  constructor(
    @InjectRepository(Content) private readonly content: Repository<Content>,
    @InjectRepository(Feedback) private readonly feedback: Repository<Feedback>,
  ) {}

  async catalog(since?: string) {
    const sinceDate = since ? new Date(since) : undefined;
    const where = sinceDate && !Number.isNaN(sinceDate.getTime()) ? { published: true, updatedAt: MoreThan(sinceDate) } : { published: true };
    const items = await this.content.find({ where, order: { updatedAt: 'ASC' }, take: 500 });
    const tallies = items.length === 0 ? [] : await this.feedback
      .createQueryBuilder('f')
      .select('f.contentId', 'contentId')
      .addSelect('SUM(CASE WHEN f.liked THEN 1 ELSE 0 END)', 'likes')
      .addSelect('SUM(CASE WHEN f.liked THEN 0 ELSE 1 END)', 'dislikes')
      .where('f.contentId IN (:...ids)', { ids: items.map((i) => i.id) })
      .groupBy('f.contentId')
      .getRawMany<{ contentId: string; likes: string; dislikes: string }>();
    const byId = new Map(tallies.map((t) => [t.contentId, t]));
    return {
      items: items.map((c) => toCatalogDto(c, Number(byId.get(c.id)?.likes ?? 0), Number(byId.get(c.id)?.dislikes ?? 0))),
      updatedAt: new Date().toISOString(),
    };
  }

  async addFeedback(familyId: string, dto: FeedbackDto): Promise<void> {
    const content = await this.content.findOne({ where: { id: dto.contentId } });
    if (!content) throw new NotFoundException('محتوا یافت نشد');
    await this.feedback.save(this.feedback.create({
      content, family: { id: familyId } as Family, liked: dto.liked, comment: dto.comment, childAge: dto.childAge,
    }));
  }
}

@Controller('content')
export class ContentController {
  constructor(private readonly service: ContentService) {}

  @Get('catalog')
  @UseGuards(FamilyAuthGuard)
  catalog(@Query('since') since?: string) {
    return this.service.catalog(since);
  }

  @Post('feedback')
  @HttpCode(204)
  @UseGuards(FamilyAuthGuard)
  feedback(@CurrentFamily() principal: FamilyPrincipal, @Body() dto: FeedbackDto) {
    return this.service.addFeedback(principal.familyId, dto);
  }
}

@Module({
  imports: [TypeOrmModule.forFeature([Content, Feedback])],
  controllers: [ContentController],
  providers: [ContentService],
  exports: [ContentService],
})
export class ContentModule {}
