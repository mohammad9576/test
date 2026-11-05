'use client';

import { useEffect, useState } from 'react';
import { api, faDate, faNumber } from '@/lib/api';

type Status = 'PENDING' | 'IN_REVIEW' | 'DONE' | 'REJECTED';

interface ReviewRequest {
  id: string;
  title: string;
  kind: string;
  url: string;
  note: string;
  childAge: number;
  requests: number;
  status: Status;
  adminNote: string;
  createdAt: string;
}

const STATUS_LABELS: Record<Status, string> = {
  PENDING: 'در انتظار',
  IN_REVIEW: 'در حال ارزیابی',
  DONE: 'ارزیابی شد',
  REJECTED: 'رد شد',
};

/** Titles parents approved for their child without an expert evaluation; the team must evaluate them. */
export default function ReviewRequestsPage() {
  const [items, setItems] = useState<ReviewRequest[]>([]);
  const [filter, setFilter] = useState<Status | ''>('PENDING');
  const [notes, setNotes] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    api<ReviewRequest[]>(`/admin/review-requests${filter ? `?status=${filter}` : ''}`)
      .then(setItems)
      .catch((e: Error) => setError(e.message));
  };
  useEffect(load, [filter]);

  async function decide(r: ReviewRequest, status: Status) {
    try {
      await api(`/admin/review-requests/${r.id}`, { method: 'POST', body: JSON.stringify({ status, adminNote: notes[r.id] ?? r.adminNote }) });
      load();
    } catch (e) {
      setError((e as Error).message);
    }
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-black">درخواست‌های ارزیابی والدین</h1>
      <p className="text-sm text-slate-600">
        والدین این عنوان‌ها را بدون ارزیابی کارشناسی برای فرزندشان تأیید کرده‌اند. هر کدام باید هر چه زودتر ارزیابی و در مدیریت محتوا ثبت شود.
        عنوان‌هایی که بیشتر درخواست شده‌اند بالاتر نمایش داده می‌شوند.
      </p>
      {error && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      <div className="flex flex-wrap gap-2">
        {(['PENDING', 'IN_REVIEW', 'DONE', 'REJECTED', ''] as const).map((s) => (
          <button key={s || 'all'} className={filter === s ? 'btn-primary' : 'btn-ghost'} onClick={() => setFilter(s)}>
            {s ? STATUS_LABELS[s] : 'همه'}
          </button>
        ))}
      </div>
      <section className="card space-y-3">
        {items.length === 0 && <p className="text-sm text-slate-500">درخواستی وجود ندارد.</p>}
        {items.map((r) => (
          <div key={r.id} className="rounded-xl p-3 ring-1 ring-black/5">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <b>{r.title}</b>
                <div className="text-xs text-slate-500">
                  {r.kind} • سن کودک: {faNumber(r.childAge)} • {faNumber(r.requests)} درخواست • {faDate(r.createdAt)}
                </div>
                {r.url && (
                  <a className="text-xs text-emerald-700 underline" href={r.url} target="_blank" rel="noreferrer">
                    {r.url}
                  </a>
                )}
                {r.note && <p className="mt-1 text-sm text-slate-700">یادداشت والد: {r.note}</p>}
              </div>
              <span className="pill bg-amber-100 text-amber-700">{STATUS_LABELS[r.status]}</span>
            </div>
            <textarea
              className="mt-2 w-full rounded-lg border p-2 text-sm"
              placeholder="یادداشت تیم ارزیابی"
              value={notes[r.id] ?? r.adminNote}
              onChange={(e) => setNotes({ ...notes, [r.id]: e.target.value })}
            />
            <div className="mt-2 flex flex-wrap gap-2">
              <button className="btn-ghost" onClick={() => decide(r, 'IN_REVIEW')}>شروع ارزیابی</button>
              <button className="btn-primary" onClick={() => decide(r, 'DONE')}>ارزیابی ثبت شد</button>
              <button className="btn-danger" onClick={() => decide(r, 'REJECTED')}>رد</button>
            </div>
          </div>
        ))}
      </section>
    </div>
  );
}
