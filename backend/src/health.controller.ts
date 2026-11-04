import { Controller, Get, Inject } from '@nestjs/common';
import { DataSource } from 'typeorm';
import Redis from 'ioredis';
import { REDIS } from './common/redis';

@Controller('health')
export class HealthController {
  constructor(private readonly db: DataSource, @Inject(REDIS) private readonly redis: Redis) {}

  @Get()
  async health() {
    const [database, cache] = await Promise.all([
      this.db.query('SELECT 1').then(() => 'up').catch(() => 'down'),
      this.redis.ping().then(() => 'up').catch(() => 'down'),
    ]);
    return { status: database === 'up' && cache === 'up' ? 'ok' : 'degraded', database, cache, time: new Date().toISOString() };
  }
}
