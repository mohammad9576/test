import { Body, Controller, Get, Injectable, Module, NotFoundException, Put, UseGuards } from '@nestjs/common';
import { InjectRepository, TypeOrmModule } from '@nestjs/typeorm';
import { Type } from 'class-transformer';
import { ArrayMaxSize, IsArray, IsBoolean, IsDateString, IsInt, IsString, Length, Max, Min, ValidateNested } from 'class-validator';
import { DataSource, Repository } from 'typeorm';
import { CurrentFamily, FamilyAuthGuard, FamilyPrincipal } from '../common/auth';
import { Child, Family } from '../entities';

export class ChildProfileDto {
  @IsInt() localId: number;
  @IsString() @Length(1, 30) name: string;
  @IsDateString() birthDate: string;
  @IsString() gender: string;
  @IsString() avatar: string;
  @IsArray() @IsString({ each: true }) @ArrayMaxSize(20) interests: string[];
  @IsString() @Length(0, 200) talents: string;
  @IsString() @Length(0, 200) abilities: string;
  @IsInt() @Min(0) stars: number;
  @IsInt() @Min(5) @Max(120) dailyLimitMinutes: number;
  @IsBoolean() bedtimeEnabled: boolean;
  @IsInt() @Min(0) @Max(1439) bedtimeStartMinutes: number;
  @IsInt() @Min(0) @Max(1439) bedtimeEndMinutes: number;
  @IsInt() @Min(1) @Max(5) maxViolence: number;
  @IsInt() @Min(1) @Max(5) maxFear: number;
  @IsInt() @Min(1) @Max(5) maxAbnormality: number;
}

export class FamilySyncDto {
  @IsString() @Length(0, 40) familyName: string;
  @IsString() @Length(0, 30) fatherName: string;
  @IsString() @Length(0, 30) motherName: string;
  @IsArray() @ArrayMaxSize(12) @ValidateNested({ each: true }) @Type(() => ChildProfileDto) children: ChildProfileDto[];
}

@Injectable()
export class FamiliesService {
  constructor(
    private readonly dataSource: DataSource,
    @InjectRepository(Family) private readonly families: Repository<Family>,
  ) {}

  async sync(familyId: string, dto: FamilySyncDto): Promise<{ familyId: string; syncedAt: string }> {
    const syncedAt = new Date();
    await this.dataSource.transaction(async (em) => {
      const family = await em.findOne(Family, { where: { id: familyId } });
      if (!family) throw new NotFoundException('خانواده یافت نشد');
      family.familyName = dto.familyName;
      family.fatherName = dto.fatherName;
      family.motherName = dto.motherName;
      family.lastSyncAt = syncedAt;
      await em.save(family);

      const existing = await em.find(Child, { where: { family: { id: familyId } } });
      const incomingIds = new Set(dto.children.map((c) => String(c.localId)));
      const toRemove = existing.filter((c) => !incomingIds.has(c.localId));
      if (toRemove.length > 0) await em.remove(toRemove);

      for (const c of dto.children) {
        const child = existing.find((e) => e.localId === String(c.localId)) ?? em.create(Child, { family, localId: String(c.localId) });
        Object.assign(child, { ...c, localId: String(c.localId) });
        await em.save(child);
      }
    });
    return { familyId, syncedAt: syncedAt.toISOString() };
  }

  async get(familyId: string) {
    const family = await this.families.findOne({ where: { id: familyId }, relations: { children: true } });
    if (!family) throw new NotFoundException();
    return family;
  }
}

@Controller('families')
@UseGuards(FamilyAuthGuard)
export class FamiliesController {
  constructor(private readonly service: FamiliesService) {}

  @Put('me')
  sync(@CurrentFamily() principal: FamilyPrincipal, @Body() dto: FamilySyncDto) {
    return this.service.sync(principal.familyId, dto);
  }

  @Get('me')
  me(@CurrentFamily() principal: FamilyPrincipal) {
    return this.service.get(principal.familyId);
  }
}

@Module({
  imports: [TypeOrmModule.forFeature([Family, Child])],
  controllers: [FamiliesController],
  providers: [FamiliesService],
})
export class FamiliesModule {}
