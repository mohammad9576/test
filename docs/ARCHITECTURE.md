# معماری کدومو

## درخت پروژه

```
admin/Dockerfile
admin/next.config.mjs
admin/postcss.config.mjs
admin/src/app/(panel)/content/[id]/page.tsx
admin/src/app/(panel)/content/page.tsx
admin/src/app/(panel)/experts/page.tsx
admin/src/app/(panel)/families/page.tsx
admin/src/app/(panel)/forum/page.tsx
admin/src/app/(panel)/layout.tsx
admin/src/app/(panel)/licenses/page.tsx
admin/src/app/(panel)/page.tsx
admin/src/app/globals.css
admin/src/app/layout.tsx
admin/src/app/login/page.tsx
admin/src/components/Shell.tsx
admin/src/lib/api.ts
admin/src/lib/types.ts
admin/tailwind.config.ts
android/app/build.gradle.kts
android/app/src/main/AndroidManifest.xml
android/app/src/main/java/com/kodomo/app/KodomoApp.kt
android/app/src/main/java/com/kodomo/app/core/security/PinHasher.kt
android/app/src/main/java/com/kodomo/app/core/security/SecureFileCipher.kt
android/app/src/main/java/com/kodomo/app/core/util/PersianCalendar.kt
android/app/src/main/java/com/kodomo/app/core/util/PersianDigits.kt
android/app/src/main/java/com/kodomo/app/data/local/Converters.kt
android/app/src/main/java/com/kodomo/app/data/local/KodomoDatabase.kt
android/app/src/main/java/com/kodomo/app/data/local/Mappers.kt
android/app/src/main/java/com/kodomo/app/data/local/dao/Daos.kt
android/app/src/main/java/com/kodomo/app/data/local/entity/Entities.kt
android/app/src/main/java/com/kodomo/app/data/prefs/SessionPreferences.kt
android/app/src/main/java/com/kodomo/app/data/remote/ApiProvider.kt
android/app/src/main/java/com/kodomo/app/data/remote/Dto.kt
android/app/src/main/java/com/kodomo/app/data/remote/KodomoApi.kt
android/app/src/main/java/com/kodomo/app/data/remote/LiveUsageSocket.kt
android/app/src/main/java/com/kodomo/app/data/repository/ActivityRepositoryImpl.kt
android/app/src/main/java/com/kodomo/app/data/repository/CloudSyncRepository.kt
android/app/src/main/java/com/kodomo/app/data/repository/ContentRepositoryImpl.kt
android/app/src/main/java/com/kodomo/app/data/repository/DownloadRepositoryImpl.kt
android/app/src/main/java/com/kodomo/app/data/repository/EncyclopediaRepositoryImpl.kt
android/app/src/main/java/com/kodomo/app/data/repository/FamilyRepositoryImpl.kt
android/app/src/main/java/com/kodomo/app/data/repository/ForumRepositoryImpl.kt
android/app/src/main/java/com/kodomo/app/data/repository/MemoryRepositoryImpl.kt
android/app/src/main/java/com/kodomo/app/data/repository/ProjectRepositoryImpl.kt
android/app/src/main/java/com/kodomo/app/data/repository/RewardRepositoryImpl.kt
android/app/src/main/java/com/kodomo/app/data/repository/SchoolRepositoryImpl.kt
android/app/src/main/java/com/kodomo/app/data/repository/SettingsRepositoryImpl.kt
android/app/src/main/java/com/kodomo/app/data/seed/BookCatalog.kt
android/app/src/main/java/com/kodomo/app/data/seed/CatalogSeeder.kt
android/app/src/main/java/com/kodomo/app/data/seed/EncyclopediaCatalog.kt
android/app/src/main/java/com/kodomo/app/data/seed/LearningCatalog.kt
android/app/src/main/java/com/kodomo/app/data/seed/MediaCatalog.kt
android/app/src/main/java/com/kodomo/app/data/seed/ParentCatalog.kt
android/app/src/main/java/com/kodomo/app/data/seed/SeedBuilders.kt
android/app/src/main/java/com/kodomo/app/di/AppModules.kt
android/app/src/main/java/com/kodomo/app/domain/engine/AgeUiSpec.kt
android/app/src/main/java/com/kodomo/app/domain/engine/Catalogs.kt
android/app/src/main/java/com/kodomo/app/domain/engine/ContentPolicy.kt
android/app/src/main/java/com/kodomo/app/domain/engine/MascotDialogueEngine.kt
android/app/src/main/java/com/kodomo/app/domain/engine/ParentGateChallenge.kt
android/app/src/main/java/com/kodomo/app/domain/model/AgeGroup.kt
android/app/src/main/java/com/kodomo/app/domain/model/Enums.kt
android/app/src/main/java/com/kodomo/app/domain/model/Models.kt
android/app/src/main/java/com/kodomo/app/domain/repository/Repositories.kt
android/app/src/main/java/com/kodomo/app/kiosk/BootCompletedReceiver.kt
android/app/src/main/java/com/kodomo/app/kiosk/DeviceOwnerManager.kt
android/app/src/main/java/com/kodomo/app/kiosk/KidsLauncherActivity.kt
android/app/src/main/java/com/kodomo/app/kiosk/KioskController.kt
android/app/src/main/java/com/kodomo/app/kiosk/KodomoDeviceAdminReceiver.kt
android/app/src/main/java/com/kodomo/app/kiosk/ScreenTimeManager.kt
android/app/src/main/java/com/kodomo/app/ui/AppViewModel.kt
android/app/src/main/java/com/kodomo/app/ui/KodomoRoot.kt
android/app/src/main/java/com/kodomo/app/ui/components/Camera.kt
android/app/src/main/java/com/kodomo/app/ui/components/Charts.kt
android/app/src/main/java/com/kodomo/app/ui/components/ChildProfileForm.kt
android/app/src/main/java/com/kodomo/app/ui/components/Common.kt
android/app/src/main/java/com/kodomo/app/ui/components/KodomoAvatar.kt
android/app/src/main/java/com/kodomo/app/ui/components/ParentGateDialog.kt
android/app/src/main/java/com/kodomo/app/ui/components/Pickers.kt
android/app/src/main/java/com/kodomo/app/ui/kids/KidArgs.kt
android/app/src/main/java/com/kodomo/app/ui/kids/KidSpaceFrame.kt
android/app/src/main/java/com/kodomo/app/ui/kids/books/BookListScreen.kt
android/app/src/main/java/com/kodomo/app/ui/kids/books/BookReaderScreen.kt
android/app/src/main/java/com/kodomo/app/ui/kids/books/BookViewModels.kt
android/app/src/main/java/com/kodomo/app/ui/kids/browser/SafeBrowserScreens.kt
android/app/src/main/java/com/kodomo/app/ui/kids/explore/ExploreScreen.kt
android/app/src/main/java/com/kodomo/app/ui/kids/games/ColorHuntGame.kt
android/app/src/main/java/com/kodomo/app/ui/kids/games/GamesScreen.kt
android/app/src/main/java/com/kodomo/app/ui/kids/games/JumpPopGame.kt
android/app/src/main/java/com/kodomo/app/ui/kids/games/MotionGameScreen.kt
android/app/src/main/java/com/kodomo/app/ui/kids/games/Sensors.kt
android/app/src/main/java/com/kodomo/app/ui/kids/games/ShakeCleanGame.kt
android/app/src/main/java/com/kodomo/app/ui/kids/games/SimonSaysGame.kt
android/app/src/main/java/com/kodomo/app/ui/kids/games/TiltRescueGame.kt
android/app/src/main/java/com/kodomo/app/ui/kids/home/InterestsScreen.kt
android/app/src/main/java/com/kodomo/app/ui/kids/home/KidHomeScreen.kt
android/app/src/main/java/com/kodomo/app/ui/kids/learning/LearningScreens.kt
android/app/src/main/java/com/kodomo/app/ui/kids/music/MusicScreen.kt
android/app/src/main/java/com/kodomo/app/ui/kids/projects/ProjectScreens.kt
android/app/src/main/java/com/kodomo/app/ui/kids/rewards/RewardsScreen.kt
android/app/src/main/java/com/kodomo/app/ui/kids/school/SchoolScreen.kt
android/app/src/main/java/com/kodomo/app/ui/kids/video/FeedbackScreen.kt
android/app/src/main/java/com/kodomo/app/ui/kids/video/VideoListScreen.kt
android/app/src/main/java/com/kodomo/app/ui/kids/video/VideoPlayerScreen.kt
android/app/src/main/java/com/kodomo/app/ui/kids/video/VideoViewModels.kt
android/app/src/main/java/com/kodomo/app/ui/mascot/KodomoMascot.kt
android/app/src/main/java/com/kodomo/app/ui/mascot/MascotVoice.kt
android/app/src/main/java/com/kodomo/app/ui/navigation/KodomoNavHost.kt
android/app/src/main/java/com/kodomo/app/ui/navigation/Routes.kt
android/app/src/main/java/com/kodomo/app/ui/onboarding/OnboardingScreen.kt
android/app/src/main/java/com/kodomo/app/ui/onboarding/OnboardingViewModel.kt
android/app/src/main/java/com/kodomo/app/ui/parent/ParentHubScreen.kt
android/app/src/main/java/com/kodomo/app/ui/parent/ParentHubViewModel.kt
android/app/src/main/java/com/kodomo/app/ui/parent/cloud/CloudAccountScreen.kt
android/app/src/main/java/com/kodomo/app/ui/parent/community/CommunityScreens.kt
android/app/src/main/java/com/kodomo/app/ui/parent/dashboard/DashboardTab.kt
android/app/src/main/java/com/kodomo/app/ui/parent/family/FamilyScreens.kt
android/app/src/main/java/com/kodomo/app/ui/parent/ratings/RatingsScreens.kt
android/app/src/main/java/com/kodomo/app/ui/parent/reports/ReportsTab.kt
android/app/src/main/java/com/kodomo/app/ui/profiles/ProfilePickerScreen.kt
android/app/src/main/java/com/kodomo/app/ui/theme/Theme.kt
android/app/src/main/res/drawable/ic_launcher_foreground.xml
android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
android/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
android/app/src/main/res/values/colors.xml
android/app/src/main/res/values/strings.xml
android/app/src/main/res/values/themes.xml
android/app/src/main/res/xml/data_extraction_rules.xml
android/app/src/main/res/xml/device_admin_policies.xml
android/app/src/main/res/xml/locales_config.xml
android/app/src/main/res/xml/network_security_config.xml
android/app/src/test/java/com/kodomo/app/DomainLogicTest.kt
android/build.gradle.kts
android/gradle/libs.versions.toml
android/settings.gradle.kts
backend/Dockerfile
backend/src/admin/admin.controller.ts
backend/src/admin/admin.dto.ts
backend/src/admin/admin.module.ts
backend/src/admin/admin.service.ts
backend/src/app.module.ts
backend/src/auth/auth.controller.ts
backend/src/auth/auth.dto.ts
backend/src/auth/auth.module.ts
backend/src/auth/auth.service.ts
backend/src/auth/sms.service.ts
backend/src/common/auth.ts
backend/src/common/redis.ts
backend/src/content/content.module.ts
backend/src/entities/index.ts
backend/src/families/families.module.ts
backend/src/forum/forum.module.ts
backend/src/health.controller.ts
backend/src/live/live.gateway.ts
backend/src/main.ts
backend/src/scripts/create-admin.ts
backend/src/storage/storage.module.ts
```

