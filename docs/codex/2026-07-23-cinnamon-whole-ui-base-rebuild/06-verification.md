# ماتریس اعتبارسنجی

## baseline تأییدشده

| بررسی | نتیجه |
|---|---|
| `python scripts/verify_lexicon_assets.py app/src/main/assets/lexicon` | passed |
| `python scripts/verify_mesh_reference.py` | passed؛ ۵٬۷۴۲ descriptor و تمام category/hash/count contractها |
| `python scripts/verify_room_migration.py` | passed برای v1-v5→v6؛ ۵۶ statement و conservation روی DB پر |
| Room schema v5 hash | `8D13191D5DC40422D7EB394217350444ADC303EE83162BDE1E040F4080035E9F` |
| Room schema v6 hash | `A619925A0A23C67FD5CA726B1DCA7C76E844E66F4EA7FBFACA90198937A5AE57` |
| MeSH payload hash | `63203108BB9F5EB5A20F1A9CC7F9E2451FF140F0DA5B0CD044A2E6A89BDCDF7E` |
| populated DB integrity/FK/logical orphans | `ok` / ۰ / ۰ |
| catalog refs/duplicates/orphans | ۰ مشکل |
| full `testDebugUnitTest` | ۱۵۹ تست در ۳۷ suite؛ ۰ failure/error/skip |
| `lintDebug` | BUILD SUCCESSFUL؛ ۰ error، ۳۲ warning و هیچ warning مربوط به Atlas |
| managed Android 35 dataset/startup tests | ۲ تست؛ ۰ failure/error/skip؛ مجموع ۱۲٫۰۱۱s |
| critical app readiness / background Atlas readiness | ۱٬۲۵۳ms / ۸٬۰۹۶ms در آخرین اجرای ترکیبی |
| Reference Atlas first seed / current check | آخرین اجرا ۳٬۵۱۵ms / ۵ms؛ دامنهٔ seed مشاهده‌شده ۳٬۱۵۳–۷٬۳۱۶ms |
| stale snapshot / learner state | stale row حذف شد؛ SRS و بوکمارک دقیقاً حفظ شدند |
| debug APK / سهم فشردهٔ Atlas | ۲۳٬۱۳۸٬۳۰۱ byte / ۹۰۵٬۵۹۴ byte معادل ۳٫۹۱٪ |
| independent audit PDF | ۲۰/۲۰ صفحه بازرسی بصری شد |

## گیت Reference Atlas

این لایه فقط وقتی معتبر است که همهٔ شروط زیر هم‌زمان برقرار باشند:

- `manifest.json` نسخه، count، hash، attribution و selection policy را صریح ثبت کند.
- payload دقیقاً ۵٬۷۴۲ رکورد یکتا با `meshUi` پایدار داشته باشد.
- هر رکورد scope note غیرخالی و دست‌کم یک tree number معتبر در ریشه‌های `A/C/E/F/G/N` داشته باشد.
- migration v5→v6 فقط جدول و indexهای جدید را اضافه کند و هیچ row کاربر را تغییر ندهد.
- seeder در transaction واحد، idempotent و با checksum/count fail-closed اجرا شود.
- خرابی Atlas هرگز باعث reset شدن lexicon، SRS، بوکمارک یا reward ledger نشود.
- CI هم corpus عمیق و هم Atlas مرجع را مستقل اعتبارسنجی کند.

## گیت‌های هر surface

| حوزه | source proof | test proof | runtime proof | visual proof |
|---|---|---|---|---|
| Shell/startup | typed gate + aliases | restored-route matrix | cold/warm start | 3 width × states |
| Lexicon | stable IDs + corpus hashes | search/detail/not-found | saved review | long text + RTL/LTR |
| Reference Atlas | MeSH UI + manifest + source hash | dataset/DAO/seeder/migration | first seed + idempotent relaunch | search/empty/error/long scope |
| Practice/review | event contracts | idempotency/replay | real receipt | interaction states |
| Quest/achievement | ledger-only | atomic claim/replay | relaunch persistence | reduced-motion |
| Clinical/fluency | embedded corpus inventory | registry/invalid arg | offline/safety | long content |
| AI roleplay | gateway fail-closed | typed failures | not-sent/degraded | recovery copy |
| Profile/settings | Room + DataStore | persistence | force-stop/relaunch | compact/expanded |
| Assets/performance | manifest + budgets | validator | decode/startup/jank | crop/fidelity |

## اندازه‌ها و دسترس‌پذیری

- width matrix: compact phone، medium/foldable، expanded tablet
- font scale: 1.0، 1.3، 2.0
- themes: default toasted-dark، light/clinical clarity، high-contrast dark
- TalkBack traversal و control semantics
- touch target حداقل 48dp
- reduced motion، sound off، haptic off
- system back و toolbar back parity

## performance budgetهای لازم پیش از asset integration

Budget نهایی باید پس از canonical assets تعیین شود. حداقل اندازه‌گیری:

- cold/warm startup روی build غیرdebug و دستگاه هدف
- frame timing و jank برای Home map، Journey و celebration
- first decode، repeat decode و memory footprint برای mascot/map/badge
- source/derived variants و density-aware loading
- no eager decode برای assetهای خارج viewport

## release truth

موفقیت testها به معنی release-ready نیست. release تنها وقتی ممکن است که:

- product gateway/session واقعی کار کند،
- signing lineage و master icon اثبات شود،
- signed upgrade با حفظ داده روی دستگاه انجام شود،
- content/editorial/clinical approval ثبت شود،
- logها redacted و secret boundary دوباره audit شوند.
