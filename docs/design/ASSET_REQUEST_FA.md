# درخواست ساخت دارایی‌های هنری جداگانه (برای ChatGPT / ابزار تصویرسازی)

این متن را همراه تصویر `00_APPROVED_STYLE_MASTER.png` برای ChatGPT بفرستید. هر تصویر باید **جداگانه**، **با پس‌زمینه‌ی کاملاً شفاف (PNG)**، **بدون هیچ متن**، در **سبک دقیق بورد مرجع** (3D clay، نور نرم از بالا-چپ، سایه‌ی نرم، رنگ‌های پاستلی شاد) ساخته شود. نام فایل‌ها را دقیقاً همین بگذارید و همه را در یک ZIP بفرستید تا در `android/app/src/main/assets/art/` قرار بگیرد؛ اپ خودکار جایگزین نسخه‌ی کدی می‌کند.

## ۱. شخصیت‌ها (۱۰۲۴×۱۰۲۴، نیم‌تنه و تمام‌قد)
دخترها **همیشه با روسری یا پوشش کامل مو**، لباس پوشیده و شاد.
- `characters/girl.png` — دختر ۵ تا ۹ ساله، روسری صورتی، لبخند، نیم‌تنه
- `characters/boy.png` — پسر ۵ تا ۹ ساله، کلاه سبز، کوله، نیم‌تنه
- `characters/little_girl.png` و `characters/little_boy.png` — ۳ تا ۵ ساله، صورت گردتر
- `characters/teen_girl.png` — دختر ۱۰ تا ۱۵، روسری بنفش، ظاهر بالغ‌تر
- `characters/teen_boy.png` — پسر ۱۰ تا ۱۵، هودی و هدفون
- برای هر کدام سه حالت هم: `_wave` (دست تکان می‌دهد)، `_happy` (خوشحال، تمام‌قد)، `_sleep` (خواب، برای صفحه‌ی وقت خواب)
- `characters/mother.png` (با روسری/چادر رنگی)، `characters/father.png`

## ۲. ربات کدومو (۱۰۲۴×۱۰۲۴)
ربات سفید و سبز با هدفون و صورت مشکی و چشم‌های سبز (مثل بورد): `robot/happy.png`، `robot/wave.png`، `robot/think.png`، `robot/sleep.png`، `robot/cheer.png`، `robot/point.png`

## ۳. ستاره‌ی جادوگر و همراه‌ها (۱۰۲۴×۱۰۲۴)
`buddies/star_wizard.png` (ستاره‌ی زرد با کلاه جادوگر بنفش) + پاندا، بره، دلفین، شیر، اژدها، جغد: `buddies/panda.png` … 

## ۴. آیکون‌های بخش‌ها (۵۱۲×۵۱۲، آیکون clay روی مربع گرد براق به رنگ بخش)
`icons/videos.png` (پخش، آبی) · `icons/games.png` (دسته‌ی بازی، صورتی) · `icons/books.png` (کتاب، نارنجی) · `icons/study.png` (دفتر و مداد، زرد) · `icons/english.png` (Aa، بنفش) · `icons/livetv.png` (تلویزیون) · `icons/music.png` (نت، بنفش) · `icons/discover.png` (ذره‌بین/لامپ، سبز) · `icons/crafts.png` (قیچی و کاغذ) · `icons/camera.png` · `icons/call.png` (گوشی سبز) · `icons/planner.png` (تقویم) · `icons/chores.png` (جارو و ستاره) · `icons/apps.png` · `icons/rewards.png` (جام) · `icons/interests.png` (قلب) · `icons/gallery.png` · `icons/lock.png`

## ۵. لایه‌های پس‌زمینه (۱۰۸۰×۲۴۰۰، شفاف جز آسمان)
برای هر سبک: `scenes/meadow_sky.png`، `scenes/meadow_hills.png`، `scenes/meadow_front.png` (گل و چمن جلو) — و همین سه لایه برای `blossom_` (قصر صورتی)، `sky_`، `galaxy_` (نوجوان، کوه و ستاره)، `night_`

## ۶. تصاویر بازی‌ها و مراحل
- کاور هر گروه بازی: `games/quest.png`، `games/body.png`، `games/party.png`، `games/brain.png`، `games/art.png`، `games/sport.png`
- نقشه‌ی ماجراجویی ۱۰ مرحله‌ای (جنگل، شهر، فضا، اقیانوس): `quests/forest_map.png` …
- شخصیت بازی در حال دویدن/پریدن: `play/boy_run.png`، `play/girl_run.png`، سکه، ستاره، جواهر

## ۷. صفحه‌های حالت
`states/bedtime.png` (کودک خوابیده با ستاره)، `states/time_up.png` (دختر بغل‌گرفتن ستاره)، `states/school.png`، `states/no_internet.png`، `states/empty_box.png`، `states/success.png`

## ۸. کاردستی‌ها (هر مرحله یک تصویر، ۱۰۲۴×۷۶۸، دست‌ها و کاغذ)
برای هر آموزش در `domain/crafts/CraftGuides.kt` (مثلاً قایق کاغذی ۶ مرحله): `crafts/o_boat_1.png` … `crafts/o_boat_6.png`

> مجوز: تصاویر باید برای استفاده‌ی تجاری در اپ آزاد باشند؛ منبع/ابزار ساخت را در `assets/art/LICENSE.txt` بنویسید.
