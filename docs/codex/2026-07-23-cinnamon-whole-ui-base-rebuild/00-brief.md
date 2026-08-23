# بریف بازسازی کامل پایهٔ UI سینامون

- Task ID: `2026-07-23-cinnamon-whole-ui-base-rebuild`
- وضعیت: `active — preview gate`
- زبان محصول: English-first در UI فعلی؛ اسناد اجرایی فارسی
- تکنولوژی: Android، Jetpack Compose، Navigation Compose، Room، DataStore

## درخواست

بازسازی کامل پایهٔ UI به تجربه‌ای زنده، جذاب، انگیزه‌بخش و عمیقاً گیمیفای‌شده؛ نه یک داشبورد خشک با انبوه کارت. نتیجه باید کیفیت یک super app آموزشی ممتاز را هدف بگیرد، اما هر پیشرفت، پاداش، مأموریت، achievement و بازخورد همچنان به evidence واقعی و منبع حقیقت پایدار متصل باشد.

## تصمیم هویت

با اختیار صریح Automate، `Cinnamon Questlands` به‌عنوان **جهت کاری جدید** انتخاب شد. این تصمیم ادعای بازیابی شناسهٔ انتخاب قبلی کاربر نیست. تصویر قدیمی شمارهٔ ۱ فقط مرجع مفهومی است و تا تولید و تأیید preview canonical، معیار fidelity پیاده‌سازی محسوب نمی‌شود.

Questlands در این پروژه به معنی جهان سفر یادگیری، نقشهٔ mastery، همراه دارچینی و memory objects است. این پروژه هیچ ارتباط محصولی با Campaign ندارد و naming، navigation یا domain model بازسازی نباید Campaign را برگرداند.

## معیارهای موفقیت

1. پوستهٔ اصلی از card-stack عمومی به یک جهان یادگیری مرحله‌دار، به‌یادماندنی و سریع تبدیل شود.
2. Home یک stage زنده با next best action روشن باشد، نه فهرست ماژول‌ها.
3. Library، Practice، Journey و Profile هرکدام نقش فضایی مشخص و در عین حال زبان بصری واحد داشته باشند.
4. rewardها فقط از ledger و receiptهای Room نمایش داده شوند؛ UI هیچ XP یا unlock جدیدی نسازد.
5. همهٔ routeهای فعلی با alias سازگار حفظ شوند و invalid argument به state صریح NotFound/Error برسد.
6. loading، empty، offline، failure، retry، replay، reduced-motion و long-content از ابتدا طراحی شوند.
7. compact، medium و expanded با transformation واقعی، نه صرفاً کش‌آمدن کارت‌ها، کار کنند.
8. mascot، map، badge، icon، celebration و texture به asset system نسخه‌دار تبدیل شوند.
9. هیچ corpus، شناسهٔ entry، تنظیمات کاربر، transaction یا migration از دست نرود.
10. پیاده‌سازی فقط پس از تأیید preview canonical شروع شود و با screenshot matrix، tests، APK و runtime upgrade proof پایان یابد.

## خارج از محدودهٔ فعلی

- پاک‌کردن یا reset دادهٔ اپ/دستگاه
- deploy زیرساخت، صدور product session یا مدیریت secretهای بیرونی
- signing production و انتشار عمومی
- تغییر محتوای بالینی/تحریری بدون review
- جایگزین‌کردن ImageGen با API/CLI دیگر بدون مجوز صریح کاربر

## منابع حقیقت

- Room v6 و migrationهای موجود
- DataStore `cinnamon_progress`
- Lexicon dataset `cinnamon.lexicon.core@2026.07.17`
- Reference Atlas `cinnamon.mesh.reference@2026.1`
- Gamification catalog/copy v1
- repository و ViewModelهای فعلی تا زمان ساخت facade سازگار
- route strings فعلی به‌عنوان legacy mappings
