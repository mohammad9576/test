'use client';

import { useEffect, useState } from 'react';
import { api } from '@/lib/api';
import { ROLE_LABELS, type Expert } from '@/lib/types';

export default function ExpertsPage() {
  const [experts, setExperts] = useState<Expert[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState({ fullName: '', phone: '', role: 'PSYCHOLOGIST', licenseNumber: '' });

  const load = () => { api<Expert[]>('/admin/experts').then(setExperts).catch((e: Error) => setError(e.message)); };
  useEffect(load, []);

  async function create(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await api('/admin/experts', { method: 'POST', body: JSON.stringify(form) });
      setForm({ fullName: '', phone: '', role: 'PSYCHOLOGIST', licenseNumber: '' });
      load();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function verify(x: Expert) {
    await api(`/admin/experts/${x.id}/verified?value=${!x.verified}`, { method: 'POST' });
    load();
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-black">پزشکان، روانشناسان و کارشناسان</h1>
      {error && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      <form onSubmit={create} className="card grid gap-3 md:grid-cols-5">
        <input className="input md:col-span-2" placeholder="نام و نام خانوادگی" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required minLength={3} />
        <input className="input" dir="ltr" placeholder="+98912..." value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} required minLength={10} />
        <select className="input" value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
          {Object.entries(ROLE_LABELS).filter(([k]) => k !== 'PARENT').map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
        <input className="input" placeholder="شماره نظام" value={form.licenseNumber} onChange={(e) => setForm({ ...form, licenseNumber: e.target.value })} />
        <button className="btn-primary md:col-span-5">افزودن کارشناس</button>
      </form>
      <div className="card overflow-x-auto p-0">
        <table className="table">
          <thead><tr><th>نام</th><th>تخصص</th><th>شماره نظام</th><th>تلفن</th><th>وضعیت</th></tr></thead>
          <tbody>
            {experts.map((x) => (
              <tr key={x.id}>
                <td className="font-bold">{x.fullName}</td>
                <td>{ROLE_LABELS[x.role] ?? x.role}</td>
                <td>{x.licenseNumber || '—'}</td>
                <td dir="ltr" className="text-right">{x.phone}</td>
                <td>
                  <button className={x.verified ? 'btn-ghost' : 'btn-primary'} onClick={() => verify(x)}>
                    {x.verified ? '✅ تأییدشده (لغو)' : 'تأیید هویت'}
                  </button>
                </td>
              </tr>
            ))}
            {experts.length === 0 && <tr><td colSpan={5} className="py-6 text-center text-slate-500">کارشناسی ثبت نشده است.</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}
