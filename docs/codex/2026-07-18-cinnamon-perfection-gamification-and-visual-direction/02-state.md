# وضعیت

- Current status: `active`
- Last updated: 2026-07-19T01:15:00+03:30
- Owner: Codex

## Current State

اقتصاد پاداش اکنون event-authoritative و اتمیک است. event، reward transaction، balance، summary، Quest progress/completion، Achievement unlock، catalog XP، Journey stage، Campaign choice و presentation receipt در transaction Room تسویه می‌شوند. Home یک Mission Pulse واقعی با چهار حالت unassigned/active/complete/claimed دارد. Journey v1 چهار فصل ترتیبی و persisted دارد؛ ارتقای واقعی v2→v3 روی دادهٔ محفوظ، تکمیل فصل اول، فعال‌شدن فصل دوم و `۳۴→۴۴ XP` با cold-relaunch بدون duplicate اثبات شد. Campaign route v1 نیز پس از prerequisite بین Precision و Momentum انتخاب می‌دهد؛ choice در Room v4 ثابت، بدون XP و فقط سازندهٔ Journey route منتخب است. ارتقای واقعی v3→v4، commitment و cold restore با balance ثابت ۴۴ اثبات شدند.

startup پس از frame واقعی loading آغاز می‌شود، catalog و Journey reconciliation namespace مستقل و fast-path دارند و Baseline Profile متمرکز با ۲٬۶۴۵ rule داخل benchmark APK بسته‌بندی شده است. full JVM suite شامل ۱۰۴ test در ۲۶ suite است؛ ۳ instrumentation test، lint، دو APK و پنج validator نیز گذرانده‌اند. release همچنان BLOCK است، زیرا credential remediation، gateway/auth production، signing/install-update و editorial/clinical approval بیرون از checkout باقی‌اند.

کاربر کانسپت بصری را قبلاً انتخاب کرده است، اما identifier canonical در state/artifactهای قابل‌بازیابی نیست. انتخاب مجدد درخواست نمی‌شود؛ فقط کار identity-bearing تا بازیابی همان شناسه متوقف است.

## تصمیم‌ها

| تاریخ | تصمیم | دلیل | شاهد |
|---|---|---|---|
| 2026-07-17 | AI فقط gateway و بدون secret موبایل | provider credential نباید وارد APK شود | verifier و contract gateway |
| 2026-07-17 | XP و receipt منحصراً در Room | جلوگیری از state محلی/farm | reward ledger و integration tests |
| 2026-07-18 | game loop فقط با completion معنایی event می‌سازد | ظاهر بازی بدون evidence پذیرفتنی نیست | Sprint/Escape/Match runtime |
| 2026-07-18 | Sound/Haptic/Reduce motion پایدار و execution-gated | preference نمایشی کافی نیست | DataStore tests و relaunch proof |
| 2026-07-18 | AI failure باید status، request ID، Retry-After و delivery state را حفظ کند | retry کور ناامن است | targeted gateway tests |
| 2026-07-18 | انتخاب بصری قبلاً انجام شده و انتخاب دوباره ممنوع است | continuity باید repair شود، نه تصمیم کاربر | تصحیح صریح کاربر |
| 2026-07-18 | Quest/Achievement settlement با learning event اتمیک است | projection به‌تنهایی authority نیست | DAO transaction و ۱۱ integration test |
| 2026-07-18 | claim Quest یک‌باره و replay-safe است | جلوگیری از اقتصاد قابل‌تقلب | unit + exact-final runtime proof |
| 2026-07-18 | startup reconciliation باید fast-path داشته باشد | startup نباید snapshot پایدار را دوباره باز کند | stage timing و source trace |
| 2026-07-18 | Baseline Profile از build غیرdebug و غیرR8 جمع‌آوری می‌شود | debug profile evidence معتبر نیست | benchmark build + packaged profile |
| 2026-07-18 | CI هم source profile و هم APK packaging را verify می‌کند | profile متروک یا package‌نشده نباید سبز بماند | `verify_baseline_profile.py` + workflow |
| 2026-07-18 | Journey definition از UI مستقل و در Room v3 persisted است | UI نباید chapter یا XP ساختگی بسازد | `JourneyCatalog`، migration v2→v3 و DAO settlement |
| 2026-07-18 | Journey reconciliation namespace مستقل دارد | catalog duplicate نباید bootstrap Journey را مسدود کند | defect واقعی upgrade، fix و `adb install -r` re-verification |
| 2026-07-18 | هر event حداکثر یک Journey stage را کامل می‌کند | evidence زودرس و جهش چندفصلی exploit نسازند | پنج integration test Journey |
| 2026-07-19 | Campaign choice یک commitment بدون reward است | tap-to-XP و route farming ممنوع‌اند | DAO guard + integration با evidence تاریخی و balance ثابت |
| 2026-07-19 | prerequisite با Journey ID و version دقیق bind می‌شود | version دیگر نباید campaign v1 را باز کند | catalog/planner/query/projection test |
| 2026-07-19 | فقط route منتخب materialize می‌شود و route دوم immutable است | انتخاب باید معنی‌دار، replay-safe و recovery-safe بماند | unique Room choice + conflict rollback + runtime cold restore |

