<div dir="rtl">

> **وضعیت این سند:** این فایل snapshot اولیهٔ ممیزی است و برای حفظ تاریخچه نگه‌داری می‌شود. عبارت‌های آن دربارهٔ «انتخاب‌نشدن کانسپت»، «settlement ناقص» و شمار آزمون‌های قدیمی superseded هستند. وضعیت جاری و شواهد نهایی در [گزارش اجرایی پرفکشن Cinnamon](CINNAMON_GAMIFICATION_VISUAL_DIRECTION_2026-07-18_FA.md) ثبت شده‌اند؛ کاربر کانسپت را قبلاً انتخاب کرده و از او انتخاب دوباره خواسته نمی‌شود.

# گزارش جامع نقد، بازطراحی و مسیر پرفکشن Cinnamon

**تاریخ ارزیابی:** ۲۶ تیر ۱۴۰۵ / 17 July 2026  
**دامنه:** محصول Android، تجربه یادگیری، گیمیفیکیشن، داده، SRS، AI، backend boundary، CI/CD، performance، error handling، copy و هویت بصری  
**وضعیت فعلی:** در حال remediation - هنوز آماده انتشار عمومی نیست  
**اصل حاکم:** جذابیت بالا، بدون امتیاز جعلی، ادعای AI جعلی، شبکه اجتماعی ساختگی یا پاداش قابل‌فارم

## جمع‌بندی مدیریتی

Cinnamon محتوای تخصصی ارزشمندی دارد: ۲۴۳ مدخل واژگانی، ۱۶۰ جزء واژه‌ساز، ۹۵ abbreviation، ۷۸ عبارت، ۴۵ confusable و ۸۰ جمله تمرینی. مسئله اصلی کمبود قابلیت نیست؛ مسئله این است که بخش بزرگی از تجربه فعلی میان «دموی پرزرق‌وبرق» و «محصول قابل اعتماد» گیر کرده است. بعضی قابلیت‌ها نام AI، Whisper، ElevenLabs، empathy score، global leaderboard، guild، ELO و reward واقعی دارند، اما پیاده‌سازی‌شان محلی، تصادفی، موقتی یا حتی صرفاً نمایشی است.

پرفکشن Cinnamon باید از سه ستون هم‌زمان ساخته شود:

1. **اعتماد:** هیچ کلید provider در APK، هیچ نتیجه یا امتیاز جعلی، هیچ leaderboard ساختگی و هیچ write نیمه‌کاره.
2. **انگیزه:** حلقه مأموریت، پیشرفت، achievement، بازخورد و celebration واقعی و قابل‌توضیح.
3. **هویت:** یک جهان بصری مشخص، mascot system، motion grammar و زبان برند واحد که پس از انتخاب کانسپت پیاده شود.

## حکم انتشار

| حوزه | وضعیت فعلی | حکم |
|---|---|---|
| مرز secret و provider | اتصال مستقیم حذف شده؛ سابقه Git و rotation بیرونی باقی است | مسدود برای انتشار |
| AI coaching | fail-closed و fallback محلی صادقانه؛ backend/auth واقعی موجود نیست | فقط حالت تمرین محلی |
| داده و seed | manifest و verifier اضافه شده؛ migration/runtime proof در حال تکمیل | مشروط |
| SRS | الگوریتم پایه موجود؛ transaction واحد review/reward هنوز لازم است | مشروط |
| گیمیفیکیشن | UI غنی اما state فعلی عمدتاً موقتی و قابل‌فارم | بازطراحی بنیادین لازم |
| هویت بصری | ۱۰ مسیر تصویری آماده؛ انتخاب نهایی کاربر انجام نشده | گیت تأیید باز است |
| CI | workflow verification کم‌مجوز شده؛ release/signing عمداً غیرفعال است | verification only |
| runtime Android | compile باید پس از ادغام کامل تکرار شود؛ device proof نهایی نداریم | اثبات نشده |

## روش ارزیابی

این گزارش بر مبنای بررسی کد، داده‌های واقعی assets، مسیرهای navigation، stateهای ViewModel، Room/DataStore، workflow، جست‌وجوی ادعاهای جعلی، بررسی build و پیش‌نمایش‌های ImageGen ساخته شده است. هر ادعا در یکی از چهار وضعیت قرار می‌گیرد:

- **اصلاح‌شده:** کد یا قرارداد تغییر کرده و check مناسب دارد.
- **در حال تکمیل:** foundation ساخته شده اما integration یا runtime proof باقی است.
- **گیت کاربر:** تصمیمی که بدون انتخاب برند/کانسپت نباید حدس زده شود.
- **مالکیت بیرونی:** rotation secret، backend، auth، signing lineage یا انتشار فروشگاه.

