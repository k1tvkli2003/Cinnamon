# اثبات اجرایی Cinnamon — مهاجرت Learning Focus v5

تاریخ آزمون: ۲۰۲۶-۰۷-۲۲  
مسیر پروژه: `C:\Users\K1\Desktop\Projects\Cinnamon`  
دستگاه ایزوله: `emulator-5582` / `Codex_API35` / API 35

## نتیجه

نسخهٔ نهایی فعلی، snapshot واقعی Room v4 را بدون reset یا بازنویسی داده به v5 ارتقا داد. سپس cold restart و نصب پاک v5 نیز جداگانه آزموده شدند. هر سه مسیر بدون crash و با `PRAGMA integrity_check = ok` و `PRAGMA foreign_key_check` خالی پایان یافتند.

یک AVD اشتراکی پیش از این proof رد شد، چون اپ دیگری در foreground بود و اسکرین‌شات را آلوده کرده بود. این موضوع تداخل foreground دستگاه بود، نه collision در package یا component. تمام نتایج پذیرفته‌شدهٔ زیر فقط با سریال صریح `emulator-5582` ثبت شده‌اند.

## artifact نهایی

| artifact | SHA-256 | اندازه |
|---|---|---:|
| `app/build/outputs/apk/debug/app-debug.apk` | `C177F545A747151E478EA8C09F7A5EFD7648FD0405AF779BBA671F29D7543B65` | 21,759,551 bytes |
| `app/build/outputs/apk/benchmark/app-benchmark.apk` | `069751C71071FF9566C63E32A5842E625BC41953EDCA059C2D5C2C72676725E1` | 14,482,138 bytes |

هر دو APK با گواهی debug آزمون امضا شده‌اند:

`33c955a5b237ce4d1b8ee44f7873f091ed60c14de1acaf99c772329bd5e37c85`

APK مرجع v4 که در اجرای `adb install -r` اولیه استفاده شد SHA-256 زیر را داشت و با همین گواهی امضا شده بود:

`7B6C074D11DC4B85E3E76D2934BF22BF46BC8D9D2F938580B2ED8CCDA1359504`

## snapshot ورودی v4

| فایل | SHA-256 |
|---|---|
| `v4-before/cinnamon.db` | `2AEFEA1348159BB7CC792C9E2ECCDA4303B946A15D6F2F99B2BFF2EA192BB747` |
| `v4-before/cinnamon.db-wal` | `458B756B16210D4A47F11AE61661A447AE42B4B21CA3E6D105B2B8D1EF262458` |
| `v4-before/cinnamon.db-shm` | `D8B9B8AE1D3914D2191E7943C541003982E1CFB98810D45861DB01B3EE3EB25A` |

پیش از launch نهایی، هر سه هش روی مسیر دقیق دیتابیس اپ دوباره تطبیق داده شدند. وضعیت v4:

- `user_version = 4`
- `7` event، `4` transaction، `2` journey، `7` stage و `1` انتخاب
- موجودی `34 XP`
- transactionها: `8 + 8 + 8 + 10`
- Foundation ذخیره‌شده: `journey.foundation.expedition@1`، active، مرحلهٔ ۲
- انتخاب frozen legacy با تمام IDها و timestamp اصلی دست‌نخورده

## نتیجهٔ مهاجرت نهایی v4 → v5

پس از نصب APK نهایی و بازکردن snapshot:

- semantic gate شامل هم‌زمان `Cinnamon` و `Foundation Plan` در تلاش ۴ پاس شد.
- `user_version = 5`
- `7` event، `4` transaction، `2` journey، `7` stage، `1` Learning Focus selection و `34 XP`
- جدول `campaign_route_choices` حذف شد (`sqlite_master` count برابر صفر).
- مقدارهای frozen انتخاب قبلی عیناً در `learning_focus_selections` حفظ شدند.
- Foundation قدیمی عیناً باقی ماند و از adapter محدود نمایش داده شد.
- `journey.foundation.plan` به‌اشتباه کنار Foundation قدیمی ساخته نشد؛ تعداد identityها یک بود.
- cold restart در تلاش ۳ گیت معنایی را پاس کرد و countها هیچ تغییری نکردند.
- `AndroidRuntime:E` و `libc:F`: هیچ موردی ثبت نشد.

snapshot پس از مهاجرت در `v5-final-after-migration/` قرار دارد:

| فایل | SHA-256 |
|---|---|
| `cinnamon.db` | `B964168688FE3E687A923DCA5FCD495CA0E3B6125509B5D72BB5E7A4C361D226` |
| `cinnamon.db-wal` | `C8E981BB245A0164BDC0882DE8929E6F7A7C44D185D83479EEC19EBD7C8D749B` |
| `cinnamon.db-shm` | `66F9814E6BA9D42C9DA298574F17DF5E7B7DE26030717091464BB784EEA5A006` |

