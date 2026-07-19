# قرارداد حفظ برای بازسازی Cinnamon

- وضعیت: فعال؛ بازسازی رابط فقط به‌صورت افزایشی و برگشت‌پذیر مجاز است.
- محیط مبنا: checkout محلی Android در `C:\Users\K1\Desktop\Projects\Cinnamon`.
- تاریخ ثبت شواهد: ۲۰۲۶-۰۷-۱۹.
- مرز: این قرارداد از دادهٔ یادگیری، محتوا، اقتصاد پاداش، مسیرهای داخلی و مرز بیرونی انتشار حفاظت می‌کند. هیچ راز واقعی در این سند ثبت نمی‌شود.

## قاعدهٔ توقف

جهت بصری توسط کاربر قبلاً انتخاب شده، اما identifier canonical آن در artifactهای قابل‌بازیابی ثبت نشده است. تا بازیابی همان identifier، فقط تغییرهای identity-bearing متوقف می‌مانند؛ انتخاب تازه یا حدس خودسرانه ممنوع است. حذف فایل، حذف مسیر، تغییر ناسازگار schema، پاک‌سازی داده یا تغییر قرارداد gateway بدون برنامهٔ rollback و تأیید صریح کاربر نیز ممنوع است.

| دارایی یا قرارداد | منبع حقیقت و شواهد مبنا | کلاس | تغییر مجاز | روش بازگردانی | اثبات پذیرش |
| --- | --- | --- | --- | --- | --- |
| پایگاه دادهٔ کاربر و تاریخچهٔ schema | `AppDatabase` نسخهٔ ۴؛ schema در `app/schemas/com.cinnamon.app.data.local.AppDatabase/4.json` با SHA-256 `D20E7D4DBDF9554E041BFCAB22859DCE45D51BA3C1C3A79881982CD5C8717C1D` | تغییر سازگار | فقط migration افزایشی و قابل آزمون | تا زمانی که export صریحِ کاربر یا account restore محصولی ساخته نشده، داده فقط روی همان دستگاه حفظ می‌شود | migration verifier، instrumentation و proof ارتقای v3→v4 با `adb install -r` روی دادهٔ محفوظ |
| رویدادها، تراکنش‌ها و رسیدهای پاداش | جدول‌های Room `gamification_events`، `reward_transactions`، `reward_summaries`، `reward_balances` و receiptها | غیرقابل‌تغییر | افزودن projection یا version جدید؛ هیچ rewrite یا حذف history مجاز نیست | فقط restore محصولیِ آینده با رضایت کاربر یا بازگردانی artifact پیشین؛ Android Auto Backup و انتقال خودکار عمداً غیرفعال‌اند | invariantهای uniqueness، idempotency و atomicity در آزمون ledger |
| تنظیمات و دادهٔ قدیمی پیشرفت | DataStore با نام `cinnamon_progress` در `ProgressStore`؛ فقط bridge یک‌باره به ledger | غیرقابل‌تغییر | adapter/خواندن سازگار؛ حذف key یا reset بدون تأیید ممنوع | تنها export/account restore صریحِ آینده؛ backup یا انتقال خودکار Android مسیر recovery نیست | import idempotent، حفظ theme/goal/focus و startup test |
| فرهنگ واژگان و seed | `app/src/main/assets/lexicon/manifest.json`، dataset `cinnamon.lexicon.core@2026.07.17`؛ ۸ فایل، ۷۰۱ item و ۲۴۳ entry؛ `origin=project-bundled-curated` و `reviewStatus=editorial-review-required-before-release` | غیرقابل‌تغییر | افزودن نسخهٔ جدید با manifest و migration مستند؛ origin/status فقط از قرارداد پذیرفته‌شده | restore فایل‌های نسخه‌دار و manifest | hashهای manifest، provenance validator/runtime test و clean-install seed |
| کاتالوگ گیمیفیکیشن | `app/src/main/assets/gamification/catalog-v1.json`، نسخهٔ `1.0.0`، SHA-256 `EC5AF859E04D654EDCB0F69FFF59ACD5091AC2E9ABD49C143013E1F26F0BD60A`؛ ۴ rarity، ۱۵ reward، ۵ presentation، ۶ level، ۱۲ achievement و ۱۲ quest | تغییر سازگار | IDهای پایدار افزایشی؛ retired IDها رزرو می‌مانند | restore asset version و adapter/نسخهٔ جدید | catalog validator و testهای threshold/ID/localization |
| Journey و chapter progress | `JourneyCatalog.kt` و جدول‌های `journey_instances` / `journey_stage_progress`؛ `journey.foundation.expedition@1` با چهار stage | غیرقابل‌تغییر برای instance موجود | definition جدید با version/ID جدید؛ stage ID و order نسخهٔ فعال rewrite نشوند | adapter/version جدید و projection سازگار؛ هیچ reset یا backfill destructive مجاز نیست | migration v3، sequential settlement، collision rollback و runtime replay proof |
| Campaign route commitment | `CampaignCatalog.kt` و `campaign_route_choices`؛ `campaign.foundation-route-choice@1` با Precision/Momentum | غیرقابل‌تغییر برای choice موجود | campaign/route version جدید افزایشی؛ route ذخیره‌شدهٔ v1 rewrite یا switch نشود | projection نسخهٔ قدیمی و adapter صریح؛ restore snapshot فقط با حفظ choice/event/Journey | migration v4، unique choice، zero-XP selection، conflict rollback و cold restore |
| نشان‌ها، تصویرها و previewهای برند | `docs/codex/.../assets/brand-concepts-v1/`؛ ده تصویر صرفاً پیش‌نمایش مفهومی‌اند | مشتق/بازساختنی | تولید asset نهایی فقط پس از بازیابی و ثبت identifier انتخاب قبلی | regenerate از concept canonical یا restore assetهای workspace | preview-to-production manifest و screenshot runtime |
| مسیرها و deep-linkهای داخلی | `AppNavigation.kt`؛ مسیرهای Home، Lexicon، Practice، Profile و routeهای activity/detail | تغییر سازگار | مسیر جدید افزایشی یا mapping؛ حذف route فقط با تأیید | adapter یا بازگردانی نگاشت قدیمی | تست navigation و بازگشت کاربر |
| مرز AI/gateway | `AiGatewayClient.kt` و endpoint `POST v1/ai/chat`؛ Android بدون token واقعی fail-closed است | تغییر سازگار | adapter سازگار با contract gateway؛ هیچ credential در APK نیست | بازگردانی configuration عمومی و build fail-closed | request contract test و runtime not-sent/degraded path |
| محیط، رازها و انتشار | `local.properties.example` فقط کلید عمومی `AI_GATEWAY_BASE_URL` را نام می‌برد؛ signing/auth/gateway تولیدی بیرون از checkout است | غیرقابل‌تغییر | فقط wiring عمومیِ سازگار؛ هیچ راز/keystore در repository نیست | مالک محصول از secret store و signing backup خود بازمی‌گرداند | release gate خارجی، امضا و install-update واقعی |