## یافته‌های بحرانی

### P0-1 - افشای credential و اتصال مستقیم provider از موبایل

نسخه قبلی `.env.example` شامل مقدارهای واقعی بود، secretها به BuildConfig می‌رفتند و کلاینت مستقیماً با provider صحبت می‌کرد. BODY logging نیز امکان ثبت payload را داشت. حذف مقدار از HEAD، تاریخچه Git یا credential صادرشده را باطل نمی‌کند.

**اصلاح انجام‌شده**

- `.env.example` ناامن حذف شد.
- Secrets Gradle Plugin و provider key از BuildConfig حذف شد.
- direct provider client، promptهای provider-side و BODY logger حذف شدند.
- تنها `AI_GATEWAY_BASE_URL` عمومی و HTTPS مجاز است.
- verifier در CI هر بازگشت direct host، provider key یا BODY logging را رد می‌کند.

**شکاف بیرونی**

- تمام credentialهای قبلی باید revoke/rotate شوند.
- اگر repository جایی منتشر شده، تاریخچه باید با هماهنگی مالک repository پاک‌سازی شود.

### P0-2 - release signing و هویت artifact

نسخه پیشین workflow می‌توانست keystore موقت بسازد و artifact را با هویت عمومی منتشر کند. چنین artifactی update lineage قابل‌اعتماد ندارد و نباید release محسوب شود.

**اصلاح انجام‌شده**

- workflow فعلی فقط lint/test/verification است.
- permission به `contents: read` محدود شده است.
- actionها با commit SHA ثابت شده‌اند.
- ساخت release تا ارائه signing lineage واقعی، نام، آیکن، version policy و channel انتشار مسدود است.

### P0-3 - امتیاز و ارزیابی جعلی

چند surface با random number، مقایسه string یا حرکت UI نتیجه‌هایی مانند empathy rating، speech score، clinical accuracy و XP می‌ساختند. این رفتار هم اعتماد را از بین می‌برد و هم reward economy را قابل‌فارم می‌کند.

**اصل اصلاح**

- اگر داده واقعی اندازه‌گیری نمی‌شود، UI باید آن را «guided rehearsal» یا self-check بنامد.
- هیچ XP از animation، random score، toggle یا متن ثابت صادر نمی‌شود.
- assessment بالینی، empathy score یا proficiency claim تنها با rubric معتبر و evidence قابل‌ردیابی مجاز است.

### P0-4 - state گیمیفیکیشن غیرپایدار و قابل تزریق

در نسخه فعلی ده‌ها UI مستقیماً `addPoints(Int)` را صدا می‌زنند. questها با checkbox claim می‌شوند، خرید defibrillator از XP کم نمی‌کند، donate حتی XP اضافه می‌کند و guild/ELO/leaderboard پشتوانه server ندارند.

**معماری هدف**

```text
Semantic learning action
  -> immutable learning event
  -> versioned reward rule
  -> single Room transaction
  -> reward transaction + projections
  -> quest/achievement evaluation
  -> persisted presentation receipt
  -> UI celebration
```

هیچ screen نباید مقدار XP دلخواه صادر کند. screen فقط رویدادی مانند `review_completed`، `cloze_round_completed` یا `sentence_rehearsal_completed` را با idempotency key پایدار ثبت می‌کند.

## اصلاحات انجام‌شده تا این نسخه

### AI و صداقت محصول

- endpoint تنها HTTPS است و URLهای دارای credential، query یا fragment رد می‌شوند.
- mode فقط از allowlist `native_coach` و `standardized_patient` می‌آید.
- system prompt، model و provider credential در Android وجود ندارد.
- پیام «ارسال نشد» از «ارسال ممکن است رخ داده باشد ولی نتیجه تأیید نشد» جدا شده است.
- idempotency key به شناسه پایدار پیام متصل شده است.
- سناریوی بیمار با ID کاتالوگی ارسال می‌شود، نه عنوان نمایشی.
- medical mode از رنگ UI استنتاج نمی‌شود؛ نوع session صریح است.
- VoiceEngine دیگر transcript، audio یا confidence جعلی تولید نمی‌کند و capabilityها را `NotConfigured` اعلام می‌کند.
- Shadowing از analyzer جعلی به rehearsal زمان‌بندی‌شده و self-check خصوصی تبدیل شده است.

### داده و seed

