'use client';

import { useParams, useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { api } from '@/lib/api';
import { CONTENT_TYPE_LABELS, INTERESTS, type Content, type ContentPage, type ContentType } from '@/lib/types';

type Draft = Omit<Content, 'id' | 'updatedAt'>;

const EMPTY: Draft = {
  type: 'VIDEO', title: '', description: '', body: '', category: '', emoji: '✨', colorStart: '4290372811', colorEnd: '4284463498',
  mediaUrl: null, durationMinutes: 0, minAge: 3, maxAge: 12, violence: 1, fear: 1, educational: 3, abnormality: 1, creativity: 3,
  familyValues: 3, positives: [], negatives: [], expertReview: '', expertName: '', interests: [], author: '', pages: [], published: false,
};

const TAGS: { key: 'violence' | 'fear' | 'educational' | 'abnormality' | 'creativity' | 'familyValues'; label: string; positive: boolean }[] = [
  { key: 'violence', label: 'خشونت', positive: false },
  { key: 'fear', label: 'ترس', positive: false },
  { key: 'abnormality', label: 'ناهنجاری', positive: false },
  { key: 'educational', label: 'آموزندگی', positive: true },
  { key: 'creativity', label: 'خلاقیت', positive: true },
  { key: 'familyValues', label: 'ارزش خانوادگی', positive: true },
];

/** Converts between Android ARGB Long colours (stored as decimal strings) and CSS hex colours. */
const argbToHex = (v: string) => `#${(Number(v) & 0xffffff).toString(16).padStart(6, '0')}`;
const hexToArgb = (hex: string) => String(0xff000000 + parseInt(hex.slice(1), 16));

export default function ContentEditorPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const isNew = params.id === 'new';
  const [draft, setDraft] = useState<Draft>(EMPTY);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    if (isNew) return;
    api<Content>(`/admin/content/${params.id}`)
      .then(({ id: _id, updatedAt: _u, ...rest }) => setDraft({ ...EMPTY, ...rest, colorStart: String(rest.colorStart), colorEnd: String(rest.colorEnd) }))
      .catch((e: Error) => setError(e.message));
  }, [isNew, params.id]);

  const set = <K extends keyof Draft>(key: K, value: Draft[K]) => setDraft((d) => ({ ...d, [key]: value }));

  async function upload(file: File) {
    setUploading(true);
    setError(null);
    try {
      const { uploadUrl, publicUrl } = await api<{ uploadUrl: string; publicUrl: string }>('/admin/uploads', { method: 'POST', body: JSON.stringify({ fileName: file.name }) });
      const res = await fetch(uploadUrl, { method: 'PUT', body: file, headers: { 'Content-Type': file.type || 'application/octet-stream' } });
      if (!res.ok) throw new Error(`آپلود ناموفق بود (${res.status})`);
      set('mediaUrl', publicUrl);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setUploading(false);
    }
  }

  async function save() {
    setSaving(true);
    setError(null);
    try {
      const body = JSON.stringify({ ...draft, mediaUrl: draft.mediaUrl || null, pages: draft.pages.map((p, i) => ({ ...p, index: i })) });
      if (isNew) await api('/admin/content', { method: 'POST', body });
      else await api(`/admin/content/${params.id}`, { method: 'PUT', body });
      router.push('/content');
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setSaving(false);
    }
  }

  async function remove() {
    if (!window.confirm('این محتوا حذف شود؟')) return;
    await api(`/admin/content/${params.id}`, { method: 'DELETE' });
    router.push('/content');
  }

  const listField = (key: 'positives' | 'negatives', label: string) => (
    <div>
      <label className="label">{label} (هر مورد در یک خط)</label>
      <textarea className="input min-h-20" value={draft[key].join('\n')} onChange={(e) => set(key, e.target.value.split('\n').map((s) => s.trim()).filter(Boolean))} />
    </div>
  );

  const updatePage = (i: number, patch: Partial<ContentPage>) => set('pages', draft.pages.map((p, j) => (j === i ? { ...p, ...patch } : p)));

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-black">{isNew ? 'محتوای جدید' : `ویرایش: ${draft.title}`}</h1>
        <div className="flex gap-2">
          {!isNew && <button className="btn-danger" onClick={remove}>حذف</button>}
          <button className="btn-primary" onClick={save} disabled={saving || !draft.title}>{saving ? 'در حال ذخیره...' : 'ذخیره'}</button>
        </div>
      </div>
      {error && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}

      <section className="card grid gap-4 md:grid-cols-2">
        <div>
          <label className="label">نوع محتوا</label>
          <select className="input" value={draft.type} onChange={(e) => set('type', e.target.value as ContentType)}>
            {Object.entries(CONTENT_TYPE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
        </div>
        <div><label className="label">عنوان</label><input className="input" value={draft.title} onChange={(e) => set('title', e.target.value)} /></div>
        <div><label className="label">دسته</label><input className="input" value={draft.category} onChange={(e) => set('category', e.target.value)} /></div>
        <div className="grid grid-cols-3 gap-2">
          <div><label className="label">ایموجی</label><input className="input" value={draft.emoji} onChange={(e) => set('emoji', e.target.value)} /></div>
          <div><label className="label">رنگ شروع</label><input type="color" className="input h-10 p-1" value={argbToHex(draft.colorStart)} onChange={(e) => set('colorStart', hexToArgb(e.target.value))} /></div>
          <div><label className="label">رنگ پایان</label><input type="color" className="input h-10 p-1" value={argbToHex(draft.colorEnd)} onChange={(e) => set('colorEnd', hexToArgb(e.target.value))} /></div>
        </div>
        <div className="md:col-span-2"><label className="label">توضیح کوتاه</label><textarea className="input min-h-20" value={draft.description} onChange={(e) => set('description', e.target.value)} /></div>
        {(draft.type === 'ARTICLE' || draft.type === 'WEEKEND_IDEA') && (
          <div className="md:col-span-2"><label className="label">متن کامل</label><textarea className="input min-h-48" value={draft.body} onChange={(e) => set('body', e.target.value)} /></div>
        )}
        <div><label className="label">پدیدآورنده / منبع</label><input className="input" value={draft.author} onChange={(e) => set('author', e.target.value)} /></div>
        <div><label className="label">مدت (دقیقه)</label><input type="number" min={0} className="input" value={draft.durationMinutes} onChange={(e) => set('durationMinutes', Number(e.target.value))} /></div>
      </section>

      <section className="card space-y-3">
        <h2 className="font-black">فایل رسانه</h2>
        <input className="input" dir="ltr" placeholder="لینک آپارات، یوتیوب یا تلوبیون، یا فایل مستقیم https://...mp4" value={draft.mediaUrl ?? ''} onChange={(e) => set('mediaUrl', e.target.value)} />
        <input type="file" accept="video/*,audio/*" disabled={uploading} onChange={(e) => e.target.files?.[0] && upload(e.target.files[0])} />
        {uploading && <p className="text-sm text-slate-500">در حال بارگذاری در MinIO...</p>}
      </section>

      <section className="card space-y-4">
        <h2 className="font-black">رده‌بندی تربیتی کدومو</h2>
        <div className="grid grid-cols-2 gap-4">
          <div><label className="label">حداقل سن</label><input type="number" min={0} max={18} className="input" value={draft.minAge} onChange={(e) => set('minAge', Number(e.target.value))} /></div>
          <div><label className="label">حداکثر سن</label><input type="number" min={0} max={18} className="input" value={draft.maxAge} onChange={(e) => set('maxAge', Number(e.target.value))} /></div>
        </div>
        <div className="grid gap-3 md:grid-cols-2">
          {TAGS.map((t) => (
            <div key={t.key}>
              <label className="label">{t.label}: {draft[t.key].toLocaleString('fa-IR')} از ۵</label>
              <input
                type="range" min={1} max={5} step={1} value={draft[t.key]} onChange={(e) => set(t.key, Number(e.target.value))}
                className={`w-full ${t.positive ? 'accent-emerald-600' : 'accent-amber-500'}`}
              />
            </div>
          ))}
        </div>
        <div className="grid gap-4 md:grid-cols-2">
          {listField('positives', 'نقاط مثبت')}
          {listField('negatives', 'نکات قابل توجه')}
        </div>
        <div><label className="label">دیدگاه کارشناس</label><textarea className="input min-h-24" value={draft.expertReview} onChange={(e) => set('expertReview', e.target.value)} /></div>
        <div><label className="label">نام کارشناس</label><input className="input" value={draft.expertName} onChange={(e) => set('expertName', e.target.value)} /></div>
        <div>
          <label className="label">علاقه‌مندی‌های مرتبط</label>
          <div className="flex flex-wrap gap-2">
            {Object.entries(INTERESTS).map(([k, v]) => {
              const on = draft.interests.includes(k);
              return (
                <button type="button" key={k} onClick={() => set('interests', on ? draft.interests.filter((x) => x !== k) : [...draft.interests, k])}
                  className={`pill px-3 py-1 ${on ? 'bg-kodomo text-white' : 'bg-kodomo-mint text-kodomo-dark'}`}>{v}</button>
              );
            })}
          </div>
        </div>
      </section>

      {(draft.type === 'BOOK' || draft.type === 'LEARNING') && (
        <section className="card space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="font-black">{draft.type === 'BOOK' ? 'صفحات کتاب' : 'گام‌های آموزش'}</h2>
            <button className="btn-ghost" onClick={() => set('pages', [...draft.pages, { index: draft.pages.length, text: '', emoji: '📄' }])}>➕ افزودن</button>
          </div>
          {draft.pages.map((p, i) => (
            <div key={i} className="flex gap-2">
              <span className="w-8 pt-2 text-center text-sm font-bold">{(i + 1).toLocaleString('fa-IR')}</span>
              <input className="input w-16" value={p.emoji} onChange={(e) => updatePage(i, { emoji: e.target.value })} />
              <textarea className="input min-h-16 flex-1" value={p.text} onChange={(e) => updatePage(i, { text: e.target.value })} />
              <button className="btn-danger" onClick={() => set('pages', draft.pages.filter((_, j) => j !== i))}>×</button>
            </div>
          ))}
        </section>
      )}

      <label className="card flex items-center gap-3">
        <input type="checkbox" checked={draft.published} onChange={(e) => set('published', e.target.checked)} className="h-5 w-5 accent-emerald-600" />
        <span className="font-bold">انتشار برای خانواده‌ها (همگام‌سازی در اپلیکیشن)</span>
      </label>
    </div>
  );
}