## نتیجهٔ نصب پاک v5

بعد از `pm clear com.cinnamon.app` و launch سرد:

- semantic gate شامل `Cinnamon` و `Foundation Plan` در تلاش ۳ پاس شد.
- دقیقاً یک `journey.foundation.plan@1` ساخته شد.
- تعداد `journey.foundation.expedition` برابر صفر بود.
- selection و reward transaction هر دو صفر بودند؛ اپ برای نصب تازه پیشرفت یا XP جعل نکرد.
- سه event موجود فقط auditهای idempotent startup بودند: `legacy_progress_imported`، `catalog_reconciled` و `journey_reconciled`؛ هیچ‌کدام reward نداشتند.
- جابه‌جایی بین `Language Sprint` و `Vocab Pairs` achievement، receipt یا transaction تولید نکرد.
- `AndroidRuntime:E` و `libc:F`: هیچ موردی ثبت نشد.

snapshot نصب پاک در `v5-final-fresh/` قرار دارد:

| فایل | SHA-256 |
|---|---|
| `cinnamon.db` | `24A9B88E29FB8188A726D15BD95891CD292573D01F5AEC9699B3EFC7C348A6E1` |
| `cinnamon.db-wal` | `69AF3CF9BCE2B9C7F1428184ABA7363FCD71280CC7880FD9F2E8FEC6F7C8773B` |
| `cinnamon.db-shm` | `F66203432B18B5F247A487B486A9AF3F8A12F279B9817085F0F9250705E53FD0` |

## گیت‌های build و device

- validator واژگان: `95` abbreviation، `45` confusable، `243` entry، `160` morpheme، `78` phrase و `80` sentence
- validator مرز secret موبایل: پاس
- validator امضای fail-closed: پاس
- validator Room: مهاجرت‌های `v1/v2/v3/v4 → v5` و ۵۲ statement پاس
- Baseline Profile: `2645` rule و profile بسته‌بندی‌شده در APK بنچمارک پاس
- unit: `122/122`
- lint: `0` error
- instrumentation روی AVD ایزوله: `3/3`
- Gradle: unit، lint، compile androidTest، debug APK و benchmark APK همگی `BUILD SUCCESSFUL`

## performance زنده

Harness پروژه با پنج process-cold run و دادهٔ موجود روی همان `emulator-5582` اجرا شد:

| build | TotalTime p50 | TotalTime p95 | prepare p50 | prepare p95 |
|---|---:|---:|---:|---:|
| debug | 12,758 ms | 13,113 ms | 2,844 ms | 3,113 ms |
| benchmark non-debuggable | 3,687 ms | 3,783 ms | 958 ms | 993 ms |

فایل‌های خام:

- `output/performance/startup-20260722-141349.json`
- `output/performance/startup-20260722-141501.json`

در جریان این اندازه‌گیری، summary harness برای مجموعهٔ percentile تمام-null دچار parameter-binding failure شد. `Get-Percentile` اکنون ورودی null/empty را می‌پذیرد؛ smoke test و اجرای پنج‌تایی پس از اصلاح پاس شدند. نتیجهٔ benchmark شواهد emulator است، نه جایگزین Macrobenchmark روی دستگاه فیزیکی.

## شواهد دیداری و accessibility

- `output/cinnamon-v5-final-fresh-home.png`
- `output/cinnamon-v5-final-upgrade-home.png`
- `output/cinnamon-v5-final-profile-cabinet.png`
- `output/cinnamon-v5-final-questboard.png`
- `output/cinnamon-v5-final-language-sprint.png`
- `output/cinnamon-v5-final-vocab-pairs.png`
- `output/cinnamon-v5-final-home-360dp.png`
- `output/cinnamon-v5-final-profile-cabinet-360dp-full.png`
- `output/cinnamon-v5-final-questboard-360dp.png`

در Questboard، تب انتخاب‌شده در UIAutomator دارای `selected=true` است و ارتفاع target همهٔ تب‌های اندازه‌گیری‌شده در هر دو viewport دقیقاً `48dp` بود. viewport فشرده با عرض واقعی `360dp` آزموده شد و پس از ثبت شواهد، density AVD به مقدار قبلی `320` بازگردانده شد.

## دامنهٔ این proof

این سند سلامت artifact دیباگ/بنچمارک، مهاجرت، نصب پاک، persistence، semantics و رندر emulator را اثبات می‌کند. این سند APK تولیدی، signing lineage فروشگاه، backend زنده، دستگاه فیزیکی یا آمادگی انتشار عمومی را اثبات نمی‌کند.
