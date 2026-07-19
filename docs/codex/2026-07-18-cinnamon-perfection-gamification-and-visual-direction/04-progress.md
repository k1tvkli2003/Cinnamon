# پیشرفت

## log

| زمان | وضعیت | اقدام / یافته | شاهد |
|---|---|---|---|
| 2026-07-17 | completed | startup validator با regex سازگار Android و coordinator/retry ساخته شد | Android instrumentation tests |
| 2026-07-17 | completed | Room event/reward ledger، SRS atomic و seed manifest تثبیت شد | source + unit/device tests |
| 2026-07-17 | completed | Make It Native از clinical roleplay گمراه‌کننده جدا شد | `make-it-native-tone-slider.png` |
| 2026-07-18 | completed | lint errors مسیر mock notification/audio برطرف شد؛ code مرده حذف شد | lint report: 0 errors |
| 2026-07-18 | completed | Weekly Challenge به سه prompt و `scenario_language_sprint` تبدیل شد | screenshot/XML + `+12 XP` |
| 2026-07-18 | completed | Transcript Escape به سه clue و `authored_transcript_escape` تبدیل شد | screenshot/XML + `+12 XP` |
| 2026-07-18 | completed | focus پس از check پاک شد؛ keyboard پایان reward را نمی‌پوشاند | `transcript-escape-focus-cleared.png` |
| 2026-07-18 | completed | Markdown، asset ledger و PDF RTL هشت‌صفحه‌ای ساخته و بصری inspect شد | `output/pdf/...FA.pdf` و renderهای Poppler |
| 2026-07-18 | completed | قرارداد حفظ و موجودی بازسازیِ پنج‌گزینه‌ای پیش از re-theme ثبت شد | `07-preservation-contract.md` و `08-rebuild-inventory-and-opinion-ledger.md` |
| 2026-07-18 | completed | startup به Ready gate تبدیل شد؛ Home/ناوبری و خوانش Room تا آماده‌شدن داده defer شدند | clean-install runtime + source trace |
| 2026-07-18 | completed | catalog copy تک‌مالک و TTS first-use شدند؛ snapshot اولیهٔ startup با قید emulator ثبت شد | mean 4.746s / median 4.448s historical debug snapshot؛ baseline مقایسه‌ای/release نیست و با harness دقیق‌تر جایگزین شد |
| 2026-07-18 | completed | verifier secret-boundary از cache تولیدی جدا شد؛ چهار verifier سبز شدند | خروجی scripts/verify_* |
| 2026-07-18 | completed | edge case شکست init TTS retry-safe شد؛ build، ۳۵ unit، ۳ instrumentation و smoke نهایی سبز شدند | APK نهایی + `home-final-startup-gated.png` |
| 2026-07-18 | completed | startup پس از اولین frame آغاز شد؛ catalog parse مستقل با import/seed سریال overlap و trace opt-in redacted اضافه شد | P1 تا P3: overlap ۰٫۴۹۹ تا ۱٫۵۴۲s؛ debug AVD فقط |
| 2026-07-18 | completed | PDF RTL پس از اصلاح فاصلهٔ عنوان‌های چاپی دوباره render و صفحه‌به‌صفحه inspect شد | ۸ صفحه A4، Poppler PNG، بدون clipping یا glyph defect |
| 2026-07-18 | completed | harness تکرارپذیر startup ساخته و روی AVD بدون پاک‌کردن داده اجرا شد | `startup-20260718-150403.json`: TotalTime p50/p95 ۱۱۵۰/۱۱۹۳ms، prepare ۲۴۵/۳۵۸ms |
| 2026-07-18 | completed | CI Android با PowerShell AST، syntax harness را guard می‌کند | `.github/workflows/android.yml`؛ remote GitHub run هنوز مشاهده نشده |
| 2026-07-18 | completed | inventory asset و محتویات APK بررسی شد | concept previewها در docs هستند، نه APK؛ حذف به‌خاطر performance انجام نشد |
| 2026-07-18 | completed | provenance manifest lexicon در مدل Kotlin، seeder و validator enforce شد | ۳ test جدید origin/reviewStatus ناشناخته را رد کردند؛ full suite: ۳۸ unit و ۳ Android test سبز |
| 2026-07-18 | completed | reward presentation از snackbar گذرا به host پایدار، dismissible و accessible ارتقا یافت | `output/cinnamon-runtime-word-match-finish-20260718.png` |
| 2026-07-18 | completed | Sound/Haptic/Reduce motion در DataStore پایدار و در execution boundary اعمال شدند | initial/changed/relaunch/restored XML/PNG؛ ۲ test preference-aware haptic |
| 2026-07-18 | completed | motionهای press/progress/pulse/waveform/confetti/typewriter/parallax برای Reduce motion ایستا یا حذف شدند | compile + source audit + runtime preference proof |
| 2026-07-18 | completed | fake podcast/audio/clinical inference و برچسب‌های گمراه‌کنندهٔ Home به authored/self-review practice تبدیل شدند | source + AvalAI briefs/raw records ۱۳ تا ۱۶ |
| 2026-07-18 | completed | catalog evidence projector اضافه شد و metric پشتیبانی‌نشده را پنهان می‌کند | ۱۲ test هدفمند |
| 2026-07-18 | completed | AI gateway failure به status/delivery/requestId/Retry-After تایپ شد | ۱۶ test هدفمند |
| 2026-07-18 | completed | full JVM/lint/assemble checkpoint ثبت شد | ۶۷ test در ۲۰ suite، ۰ failure/error/skip؛ lint ۰ error |
| 2026-07-18 | completed | instrumentation روی `Codex_API35` اجرا شد | ۳/۳ passed، Android 15 |
| 2026-07-18 | completed | baseline تازهٔ startup ثبت شد | `startup-20260718-160435.json`: trace ۵/۵، TotalTime p50/p95 ۱۵۳۸/۱۶۳۴ms |
| 2026-07-18 | corrected | ثبت شد که کاربر کانسپت را قبلاً انتخاب کرده و فقط identifier canonical گم شده است | هیچ انتخاب مجددی درخواست نمی‌شود؛ identity-bearing work فقط تا continuity recovery متوقف است |
| 2026-07-18 | completed | Quest/Achievement/Catalog settlement در transaction همان learning event اتمیک و idempotent شد | ۱۱ Room integration test + planner tests |
| 2026-07-18 | completed | Mission Pulse با state واقعی به Home افزوده شد | unassigned/active/complete/claimed + copy tests |
| 2026-07-18 | completed | مسیر دقیق نهایی Mission روی APK نصب‌شده اجرا شد | ۳ review، `24/80 → 34/80 XP`، claim یک‌باره و replay guard در PNG/XML |
| 2026-07-18 | completed | full checkpoint نهایی ثبت شد | ۸۲ test در ۲۲ suite، ۳ instrumentation، lint ۰ error، debug APK SHA-256 `DF6296…187C` |
| 2026-07-18 | completed | benchmark build و Baseline Profile متمرکز ساخته شد | ۲٬۶۴۵ rule؛ `baseline.prof/profm` داخل APK؛ p50 TotalTime حدود ۷٫۶٪ بهتر در مقایسهٔ هم‌ساخت emulator |
| 2026-07-18 | completed | verifier Baseline Profile و packaging gate به CI افزوده شد | source rule ownership/duplicate/count + APK ZIP entries |
| 2026-07-18 | completed | گزارش RTL تازه با evidence runtime، performance، roadmap و continuity هویت ساخته شد | PDF ده‌صفحه‌ای Tagged؛ همهٔ صفحات Poppler inspect شدند؛ crop اسکرین‌شات‌ها پیش از پذیرش اصلاح شد |
| 2026-07-18 | completed | Room به v3 با دو جدول Journey و migration افزایشی ارتقا یافت | schema v3 + verifier مستقیم v1→v2، v2→v3 و chain v1→v3؛ ۳۹ statement |
| 2026-07-18 | completed | `Foundation Expedition` چهار‌فصلی و settlement اتمیک Journey پیاده شد | ۸۰ XP authored؛ stageهای sequential؛ پنج integration test + دو planner test |
| 2026-07-18 | corrected | bootstrap Journey از catalog reconciliation جدا شد | defect upgrade روی دادهٔ محفوظ runtime کشف شد؛ `reconcileJourney()` مستقل، rebuild و re-install موفق |
| 2026-07-18 | completed | Home Journey Pulse و Questboard Journey Map از projection persisted ساخته شدند | loading/ready/recovery، chapter secured/active/locked، CTA و Reduce motion |
| 2026-07-18 | completed | proof ارتقای واقعی Journey ثبت شد | `user_version=3`، فصل ۱ complete، فصل ۲ active، `34→44 XP`، relaunch با یک event/transaction |
| 2026-07-18 | completed | checkpoint پس از Journey سبز شد | ۹۰ test در ۲۳ suite، ۳ instrumentation، lint ۰ error، چهار validator و diff check |
| 2026-07-18 | completed | artifact و Baseline Profile نهایی دوباره ساخته و verify شدند | debug `0ECCEF…0D3A`، benchmark `C8028E…D393`، profile ۲٬۶۴۵ rule |
| 2026-07-18 | measured | شمارش LOC به‌صورت authored/generated/docs تفکیک شد | ۳۱٬۵۷۴ authored بدون schema؛ ۳۴٬۸۲۱ مهندسی/داده؛ ۳۹٬۲۱۲ کل متن فنی با docs/profile |
| 2026-07-19 | completed | گزارش RTL پس از Journey بازتولید و visually verified شد | ۱۱ صفحه A4 Tagged؛ Poppler render و inspection تک‌تک صفحات؛ SHA-256 `55CA5C…A81A` |
| 2026-07-19 | completed | Campaign v1 با دو route هم‌ارزش و prerequisite نسخه‌دار پیاده شد | Precision/Momentum؛ هرکدام سه فصل و ۱۱۰ XP authored |
| 2026-07-19 | completed | Room v4 و commitment اتمیک افزوده شد | choice واحد، conflict rollback، zero-XP event و فقط Journey route منتخب |
| 2026-07-19 | corrected | انتخاب route با evidence تاریخی می‌توانست همان tap پاداش بدهد | `settleEvidenceOnThisEvent=false` و DAO منع reward/settlement هم‌زمان؛ integration guard |
| 2026-07-19 | completed | Campaign UI از state واقعی ساخته شد | Locked/Choose/Confirm/Ready/Unavailable؛ دو جهت بصری و CTAهای ۴۸dp |
| 2026-07-19 | completed | ارتقای واقعی v3→v4 و commitment روی APK نصب‌شده اثبات شد | Journey/۴۴ XP حفظ شد؛ Precision 0/3؛ صفر transaction؛ cold relaunch countهای 1/1/1/3 |
| 2026-07-19 | corrected | Android startup test fresh-only بود و دادهٔ route محفوظ را failure می‌دانست | تست state-preserving شد و exact Foundation/route contracts را می‌سنجد |
| 2026-07-19 | completed | checkpoint Campaign سبز شد | ۱۰۴ test/۲۶ suite، ۳ Android test، lint ۰ error، پنج validator، دو APK |
| 2026-07-19 | measured | LOC پس از Campaign دوباره‌شماری شد | ۳۳٬۲۲۹ authored؛ ۳۸٬۳۴۳ مهندسی/داده؛ Kotlin/build logic برابر ۲۳٬۹۳۶ |
| 2026-07-19 | completed | گزارش RTL Campaign بازتولید و visually verified شد | ۱۲ صفحه A4 Tagged؛ inspection همهٔ صفحات؛ clipping عنوان صفحهٔ ۱۱ اصلاح شد؛ SHA-256 `9FC34F60…AA3C` |

## انجام‌شده تا اینجا

- ۱۰ preview ImageGen به‌صورت project-bound کپی و به‌عنوان Mock Preview ثبت شد.
- دو game loop بدون ledger به flowهای سه‌مرحله‌ای با reward واقعی تبدیل شد.
- تست واحد برای match ruleهای Sprint و Escape اضافه شد.
- گزارش اجرایی فارسی با status release، continuity هویت، قرارداد بازسازی و شواهد تازه به‌روزرسانی شد.

## گام بعد

1. identifier همان کانسپت قبلاً منتخب را از continuity بازیابی و canonical کن؛ انتخاب تازه از کاربر نخواه.
2. route engine v1 را به campaign pack چند‌هفته‌ای/episodic گسترش بده و collection، personalization و accessibility matrix را با contract کامل توسعه بده.
3. performance را روی دستگاه فیزیکی هدف و معیار cold/warm/jank اثبات کن.
4. پس از فراهم‌شدن زیرساخت بیرونی، release/auth/signing و محتوای تأییدشده را دوباره audit کن.
