'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { getToken, setToken } from '@/lib/api';

const NAV = [
  { href: '/', label: 'داشبورد', icon: '📊' },
  { href: '/content', label: 'مدیریت محتوا', icon: '🎬' },
  { href: '/review-requests', label: 'درخواست ارزیابی', icon: '📝' },
  { href: '/families', label: 'خانواده‌ها', icon: '👨‍👩‍👧' },
  { href: '/licenses', label: 'لایسنس و مالی', icon: '💳' },
  { href: '/forum', label: 'نظارت بر تالارها', icon: '💬' },
  { href: '/experts', label: 'کارشناسان', icon: '🩺' },
];

export default function Shell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!getToken()) router.replace('/login');
    else setReady(true);
  }, [router]);

  if (!ready) return null;

  return (
    <div className="min-h-screen lg:flex">
      <aside className={`${open ? 'block' : 'hidden'} w-full shrink-0 bg-kodomo-ink p-4 text-white lg:block lg:min-h-screen lg:w-64`}>
        <div className="mb-6 flex items-center gap-2">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-kodomo text-lg font-black">ک</div>
          <div>
            <div className="font-black text-emerald-300">کدومو</div>
            <div className="text-[11px] text-slate-400">پنل سوپر ادمین</div>
          </div>
        </div>
        <nav className="space-y-1">
          {NAV.map((item) => {
            const active = item.href === '/' ? pathname === '/' : pathname.startsWith(item.href);
            return (
              <Link
                key={item.href}
                href={item.href}
                onClick={() => setOpen(false)}
                className={`flex items-center gap-2 rounded-xl px-3 py-2 text-sm font-bold ${active ? 'bg-kodomo text-white' : 'text-slate-300 hover:bg-white/10'}`}
              >
                <span>{item.icon}</span>
                {item.label}
              </Link>
            );
          })}
        </nav>
        <button
          className="mt-8 w-full rounded-xl bg-white/10 px-3 py-2 text-sm text-slate-200 hover:bg-white/20"
          onClick={() => {
            setToken(null);
            router.replace('/login');
          }}
        >
          خروج
        </button>
      </aside>
      <div className="min-w-0 flex-1">
        <header className="flex items-center justify-between bg-white px-4 py-3 shadow-sm lg:hidden">
          <span className="font-black text-kodomo">پنل کدومو</span>
          <button className="btn-ghost" onClick={() => setOpen((v) => !v)}>منو</button>
        </header>
        <main className="mx-auto max-w-6xl p-4 lg:p-8">{children}</main>
      </div>
    </div>
  );
}
