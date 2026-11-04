import { Body, Controller, Delete, Get, HttpCode, Param, ParseBoolPipe, ParseUUIDPipe, Post, Put, Query, UseGuards } from '@nestjs/common';
import { AdminAuthGuard, AdminRoles } from '../common/auth';
import { StorageService } from '../storage/storage.module';
import {
  AdminUserInput, ContentInput, ExpertInput, ExpertReplyInput, LicenseInput, OrganizationInput, ReportDecision, UploadRequest,
} from './admin.dto';
import { AdminService } from './admin.service';

@Controller('admin')
@UseGuards(AdminAuthGuard)
export class AdminController {
  constructor(private readonly admin: AdminService, private readonly storage: StorageService) {}

  @Get('stats') stats() { return this.admin.stats(); }

  // Content CMS
  @Get('content') @AdminRoles('CONTENT_EDITOR') listContent(@Query('type') type?: string) { return this.admin.listContent(type); }
  @Get('content/:id') @AdminRoles('CONTENT_EDITOR') getContent(@Param('id', ParseUUIDPipe) id: string) { return this.admin.getContent(id); }
  @Get('content/:id/preview') @AdminRoles('CONTENT_EDITOR') preview(@Param('id', ParseUUIDPipe) id: string) { return this.admin.previewCatalogItem(id); }
  @Post('content') @AdminRoles('CONTENT_EDITOR') createContent(@Body() input: ContentInput) { return this.admin.createContent(input); }
  @Put('content/:id') @AdminRoles('CONTENT_EDITOR') updateContent(@Param('id', ParseUUIDPipe) id: string, @Body() input: ContentInput) { return this.admin.updateContent(id, input); }
  @Delete('content/:id') @HttpCode(204) @AdminRoles('CONTENT_EDITOR') deleteContent(@Param('id', ParseUUIDPipe) id: string) { return this.admin.deleteContent(id); }
  @Post('uploads') @AdminRoles('CONTENT_EDITOR') upload(@Body() body: UploadRequest) { return this.storage.presignUpload(body.fileName); }

  // Feedback
  @Get('feedback/pending') @AdminRoles('MODERATOR') pendingFeedback() { return this.admin.pendingFeedback(); }
  @Post('feedback/:id/approval') @HttpCode(204) @AdminRoles('MODERATOR')
  approveFeedback(@Param('id', ParseUUIDPipe) id: string, @Query('approved', ParseBoolPipe) approved: boolean) { return this.admin.setFeedbackApproval(id, approved); }

  // Families, organisations, licenses, finance
  @Get('families') listFamilies() { return this.admin.listFamilies(); }
  @Get('organizations') listOrganizations() { return this.admin.listOrganizations(); }
  @Post('organizations') @AdminRoles('FINANCE') createOrganization(@Body() input: OrganizationInput) { return this.admin.createOrganization(input); }
  @Get('licenses') @AdminRoles('FINANCE') listLicenses() { return this.admin.listLicenses(); }
  @Post('licenses') @AdminRoles('FINANCE') createLicense(@Body() input: LicenseInput) { return this.admin.createLicense(input); }
  @Post('licenses/:id/active') @HttpCode(204) @AdminRoles('FINANCE')
  setLicenseActive(@Param('id', ParseUUIDPipe) id: string, @Query('value', ParseBoolPipe) value: boolean) { return this.admin.setLicenseActive(id, value); }
  @Get('finance/monthly') @AdminRoles('FINANCE') finance() { return this.admin.financialReport(); }

  // Forum moderation & experts
  @Get('forum/reports') @AdminRoles('MODERATOR') reports() { return this.admin.openReports(); }
  @Post('forum/reports/:id') @HttpCode(204) @AdminRoles('MODERATOR')
  decide(@Param('id', ParseUUIDPipe) id: string, @Body() body: ReportDecision) { return this.admin.decideReport(id, body.status); }
  @Get('forum/topics') @AdminRoles('MODERATOR') topics() { return this.admin.listTopics(); }
  @Post('forum/topics/:id/hidden') @HttpCode(204) @AdminRoles('MODERATOR')
  hideTopic(@Param('id', ParseUUIDPipe) id: string, @Query('value', ParseBoolPipe) value: boolean) { return this.admin.setTopicHidden(id, value); }
  @Post('forum/topics/:id/expert-reply') @AdminRoles('MODERATOR')
  expertReply(@Param('id', ParseUUIDPipe) id: string, @Body() body: ExpertReplyInput) { return this.admin.expertReply(id, body); }
  @Get('experts') @AdminRoles('MODERATOR') experts() { return this.admin.listExperts(); }
  @Post('experts') @AdminRoles('MODERATOR') createExpert(@Body() input: ExpertInput) { return this.admin.createExpert(input); }
  @Post('experts/:id/verified') @HttpCode(204) @AdminRoles('MODERATOR')
  verifyExpert(@Param('id', ParseUUIDPipe) id: string, @Query('value', ParseBoolPipe) value: boolean) { return this.admin.verifyExpert(id, value); }

  // Admin users: SUPER_ADMIN only
  @Get('users') @AdminRoles('SUPER_ADMIN') users() { return this.admin.listAdmins(); }
  @Post('users') @AdminRoles('SUPER_ADMIN') createUser(@Body() input: AdminUserInput) { return this.admin.createAdmin(input); }
}
