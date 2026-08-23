# Handoff

## نتیجهٔ این checkpoint

- ممیزی مستقل فریز و PDF فارسی RTL تولید شد.
- هر ۲۰ صفحهٔ PDF بصری بررسی و پذیرفته شد.
- سه موجودی موازی route/state، data/migration و visual evidence ادغام شدند.
- قرارداد حفظ Room v6، corpus، catalog، embedded content و routeها نوشته شد.
- Reference Atlas رسمی MeSH 2026 با ۵٬۷۴۲ descriptor به‌صورت لایه‌ای مستقل از دک یادگیری تولید شد.
- migration افزایشی v5→v6، seeder اتمیک، DAO جست‌وجو، manifest و validatorهای CI اضافه شدند.
- دو تست managed Android 35 بدون failure، critical Ready برابر ۱٬۲۵۳ms و Atlas Ready برابر ۸٬۰۹۶ms را در آخرین اجرای ترکیبی ثبت کردند.
- snapshot قدیمی به‌صورت اتمیک reconcile می‌شود و تست دستگاه حفظ دقیق SRS/بوکمارک را ثابت کرد.
- Questlands به‌عنوان جهت کاری جدید Automate ثبت شد.
- گیت preview فعال مانده و هیچ mutation جدید whole-UI انجام نشده است.

## مهم‌ترین یافته‌ها

1. UI فعلی هنوز card-heavy و از نظر جهان محصول/mascot/asset system کم‌عمق است.
2. startup gate کل graph را نمی‌پوشاند.
3. invalid route argumentها spinner یا silent fallback می‌سازند.
4. recovery سراسری command اشتباه اجرا می‌کند.
5. فایل‌های بزرگ، presentation و authored corpus را مخلوط کرده‌اند.
6. corpus ID به file/order حساس است.
7. schemaهای v5 و v6 critical هستند و باید همراه migration و checksumها version-control شوند.
8. gateway source boundary بهتر شده، ولی live session/deploy/signing اثبات نشده است.

## قدم بعدی دقیق

پس از refresh کردن OAuth ImageGen یا مجوز صریح CLI fallback:

1. QL-01 تا QL-04 تولید و در `assets/previews/` ذخیره شوند.
2. previewها با state/route/ratio checklist بررسی شوند.
3. کاربر canonical board را تأیید کند.
4. سپس foundation architecture با typed route و StartupGate آغاز شود.

## دستورات baseline

```powershell
python scripts/verify_lexicon_assets.py app/src/main/assets/lexicon
python scripts/verify_mesh_reference.py
python scripts/verify_room_migration.py
```

برای runtime upgrade، فقط install جایگزین:

```powershell
adb install -r <apk-path>
```

uninstall و `pm clear` ممنوع‌اند.

## محدودیت‌های باز

- ImageGen OAuth token revoked
- ممیزی مستقل Jules برای توسعهٔ دیتاست هنوز در حال اجرا است
- backend session/gateway و signing continuity بیرون از proof فعلی
- physical-device release performance هنوز اندازه‌گیری نشده
- محتوای corpus نیازمند editorial/clinical review است
