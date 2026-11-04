'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { api, setToken } from '@/lib/api';

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const res = await api<{ accessToken: string }>('/auth/admin/login', { method: 'POST', body: JSON.stringify({ email, password }) });
      setToken(res.accessToken);
      router.replace('/');
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-gradient-to-b from-kodomo-mint to-kodomo-cream p-4">
      <form onSubmit={submit} className="card w-full max-w-sm space-y-4">
        <div className="text-center">
          <div className="mx-auto mb-2 flex h-14 w-14 items-center justify-center rounded-2xl bg-kodomo text-2xl font-black text-white">ک</div>
          <h1 className="text-xl font-black text-kodomo">پنل مدیریت کدومو</h1>
          <p className="text-xs text-slate-500">همراه امن خانواده</p>
        </div>
        <div>
          <label className="label" htmlFor="email">ایمیل</label>
          <input id="email" className="input" type="email" dir="ltr" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </div>
        <div>
          <label className="label" htmlFor="password">رمز عبور</label>
          <input id="password" className="input" type="password" dir="ltr" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={8} />
        </div>
        {error && <p className="text-sm text-red-600">{error}</p>}
        <button className="btn-primary w-full" disabled={busy}>{busy ? 'در حال ورود...' : 'ورود'}</button>
      </form>
    </main>
  );
}
