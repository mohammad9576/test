import { Logger, Module } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { InjectRepository, TypeOrmModule } from '@nestjs/typeorm';
import { OnGatewayConnection, OnGatewayDisconnect, WebSocketGateway } from '@nestjs/websockets';
import { IncomingMessage } from 'http';
import { Repository } from 'typeorm';
import { WebSocket } from 'ws';
import { Principal, bearerToken } from '../common/auth';
import { Family, UsageEvent } from '../entities';

interface UsageMessage {
  type: 'usage';
  childLocalId: number;
  childName: string;
  category: string;
  title: string;
  usedSecondsToday: number;
  limitSeconds: number;
  timestamp: string;
}

function isUsageMessage(value: unknown): value is UsageMessage {
  const v = value as Partial<UsageMessage>;
  return !!v && v.type === 'usage' && typeof v.childLocalId === 'number' && typeof v.usedSecondsToday === 'number' && typeof v.limitSeconds === 'number';
}

/**
 * Live consumption monitoring. Child devices push usage heartbeats; every other device of the same family and the
 * super-admin dashboard receive them in real time. Browsers authenticate with `?token=`, apps with a Bearer header.
 */
@WebSocketGateway({ path: '/live' })
export class LiveGateway implements OnGatewayConnection, OnGatewayDisconnect {
  private readonly logger = new Logger(LiveGateway.name);
  private readonly families = new Map<string, Set<WebSocket>>();
  private readonly admins = new Set<WebSocket>();
  private readonly principals = new WeakMap<WebSocket, Principal>();

  constructor(
    private readonly jwt: JwtService,
    @InjectRepository(UsageEvent) private readonly events: Repository<UsageEvent>,
  ) {}

  handleConnection(client: WebSocket, request: IncomingMessage): void {
    const url = new URL(request.url ?? '/live', 'http://localhost');
    const token = bearerToken(request.headers.authorization) ?? url.searchParams.get('token');
    let principal: Principal;
    try {
      principal = this.jwt.verify<Principal>(token ?? '');
    } catch {
      client.close(4401, 'unauthorized');
      return;
    }
    this.principals.set(client, principal);
    if (principal.kind === 'admin') {
      this.admins.add(client);
    } else {
      const set = this.families.get(principal.familyId) ?? new Set<WebSocket>();
      set.add(client);
      this.families.set(principal.familyId, set);
    }
    client.on('message', (raw) => void this.onMessage(client, raw.toString()));
  }

  handleDisconnect(client: WebSocket): void {
    const principal = this.principals.get(client);
    if (!principal) return;
    if (principal.kind === 'admin') {
      this.admins.delete(client);
    } else {
      const set = this.families.get(principal.familyId);
      set?.delete(client);
      if (set && set.size === 0) this.families.delete(principal.familyId);
    }
  }

  private async onMessage(client: WebSocket, raw: string): Promise<void> {
    const principal = this.principals.get(client);
    if (!principal || principal.kind !== 'family') return;
    let parsed: unknown;
    try {
      parsed = JSON.parse(raw);
    } catch {
      return;
    }
    if (!isUsageMessage(parsed)) return;
    try {
      await this.events.insert({
        family: { id: principal.familyId } as Family,
        childLocalId: String(parsed.childLocalId),
        childName: String(parsed.childName).slice(0, 60),
        category: String(parsed.category).slice(0, 20),
        title: String(parsed.title ?? '').slice(0, 120),
        usedSecondsToday: Math.max(0, Math.round(parsed.usedSecondsToday)),
        limitSeconds: Math.max(0, Math.round(parsed.limitSeconds)),
      });
    } catch (e) {
      this.logger.warn(`Unable to persist usage event: ${(e as Error).message}`);
    }
    const outgoing = JSON.stringify({ ...parsed, familyId: principal.familyId });
    for (const peer of this.families.get(principal.familyId) ?? []) {
      if (peer !== client && peer.readyState === WebSocket.OPEN) peer.send(outgoing);
    }
    for (const admin of this.admins) {
      if (admin.readyState === WebSocket.OPEN) admin.send(outgoing);
    }
  }

  connectedFamilies(): number {
    return this.families.size;
  }
}

@Module({
  imports: [TypeOrmModule.forFeature([UsageEvent])],
  providers: [LiveGateway],
  exports: [LiveGateway],
})
export class LiveModule {}
