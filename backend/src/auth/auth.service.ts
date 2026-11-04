import { BadRequestException, HttpException, HttpStatus, Inject, Injectable, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { InjectRepository } from '@nestjs/typeorm';
import * as bcrypt from 'bcryptjs';
import { createHash, randomInt, randomUUID, timingSafeEqual } from 'crypto';
import Redis from 'ioredis';
import { Repository } from 'typeorm';
import { REDIS } from '../common/redis';
import { AdminPrincipal, FamilyPrincipal } from '../common/auth';
import { AdminUser, Family } from '../entities';
import { SmsService } from './sms.service';

const OTP_TTL_SECONDS = 120;
const OTP_MAX_ATTEMPTS = 5;
const OTP_RESEND_SECONDS = 60;

function sha256(value: string): string {
  return createHash('sha256').update(value).digest('hex');
}

@Injectable()
export class AuthService {
  constructor(
    @Inject(REDIS) private readonly redis: Redis,
    private readonly sms: SmsService,
    private readonly jwt: JwtService,
    @InjectRepository(Family) private readonly families: Repository<Family>,
    @InjectRepository(AdminUser) private readonly admins: Repository<AdminUser>,
  ) {}

  private normalize(phone: string): string {
    return phone.replace(/\s+/g, '');
  }

  async requestOtp(rawPhone: string): Promise<{ requestId: string; expiresInSeconds: number }> {
    const phone = this.normalize(rawPhone);
    const throttleKey = `otp:throttle:${phone}`;
    const allowed = await this.redis.set(throttleKey, '1', 'EX', OTP_RESEND_SECONDS, 'NX');
    if (allowed !== 'OK') {
      throw new HttpException('لطفاً یک دقیقه بعد دوباره تلاش کنید', HttpStatus.TOO_MANY_REQUESTS);
    }
    const code = randomInt(0, 1_000_000).toString().padStart(6, '0');
    const requestId = randomUUID();
    await this.redis.set(`otp:${requestId}`, JSON.stringify({ phone, hash: sha256(code), attempts: 0 }), 'EX', OTP_TTL_SECONDS);
    await this.sms.sendOtp(phone, code);
    return { requestId, expiresInSeconds: OTP_TTL_SECONDS };
  }

  async verifyOtp(requestId: string, rawPhone: string, code: string): Promise<{ accessToken: string; refreshToken: string; familyId: string }> {
    const phone = this.normalize(rawPhone);
    const key = `otp:${requestId}`;
    const raw = await this.redis.get(key);
    if (!raw) throw new BadRequestException('کد منقضی شده است؛ دوباره درخواست دهید');
    const record = JSON.parse(raw) as { phone: string; hash: string; attempts: number };
    if (record.phone !== phone) throw new BadRequestException('شماره با درخواست مطابقت ندارد');
    if (record.attempts >= OTP_MAX_ATTEMPTS) {
      await this.redis.del(key);
      throw new HttpException('تعداد تلاش‌ها بیش از حد مجاز است', HttpStatus.TOO_MANY_REQUESTS);
    }
    const ok = timingSafeEqual(Buffer.from(record.hash, 'hex'), Buffer.from(sha256(code), 'hex'));
    if (!ok) {
      record.attempts += 1;
      const ttl = await this.redis.ttl(key);
      await this.redis.set(key, JSON.stringify(record), 'EX', Math.max(ttl, 1));
      throw new UnauthorizedException('کد تأیید اشتباه است');
    }
    await this.redis.del(key);

    let family = await this.families.findOne({ where: { phone } });
    if (!family) family = await this.families.save(this.families.create({ phone }));
    const principal: FamilyPrincipal = { kind: 'family', familyId: family.id, phone };
    const accessToken = await this.jwt.signAsync(principal);
    const refreshToken = await this.jwt.signAsync({ ...principal, refresh: true }, { expiresIn: '180d' });
    return { accessToken, refreshToken, familyId: family.id };
  }

  async adminLogin(email: string, password: string): Promise<{ accessToken: string; role: string; fullName: string }> {
    const admin = await this.admins.findOne({ where: { email: email.toLowerCase() } });
    if (!admin || !(await bcrypt.compare(password, admin.passwordHash))) {
      throw new UnauthorizedException('ایمیل یا رمز عبور اشتباه است');
    }
    const principal: AdminPrincipal = { kind: 'admin', adminId: admin.id, role: admin.role };
    return { accessToken: await this.jwt.signAsync(principal, { expiresIn: '12h' }), role: admin.role, fullName: admin.fullName };
  }
}
