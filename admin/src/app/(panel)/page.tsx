'use client';

import { useEffect, useRef, useState } from 'react';
import { WS_URL, api, faNumber, getToken } from '@/lib/api';
import type { Stats } from '@/lib/types';

interface LiveEvent {
  familyId: string;
  childName: string;
  category: string;
  title: string;
  usedSecondsToday: number;
  limitSeconds: number;
  timestamp: string;
}

const CATEGORY_LABELS: Record<string, string> = {
  VIDEO: 'فیلم', BOOK: 'کتاب', GAME: 'بازی', MUSIC: 'موسیقی', LEARNING: 'آموزش', SCHOOL: 'مدرسه', EXPLORE: 'کشف', BROWSER: 'مرورگر', PROJECT: 'پروژه', HOME: 'خانه',
};

export default function DashboardPage() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [events, setEvents] = useState<LiveEvent[]>([]);
  const [socketState, setSocketState] = useState<'connecting' | 'open' | 'closed'>('connecting');
  const socketRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    api<Stats>('/admin/stats').then(setStats).catch((e: Error) => setError(e.message));
  }, []);

  useEffect(() => {
    let cancelled = false;
    let retry: ReturnType<typeof setTimeout> | undefined;
    const connect = () => {
      const token = getToken();
      if (!token || cancelled) return;
      const ws = new WebSocket(`${WS_URL}?token=${encodeURIComponent(token)}`);
      socketRef.current = ws;
      setSocketState('connecting');
      ws.onopen = () => setSocketState('open');
      ws.onmessage = (m) => {
        try {
          const event = JSON.parse(String(m.data)) as LiveEvent;
          setEvents((prev) => [event, ...prev].slice(0, 50));
        } catch {
          /* ignore malformed frames */
        }
      };
      ws.onclose = () => {
        setSocketState('closed');
        if (!cancelled) retry = setTimeout(connect, 5000);
      };
    };
    connect();
    return () => {
      cancelled = true;
      if (retry) clearTimeout(retry);
      socketRef.current?.close();
    };
  }, []);

  const maxMinutes = Math.max(1, ...(stats?.usageMinutesLast24h.map((u) => u.minutes) ?? [1]));

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-black">داشبورد کلان</h1>
      {error && <p className="text-red-600">{error}</p>}
      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <Tile label="خانواده‌ها" value={stats?.families} />
        <Tile label="کودکان" value={stats?.children} />
        <Tile label="محتوای منتشرشده" value={stats ? `${faNumber(stats.published)} / ${faNumber(stats.content)}` : undefined} />
        <Tile label="لایسنس فعال" value={stats?.activeLicenses} />
        <Tile label="گزارش تخلف باز" value={stats?.openReports} warn={(stats?.openReports ?? 0) > 0} />
        <Tile label="کارشناس در انتظار تأیید" value={stats?.pendingExperts} warn={(stats?.pendingExperts ?? 0) > 0} />
        <Tile label="درآمد کل (ریال)" value={stats ? faNumber(stats.revenueRials) : undefined} />
        <Tile label="خانواده‌های آنلاین" value={stats?.liveFamilies} />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <section className="card">
          <h2 className="mb-4 font-black">مصرف ۲۴ ساعت گذشته (دقیقه)</h2>
          {stats && stats.usageMinutesLast24h.length === 0 && <p className="text-sm text-slate-500">هنوز داده‌ای ثبت نشده است.</p>}
          <div className="space-y-2">
            {stats?.usageMinutesLast24h.map((u) => (
              <div key={u.category} className="flex items-center gap-3 text-sm">
                <span className="w-16 shrink-0">{CATEGORY_LABELS[u.category] ?? u.category}</span>
                <div className="h-3 flex-1 rounded-full bg-kodomo-mint">
                  <div className="h-3 rounded-full bg-kodomo" style={{ width: `${(u.minutes / maxMinutes) * 100}%` }} />
                </div>
                <span className="w-12 text-left">{faNumber(u.minutes)}</span>
              </div>
            ))}
          </div>
        </section>

        <section className="card">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="font-black">پایش زنده‌ی مصرف</h2>
            <span className={`pill ${socketState === 'open' ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'}`}>
              {socketState === 'open' ? 'متصل' : socketState === 'connecting' ? 'در حال اتصال' : 'قطع'}
            </span>
          </div>
          {events.length === 0 && <p className="text-sm text-slate-500">رویدادی دریافت نشده است. دستگاه‌های کودک هر ۳۰ ثانیه گزارش می‌دهند.</p>}
          <ul className="max-h-80 space-y-2 overflow-y-auto text-sm">
            {events.map((e, i) => {
              const pct = e.limitSeconds > 0 ? Math.min(100, Math.round((e.usedSecondsToday / e.limitSeconds) * 100)) : 0;
              return (
                <li key={`${e.timestamp}-${i}`} className="rounded-xl bg-kodomo-mint/60 p-2">
                  <div className="flex justify-between">
                    <b>{e.childName}</b>
                    <span className="text-xs text-slate-500">{new Date(e.timestamp).toLocaleTimeString('fa-IR')}</span>
                  </div>
                  <div className="text-xs">{CATEGORY_LABELS[e.category] ?? e.category} • {e.title}</div>
                  <div className="mt-1 h-1.5 rounded-full bg-white">
                    <div className={`h-1.5 rounded-full ${pct >= 90 ? 'bg-red-500' : 'bg-kodomo'}`} style={{ width: `${pct}%` }} />
                  </div>
                </li>
              );
            })}
          </ul>
        </section>
      </div>
    </div>
  );
}

function Tile({ label, value, warn }: { label: string; value: number | string | undefined; warn?: boolean }) {
  return (
    <div className={`card ${warn ? 'ring-amber-300' : ''}`}>
      <div className="text-xs font-bold text-slate-500">{label}</div>
      <div className={`mt-1 text-2xl font-black ${warn ? 'text-amber-600' : 'text-kodomo'}`}>
        {value === undefined ? '…' : typeof value === 'number' ? faNumber(value) : value}
      </div>
    </div>
  );
}
