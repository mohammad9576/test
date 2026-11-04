import { IsEmail, IsString, Length, Matches } from 'class-validator';

export class OtpRequestDto {
  @Matches(/^\+98 ?9\d{9}$/, { message: 'شماره موبایل معتبر نیست' })
  phone: string;
}

export class OtpVerifyDto {
  @IsString() requestId: string;
  @Matches(/^\+98 ?9\d{9}$/) phone: string;
  @Length(4, 6) code: string;
}

export class AdminLoginDto {
  @IsEmail() email: string;
  @IsString() @Length(8, 128) password: string;
}
