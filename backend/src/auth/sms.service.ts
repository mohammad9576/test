import { Injectable, Logger, ServiceUnavailableException } from '@nestjs/common';

/**
 * Sends OTP codes through the Kavenegar "verify/lookup" API. When no API key is configured (local development),
 * the code is written to the server log instead so the flow can be exercised end to end.
 */
@Injectable()
export class SmsService {
  private readonly logger = new Logger(SmsService.name);

  async sendOtp(phone: string, code: string): Promise<void> {
    const apiKey = process.env.KAVENEGAR_API_KEY;
    if (!apiKey) {
      this.logger.warn(`KAVENEGAR_API_KEY is not set – development OTP for ${phone}: ${code}`);
      return;
    }
    const template = process.env.KAVENEGAR_OTP_TEMPLATE ?? 'kodomo-otp';
    const receptor = phone.replace(/^\+98/, '0');
    const url = `https://api.kavenegar.com/v1/${encodeURIComponent(apiKey)}/verify/lookup.json?receptor=${encodeURIComponent(receptor)}&token=${encodeURIComponent(code)}&template=${encodeURIComponent(template)}`;
    const response = await fetch(url, { method: 'POST' });
    if (!response.ok) {
      this.logger.error(`Kavenegar responded ${response.status}: ${await response.text()}`);
      throw new ServiceUnavailableException('ارسال پیامک ناموفق بود');
    }
  }
}
