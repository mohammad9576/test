'use client';

import { useEffect, useState } from 'react';
import { api, faDate, faNumber } from '@/lib/api';
import { INTERESTS, type Family } from '@/lib/types';

function age(birth: string): number {
  const b = new Date(birth);
  const now = new Date();
  let years = now.getFullYear() - b.getFullYear();
  if (now.getMonth() < b.getMonth() || (now.getMonth() === b.getMonth() && now.getDate() < b.getDate())) years--;
  return Math.max(0, years);
}

export default function FamiliesPage() {
  const [families, setFamilies] = useState<Family[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState('');

  useEffect(() => {
    api<Family[]>('/admin/families').then(setFamilies).catch((e: Error) => setError(e.message));
  }, []);

  const filtered = families.filter((f) => !query || f.phone.includes(query) || f.familyName.includes(query));

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-black">خانواده‌ها و کاربران</h1>
      <input className="input max-w-sm" placeholder="جستجو با شماره یا نام خانوادگی" value={query} onChange={(e) => setQuery(e.target.value)} />
      {error && <p className="text-red-600">{error}</p>}
      <div className="grid gap-4 md:grid-cols-2">
        {filtered.map((f) => (
          <div key={f.id} className="card space-y-2">
            <div className="flex items-start justify-between">
              <div>
                <h2 className="font-black">{f.familyName || 'بدون نام'}</h2>
                <div className="text-xs text-slate-500" dir="ltr">{f.phone}</div>
              </div>
              <span className="pill bg-kodomo-mint text-kodomo-dark">{faNumber(f.children.length)} کودک</span>
            </div>
            <div className="text-sm">پدر: {f.fatherName || '—'} • مادر: {f.motherName || '—'}</div>
            {f.organization && <div className="text-xs">عضو: {f.organization.name}</div>}
            <ul className="space-y-1 text-sm">
              {f.children.map((c) => (
                <li key={c.id} className="rounded-xl bg-kodomo-mint/50 px-3 py-2">
                  <b>{c.name}</b> • {faNumber(age(c.birthDate))} ساله • سقف روزانه {faNumber(c.dailyLimitMinutes)} دقیقه • ⭐ {faNumber(c.stars)}
                  <div className="text-xs text-slate-500">{c.interests.map((i) => INTERESTS[i] ?? i).join('، ')}</div>
                </li>
              ))}
            </ul>
            <div className="text-xs text-slate-400">ثبت: {faDate(f.createdAt)}{f.lastSyncAt ? ` • آخرین همگام‌سازی: ${faDate(f.lastSyncAt)}` : ''}</div>
          </div>
        ))}
        {filtered.length === 0 && <p className="text-slate-500">خانواده‌ای یافت نشد.</p>}
      </div>
    </div>
  );
}