## مانع‌ها

- **مالک محصول / امنیت:** credentialهای تاریخی باید revoke/rotate و history/cache/cloneها remediation شوند.
- **زیرساخت:** gateway، issuer/session، rate limit، abuse control و observability production باید deploy و اثبات شوند.
- **انتشار:** signing lineage، channel و install-update واقعی با artifact امضاشده لازم‌اند.
- **محتوا:** ۸ فایل، ۷۰۱ item و ۲۴۳ entry باید بازبینی تحریری/بالینی ثبت‌شده بگیرند.
- **continuity هویت:** identifier canonical کانسپت قبلاً منتخب باید از artifact معتبر بازیابی و ثبت شود؛ از کاربر انتخاب تازه خواسته نمی‌شود.

## Done

- settlement اتمیک و idempotent Quest/Achievement/Catalog.
- Home Mission Pulse با state واقعی، semantics و کنترل‌های حداقل ۴۸dp.
- exact-final runtime proof برای assign، سه review، complete، claim و replay guard.
- ۸۲ unit/JVM test، ۳ instrumentation test، lint و debug APK.
- benchmark APK، Baseline Profile متمرکز، verifier مستقل و CI packaging gate.
- Journey v1 چهار‌فصلی، migration v3، Home pulse، Journey Map، recovery و replay guard واقعی.
- Campaign route v1 دوشاخه با Room v4، zero-XP choice، confirmation، route map و cold restore.
- ۱۰۴ unit/JVM test در ۲۶ suite، ۳ instrumentation test، lint و debug/benchmark APK نهایی.
- شمارش شفاف: ۳۳٬۲۲۹ خط مهندسی/دادهٔ تألیفی بدون schema generated؛ ۳۸٬۳۴۳ با schemaها.
- feedback preference، Reduce motion، typed AI failures و copy صادقانه.
- گزارش Markdown جاری به evidence نهایی تازه شد.
- PDF RTL Campaign به ۱۲ صفحه بازتولید، structurally checked و پس از inspection تک‌تک صفحه‌ها پذیرفته شد.

## Remaining

- identifier هویت منتخب قبلی را بازیابی و canonical کن؛ سپس mascot/palette/badge/shell را دقیق پیاده کن.
- route engine v1 را به campaign pack چند‌هفته‌ای/episodic توسعه بده؛ collection، personalization و content depth را نیز با state/persistence/failure/accessibility بساز.
- release/profile performance را روی دستگاه فیزیکی هدف و Macrobenchmark/jank budget اثبات کن.
- پس از فراهم‌شدن اختیار بیرونی، چهار گیت release را دوباره audit کن.
