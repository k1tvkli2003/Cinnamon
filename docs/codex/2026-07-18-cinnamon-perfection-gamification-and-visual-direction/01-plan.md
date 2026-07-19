# برنامه

## رویکرد

ابتدا قرارداد اعتماد، reward، feedback و failure را اصلاح می‌کنیم، سپس رفتار را روی Android ثابت می‌کنیم. کاربر هویت بصری را قبلاً انتخاب کرده است؛ فقط identifier canonical آن باید از continuity قبلی بازیابی و ثبت شود. تا آن زمان کارهای identity-bearing متوقف‌اند، نه توسعهٔ فنی.

## گام‌ها

| گام | وضعیت | یادداشت |
|---|---|---|
| AI boundary، seed، catalog و Room ledger | completed | gateway-only client، startup coordinator و ledger immutable ساخته شد |
| حذف ادعا/امتیاز جعلی از game flows | completed | Sprint و Escape سه‌مرحله‌ای با receipt واقعی شدند |
| runtime، unit و Android instrumentation proof | completed / recurring | آخرین full checkpoint: ۱۰۴ JVM test در ۲۶ suite و ۳ instrumentation؛ پس از هر settlement/build تازه باید تکرار شود |
| startup readiness و کار غیرضروری | completed | Home/ناوبری تا Ready گیت شدند؛ catalog یک مالک دارد، TTS lazy و catalog parse با seed مستقل overlap می‌شود |
| startup trace و روش اندازه‌گیری | completed | timing opt-in/redacted، harness تکرارپذیر و baseline پنج-run process-cold ثبت شد |
| CI syntax guard برای harness | completed | job کم‌اختیار Android، script را با PowerShell AST parse می‌کند؛ benchmark را شبیه‌سازی نمی‌کند |
| lint errors | completed | ۰ error؛ warningهای اطلاعاتی جدا ثبت شده‌اند |
| preferenceهای feedback و Reduce motion | completed | DataStore، runtime gating و force-stop/relaunch proof ثبت شد |
| typed AI gateway errors | completed | status، delivery state، request ID و Retry-After با ۱۶ test هدفمند |
| catalog evidence projection | completed | ۱۲ test هدفمند؛ metric پشتیبانی‌نشده جعل نمی‌شود |
| settlement اتمیک quest/achievement | completed | unlock/progress/claim پایدار و idempotent در Room authority است |
| Journey و Campaign route v1 | completed | Room v4، انتخاب دوشاخهٔ zero-XP، save/restore، conflict rollback و runtime proof |
| Campaign pack چند‌هفته‌ای/episodic | pending | route engine حاضر است؛ episode manifest، زمان‌بندی و پایان روایی هنوز ساخته نشده‌اند |
| بازیابی شناسهٔ هویت منتخب | pending continuity recovery | انتخاب قبلاً انجام شده؛ انتخاب تازه از کاربر خواسته نمی‌شود |
| بازطراحی pixel-faithful با concept منتخب | pending identity only | پس از بازیابی identifier و ثبت asset manifest، با Copy screenshot-diff |
| backend/auth/signing/release proof | blocked | مالک محصول و زیرساخت بیرونی لازم است |
| PDF فارسی RTL و handoff | in_progress refresh | generator با شواهد جدید تازه می‌شود؛ PDF پس از validation نهایی regenerate و inspect می‌شود |

## رابط‌ها و artifactها

- `GamificationRepository` و `UserProgressViewModel`
- `GamificationHubScreen`، `NativeFluencyPlaygroundScreen`، `ClinicalSimLabsScreen`
- `docs/audit/CINNAMON_GAMIFICATION_VISUAL_DIRECTION_2026-07-18_FA.md`
- `output/pdf/Cinnamon_Perfection_Gamification_Visual_Direction_FA.pdf`
- `scripts/measure_startup.ps1` و `output/performance/startup-20260718-160435.json`
- `assets/brand-concepts-v1/`
- `07-preservation-contract.md` و `08-rebuild-inventory-and-opinion-ledger.md`

## ریسک‌ها

- گم‌شدن identifier انتخاب قبلی نباید به حدس هویت یا درخواست انتخاب مجدد منجر شود؛ فقط mascot/art نهایی تا بازیابی متوقف می‌ماند.
- projection quest/achievement نباید به‌اشتباه settlement پایدار گزارش شود.
- release بدون rotation، backend/auth و signing lineage ممنوع است.
- هشدارهای dependency نباید با upgrade کورکورانه رفع شوند؛ migration compatibility لازم دارند.
- هم‌پوشانی startup فقط proof تشخیصی است؛ budget blocking تا profile/release و دستگاه هدف ساخته نمی‌شود.

## checkهای پذیرش

- reward تنها بعد از round سه‌مرحله‌ای و با receipt واقعی دیده شود.
- replay XP تکراری نسازد.
- PDF در PNGهای renderشده RTL، خوانا و بدون clipping باشد.
- هر preview در مدارک صریحاً Mock Preview نامیده شود.