- `manifest.json` نسخه، count و SHA-256 نرمال‌شده برای ۸ asset را نگه می‌دارد.
- verifier مستقل موارد زیر را کنترل می‌کند:
  - UTF-8 و JSON معتبر
  - root collection صحیح
  - فیلدهای ضروری غیرخالی
  - enumهای domain/level/kind/register
  - duplicate identity در کل corpus
  - count دقیق و hash هر فایل
- seeder ابتدا همه فایل‌ها را validate/parse می‌کند و سپس تمام insertها را در یک Room transaction انجام می‌دهد.
- marker seed فقط پس از commit نوشته می‌شود.
- Home برای first-run حالت `Validating / Ready / Failed` و retry واقعی دارد؛ شکست، content نیمه‌نوشته نشان نمی‌دهد.

### CI و امنیت زنجیره تأمین

- اجرای workflow فقط روی PR، main و dispatch دستی است.
- release/deploy/signing از workflow verification حذف شده است.
- reportهای lint/test به‌عنوان artifact نگه‌داری می‌شوند.
- secret-boundary و lexicon-integrity پیش از Gradle اجرا می‌شوند.

## معماری گیمیفیکیشن پیشنهادی

### حلقه اصلی روزانه

```text
ورود کوتاه
  -> انتخاب مأموریت ۳ تا ۸ دقیقه‌ای
  -> تمرین فعال
  -> بازخورد قابل توضیح
  -> پاداش واقعی و محدود
  -> یک انتخاب بعدی واضح
  -> پایان دلپذیر بدون اجبار
```

### منابع و اقتصاد

| منبع | کارکرد | قانون سلامت اقتصاد |
|---|---|---|
| XP | نمایش effort و consistency | فقط از event معنایی، cap روزانه برای eventهای تکراری |
| Level | بازکردن مسیر و محتوای جدید | از XP تجمعی ledger، بدون downgrade ناگهانی |
| Streak | تداوم مطالعه | بر اساس study day محلی با grace شفاف |
| Streak Repair | جبران محدود | موجودی پایدار؛ خرید واقعی باید balance را کم کند |
| Mastery | کیفیت SRS هر حوزه | از schedule واقعی، نه XP یا self-report |
| Cosmetic unlock | حس مالکیت | پس از انتخاب کانسپت برند تعریف می‌شود |

### قواعد ضد farm

- `idempotencyKey` یکتا برای هر action/session item.
- پاداش اولین completion از retry جدا است.
- replay برای تمرین آزاد ممکن است، ولی XP آن محدود یا صفر است.
- slider، checkbox، باز کردن صفحه و animation هیچ پاداشی ندارند.
- reward، quest و achievement در یک transaction ارزیابی می‌شوند.
- presentation animation فقط receipt را acknowledge می‌کند و هرگز grant جدید نمی‌سازد.

### questها

quest خوب باید رفتار یادگیری مشخص داشته باشد؛ مثال:

- سه review سررسیدشده را کامل کن.
- یک cloze round را با حداقل ۸۰٪ پاسخ درست تمام کن.
- از سه حوزه متفاوت یک واژه مرور کن.
- یک عبارت patient-friendly را در self-check ذخیره کن.

quest نامعتبر:

- ۵۰ XP بگیر.
- روی دکمه donate بزن.
- یک screen را باز کن.
- empathy score ساختگی را به ۹۰٪ برسان.

### achievementها

achievementها باید stable ID، criteria version، progress، rarity، unlock event و title/description جدا از منطق داشته باشند. خانواده‌های پیشنهادی:

- شروع: نخستین review، نخستین cloze، نخستین bookmark.
- عمق: ۲۵/۱۰۰/۲۵۰ واژه با interval واقعی.
- تنوع: مرور چند domain در یک هفته.
- دقت: sessionهای بدون پاسخ اشتباه با حداقل sample size.
- تداوم: study dayهای واقعی، نه بازکردن app.
- مهارت ارتباطی: تکمیل rubricهای معتبر، نه score تصادفی.
- کشف: morpheme، confusable و phrase collection.

## معماری اطلاعات و navigation

ساختار فعلی چهار tab اصلی دارد: Home، Lexicon، Practice و Profile. route IDهای فعلی برای حفظ state قابل نگه‌داری‌اند، اما mental model باید پس از انتخاب کانسپت به این نقش‌ها تبدیل شود:

| نقش | محتوای هدف |
|---|---|
| Today | مأموریت امروز، streak، due queue، یک انتخاب اصلی |
| Library | واژه، ریشه، abbreviation، phrase و mix-up |
| Play | review، بازی‌ها، rehearsal و سناریوها |
| Journey | level، mastery، achievement، collection و تنظیمات |