## مسیرهای حفاظت‌شده

| مسیر فعلی | پارامتر | رفتار حفاظت‌شده | سیاست بازسازی |
| --- | --- | --- | --- |
| `home` | — | آغاز، progress و دسترسی به moduleها | نگه‌داری؛ presentation بازطراحی می‌شود |
| `lexicon` و `entry/{id}` | شناسهٔ entry | جست‌وجو و جزئیات لغت | نگه‌داری؛ detail فقط با همان ID داده را می‌خواند |
| `practice`، `review`، `cloze`، `vocab_match`، `unscramble` | — | تمرین و اثرهای پایدار | نگه‌داری؛ هیچ interaction نباید bypass ledger بسازد |
| `gamification_hub` | — | Journey Map، Campaign route، progress، quest و receipt | نگه‌داری؛ UI جدید باید choice/stage/receiptهای Room را مصرف کند و route یا stage جعلی نسازد |
| `clinical_sim_labs`، `scenario_select`، `patient_chat/{scenarioId}` | شناسهٔ scenario | تمرین راهنمای زبان و مرز ایمنی بالینی | نگه‌داری؛ ادعای تشخیص/پایش واقعی ممنوع |
| `native_fluency_playground/{tab}` و `make_it_native` | نام tab | تمرین روانی/لحن | نگه‌داری؛ `make_it_native` به تمرین لحن هدایت می‌شود |
| `profile` | — | نمایش journey و preferenceها | نگه‌داری؛ theme/goal/focus DataStore حفظ می‌شود |

## snapshot و rollback

- Snapshot منبع و تغییرات: `git status --short` در آغاز چرخهٔ بازسازی ثبت شد؛ worktree از قبل حاوی اصلاحات جاری است و نباید reset یا overwrite شود.
- دادهٔ واقعی کاربر در workspace وجود ندارد و Auto Backup/انتقال خودکار Android عمداً برای دادهٔ app-owned غیرفعال است؛ بنابراین هیچ migration ناسازگار، پاک‌سازی داده یا route removal در این مرحله مجاز نیست. هر recovery آینده به export صریح یا account restore با رضایت کاربر نیاز دارد.
- برای assetهای نسخه‌دار، manifest/hash و source asset مسیر بازگردانی‌اند.
- برای کد رابط، تغییرات باید افزایشی باشند؛ قبل از حذف هر surface، screenshot/آزمون مسیر پیشین و rollback patch لازم است.

## گیت تأیید پیش از بازسازی بصری

1. identifier همان concept یا blend قبلاً منتخب از artifact معتبر بازیابی و canonical ثبت شود؛ تأیید/انتخاب دوباره درخواست نشود.
2. مسیر canonical به asset decomposition manifest تبدیل شود.
3. فقط routeهای جدید/adapterهای افزایشی ساخته شوند؛ مسیرهای قدیمی تا اثبات parity باقی بمانند.
4. پیش از هر حذف، اثر کاربر، rollback و ماتریس پذیرش ارائه و تأیید صریح دریافت شود.
