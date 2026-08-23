# توسعهٔ شدید دیتاست: Cinnamon Reference Atlas

## نتیجهٔ این checkpoint

Cinnamon اکنون دو لایهٔ داده با مسئولیت‌های روشن دارد:

| لایه | نسخه | اندازه | نقش |
|---|---:|---:|---|
| Deep Learning Core | `cinnamon.lexicon.core@2026.07.17` | ۷۰۱ رکورد | محتوای تألیفی عمیق، تمرین، SRS، بوکمارک و مسیر یادگیری |
| MeSH Reference Atlas | `cinnamon.mesh.reference@2026.1` | ۵٬۷۴۲ descriptor | مرجع گسترده، جست‌وجو، taxonomy و کشف واژه‌های پزشکی |
| مجموع data plane | — | ۶٬۴۴۳ رکورد | بیش از ۹ برابر baseline و بدون رقیق‌کردن دک آموزشی |

Reference Atlas بیش از ۲۳ برابر تعداد entryهای عمیق core است، اما عمداً جایگزین core نمی‌شود. هر داده‌ای که به learner state متصل است همچنان فقط در لایهٔ عمیق نگه‌داری می‌شود.

## چرا یک لایهٔ مستقل؟

شناسه‌های بخشی از corpus قدیمی به ترتیب و فایل وابسته‌اند و SRS، بوکمارک و تاریخچهٔ کاربر به آن‌ها متصل است. append کردن هزاران واژه به همان deck می‌توانست:

- شناسه‌ها را جابه‌جا کند؛
- کیفیت آموزشی را با رکوردهای سطحی رقیق کند؛
- زمان seed و startup را برای همهٔ کاربران بالا ببرد؛
- migration و rollback را پرریسک کند.

بنابراین Atlas جدول، DAO، repository، نسخه و checkpoint مستقل دارد. شکست یا بازسازی آن هیچ مجوزی برای تغییر `lexicon_entries`، review queue، بوکمارک یا reward ledger ندارد.

## منبع و مجوز