ریسک فعلی، mega-screenهای ۱۰۰۰ تا ۱۶۰۰ خطی با sidebar ثابت و ده‌ها module داخلی است. هر module باید route/state مستقل، loading/error semantics و event contract مستقل داشته باشد.

## ده کانسپت برند و محیط اپ

همه کانسپت‌ها پیش‌نمایش کامل ImageGen دارند. انتخاب یک کانسپت یا blend صریح، گیت شروع بازطراحی UI، mascot و motion است.

| # | کانسپت | هویت | نقطه قوت | ریسک |
|---:|---|---|---|---|
| ۱ | Cinnamon Questlands | نقشه ماجراجویی و قلمروهای دانشی | قوی‌ترین حس journey و progression | مراقبت از شلوغی نقشه |
| ۲ | Morph Lab | آزمایشگاه واژه‌سازی | بسیار متناسب با ریشه‌شناسی و علم | ممکن است سرد شود اگر mascot ضعیف باشد |
| ۳ | Clinical Comicverse | کمیک پزشکی اپیزودیک | انرژی، روایت و shareability بالا | نیازمند art pipeline منسجم |
| ۴ | Cinnamon Care City | شهر زنده مهارت‌ها | توسعه‌پذیر برای hub و collection | نیازمند hierarchy قوی |
| ۵ | Pulse Jam | ریتم، موسیقی و ضربان | مناسب shadowing و fluency | همه contentها موسیقایی نیستند |
| ۶ | Cinnamon Orbit | منظومه مهارت‌ها | mastery map زیبا و scalable | خطر دورشدن از گرمای برند |
| ۷ | Cinnamon Forge | ساخت و صیقل زبان | progression ملموس و rewarding | استعاره forge باید نرم و انسانی بماند |
| ۸ | Case Noir | پرونده و معمای بالینی | تمرکز، intrigue و case-based learning | برای استفاده روزانه ممکن است تیره شود |
| ۹ | Shift Theatre | صحنه اجرای نقش‌های بالینی | عالی برای roleplay و سناریو | lexicon باید backstage منسجم بگیرد |
| ۱۰ | Living Lexicon Wilds | جهان طبیعی واژه‌های زنده | mascot و collection بسیار قوی | نیازمند کنترل fantasy/medical balance |

### مسیر فایل‌های پیش‌نمایش

```text
C:\Users\K1\.codex\visualizations\2026\07\17\019f6e50-6eb8-7671-b862-ba27319ec7f7\cinnamon-brand-concepts-v1\
```

فایل‌ها از `01-cinnamon-questlands.png` تا `10-living-lexicon-wilds.png` نام‌گذاری شده‌اند.

### پیشنهاد انتخاب

- اگر هدف اصلی retention و حس سفر است: **Questlands + Living Lexicon Wilds**.
- اگر تمایز علمی/حرفه‌ای مهم‌تر است: **Morph Lab + Cinnamon Forge**.
- اگر roleplay و محتوای نمایشی محور است: **Clinical Comicverse + Shift Theatre**.
- اگر محصول باید آرام‌تر و premium باشد: **Cinnamon Orbit با گرمای Cinnamon Forge**.

هیچ blend نباید بیش از دو استعاره اصلی داشته باشد؛ سه جهان هم‌زمان محصول را شلوغ و بی‌هویت می‌کند.

## mascot system پس از انتخاب کانسپت

mascot نباید فقط sticker باشد. حداقل stateهای لازم:

- idle و welcome
- mission offered
- effort encouragement
- correct answer
- recoverable mistake
- streak protected
- achievement unlock
- offline/degraded
- reduced-motion variant

قواعد:

- mascot هرگز پزشک واقعی یا authority بالینی جلوه نمی‌کند.
- اشتباه کاربر را شرم‌آور نمی‌کند.
- celebration متناسب با اندازه پاداش است.
- pose و silhouette باید در ۲۴dp تا hero-size قابل تشخیص باشد.
- متن حیاتی، control و داده زنده داخل تصویر flatten نمی‌شود.

## motion grammar

| رویداد | motion | محدودیت |
|---|---|---|
| پاسخ صحیح | 180-260ms spring + tint | بدون پرش layout |
| پاسخ غلط | shake بسیار ملایم + hint | بدون تنبیه بصری |
| reward کوچک | count-up کوتاه | یک بار برای هر receipt |
| achievement | reveal چندمرحله‌ای | قابل skip و reduced-motion |
| route | fade/scale جهت‌دار | حفظ context و back expectation |
| loading | skeleton یا progress صادقانه | بدون fake completion |

## backend و AI

backend در repository فعلی وجود ندارد. قرارداد پیشنهادی gateway:

