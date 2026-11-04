import {
  CanActivate,
  ExecutionContext,
  ForbiddenException,
  Injectable,
  SetMetadata,
  UnauthorizedException,
  createParamDecorator,
} from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { JwtService } from '@nestjs/jwt';
import { Request } from 'express';
import { AdminRole } from '../entities';

export interface FamilyPrincipal {
  kind: 'family';
  familyId: string;
  phone: string;
}

export interface AdminPrincipal {
  kind: 'admin';
  adminId: string;
  role: AdminRole;
}

export type Principal = FamilyPrincipal | AdminPrincipal;

export function bearerToken(header: string | string[] | undefined): string | null {
  const value = Array.isArray(header) ? header[0] : header;
  if (!value || !value.startsWith('Bearer ')) return null;
  return value.slice('Bearer '.length).trim();
}

@Injectable()
export class FamilyAuthGuard implements CanActivate {
  constructor(private readonly jwt: JwtService) {}

  canActivate(context: ExecutionContext): boolean {
    const req = context.switchToHttp().getRequest<Request & { principal?: Principal }>();
    const token = bearerToken(req.headers.authorization);
    if (!token) throw new UnauthorizedException();
    try {
      const payload = this.jwt.verify<Principal>(token);
      if (payload.kind !== 'family') throw new UnauthorizedException();
      req.principal = payload;
      return true;
    } catch {
      throw new UnauthorizedException();
    }
  }
}

export const ROLES_KEY = 'admin_roles';
export const AdminRoles = (...roles: AdminRole[]) => SetMetadata(ROLES_KEY, roles);

@Injectable()
export class AdminAuthGuard implements CanActivate {
  constructor(private readonly jwt: JwtService, private readonly reflector: Reflector) {}

  canActivate(context: ExecutionContext): boolean {
    const req = context.switchToHttp().getRequest<Request & { principal?: Principal }>();
    const token = bearerToken(req.headers.authorization);
    if (!token) throw new UnauthorizedException();
    let payload: Principal;
    try {
      payload = this.jwt.verify<Principal>(token);
    } catch {
      throw new UnauthorizedException();
    }
    if (payload.kind !== 'admin') throw new UnauthorizedException();
    const required = this.reflector.getAllAndOverride<AdminRole[] | undefined>(ROLES_KEY, [context.getHandler(), context.getClass()]);
    if (required && required.length > 0 && payload.role !== 'SUPER_ADMIN' && !required.includes(payload.role)) {
      throw new ForbiddenException('نقش شما اجازه‌ی این عملیات را ندارد');
    }
    req.principal = payload;
    return true;
  }
}

export const CurrentFamily = createParamDecorator((_: unknown, ctx: ExecutionContext): FamilyPrincipal => {
  const req = ctx.switchToHttp().getRequest<{ principal: FamilyPrincipal }>();
  return req.principal;
});

export const CurrentAdmin = createParamDecorator((_: unknown, ctx: ExecutionContext): AdminPrincipal => {
  const req = ctx.switchToHttp().getRequest<{ principal: AdminPrincipal }>();
  return req.principal;
});
