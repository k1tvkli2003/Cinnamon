# handoff

## وضعیت تحویل

Foundation گیمیفیکیشن اکنون settlement اتمیک، receipt پایدار، Quest/Achievement واقعی و claim یک‌باره دارد. Mission Pulse روی Home از دادهٔ due و evidence مرور تغذیه می‌شود. Journey v1 چهار فصل ترتیبی و persisted دارد. Campaign route v1 روی Room v4 پس از prerequisite نسخه‌دار بین Precision و Momentum انتخاب می‌دهد؛ choice بدون XP، immutable و سازندهٔ فقط Journey منتخب است. ارتقای واقعی v3→v4، commitment با balance ثابت ۴۴ و cold restore ثبت شده‌اند. full checkpoint شامل ۱۰۴ JVM test در ۲۶ suite، ۳ instrumentation test، lint، پنج validator و debug/benchmark APK موفق است. PDF فارسی ۱۲ صفحه‌ای نیز پس از Poppler inspection و اصلاح clipping پذیرفته شد.

release همچنان BLOCK است. manifest داده بازبینی تحریری/بالینی می‌خواهد؛ backend/auth/gateway، credential remediation و signing/install-update proof بیرون از checkout هستند. کاربر هویت را قبلاً انتخاب کرده، ولی identifier canonical در artifactهای قابل‌بازیابی نیست؛ انتخاب مجدد نباید درخواست شود.

## مسیرهای مهم

- settlement: `app/src/main/java/com/cinnamon/app/data/local/GamificationDao.kt`
- planner/contracts: `app/src/main/java/com/cinnamon/app/domain/gamification/`
- repository: `app/src/main/java/com/cinnamon/app/data/gamification/GamificationRepository.kt`
- Journey definition/planner: `app/src/main/java/com/cinnamon/app/domain/gamification/JourneyCatalog.kt` و `app/src/main/java/com/cinnamon/app/data/gamification/JourneySettlementPlanner.kt`
- Campaign definition/planner: `app/src/main/java/com/cinnamon/app/domain/gamification/CampaignCatalog.kt` و `app/src/main/java/com/cinnamon/app/data/gamification/CampaignSettlementPlanner.kt`
- Room v4/migration: `app/src/main/java/com/cinnamon/app/data/local/AppDatabase.kt` و `AppDatabaseMigrations.kt`
- startup: `app/src/main/java/com/cinnamon/app/data/startup/`
- Mission UI/copy: `app/src/main/java/com/cinnamon/app/ui/screens/home/`
- reward presentation: `app/src/main/java/com/cinnamon/app/ui/components/RewardPresentationHost.kt`
- Baseline Profile: `app/src/main/baseline-prof.txt`
- generation/measurement: `scripts/generate_baseline_profile.ps1` و `scripts/measure_startup.ps1`
- verification: `scripts/verify_baseline_profile.py`
- report: `docs/audit/CINNAMON_GAMIFICATION_VISUAL_DIRECTION_2026-07-18_FA.md`

## invariantهای ادامه

1. UI هیچ‌گاه XP، unlock یا completion را مستقیم نمی‌نویسد.
2. هر learning event occurrence key و semantic evidence دارد.
3. replay، retry و collision باید idempotent یا rollback-safe بمانند.
4. stage قفل‌شده evidence زودرس را مصرف نمی‌کند؛ هر event حداکثر یک stage را کامل می‌کند.
5. Campaign choice هیچ XP نمی‌دهد، با settlement دیگری share نمی‌شود و برای هر campaign version فقط یک route دارد.
6. prerequisite و route با ID + version دقیق bind می‌شوند؛ route انتخاب‌نشده materialize نمی‌شود.
7. motion، sound و haptic باید preference و accessibility را رعایت کنند.
8. هیچ secret یا provider token وارد Android/CI/artifact نمی‌شود.
9. داده، route و schema با migration/adapter افزایشی حفظ می‌شوند.
10. هویت منتخب حدس زده نمی‌شود؛ همان identifier قبلی بازیابی و canonical می‌شود.

## روش ادامه

1. PDF جاری را تولید و همهٔ صفحه‌ها را بصری inspect کن.
2. route engine v1 را به campaign pack چند‌هفته‌ای/episodic توسعه بده؛ collection و personalization را نیز یکی‌یکی با state machine و ledger بساز.
3. identifier انتخاب قبلی را از continuity معتبر بازیابی و در manifest تصمیم ثبت کن.
4. سپس mascot، badge، shell و motion همان کانسپت را با assetهای project-bound و screenshot-diff اجرا کن.
5. performance را روی دستگاه فیزیکی هدف با Macrobenchmark، cold/warm و frame/jank budget اثبات کن.
6. بعد از فراهم‌شدن authority بیرونی، credential/gateway/signing/content release gate را دوباره audit کن.

## Verification

جزئیات جاری در `05-verification.md` است. هیچ ادعای release-ready یا visual fidelity نهایی قبل از رفع گیت‌های ثبت‌شده مجاز نیست.
