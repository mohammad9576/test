export type ContentType = 'VIDEO' | 'BOOK' | 'MUSIC' | 'LEARNING' | 'EXPLORE' | 'ARTICLE' | 'WEEKEND_IDEA';

export const CONTENT_TYPE_LABELS: Record<ContentType, string> = {
  VIDEO: 'فیلم و انیمیشن',
  BOOK: 'کتاب',
  MUSIC: 'موسیقی',
  LEARNING: 'آموزش',
  EXPLORE: 'کشف محتوا',
  ARTICLE: 'مقاله والدین',
  WEEKEND_IDEA: 'دورهمی آخر هفته',
};

export const INTERESTS: Record<string, string> = {
  SPORT: 'ورزش', ASTRONOMY: 'نجوم', ART: 'هنر', GAMES: 'بازی', SCIENCE: 'علوم', ANIMALS: 'حیوانات', MUSIC: 'موسیقی', BOOKS: 'کتاب', MOVIES: 'فیلم',
};

export interface ContentPage { index: number; text: string; emoji: string }

export interface Content {
  id: string;
  type: ContentType;
  title: string;
  description: string;
  body: string;
  category: string;
  emoji: string;
  colorStart: string;
  colorEnd: string;
  mediaUrl: string | null;
  durationMinutes: number;
  minAge: number;
  maxAge: number;
  violence: number;
  fear: number;
  educational: number;
  abnormality: number;
  creativity: number;
  familyValues: number;
  positives: string[];
  negatives: string[];
  expertReview: string;
  expertName: string;
  interests: string[];
  author: string;
  pages: ContentPage[];
  published: boolean;
  updatedAt: string;
}

export interface Stats {
  families: number;
  children: number;
  content: number;
  published: number;
  openReports: number;
  activeLicenses: number;
  pendingExperts: number;
  revenueRials: string;
  liveFamilies: number;
  usageMinutesLast24h: { category: string; minutes: number }[];
}

export interface Child { id: string; name: string; birthDate: string; dailyLimitMinutes: number; stars: number; interests: string[] }
export interface Organization { id: string; name: string; kind: 'SCHOOL' | 'ORGANIZATION'; city?: string; contactPhone?: string }
export interface Family {
  id: string; phone: string; familyName: string; fatherName: string; motherName: string; children: Child[];
  organization?: Organization | null; lastSyncAt?: string | null; createdAt: string;
}
export interface License {
  id: string; kind: 'FAMILY' | 'SCHOOL' | 'ORGANIZATION'; seats: number; startsAt: string; endsAt: string; priceRials: string; active: boolean;
  family?: Family | null; organization?: Organization | null;
}
export interface FinanceRow { month: string; paid: string; refunded: string; transactions: string }
export interface ForumPost { id: string; body: string; authorName: string; authorRole: string; hidden: boolean; createdAt: string; topic?: ForumTopic }
export interface ForumTopic { id: string; title: string; body: string; authorName: string; category: string; hidden: boolean; createdAt: string; posts?: ForumPost[] }
export interface ForumReport { id: string; reason: string; status: string; createdAt: string; post: ForumPost }
export interface Expert { id: string; fullName: string; phone: string; role: string; licenseNumber: string; verified: boolean }

export const ROLE_LABELS: Record<string, string> = {
  PARENT: 'والد', PSYCHOLOGIST: 'روانشناس کودک', COUNSELOR: 'مشاور خانواده', PEDIATRICIAN: 'پزشک کودکان', MEDIA_EXPERT: 'کارشناس رسانه',
};
