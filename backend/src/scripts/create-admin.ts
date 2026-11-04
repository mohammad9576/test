import 'reflect-metadata';
import * as bcrypt from 'bcryptjs';
import { DataSource } from 'typeorm';
import { ALL_ENTITIES, AdminUser } from '../entities';

/**
 * Creates (or resets) the first SUPER_ADMIN account.
 * Usage: ADMIN_EMAIL=a@b.ir ADMIN_PASSWORD='...' ADMIN_NAME='مدیر' npm run seed:admin
 */
async function main(): Promise<void> {
  const email = process.env.ADMIN_EMAIL?.toLowerCase();
  const password = process.env.ADMIN_PASSWORD;
  if (!email || !password || password.length < 8) {
    throw new Error('ADMIN_EMAIL and ADMIN_PASSWORD (min 8 chars) are required');
  }
  const ds = new DataSource({ type: 'postgres', url: process.env.DATABASE_URL, entities: ALL_ENTITIES, synchronize: true });
  await ds.initialize();
  const repo = ds.getRepository(AdminUser);
  const existing = await repo.findOne({ where: { email } });
  const passwordHash = await bcrypt.hash(password, 12);
  if (existing) {
    existing.passwordHash = passwordHash;
    existing.role = 'SUPER_ADMIN';
    await repo.save(existing);
  } else {
    await repo.save(repo.create({ email, passwordHash, fullName: process.env.ADMIN_NAME ?? 'مدیر کل', role: 'SUPER_ADMIN' }));
  }
  await ds.destroy();
  process.stdout.write(`SUPER_ADMIN ready: ${email}\n`);
}

main().catch((e) => {
  process.stderr.write(`${(e as Error).message}\n`);
  process.exit(1);
});
