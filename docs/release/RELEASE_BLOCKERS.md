# Release gate — Cinnamon Android

## وضعیت

انتشار خودکار تولیدی عمداً غیرفعال است. Workflow قبلی می‌توانست روی هر branch با دسترسی نوشتن، credentialهای provider و keystore موقت build/release بسازد. اکنون فقط verification بدون secret اجرا می‌شود.

## پیش‌نیازهای مالک محصول

1. همهٔ کلیدهای provider که قبلاً وارد Git شده‌اند را revoke/rotate کنید؛ کلید جدید هرگز نباید وارد APK، Gradle، `.env.example` یا workflow شود.
2. مشخص کنید که این package یک محصول جدید است یا update محصول منتشرشده. برای update باید `applicationId` و signing lineage قبلی حفظ شود.
3. fingerprint عمومی SHA-256 کلید upload/app-signing، alias کلید و محل backup امن را ثبت کنید؛ مقدار private key یا password را در chat یا Git ثبت نکنید.
4. یک master icon واقعی و تأییدشدهٔ برند فراهم کنید. launcher فعلی برای identity نهایی تأیید نشده است.
5. نسخهٔ منتشرشدهٔ فعلی، کانال توزیع (Play/direct APK) و policy نسخه‌گذاری را تأیید کنید.

## قرارداد workflow تولیدی بعدی

بعد از تکمیل موارد بالا، workflow جداگانه باید فقط از tag محافظت‌شده یا environment محافظت‌شده اجرا شود و این properties را داشته باشد:

- `permissions: contents: read` در سطح workflow و فقط `contents: write` در job انتشار؛
- signing fail-closed: بدون keystore و password معتبر، build release شکست بخورد؛
- هیچ `.env` یا credential provider در runner، artifact یا log ساخته نشود؛
- APK/AAB امضاشده، package ID، versionCode/versionName و signature بررسی شوند؛
- checksum و release notes به draft release متصل شوند؛
- update از artifact قبلی روی دستگاه/شبیه‌ساز واقعاً آزمایش شود.

## محدودیت فعلی

این مخزن هنوز تاریخچهٔ Git دارای credential قبلی دارد. حذف فایل فعلی به‌تنهایی تاریخچه، cloneهای قدیمی یا cacheهای CI را پاک نمی‌کند؛ بازنویسی کنترل‌شدهٔ تاریخچه و invalidation کلیدهای قدیمی، اقدامی بیرون از workspace و نیازمند مالک repository است.