- `POST /v1/ai/chat`
- product session کوتاه‌عمر
- mode allowlist
- scenario ID کاتالوگی
- durable idempotency
- rate limit و abuse control
- prompt policy سمت سرور
- timeout، circuit breaker و redacted logs
- provider secret فقط در secret manager

تا وقتی auth/backend واقعی وجود ندارد، UI باید local guided practice را نمایش دهد و هیچ live-AI claim نداشته باشد.

## performance

ریسک‌های اصلی:

- mega-screenهای Compose با stateهای زیاد و recomposition گسترده.
- animationهای infinite و Canvasهای متعدد بدون lifecycle budget.
- ساخت Retrofit/OkHttp برای هر درخواست به‌جای singleton کنترل‌شده.
- queryهای `ORDER BY RANDOM()` در چند مسیر؛ با رشد corpus هزینه افزایش می‌یابد.
- hardcoded UI و عدم decomposition، screenshot profiling را دشوار کرده است.

معیار پذیرش:

- startup و first meaningful content روی دستگاه واقعی اندازه‌گیری شود.
- jank در scroll و celebration با Macrobenchmark/FrameMetrics بررسی شود.
- memory leak در TTS، coroutine و long-lived ViewModel بررسی شود.
- network client singleton و cancellation-aware باشد.
- asset اندازه/فرمت پس از انتخاب کانسپت budget داشته باشد.

## accessibility و multilingual

- متن‌ها اکنون تقریباً همگی hardcoded هستند و `stringResource` استفاده نمی‌شود.
- `supportsRtl=true` به‌تنهایی localization نیست.
- contrast در mega-screenها به‌علت `Color.White/Black/Gray` ثابت تضمین نمی‌شود.
- dynamic type، ۲۰۰٪ text، focus order، touch target و reduced motion باید gate نهایی باشند.
- فارسی و انگلیسی باید با واژگان محصولی ثابت، plural/date/number درست و assetهای بدون متن حیاتی پیاده شوند.

## نقشه اجرایی

### فاز ۰ - اعتماد و release safety

- rotate/revoke credentialهای سابق.
- تثبیت gateway boundary و CI.
- تعیین signing lineage و identity انتشار.

### فاز ۱ - data/reward foundation

- Room migration additive و schema export.
- event/reward ledger و projectionها.
- seed transaction و dataset manifest.
- migration تست‌شده از v1.

### فاز ۲ - behavior integrity

- review transaction واحد.
- حذف تمام `addPoints(Int)` از UI.
- حذف scoreها و social claimهای جعلی.
- persistence واقعی quest/achievement/reward receipt.

### فاز ۳ - انتخاب هویت

- انتخاب کانسپت یا blend دوگانه.
- approval برای palette، typography، mascot family، navigation vocabulary و motion.
- تولید preview واقعی برای Today، Library، Play، Journey و یک success state.

### فاز ۴ - بازطراحی shell و core loop

- design tokenها و component system.
- Today mission loop.
- review و بازی‌های اصلی.
- reward presentation و achievement gallery.

### فاز ۵ - QA و انتشار

- screenshot test routeهای واقعی.
- small phone، Pixel-class، RTL/LTR، ۲۰۰٪ text، dark/light و reduced motion.
- emulator/device runtime proof.
- release signing و artifact verification.

## ماتریس راستی‌آزمایی نهایی

| الزام | شاهد لازم |
|---|---|
| secret در موبایل نیست | scanner + source search + APK scan |
| migration امن است | Room migration test از fixture واقعی v1 |
| reward دوباره grant نمی‌شود | concurrency/idempotency test |
| review نیمه‌کاره نمی‌ماند | transaction rollback test |
| seed ناقص نمایش داده نمی‌شود | corrupted-asset rollback test |
| UI جذاب و منسجم است | preview approval + screenshot comparison |
| responsive است | حداقل سه viewport + dynamic type |
| دسترس‌پذیر است | semantics/focus/contrast/reduced-motion checks |
| performance قابل قبول است | device benchmark و frame evidence |
| release معتبر است | signed artifact، lineage و install/update proof |

## تصمیم موردنیاز از کاربر

برای عبور از گیت بصری، یکی از این پاسخ‌ها کافی است:

- شماره یک کانسپت، مثلاً `7`.
- یک blend دوگانه، مثلاً `1 + 10`.
- یک کانسپت با تغییر مشخص، مثلاً `2، اما گرم‌تر و با mascot حیوانی`.

تا قبل از این انتخاب، foundation فنی، داده، reward، امنیت و گزارش قابل تکمیل‌اند؛ اما بازنویسی هویت UI، mascot و assetهای نهایی نباید حدس زده شود.

</div>