## لایه‌های اپ اندروید (Clean Architecture + MVVM)

```
ui (Compose screens + @HiltViewModel)  ──►  domain (models, engines, repository interfaces)  ◄──  data (Room, DataStore, Retrofit/OkHttp, seed)
                     ▲                                                                         │
                     └────────────── kiosk (KidsLauncherActivity, DeviceOwnerManager, ScreenTimeManager) ◄┘
```

- **domain/engine**: `AgeUiSpec` (موتور تطبیق سنی: نوپا ۲–۴، کودک ۵–۸، نوجوان ۹+)، `ParentGateChallenge` و `ParentGateThrottle`، `MascotDialogueEngine`، `ContentPolicy` (فیلتر سن + آستانه‌های خشونت/ترس/ناهنجاری)، کاتالوگ مدال‌ها و پاداش‌های خانوادگی.
- **data/local**: پایگاه Room با ۱۸ جدول (خانواده، کودکان، تنظیمات والدین، محتوا و صفحات، بازخورد، دانلود آفلاین، لاگ فعالیت، برنامه/تکالیف/امتحان، پروژه‌ها، مدال‌ها، پاداش‌ها، دانشنامه FTS4، تالار، خاطرات).
- **kiosk/ScreenTimeManager**: شمارش ثانیه‌به‌ثانیه به تفکیک دسته، هشدار دوستانه‌ی ۵ دقیقه‌ای ربات، قفل در پایان سهمیه یا ساعت خواب (با کاهش نور)، ضربان زنده‌ی WebSocket هر ۳۰ ثانیه.
- **امنیت**: PIN با PBKDF2-SHA256 (۱۲۰هزار تکرار)، گالری خاطرات با AES-256-GCM در Android Keystore، `FLAG_SECURE`، پنهان‌سازی پنجره‌های شناور (API 31+)، بدون پشتیبان‌گیری ابری داده‌ها، مرورگر امن بدون WebView و بدون دسترسی به وب آزاد.

## قرارداد API (پیشوند `/v1`)

| متد | مسیر | توضیح |
|---|---|---|
| POST | `/auth/otp/request` | `{phone}` → `{requestId, expiresInSeconds}` (Redis، محدودیت ارسال ۶۰ ثانیه) |
| POST | `/auth/otp/verify` | `{requestId, phone, code}` → `{accessToken, refreshToken, familyId}` |
| PUT | `/families/me` | همگام‌سازی ابری پروفایل خانواده و کودکان |
| GET | `/content/catalog?since=` | کاتالوگ منتشرشده با شمارش نظر همسالان |
| POST | `/content/feedback` | ثبت «دوست داشتم/نداشتم» کودک |
| GET/POST | `/forum/topics`، `/forum/topics/:id/posts`، `/forum/posts/:id/report` | تالار والدین |
| WS | `/live` | پایش زنده‌ی مصرف (اپ: هدر Bearer، پنل: `?token=`) |
| * | `/admin/...` | CMS، بارگذاری MinIO، خانواده‌ها، لایسنس و مالی، نظارت تالار، کارشناسان، کاربران ادمین (نقش‌محور) |
