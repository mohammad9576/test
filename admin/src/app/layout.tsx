import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'پنل مدیریت کدومو',
  description: 'مدیریت محتوا، رده‌بندی تربیتی، تالارها، لایسنس‌ها و گزارش‌های سازمانی کدومو',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="fa" dir="rtl">
      <body>{children}</body>
    </html>
  );
}
