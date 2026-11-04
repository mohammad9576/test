'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { api, faDate } from '@/lib/api';
import { CONTENT_TYPE_LABELS, type Content, type ContentType } from '@/lib/types';

export default function ContentListPage() {
  const [items, setItems] = useState<Content[]>([]);
  const [type, setType] = useState<ContentType | ''>('');
  const [query, setQuery] = useState('');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api<Content[]>(`/admin/content${type ? `?type=${type}` : ''}`).then(setItems).catch((e: Error) => setError(e.message));
  }, [type]);

  const filtered = items.filter((i) => !query || i.title.includes(query) || i.category.includes(query));

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-black">مدیریت محتوا (CMS)</h1>
        <Link href="/content/new" className="btn-primary">➕ محتوای جدید</Link>
      </div>
      <div className="card flex flex-wrap gap-3">
        <select className="input max-w-xs" value={type} onChange={(e) => setType(e.target.value as ContentType | '')}>
          <option value="">همه‌ی انواع</option>
          {Object.entries(CONTENT_TYPE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
        <input className="input max-w-xs" placeholder="جستجو در عنوان یا دسته" value={query} onChange={(e) => setQuery(e.target.value)} />
      </div>
      {error && <p className="text-red-600">{error}</p>}
      <div className="card overflow-x-auto p-0">
        <table className="table">
          <thead>
            <tr><th>عنوان</th><th>نوع</th><th>سن</th><th>خشونت/ترس/ناهنجاری</th><th>آموزندگی</th><th>وضعیت</th><th>به‌روزرسانی</th></tr>
          </thead>
          <tbody>
            {filtered.map((c) => (
              <tr key={c.id} className="hover:bg-kodomo-mint/40">
                <td><Link className="font-bold text-kodomo" href={`/content/${c.id}`}>{c.emoji} {c.title}</Link><div className="text-xs text-slate-500">{c.category}</div></td>
                <td>{CONTENT_TYPE_LABELS[c.type]}</td>
                <td>{c.minAge.toLocaleString('fa-IR')}–{c.maxAge.toLocaleString('fa-IR')}</td>
                <td>{[c.violence, c.fear, c.abnormality].map((v) => v.toLocaleString('fa-IR')).join(' / ')}</td>
                <td>{c.educational.toLocaleString('fa-IR')}</td>
                <td>{c.published ? <span className="pill bg-emerald-100 text-emerald-700">منتشر</span> : <span className="pill bg-slate-100 text-slate-600">پیش‌نویس</span>}</td>
                <td className="text-xs">{faDate(c.updatedAt)}</td>
              </tr>
            ))}
            {filtered.length === 0 && <tr><td colSpan={7} className="py-8 text-center text-slate-500">محتوایی یافت نشد.</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}
