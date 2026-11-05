import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import {
  AdminUser, Child, Content, Expert, Family, Feedback, ForumPost, ForumReport, ForumTopic, License, Organization, Payment, ReviewRequest, UsageEvent,
} from '../entities';
import { LiveModule } from '../live/live.gateway';
import { AdminController } from './admin.controller';
import { AdminService } from './admin.service';

@Module({
  imports: [
    TypeOrmModule.forFeature([Content, Family, Child, Feedback, License, Organization, Payment, ForumTopic, ForumPost, ForumReport, Expert, UsageEvent, AdminUser, ReviewRequest]),
    LiveModule,
  ],
  controllers: [AdminController],
  providers: [AdminService],
})
export class AdminModule {}
