# کدومو (Kodomo) – سوپر اپلیکیشن و لانچر امن کودک و خانواده

زیست‌بوم دیجیتال خانواده در سه بخش به‌هم‌پیوسته:

| بخش | مسیر | فناوری |
|---|---|---|
| اپلیکیشن اندروید (لانچر کودک + هاب والدین) | `android/` | Kotlin، Jetpack Compose/Material 3، MVVM + Clean Architecture، Hilt، Room (FTS4)، DataStore، Coroutines/Flow، Media3 ExoPlayer، CameraX |
| سرور (REST + WebSocket) | `backend/` | NestJS (ماژولار مونولیت)، PostgreSQL/TypeORM، Redis، MinIO/S3، JWT + OTP |
| پنل سوپر ادمین | `admin/` | Next.js 14 (App Router)، React 18، Tailwind CSS |

## ساخت APK

```bash
cd android
./gradlew assembleDebug        # خروجی: app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # تست‌های واحد منطق دامنه
```

پایپ‌لاین `.github/workflows/build-apk.yml` روی هر push همین مراحل را روی JDK 17 و Android SDK 35 اجرا می‌کند و `app-debug.apk` را به‌عنوان artifact منتشر می‌کند.

## فعال‌سازی قفل کامل کیوسک (Device Owner)

روی دستگاهی که حساب گوگل ندارد (یا پس از بازنشانی کارخانه‌ای):

```bash
adb install app-debug.apk
adb shell dpm set-device-owner com.kodomo.app/.kiosk.KodomoDeviceAdminReceiver
```

سپس در «پنل والدین ← داشبورد ← قفل امن کیوسک» کلید را روشن کنید. در این حالت:
`startLockTask` با `setLockTaskPackages`، غیرفعال‌سازی نوار وضعیت و تنظیمات سریع (`setStatusBarDisabled` و `LOCK_TASK_FEATURE_NONE`)، ثبت کدومو به‌عنوان HOME دائمی، و محدودیت‌های `DISALLOW_SAFE_BOOT`، `DISALLOW_FACTORY_RESET`، `DISALLOW_INSTALL_APPS`، `DISALLOW_UNINSTALL_APPS`، `DISALLOW_MOUNT_PHYSICAL_MEDIA`، `DISALLOW_USB_FILE_TRANSFER`، `DISALLOW_CREATE_WINDOWS` و `DISALLOW_CONFIG_DATE_TIME` اعمال می‌شوند.
بدون Device Owner، اپ به حالت «سنجاق صفحه» اندروید (با تأیید کاربر) و لانچر پیش‌فرض بسنده می‌کند.

خروج از محیط کودک فقط با «گیت والدین» ممکن است: نگه داشتن دکمه‌ی قفل + رمز ۴ رقمی + سؤال ریاضی تصادفی (ارقام فارسی و انگلیسی پذیرفته می‌شوند؛ پس از ۵ خطا قفل موقت).

## راه‌اندازی سرور و پنل

```bash
cp backend/.env.example .env          # حتماً JWT_SECRET را مقداردهی کنید
docker compose up -d --build          # PostgreSQL، Redis، MinIO، API (پورت 3000)، پنل (پورت 3001)
docker compose exec api sh -c "ADMIN_EMAIL=admin@kodomo.ir ADMIN_PASSWORD='***' node dist/scripts/create-admin.js"
```

در اپ اندروید آدرس سرور در «خانواده ← حساب ابری» قابل تنظیم است (پیش‌فرض `BuildConfig.API_BASE_URL`).
بدون `KAVENEGAR_API_KEY` کد OTP در لاگ سرور چاپ می‌شود (حالت توسعه).

جزئیات معماری، درخت پروژه و قرارداد API در [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) آمده است.

## مجوز محتوای نمونه

- فونت Vazirmatn – SIL Open Font License (`docs/licenses/Vazirmatn-OFL.txt`)
- ویدیوهای نمونه‌ی داخلی: فیلم‌های آزاد Blender Foundation (CC-BY)؛ قطعات موسیقی نمونه: SoundHelix
- قصه‌ها، درس‌ها، دانشنامه و مقاله‌ها متن اصلی نوشته‌شده برای کدومو هستند. کاتالوگ اصلی از طریق CMS پنل ادمین منتشر و در اپ همگام می‌شود.