منبع canonical، فایل descriptor رسمی [NLM MeSH 2026](https://www.nlm.nih.gov/databases/download/mesh.html) است. استفاده و attribution تابع [NLM Terms and Conditions](https://www.nlm.nih.gov/databases/download/terms_and_conditions.html) و [NLM Web Policies](https://www.nlm.nih.gov/web_policies.html) است.

ورودی build:

- archive: `desc2026.gz`
- source SHA-256: `9FE35B3170652376A592DAF69E91A80D6C693ECAF9C571CEB701D04204CB357D`
- attribution: `Courtesy of the U.S. National Library of Medicine.`
- endorsement: هیچ تأیید یا حمایت NLM ادعا نمی‌شود.

OpenStax Anatomy & Physiology به‌علت محدودیت‌های CC BY-NC-SA و سیاست‌های اضافی ingestion، و RxNorm full به‌علت ترکیب محتوای public و proprietary/UMLS، در این مرحله وارد محصول نشده‌اند. گستردگی نباید از مسیر مبهم‌کردن مجوز به‌دست آید.

## policy انتخاب

از descriptorهای 2026 فقط رکوردهایی انتخاب شده‌اند که:

1. scope note غیرخالی دارند؛
2. دست‌کم یک tree number در ریشه‌های `A/C/E/F/G/N` دارند؛
3. عمق دست‌کم یکی از tree numberها حداکثر ۳ است.

پوشش taxonomy با امکان هم‌پوشانی:

| ریشه | حوزه | تعداد |
|---|---|---:|
| `A` | Anatomy | ۶۹۷ |
| `C` | Diseases | ۱٬۸۵۳ |
| `E` | Analytical, Diagnostic and Therapeutic Techniques | ۱٬۵۳۴ |
| `F` | Psychiatry and Psychology | ۴۹۳ |
| `G` | Phenomena and Processes | ۱٬۱۶۷ |
| `N` | Health Care | ۶۳۲ |

هم‌پوشانی طبیعی MeSH حفظ می‌شود؛ جمع categoryها الزاماً برابر count کل نیست.

## قرارداد هر رکورد

هر خط `mesh-reference-2026.jsonl` شامل این فیلدها است:

- `meshUi`: شناسهٔ پایدار و primary key؛
- `term` و `synonyms`: واژهٔ ترجیحی و entry terms؛
- `scopeNote`: تعریف رسمی descriptor؛
- `treeNumbers` و `categoryRoots`: جایگاه taxonomy؛
- `introducedYear` و `lastUpdated`: provenance زمانی؛
- داده‌های مشتق‌شدهٔ جست‌وجو در Room، نه payload source.

فایل payload دارای ۲٬۶۴۲٬۰۲۶ بایت و SHA-256 زیر است:

`63203108BB9F5EB5A20F1A9CC7F9E2451FF140F0DA5B0CD044A2E6A89BDCDF7E`

## pipeline قابل بازتولید

```powershell
python scripts/build_mesh_reference.py
python scripts/verify_mesh_reference.py
python scripts/verify_lexicon_assets.py app/src/main/assets/lexicon
python scripts/verify_room_migration.py
```

`build_mesh_reference.py` فقط از archive staging و hash قفل‌شده تولید می‌کند. CI دانلود زنده انجام نمی‌دهد؛ artifact commit‌شده را با manifest و invariants بررسی می‌کند تا upstream drift باعث build غیرقطعی نشود.

بودجه‌های fail-closed نیز در manifest، validator و runtime یکسان قفل شده‌اند: حداکثر ۶٬۵۰۰ descriptor، payload چهار میلیون بایت، term با ۹۶ نویسه، scope note با ۱٬۲۰۰ نویسه، ۶۴ مترادف و ۱۶ tree number برای هر descriptor. رشد بعدی باید آگاهانه این قرارداد را بازبینی کند، نه اینکه بی‌صدا startup و APK را متورم کند.

## ادغام runtime

- Room از v5 به v6 با migration افزایشی ارتقا یافته است.
- جدول `mesh_reference` دارای primary key پایدار و سه index برای term، normalized term و source year است.
- DAO جست‌وجوی term/synonym/scope و فیلتر category را با limit محدود ارائه می‌کند.
- seeder از Moshi strict، hash/count/attribution checks و transaction واحد استفاده می‌کند.
- Atlas یک snapshot فقط‌خواندنی و قابل‌جایگزینی است: در update سالانه، snapshot قبلی و جدید داخل همان transaction تعویض می‌شوند تا descriptor حذف‌شده یا renameشده باقی نماند؛ rollback همچنان اتمیک است.
- نسخهٔ seed در DataStore با کلید مستقل `mesh_reference_seeded_version` ثبت می‌شود.
- Atlas prerequisite حیاتی startup نیست: AppStartup پس از آماده‌شدن core به `Ready` می‌رسد و Atlas با ۱٫۵ ثانیه فاصله در scope پس‌زمینه گرم می‌شود. failure آن Home، review، Questboard یا authored lexicon را قفل نمی‌کند. managed-device test برای critical readiness نیز budget پنج‌ثانیه‌ای دارد.

## شواهد فعلی

| اثبات | نتیجه |
|---|---|
| generator | ۵٬۷۴۲ descriptor، deterministic output |
| Atlas validator | count/hash/category/identity/scope/tree invariants passed |
| corpus validator | ۷۰۱ رکورد deep core بدون تغییر passed |
| migration verifier | v1-v5→v6 و populated conservation passed؛ ۵۶ statement |
| dataset contract unit test | ۲ تست passed |
| Room migration v6 test | ۴ تست passed |
| DAO test | ۳ تست passed؛ snapshot reconciliation نیز پوشش داده شد |
| startup recovery-copy test | ۴ تست passed |
| full JVM suite | ۱۵۹ تست در ۳۷ suite؛ ۰ failure/error/skip |
| Android lint | BUILD SUCCESSFUL؛ ۰ error و هیچ warning مربوط به Atlas |
| managed-device instrumentation | Android 35؛ ۲ تست، ۰ failure/error/skip؛ ۱۲٫۰۱۱s |
| critical Ready / background Atlas Ready | ۱٬۲۵۳ms / ۸٬۰۹۶ms در آخرین اجرای ترکیبی |
| first seed / current check | ۳٬۵۱۵ms / ۵ms در آخرین اجرا؛ دامنهٔ seed مشاهده‌شده ۳٬۱۵۳–۷٬۳۱۶ms |
| APK footprint | Atlas فشرده ۹۰۵٬۵۹۴ byte؛ ۳٫۹۱٪ از debug APK |

تست instrumentation seeder روی managed Android 35 اجرا شد: seed اولیه، count دقیق، lookup شناسهٔ `D000005`، جست‌وجوی term/synonym/scope با category isolation، حذف یک stale snapshot مصنوعی، حفظ SRS/بوکمارک، reseed idempotent و restore وضعیت اولیه را ثابت کرد. budget دستگاه QA برای seed اول ۱۵ ثانیه، current-check یک ثانیه و query مرجع یک ثانیه است.

## گیت‌های باقیمانده

1. upgrade واقعی build امضاشده با `adb install -r` روی دیتای قدیمی‌تر از v6.
2. اندازه‌گیری اثر Atlas در cold/warm startup کل اپ روی دستگاه فیزیکی هدف؛ اندازه‌گیری seeder مستقل انجام شده است.
3. دریافت و ادغام ممیزی مستقل Jules در session `15306253539687638977`.
4. طراحی surface جست‌وجو و جزئیات Atlas فقط بعد از preview canonical UI.
5. editorial/clinical review برای هر محتوای تألیفی جدید؛ MeSH به‌عنوان مرجع رسمی معرفی می‌شود، نه توصیهٔ پزشکی.
