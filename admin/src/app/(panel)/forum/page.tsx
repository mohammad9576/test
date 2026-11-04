'use client';

import { useEffect, useState } from 'react';
import { api, faDate, faNumber } from '@/lib/api';
import { ROLE_LABELS, type Expert, type ForumReport, type ForumTopic } from '@/lib/types';

export default function ForumModerationPage() {
  const [reports, setReports] = useState<ForumReport[]>([]);
  const [topics, setTopics] = useState<ForumTopic[]>([]);
  const [experts, setExperts] = useState<Expert[]>([]);
  const [reply, setReply] = useState<{ topicId: string; expertId: string; body: string } | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    Promise.all([api<ForumReport[]>('/admin/forum/reports'), api<ForumTopic[]>('/admin/forum/topics'), api<Expert[]>('/admin/experts')])
      .then(([r, t, e]) => { setReports(r); setTopics(t); setExperts(e.filter((x) => x.verified)); })
      .catch((e: Error) => setError(e.message));
  };
  useEffect(load, []);

  async function decide(id: string, status: 'DISMISSED' | 'ACTIONED') {
    await api(`/admin/forum/reports/${id}`, { method: 'POST', body: JSON.stringify({ status }) });
    load();
  }

  async function hide(t: ForumTopic) {
    await api(`/admin/forum/topics/${t.id}/hidden?value=${!t.hidden}`, { method: 'POST' });
    load();
  }

  async function sendReply() {
    if (!reply) return;
    try {
      await api(`/admin/forum/topics/${reply.topicId}/expert-reply`, { method: 'POST', body: JSON.stringify({ expertId: reply.expertId, body: reply.body }) });
      setReply(null);
      load();
    } catch (e) {
      setError((e as Error).message);
    }
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-black">نظارت بر تالارهای گفتگو</h1>
      {error && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}

      <section className="card space-y-3">
        <h2 className="font-black">گزارش‌های تخلف ({faNumber(reports.length)})</h2>
        {reports.length === 0 && <p className="text-sm text-slate-500">گزارش بازی وجود ندارد.</p>}
        {reports.map((r) => (
          <div key={r.id} className="rounded-xl border border-amber-200 bg-amber-50 p-3">
            <div className="text-xs text-slate-500">تاپیک: {r.post.topic?.title} • {faDate(r.createdAt)} • دلیل: {r.reason}</div>
            <p className="my-2 text-sm">«{r.post.body}» — {r.post.authorName}</p>
            <div className="flex gap-2">
              <button className="btn-danger" onClick={() => decide(r.id, 'ACTIONED')}>پنهان کردن پست</button>
              <button className="btn-ghost" onClick={() => decide(r.id, 'DISMISSED')}>رد گزارش</button>
            </div>
          </div>
        ))}
      </section>

      <section className="card space-y-3">
        <h2 className="font-black">تاپیک‌ها</h2>
        {topics.map((t) => {
          const expertAnswered = t.posts?.some((p) => p.authorRole !== 'PARENT');
          return (
            <div key={t.id} className={`rounded-xl p-3 ring-1 ring-black/5 ${t.hidden ? 'bg-slate-100 opacity-60' : 'bg-white'}`}>
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <b>{t.title}</b>
                  <div className="text-xs text-slate-500">{t.authorName} • {t.category} • {faNumber(t.posts?.length ?? 0)} پاسخ • {faDate(t.createdAt)}</div>
                </div>
                <div className="flex items-center gap-2">
                  {expertAnswered ? <span className="pill bg-emerald-100 text-emerald-700">پاسخ کارشناس</span> : <span className="pill bg-amber-100 text-amber-700">بدون پاسخ کارشناس</span>}
                  <button className="btn-ghost" onClick={() => setReply({ topicId: t.id, expertId: experts[0]?.id ?? '', body: '' })} disabled={experts.length === 0}>پاسخ کارشناس</button>
                  <button className="btn-danger" onClick={() => hide(t)}>{t.hidden ? 'نمایش' : 'پنهان'}</button>
                </div>
              </div>
              <p className="mt-2 text-sm text-slate-700">{t.body}</p>
              {reply?.topicId === t.id && (
                <div className="mt-3 space-y-2 rounded-xl bg-kodomo-mint p-3">
                  <select className="input" value={reply.expertId} onChange={(e) => setReply({ ...reply, expertId: e.target.value })}>
                    {experts.map((x) => <option key={x.id} value={x.id}>{x.fullName} – {ROLE_LABELS[x.role] ?? x.role}</option>)}
                  </select>
                  <textarea className="input min-h-24" value={reply.body} onChange={(e) => setReply({ ...reply, body: e.target.value })} placeholder="پاسخ تخصصی..." />
                  <div className="flex gap-2">
                    <button className="btn-primary" onClick={sendReply} disabled={!reply.body || !reply.expertId}>ارسال</button>
                    <button className="btn-ghost" onClick={() => setReply(null)}>انصراف</button>
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </section>
    </div>
  );
}
