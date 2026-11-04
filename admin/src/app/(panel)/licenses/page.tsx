'use client';

import { useEffect, useState } from 'react';
import { api, faDate, faNumber } from '@/lib/api';
import type { Family, FinanceRow, License, Organization } from '@/lib/types';

const KIND_LABELS = { FAMILY: 'خانوادگی', SCHOOL: 'مدرسه', ORGANIZATION: 'سازمانی' } as const;

export default function LicensesPage() {
  const [licenses, setLicenses] = useState<License[]>([]);
  const [orgs, setOrgs] = useState<Organization[]>([]);
  const [families, setFamilies] = useState<Family[]>([]);
  const [finance, setFinance] = useState<FinanceRow[]>([]);
  const [error, setError] = useState<string | null>(null);
  const today = new Date().toISOString().slice(0, 10);
  const nextYear = new Date(Date.now() + 365 * 864e5).toISOString().slice(0, 10);
  const [form, setForm] = useState({ kind: 'FAMILY' as License['kind'], familyId: '', organizationId: '', seats: 1, startsAt: today, endsAt: nextYear, priceRials: '0' });
  const [org, setOrg] = useState({ name: '', kind: 'SCHOOL' as Organization['kind'], city: '', contactPhone: '' });

  const load = () => {
    Promise.all([
      api<License[]>('/admin/licenses'), api<Organization[]>('/admin/organizations'), api<Family[]>('/admin/families'), api<FinanceRow[]>('/admin/finance/monthly'),
    ])
      .then(([l, o, f, fin]) => { setLicenses(l); setOrgs(o); setFamilies(f); setFinance(fin); })
      .catch((e: Error) => setError(e.message));
  };
  useEffect(load, []);

  async function createLicense(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await api('/admin/licenses', {
        method: 'POST',
        body: JSON.stringify({
          kind: form.kind, seats: Number(form.seats), priceRials: form.priceRials || '0',
          startsAt: new Date(form.startsAt).toISOString(), endsAt: new Date(form.endsAt).toISOString(),
          ...(form.kind === 'FAMILY' ? { familyId: form.familyId } : { organizationId: form.organizationId }),
        }),
      });
      load();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function createOrg(e: React.FormEvent) {
    e.preventDefault();
    try {
      await api('/admin/organizations', { method: 'POST', body: JSON.stringify({ ...org, city: org.city || undefined, contactPhone: org.contactPhone || undefined }) });
      setOrg({ name: '', kind: 'SCHOOL', city: '', contactPhone: '' });
      load();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function toggle(l: License) {
    await api(`/admin/licenses/${l.id}/active?value=${!l.active}`, { method: 'POST' });
    load();
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-black">لایسنس‌ها، اشتراک‌ها و گزارش مالی</h1>
      {error && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      <div className="grid gap-6 lg:grid-cols-2">
        <form onSubmit={createLicense} className="card space-y-3">
          <h2 className="font-black">صدور لایسنس</h2>
          <select className="input" value={form.kind} onChange={(e) => setForm({ ...form, kind: e.target.value as License['kind'] })}>
            {Object.entries(KIND_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
          {form.kind === 'FAMILY' ? (
            <select className="input" value={form.familyId} onChange={(e) => setForm({ ...form, familyId: e.target.value })} required>
              <option value="">انتخاب خانواده</option>
              {families.map((f) => <option key={f.id} value={f.id}>{f.familyName || f.phone}</option>)}
            </select>
          ) : (
            <select className="input" value={form.organizationId} onChange={(e) => setForm({ ...form, organizationId: e.target.value })} required>
              <option value="">انتخاب مدرسه/سازمان</option>
              {orgs.filter((o) => (form.kind === 'SCHOOL' ? o.kind === 'SCHOOL' : o.kind === 'ORGANIZATION')).map((o) => <option key={o.id} value={o.id}>{o.name}</option>)}
            </select>
          )}
          <div className="grid grid-cols-2 gap-2">
            <div><label className="label">تعداد کاربر</label><input type="number" min={1} className="input" value={form.seats} onChange={(e) => setForm({ ...form, seats: Number(e.target.value) })} /></div>
            <div><label className="label">مبلغ (ریال)</label><input className="input" dir="ltr" value={form.priceRials} onChange={(e) => setForm({ ...form, priceRials: e.target.value.replace(/\D/g, '') })} /></div>
            <div><label className="label">شروع</label><input type="date" className="input" value={form.startsAt} onChange={(e) => setForm({ ...form, startsAt: e.target.value })} /></div>
            <div><label className="label">پایان</label><input type="date" className="input" value={form.endsAt} onChange={(e) => setForm({ ...form, endsAt: e.target.value })} /></div>
          </div>
          <button className="btn-primary w-full">صدور</button>
        </form>
        <form onSubmit={createOrg} className="card space-y-3">
          <h2 className="font-black">ثبت مدرسه یا سازمان</h2>
          <input className="input" placeholder="نام" value={org.name} onChange={(e) => setOrg({ ...org, name: e.target.value })} required minLength={2} />
          <select className="input" value={org.kind} onChange={(e) => setOrg({ ...org, kind: e.target.value as Organization['kind'] })}>
            <option value="SCHOOL">مدرسه</option>
            <option value="ORGANIZATION">سازمان</option>
          </select>
          <input className="input" placeholder="شهر" value={org.city} onChange={(e) => setOrg({ ...org, city: e.target.value })} />
          <input className="input" placeholder="تلفن تماس" dir="ltr" value={org.contactPhone} onChange={(e) => setOrg({ ...org, contactPhone: e.target.value })} />
          <button className="btn-ghost w-full">ثبت</button>
        </form>
      </div>

      <section className="card overflow-x-auto p-0">
        <table className="table">
          <thead><tr><th>نوع</th><th>دارنده</th><th>ظرفیت</th><th>اعتبار</th><th>مبلغ</th><th>وضعیت</th></tr></thead>
          <tbody>
            {licenses.map((l) => (
              <tr key={l.id}>
                <td>{KIND_LABELS[l.kind]}</td>
                <td>{l.family?.familyName || l.family?.phone || l.organization?.name}</td>
                <td>{faNumber(l.seats)}</td>
                <td className="text-xs">{faDate(l.startsAt)} تا {faDate(l.endsAt)}</td>
                <td>{faNumber(l.priceRials)}</td>
                <td><button className={l.active ? 'btn-ghost' : 'btn-danger'} onClick={() => toggle(l)}>{l.active ? 'فعال' : 'غیرفعال'}</button></td>
              </tr>
            ))}
            {licenses.length === 0 && <tr><td colSpan={6} className="py-6 text-center text-slate-500">لایسنسی صادر نشده است.</td></tr>}
          </tbody>
        </table>
      </section>

      <section className="card">
        <h2 className="mb-3 font-black">گزارش مالی ماهانه</h2>
        <table className="table">
          <thead><tr><th>ماه</th><th>دریافتی (ریال)</th><th>بازپرداخت</th><th>تراکنش‌ها</th></tr></thead>
          <tbody>
            {finance.map((r) => (
              <tr key={r.month}><td dir="ltr" className="text-right">{r.month}</td><td>{faNumber(r.paid)}</td><td>{faNumber(r.refunded)}</td><td>{faNumber(r.transactions)}</td></tr>
            ))}
            {finance.length === 0 && <tr><td colSpan={4} className="py-6 text-center text-slate-500">تراکنشی ثبت نشده است.</td></tr>}
          </tbody>
        </table>
      </section>
    </div>
  );
}
