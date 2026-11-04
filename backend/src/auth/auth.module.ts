import { Global, Module } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { TypeOrmModule } from '@nestjs/typeorm';
import { AdminAuthGuard, FamilyAuthGuard } from '../common/auth';
import { AdminUser, Family } from '../entities';
import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';
import { SmsService } from './sms.service';

@Global()
@Module({
  imports: [
    TypeOrmModule.forFeature([Family, AdminUser]),
    JwtModule.registerAsync({
      global: true,
      useFactory: () => {
        const secret = process.env.JWT_SECRET;
        if (!secret || secret.length < 24) throw new Error('JWT_SECRET must be set to a random string of at least 24 characters');
        return { secret, signOptions: { expiresIn: Number(process.env.JWT_TTL_SECONDS ?? 2_592_000) } };
      },
    }),
  ],
  controllers: [AuthController],
  providers: [AuthService, SmsService, FamilyAuthGuard, AdminAuthGuard],
  exports: [FamilyAuthGuard, AdminAuthGuard],
})
export class AuthModule {}
