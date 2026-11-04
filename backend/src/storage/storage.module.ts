import { Global, Injectable, Logger, Module, OnModuleInit } from '@nestjs/common';
import { Client } from 'minio';
import { randomUUID } from 'crypto';

/** MinIO / S3 media storage. Admin uploads go straight from the browser to the bucket via presigned PUT URLs. */
@Injectable()
export class StorageService implements OnModuleInit {
  private readonly logger = new Logger(StorageService.name);
  private readonly bucket = process.env.MINIO_BUCKET ?? 'kodomo-media';
  private readonly client = new Client({
    endPoint: process.env.MINIO_ENDPOINT ?? 'localhost',
    port: Number(process.env.MINIO_PORT ?? 9000),
    useSSL: process.env.MINIO_USE_SSL === 'true',
    accessKey: process.env.MINIO_ACCESS_KEY ?? '',
    secretKey: process.env.MINIO_SECRET_KEY ?? '',
  });

  async onModuleInit(): Promise<void> {
    try {
      if (!(await this.client.bucketExists(this.bucket))) {
        await this.client.makeBucket(this.bucket);
        await this.client.setBucketPolicy(this.bucket, JSON.stringify({
          Version: '2012-10-17',
          Statement: [{ Effect: 'Allow', Principal: { AWS: ['*'] }, Action: ['s3:GetObject'], Resource: [`arn:aws:s3:::${this.bucket}/public/*`] }],
        }));
      }
    } catch (e) {
      this.logger.error(`MinIO bucket initialisation failed: ${(e as Error).message}`);
    }
  }

  async presignUpload(fileName: string, folder: 'public' | 'private' = 'public'): Promise<{ uploadUrl: string; objectKey: string; publicUrl: string }> {
    const safe = fileName.replace(/[^a-zA-Z0-9._-]/g, '_').slice(-80);
    const objectKey = `${folder}/${new Date().toISOString().slice(0, 10)}/${randomUUID()}-${safe}`;
    const uploadUrl = await this.client.presignedPutObject(this.bucket, objectKey, 60 * 30);
    const base = process.env.MINIO_PUBLIC_URL ?? `http://localhost:9000/${this.bucket}`;
    return { uploadUrl, objectKey, publicUrl: `${base.replace(/\/$/, '')}/${objectKey}` };
  }
}

@Global()
@Module({ providers: [StorageService], exports: [StorageService] })
export class StorageModule {}
