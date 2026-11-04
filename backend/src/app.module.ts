import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { AdminModule } from './admin/admin.module';
import { AuthModule } from './auth/auth.module';
import { RedisModule } from './common/redis';
import { ContentModule } from './content/content.module';
import { ALL_ENTITIES } from './entities';
import { FamiliesModule } from './families/families.module';
import { ForumModule } from './forum/forum.module';
import { HealthController } from './health.controller';
import { LiveModule } from './live/live.gateway';
import { StorageModule } from './storage/storage.module';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    TypeOrmModule.forRootAsync({
      useFactory: () => ({
        type: 'postgres',
        url: process.env.DATABASE_URL ?? 'postgres://kodomo:kodomo@localhost:5432/kodomo',
        entities: ALL_ENTITIES,
        // Schema is managed by TypeORM synchronisation in this deployment profile; switch to migrations for multi-node rollouts.
        synchronize: process.env.DB_SYNCHRONIZE !== 'false',
        logging: ['error', 'warn'],
      }),
    }),
    RedisModule,
    StorageModule,
    AuthModule,
    FamiliesModule,
    ContentModule,
    ForumModule,
    LiveModule,
    AdminModule,
  ],
  controllers: [HealthController],
})
export class AppModule {}
