# Cinnamon: پرفکشن گیمیفیکیشن و جهت بصری

- Task ID: `2026-07-18-cinnamon-perfection-gamification-and-visual-direction`
- Status: `active`
- Created: 2026-07-18T01:23:07
- Language: Persian

## Request

نقد عمیق محصول و رسیدن به تجربه‌ای کاملاً جذاب، انگیزه‌بخش و گیمیفای‌شده؛ نه صرفاً یک اپِ کاربردی یا محیط خشک. درخواست شامل گزارش Markdown و PDF فارسی، ایده‌پردازی/پیشنهادهای دقیق، ده مسیر هویت بصری همراه preview ImageGen، و ادامهٔ اصلاحات واقعی تا رسیدن به پرفکشن است.

## Success Criteria

- هیچ امتیاز، AI، صوت یا ارزیابی بالینیِ جعلی به‌عنوان قابلیت واقعی نمایش داده نشود.
- حلقه‌های تمرین کامل، idempotent و به Room ledger متصل باشند.
- مسیرهای startup، seed و catalog روی Android واقعی آزموده شوند.
- تا Ready شدن startup، دادهٔ موقت/مسیر قابل لمس نمایش داده نشود و هر سنجش کارایی با مرز emulator/debug گزارش شود.
- ده preview تصویری به‌روشنی به‌عنوان mock/آرشیو تصمیم ثبت شوند؛ انتخاب قبلی کاربر محفوظ بماند و به‌دلیل شکاف ثبت identifier از او انتخاب دوباره خواسته نشود.
- preferenceهای feedback و Reduce motion در منبع حقیقت پایدار باشند و در runtime رفتار واقعی را gate کنند.
- projection و settlement گیمیفیکیشن از هم تفکیک شوند؛ unlock یا quest completion بدون evidence و transaction اتمیک «کامل» اعلام نشود.
- گزارش Markdown و PDF فارسی RTL با شواهد، اولویت‌ها، تصمیم‌های لازم و محدودیت‌های release تحویل شود.

## زمینه

Cinnamon یک محصول Android برای medical English و fluency است. checkout در شروع clean بود و release عمومی به‌دلیل credential history، backend/auth/gateway و signing lineage هنوز مجاز نیست.

## در محدوده

- reward ledger، تجربهٔ بازی‌ها، copy و navigation قابل‌اثبات
- startup/seed/catalog/AI boundary/error/runtime verification
- گزارش، preview ledger و PDF فارسی RTL
- قرارداد حفظ و موجودی بازسازی پیش از هر تغییر بصری نهایی
- جهت بصری، mascot و motion proposal بدون جعل implementation

## خارج از محدوده

- rotate/revoke کردن credential خارجی، بازنویسی history Git یا deploy backend
- ساخت release artifact با signing production
- انتخاب خودسرانه یا درخواست انتخاب مجدد هویت نهایی؛ identifier همان انتخاب قبلی باید بازیابی و canonical شود

## فرض‌ها

- previewهای ImageGen آرشیو جهت‌های بررسی‌شده‌اند، اما معیار fidelity implementation نیستند.
- کاربر انتخاب را قبلاً انجام داده است؛ نبود identifier در artifactهای قابل‌بازیابی یک خطای continuity این task است، نه نبود تصمیم کاربر.
- تا وقتی backend و product session وجود ندارد، AI فقط guided practice محلی است.
