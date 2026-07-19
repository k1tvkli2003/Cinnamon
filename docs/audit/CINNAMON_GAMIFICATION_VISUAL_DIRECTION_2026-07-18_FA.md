<div dir="rtl">

# گزارش اجرایی پرفکشن Cinnamon

## گیمیفیکیشن واقعی، مقیاس محصول، کارایی و گیت‌های انتشار

**تاریخ:** ۲۸ تیر ۱۴۰۵ / 19 July 2026  
**دامنه:** Android runtime، اقتصاد پاداش، Quest و Achievement، Journey و Campaign نسخه‌دار، SRS و داده، تجربهٔ تمرین، startup، Baseline Profile، AI boundary، CI، release safety، IA، copy، دسترس‌پذیری و هویت بصری  
**وضعیت کار:** `active` — foundation، مسیر روزانه، Journey و نخستین Campaign شاخه‌دار اثبات شده‌اند؛ بازسازی کامل سوپراپ هنوز پایان نیافته است.  
**حکم انتشار عمومی:** **BLOCK** تا رفع چهار گیت بیرونی/محتوایی در انتهای گزارش.

> **پیوستگی هویت:** کاربر کانسپت را قبلاً انتخاب کرده است. نام یا شمارهٔ canonical آن در state و artifactهای قابل‌بازیابی این task ثبت نشده؛ این نقص ثبت تصمیم است، نه نبود تصمیم کاربر. انتخاب دوباره درخواست نمی‌شود. فقط کارهای هویت‌محور مانند mascot نهایی، badge art، palette نهایی و سنجش pixel-fidelity تا بازیابی همان شناسه متوقف‌اند؛ توسعهٔ مستقل محصول ادامه دارد.

## حکم مدیریتی

Cinnamon دیگر صرفاً یک پوستهٔ مرتب با XP نمایشی نیست. مسیر واقعی امروز از دادهٔ SRS شروع می‌شود، Quest متناسب با evidence موجود می‌سازد، مرورهای معتبر را در Room ثبت می‌کند، XP و پیشرفت را در یک transaction تسویه می‌کند، یک receipt قابل‌مصرف به UI می‌دهد و claim را فقط یک بار می‌پذیرد. روی همین foundation، `Foundation Expedition` چهار‌فصلی و `Foundation Route Choice` شاخه‌دار از Room تغذیه می‌شوند. کاربر پس از فصل نخست بین `Precision Trail` و `Momentum Circuit` انتخاب می‌کند؛ انتخاب در Room v4 اتمیک و تغییرناپذیر است، هیچ XP نمی‌دهد و فقط Journey همان route را می‌سازد. Home و Questboard صرفاً projection state ذخیره‌شده‌اند. مسیر روزانه، Journey و Campaign روی APK نصب‌شده و دادهٔ ارتقایافته اجرا و با screenshot، UI XML و snapshot دیتابیس ثبت شده‌اند.

با این حال، «پرفکشن کامل» یا «آمادگی انتشار» هنوز ادعا نمی‌شود. عمق فنی reward loop، Journey v1 و Campaign route v1 اکنون مناسب یک هستهٔ جدی است، اما packهای چند‌هفته‌ای و episodic، collection، personalization، مقیاس محتوایی، identity منتخب، backend عملیاتی، proof امضای release و تأیید تحریری/بالینی هنوز تکمیل نشده‌اند.

## پاسخ دقیق به نگرانی مقیاس و تعداد خطوط

شمارش تکرارپذیر فعلی با حذف `build/`، `.gradle/`، `output/`، `tmp/`، `qa/` و cacheها انجام شد. برای جلوگیری از بزرگ‌نمایی، schemaهای تولیدشدهٔ Room، مستندات و Baseline Profile جدا گزارش می‌شوند:

| بخش | فایل | خط فیزیکی | خط غیرخالی |
|---|---:|---:|---:|
| Kotlin/Java و build logic | ۹۷ | ۲۳٬۹۳۶ | ۲۲٬۲۳۸ |
| script، config، resource و asset داده‌ایِ تألیفی | ۳۳ | ۹٬۲۹۳ | ۹٬۰۶۶ |
| **تألیفی؛ بدون schemaهای generated Room** | **۱۳۰** | **۳۳٬۲۲۹** | **۳۱٬۳۰۴** |
| schemaهای generated Room | ۳ | ۵٬۱۱۴ | ۵٬۱۱۴ |
| **جمع مهندسی و داده** | **۱۳۳** | **۳۸٬۳۴۳** | **۳۶٬۴۱۸** |
| مستندات Markdown | ۳۶ | ۱٬۸۳۰ | ۱٬۳۹۲ |
| **جمع مهندسی، داده و مستندات** | **۱۶۹** | **۴۰٬۱۷۳** | **۳۷٬۸۱۰** |
| Baseline Profile متمرکز؛ جدا از جمع بالا | ۱ | ۲٬۶۴۵ | ۲٬۶۴۵ |

بنابراین حتی با حذف schemaهای generated، مهندسی و دادهٔ تألیفی به ۳۳٬۲۲۹ خط رسیده است و Kotlin/build logic به‌تنهایی ۲۳٬۹۳۶ خط فیزیکی دارد. جمع متن فنی با schema، Markdown و Baseline Profile برابر ۴۲٬۸۱۸ خط فیزیکی است؛ این عدد عمداً «کد خالص» نامیده نمی‌شود. برداشت «کل پروژه ۳هزار خط است» با filesystem هم‌خوان نیست. در عین حال، عدد ۳۰هزار هدف مصنوعی نیست: یک سوپراپ با تعداد صفحه ساخته نمی‌شود؛ هر capability باید state machine، persistence، failure path، accessibility، امنیت و runtime proof داشته باشد.

## وضعیت واقعی در یک نگاه

| حوزه | وضعیت | شاهد / مرز |
|---|---|---|
| Room reward ledger | **پیاده‌سازی و اثبات‌شده** | event، transaction، balance، summary و receipt منبع حقیقت‌اند |
| Quest و Achievement settlement | **اتمیک و پایدار** | evidence، progress، completion، unlock، XP و claim در transaction؛ duplicate/replay ایمن |
| Mission Pulse در Home | **runtime proof** | unassigned، active، complete و claimed از state واقعی؛ بدون checkbox جعلی |
| Journey v1 | **پیاده‌سازی و runtime proof** | Room v3، چهار فصل ترتیبی، XP اتمیک، recovery واقعی و نقشهٔ persisted |
| Campaign route v1 | **پیاده‌سازی و runtime proof** | Room v4، دو route، انتخاب اتمیک بدون XP، save/restore، recovery و cold-relaunch guard |
| بازی‌ها و مرور | **پاداش معنایی** | فقط completion معتبر event می‌سازد؛ `Again` و replay کور پاداش تازه نمی‌سازند |
| feedback و دسترس‌پذیری | **پیاده‌سازی** | Sound، Haptic و Reduce motion پایدار؛ کنترل‌ها حداقل ۴۸dp و semantics معنی‌دار |
| startup | **اندازه‌گیری تکرارپذیر** | stage timing، reconciliation و overlap در harness ثبت می‌شوند |
| Baseline Profile | **ساخته و بسته‌بندی‌شده** | ۲٬۶۴۵ rule متمرکز؛ `baseline.prof` و `baseline.profm` داخل benchmark APK |
| AI / secret boundary | **در کد fail-closed؛ زیرساخت بیرونی باز** | Android فقط gateway HTTPS را می‌پذیرد؛ provider secret در APK نیست |
| lexicon و provenance | **ساختاری معتبر؛ محتوایی مشروط** | ۸ فایل، ۷۰۱ item، ۲۴۳ entry؛ review status هنوز تأیید انتشار نیست |
| هویت بصری منتخب | **تصمیم موجود؛ شناسهٔ canonical گمشده** | انتخاب دوباره ممنوع؛ کار مستقل ادامه دارد، identity-bearing work منتظر بازیابی مرجع است |
| انتشار عمومی | **BLOCK** | credential، gateway/auth، signing/update و editorial/clinical approval بازند |

## ۱. اقتصاد پاداش: از نمایش به authority واقعی

### قرارداد تسویه

```text
عمل یادگیری قابل‌اثبات
  → LearningEvent با occurrence key پایدار
  → قانون پاداش نسخه‌دار
  → transaction واحد Room
       event + reward transaction + balance + daily summary
       quest progress/completion + achievement unlock + catalog XP
       journey initialization/progress/completion + milestone XP
       campaign route choice + selected route initialization
       presentation receipt
  → projection فقط‌خواندنی برای UI
  → claim یک‌باره با uniqueness و replay guard
```

### چیزهایی که اکنون enforce می‌شوند

- XP از tap، toggle، slider، بازکردن صفحه، animation یا banner صادر نمی‌شود.
- collision شناسه یا payload متناقض کل transaction را rollback می‌کند.
- unlock تکراری achievement یا settlement تکراری catalog، XP دوباره تولید نمی‌کند.
- daily activity cap برای eventهای farmable اعمال می‌شود؛ XP کاتالوگِ واقعاً unlocked با آن اشتباه گرفته نمی‌شود.
- XP فصل Journey مانند milestone کاتالوگ farmable نیست و از cap فعالیت روزانه مستثنا می‌ماند.
- انتخاب Campaign فقط با event اختصاصی انجام می‌شود، با هیچ reward/settlement دیگری share نمی‌شود و حتی با evidence تاریخی واجدشرایط، صفر XP می‌دهد.
- هر campaign version برای هر actor حداکثر یک route دارد؛ انتخاب route متناقض کل event را rollback می‌کند.
- prerequisite با شناسه و **نسخهٔ دقیق** Journey سنجیده می‌شود؛ Journey نسخهٔ دیگر قفل را اشتباهی باز نمی‌کند.
- فقط Journey route انتخاب‌شده ساخته می‌شود؛ route انتخاب‌نشده row، progress یا transaction ندارد.
- stage قفل‌شده evidence زودرس را مصرف نمی‌کند و هر event حداکثر یک stage را کامل می‌کند.
- Quest مرور روزانه از تعداد itemهای due و شناسهٔ distinct review evidence استفاده می‌کند.
- `Again` به‌عنوان مرور موفق، progress یا streak جعل نمی‌شود.
- mastery از evidence واقعی repetition و interval می‌آید.
- claim دستی فقط پس از completion معتبر و فقط یک بار پذیرفته می‌شود.
- UI receipt را نمایش می‌دهد؛ UI حق نوشتن مستقیم موجودی XP ندارد.

### پوشش آزمون settlement

آزمون‌های integration سناریوهای unlock اتمیک، rollback collision، عدم پاداش تکراری، evidence متمایز، claim یک‌باره، replay و forged claim را پوشش می‌دهند. پنج سناریوی Journey ترتیب stageها، نادیده‌گرفتن evidence زودرس، تکمیل کامل چهار فصل، معافیت milestone از cap و rollback برخورد transaction را می‌آزمایند. چهار سناریوی Campaign نیز prerequisite rollback، zero-XP commitment با evidence تاریخی، replay/conflict guard و materialize نشدن route انتخاب‌نشده را می‌سنجند. planner و projection جداگانه stable ID، version binding و حالت‌های Locked/Choose/Ready/Recovery را پوشش می‌دهند.

## ۲. Mission Pulse: حلقهٔ روزانهٔ واقعی و انگیزه‌بخش

کارت مأموریت بین هدف روزانه و محتوای Home قرار گرفته و چهار حالت واقعی دارد:

1. **Unassigned:** اگر deck شواهد کافی ندارد، مأموریت جعلی نشان داده نمی‌شود؛ CTA کاربر را به افزودن واژه هدایت می‌کند.
2. **Active:** عنوان، توضیح، XP، progress و CTA از Quest ذخیره‌شده می‌آیند.
3. **Complete:** پس از سه مرور distinct، `3 / 3 verified` و دکمهٔ `Claim +10 XP` ظاهر می‌شود.
4. **Claimed:** کارت `Quest cleared` و `Reward claimed` را نشان می‌دهد و دیگر clickable نیست.

### proof روی APK نهایی

| مرحله | state مشاهده‌شده | artifact |
|---|---|---|
| مأموریت فعال | `Review three that are ready`، `0 / 3 verified`، `+10 XP` | `output/cinnamon-runtime-final-quest-assigned-20260718.png` |
| جلسهٔ مرور | سه ثبت معتبر؛ شمارندهٔ session برابر `3 / 16` | `output/cinnamon-runtime-final-review-dismissed-20260718.xml` |
| آمادهٔ دریافت | `24 / 80 XP`، `Daily quest complete`، `3 / 3 verified` | `output/cinnamon-runtime-final-quest-complete-20260718.png` |
| دریافت‌شده | `34 / 80 XP`، `Quest cleared`، `Reward claimed` | `output/cinnamon-runtime-final-quest-claimed-20260718.png` |
| replay guard | لمس دوباره؛ XP همچنان `34 / 80` و بدون receipt تازه | `output/cinnamon-runtime-final-quest-replay-guard-20260718.png` |

این proof روی همان debug APK با SHA-256 ثبت‌شده در بخش verification انجام شده؛ screenshot جای آزمون دیتابیس را نمی‌گیرد و آزمون دیتابیس نیز جای runtime proof را نگرفته است.

## ۳. Journey v1: از مأموریت روزانه تا مسیر ماندگار

`Foundation Expedition` یک تعریف نسخه‌دار با شناسهٔ `journey.foundation.expedition` و چهار فصل ترتیبی است:

| فصل | evidence واقعی | هدف | پاداش |
|---|---|---:|---:|
| Spark the memory trail | واژه‌های distinct مرورشده | ۳ | ۱۰ XP |
| Cross-train your recall | قالب‌های distinct تمرین | ۲ | ۲۰ XP |
| Lock in a lasting mastery | mastery پس از مرور delayed | ۱ | ۲۰ XP |
| Build a three-day rhythm | روزهای distinct فعالیت معنادار | ۳ | ۳۰ XP |

تعریف Journey از UI مستقل است. `journey_instances` و `journey_stage_progress` در migration افزایشی v2→v3 ساخته می‌شوند؛ DAO در همان transaction که event، reward transaction، balance، summary و receipt را ثبت می‌کند، stage را تکمیل و stage بعدی را فعال می‌کند. UI اجازه ندارد فصل جعلی بسازد: اگر rowها ناقص یا ناسازگار باشند، حالت recovery با Retry واقعی نشان داده می‌شود.

### proof ارتقای واقعی و replay guard

APK با `adb install -r` روی دادهٔ موجود نصب شد؛ app data reset نشد. نخستین اجرا یک defect ارتقا را آشکار کرد: bootstrap اولیهٔ Journey به catalog reconciliation تکراری متصل بود و Home به recovery می‌رفت. bootstrap به `reconcileJourney()` مستقل با namespace idempotency نسخه‌دار منتقل شد، APK دوباره ساخته و روی همان داده نصب شد. نتیجهٔ نهایی:

| شاهد | مقدار مشاهده‌شده |
|---|---|
| schema | `PRAGMA user_version = 3`؛ دو جدول Journey حاضر |
| instance | `active`، `currentStageOrder = 2`، definition version `1` |
| stageها | فصل ۱ `completed 3/3`؛ فصل ۲ `active 0/2`؛ فصل‌های ۳ و ۴ `locked` |
| settlement | دقیقاً یک `journey_reconciled` و یک transaction با rule فصل اول |
| balance | `34 → 44 XP`؛ پاداش Journey دقیقاً `+10 XP` |
| cold relaunch | event count = ۱، transaction count = ۱، balance = ۴۴؛ پاداش تکرار نشد |

artifactهای runtime:

- Home Journey Pulse: `output/cinnamon-runtime-journey-home-active-v3-20260718.png`
- Journey Map کامل: `output/cinnamon-runtime-journey-map-v3-20260718.png`
- cold-relaunch replay guard: `output/cinnamon-runtime-journey-replay-guard-v3-20260718.png`
- snapshot دیتابیس همراه WAL: `tmp/cinnamon-runtime-v3.db` و `tmp/cinnamon-runtime-v3-replay.db`

این UI عمداً خشک نیست: کارت Home گرادیان amber→green، rail چهار checkpoint، chapter فعال با glow سازگار با Reduce motion، XP tag و CTA زنده دارد. Questboard نیز summary، فصل‌های secured/active/locked و progress واقعی را به‌صورت یک نقشهٔ پیوسته نمایش می‌دهد.

## ۴. Campaign route v1: انتخاب معنادار، نه دکمهٔ تزئینی

پس از secure شدن `stage.memory-spark` در نسخهٔ دقیق `journey.foundation.expedition@1`، کاربر بین دو route هم‌ارزش اما رفتاری متفاوت انتخاب می‌کند:

| route | فصل ۱ | فصل ۲ | فصل ۳ | ارزش authored |
|---|---|---|---|---:|
| Precision Trail | ۳ قالب تمرین distinct | ۲ mastery با تأخیر | ۵ روز فعالیت معنادار | ۱۱۰ XP |
| Momentum Circuit | ۸ واژهٔ مرورشدهٔ distinct | ۴ قالب تمرین distinct | ۵ روز فعالیت معنادار | ۱۱۰ XP |

هر route یک `JourneyDefinition` مستقل و نسخه‌دار دارد. انتخاب با جدول `campaign_route_choices` در migration افزایشی v3→v4 ذخیره می‌شود. unique index روی actor + campaign definition + version اجازهٔ انتخاب دوم نمی‌دهد. event، choice، Journey instance و سه stage انتخاب‌شده در یک transaction نوشته می‌شوند؛ اگر prerequisite ناقص، route متناقض یا collision موجود باشد، همه‌چیز rollback می‌شود.

### اصل ضدتقلب انتخاب

خود انتخاب route پاداش نیست. DAO فقط event نوع `campaign_route_selected` را می‌پذیرد، transaction/receipt/catalog settlement هم‌زمان را ممنوع می‌کند و Journey route را با `settleEvidenceOnThisEvent=false` فقط initialize می‌کند. در تست integration حتی سه قالب تمرین تاریخیِ کافی برای فصل اول Precision از قبل ثبت شد؛ انتخاب route همچنان summary صفر، transaction صفر و balance بدون تغییر داشت.

### state و UI واقعی

- **Locked:** تا فصل prerequisite دقیق تکمیل نشده باشد؛ routeها قابل‌انتخاب نیستند.
- **Choose:** دو کارت با جهت بصری amber/precision و cyan/momentum، ۱۱۰ XP کل، توضیح trade-off و CTA حداقل ۴۸dp.
- **Confirm:** دیالوگ صریح می‌گوید انتخاب در این campaign version قابل‌تعویض نیست و هیچ XP نمی‌دهد.
- **Ready:** route ذخیره‌شده، `0/3` تا `3/3`، XP secured و سه فصل persisted با active/locked/completed واقعی.
- **Unavailable:** row ناقص، چند choice یا mismatch نسخه/route progress جعل نمی‌کند؛ recovery copy داده را دست‌نخورده اعلام می‌کند.

### proof ارتقای v3→v4، commitment و cold relaunch

snapshot واقعی Journey v3 با `PRAGMA user_version=3`، balance برابر ۴۴، فصل ۱ complete و فصل ۲ active داخل APK قدیمی بازگردانده شد. APK تازه با `adb install -r` روی همان app data نصب شد و بدون reset نتیجهٔ زیر ثبت شد:

| شاهد | مقدار مشاهده‌شده |
|---|---|
| migration | `user_version: 3 → 4`؛ جدول و سه index Campaign افزوده شد |
| حفظ داده | Foundation همچنان فصل ۲ active؛ balance همچنان ۴۴ XP؛ یک Journey reward قبلی حفظ شد |
| قبل از انتخاب | choice count = ۰؛ route Journey count = ۰؛ event جعلی ساخته نشد |
| commitment | دقیقاً یک `route.precision-trail@1` و یک Journey با سه stage `active/locked/locked` |
| انتخاب بدون پاداش | summary XP = ۰؛ transaction count = ۰؛ route reward count = ۰؛ balance = ۴۴ |
| cold relaunch | choice = ۱، event = ۱، route instance = ۱، stage = ۳، balance = ۴۴ |

artifactهای runtime:

- انتخاب کامل دو route: `output/cinnamon-runtime-v4-campaign-choice-20260719.png`
- هشدار commitment: `output/cinnamon-runtime-v4-campaign-confirm-20260719.png`
- route فعال پس از commit: `output/cinnamon-runtime-v4-campaign-active-20260719.png`
- route بازیابی‌شده پس از cold relaunch: `output/cinnamon-runtime-v4-campaign-cold-restored-20260719.png`
- snapshot ارتقای v4: `tmp/cinnamon-runtime-v4-upgrade.db`
- snapshot commitment و replay: `tmp/cinnamon-runtime-v4-campaign-active.db` و `tmp/cinnamon-runtime-v4-campaign-relaunch.db`

## ۵. کاتالوگ گیمیفیکیشن

کاتالوگ فعلی شامل ۴ rarity، ۱۵ reward، ۵ presentation، ۶ progression level، ۱۲ achievement و ۱۲ quest است. `CatalogSettlementPlanner` و `CatalogProgressProjector` دو مسئولیت جدا دارند:

- projector وضعیت قابل‌خواندن را فقط از evidence پشتیبانی‌شده می‌سازد؛ metric ناشناخته progress جعلی نمایش نمی‌دهد.
- planner تصمیم settlement را با threshold و شناسه‌های پایدار می‌سازد؛ DAO آن را اتمیک اعمال می‌کند.
- مرز هفته ISO و مستقل از timezone محاسبه می‌شود.
- reconciliation هنگام startup فقط eventهای اعمال‌نشده را می‌بیند و مسیر fast-path از بازکردن snapshot تکراری جلوگیری می‌کند.

## ۶. copy، حس محصول و دسترس‌پذیری

خشکی تجربه فقط با رنگ و confetti حل نمی‌شود. متن و feedback باید کاربر را در یک loop کوتاه، روشن و محترمانه نگه دارند:

- پیام‌ها مشخص می‌کنند چه عمل معتبری ثبت شده و چند XP اضافه شده است.
- banner پاداش dismissible است و animation آن authority پاداش نیست.
- success، retry و error از هم متمایزند؛ ambiguity به retry کور تبدیل نمی‌شود.
- متن‌های حساس پیشین با brief و بازبینی ثانویهٔ AvalAI ثبت شده‌اند. برای copy تازهٔ Journey، دو تلاش ثانویه به‌دلیل خطای SSL و سپس DNS شکست خورد. برای Campaign نیز prompt مستقل در `qa/agent-notes/campaign-route-copy-review-prompt.txt` ثبت شد، اما `minimax-m3` پس از ۸۶٫۵ ثانیه با SSL unexpected EOF شکست خورد. هر دو مجموعه evidence-first به‌صورت محلی بازبینی شدند؛ review خارجیِ انجام‌نشده پنهان نمی‌شود.
- `Spoken Rhythm Practice`، authored tone comparison و handoff drill جای ادعای AI/clinical ساختگی را گرفته‌اند.
- waveform بالینی صریحاً abstract و `NOT AN ECG` است.
- Home ماژول‌ها را با کارکرد واقعی معرفی می‌کند؛ `AI Patient` یا simulation دروغین نمایش داده نمی‌شود.
- Sound effects، Haptic feedback و Reduce motion در DataStore پایدارند و در نقطهٔ اجرا gate می‌شوند.
- press scale، pulse، waveform، confetti، shimmer، typewriter و transitionهای بررسی‌شده در Reduce motion خاموش یا ایستا می‌شوند.

## ۷. startup و Baseline Profile

### معماری startup

- `CinnamonApp` یک scope تحت مالکیت Application دارد.
- `MainActivity` آغاز کار سنگین را تا دو frame بعد از نمایش loading واقعی عقب می‌اندازد.
- import قدیمی، catalog reconciliation و Journey reconciliation هرکدام event ID و namespace مستقل و مسیر fast-path دارند.
- parsing مستقل catalog با مسیر سریال import/seed overlap می‌کند.
- timing فقط با log tag تشخیصی opt-in فعال می‌شود و payload کاربر را log نمی‌کند.

### مقایسهٔ هم‌ساخت روی یک emulator

هر دو measurement روی همان non-debuggable، non-minified benchmark APK و `Codex_API35` با پنج trace موفق انجام شده‌اند:

| سنجه | compilation `none` p50 / p95 | focused profile p50 / p95 | تغییر p50 |
|---|---:|---:|---:|
| `am TotalTime` | ۲۳۰۱ / ۲۴۶۸ ms | ۲۱۲۷ / ۲۲۲۶ ms | حدود **۷٫۶٪ بهتر** |
| startup `prepare` | ۷۴۸ / ۸۵۱ ms | ۶۵۱ / ۷۹۵ ms | حدود **۱۳٪ بهتر** |
| reconciliation | ۲۴ / ۳۷ ms | ۳۱ / ۴۰ ms | در محدودهٔ noise؛ ادعای بهبود ندارد |
| overlap | ۲۰۸ / ۳۲۷ ms | ۱۷۷ / ۲۶۹ ms | فقط diagnostic |

artifactها:

- بدون compilation: `output/performance/startup-20260718-214213.json`
- focused `speed-profile`: `output/performance/startup-20260718-214111.json`
- full collected profile: `output/performance/baseline-profile-full-20260718-213744.txt`
- focused source: `app/src/main/baseline-prof.txt` با ۲٬۶۴۵ rule متعلق به `com/cinnamon/app`

benchmark APK نهایی شامل `assets/dexopt/baseline.prof` با ۱۲٬۳۱۴ byte و `baseline.profm` با ۶۳۷ byte است. verifier مستقل source را از نظر count، duplicate و rule خارجی بررسی می‌کند و CI نیز packaging این دو entry را می‌سنجد.

این روش با راهنمای رسمی [Android Baseline Profiles](https://developer.android.com/topic/performance/baselineprofiles/overview) و [manual collection](https://developer.android.com/topic/performance/baselineprofiles/manually-create-measure) هم‌راستاست. عددها فقط **benchmark emulator evidence** هستند؛ اثبات release روی دستگاه فیزیکی هدف نیستند.

## ۸. داده، SRS و محتوای محصول

| دارایی | وضعیت ثبت‌شده |
|---|---|
| lexicon | ۸ فایل، ۷۰۱ item، ۲۴۳ entry |
| provenance | `origin=project-bundled-curated` |
| review status | `editorial-review-required-before-release` |
| SRS evidence | due item، distinct review، mastery، study-day/week و breadth queryهای واقعی |
| gamification | ۴ rarity، ۱۵ reward، ۵ presentation، ۶ level، ۱۲ achievement، ۱۲ quest |
| Journey | یک definition نسخه‌دار، چهار stage ترتیبی، ۸۰ XP authored |
| Campaign | یک definition نسخه‌دار، دو route و شش stage authored؛ هر route برابر ۱۱۰ XP |
| previewهای برند | ۱۰ Mock Preview فقط در docs؛ داخل APK نیستند |

validator hash، ساختار، origin و review status را بررسی می‌کند. این به معنی تأیید علمی، تحریری یا بالینی corpus نیست؛ approval باید فرایند انسانی و ثبت‌شده داشته باشد.

## ۹. AI، backend و مرز اعتماد

Android فقط با gateway متعلق به محصول صحبت می‌کند و provider key، model secret یا token ثابت نباید وارد APK شود. failureهای gateway به دسته‌های مشخص 400/401/403/409/429/5xx، request ID، `Retry-After` و delivery state تفکیک شده‌اند:

- `NotSent`: درخواست ارسال نشده؛ retry معمولاً ایمن‌تر است.
- `ResponseReceived`: پاسخ خطای معتبر دریافت شده است.
- `UnknownAfterSend`: وضعیت delivery نامطمئن است؛ UI نباید retry کور یا claim قطعی بسازد.

در نبود session معتبر، محصول fail-closed می‌ماند و guided practice محلی را نشان می‌دهد. این طراحی امن، جای backend production، issuer، rate limit، abuse controls و observability را نمی‌گیرد.

## ۱۰. شواهد فنی checkpoint نهایی

| بررسی | نتیجه | شاهد / مرز |
|---|---|---|
| full JVM suite | **passed** | ۱۰۴ test در ۲۶ suite؛ ۰ failure، ۰ error، ۰ skip |
| settlement integration | **passed** | atomic unlock، collision rollback، duplicate، claim/replay/forgery، پنج سناریوی Journey و چهار سناریوی Campaign |
| Campaign projection | **passed** | Locked/Choose/Ready/Recovery، prerequisite version binding و missing/ambiguous persistence |
| `connectedDebugAndroidTest` | **passed** | ۳/۳ روی `Codex_API35`، Android 15 |
| lintDebug | **passed** | ۰ error و ۳۰ warning اطلاعاتی/version-related |
| `assembleDebug` | **passed** | APK برابر ۲۱٬۸۲۵٬۳۷۵ byte |
| debug APK digest | **ثبت شد** | `58EA0223E14B2B2FF01CBD301A9A7C2298872E51A3A4BDA7FB87962C4FF95A6F` |
| benchmark APK | **ساخته شد** | ۱۴٬۴۶۵٬۷۵۴ byte؛ SHA-256: `7B6C074D11DC4B85E3E76D2934BF22BF46BC8D9D2F938580B2ED8CCDA1359504` |
| Baseline Profile source | **passed** | ۲٬۶۴۵ rule متمرکز؛ ۰ rule خارجی و ۰ duplicate |
| Baseline Profile packaging | **passed** | `baseline.prof` = ۱۲٬۳۱۴ byte و `baseline.profm` = ۶۳۷ byte داخل benchmark APK |
| Mission runtime | **passed** | assign → ۳ review → complete → claim → replay guard |
| Journey runtime upgrade | **passed** | v2→v3 روی دادهٔ محفوظ؛ فصل ۱ complete، فصل ۲ active، `34→44 XP` و cold-relaunch بدون duplicate |
| Campaign runtime upgrade | **passed** | v3→v4 با دادهٔ محفوظ؛ choice و route persisted، صفر XP، balance=44 و cold-relaunch بدون duplicate |
| secret boundary | **passed در HEAD** | verifier source/APK؛ historical credential remediation همچنان بیرونی |
| Room migration | **passed** | v1→v2، v2→v3، v3→v4 و chain v1→v4؛ ۴۳ statement افزایشی، schema جاری v4 |
| lexicon contracts | **passed ساختاری** | provenance/status معتبر؛ کیفیت محتوا هنوز گیت بیرونی |
| گزارش RTL PDF | **passed** | ۱۲ صفحه A4 Tagged؛ رندر Poppler و inspection همهٔ صفحه‌ها؛ defect عنوان صفحهٔ ۱۱ اصلاح و بازبینی شد |

فرمان full checkpoint:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleBenchmark --no-daemon --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon --console=plain
python scripts/verify_baseline_profile.py app/src/main/baseline-prof.txt --apk app/build/outputs/apk/benchmark/app-benchmark.apk
python scripts/verify_room_migration.py
python scripts/verify_lexicon_assets.py
python scripts/verify_mobile_secret_boundary.py
python scripts/verify_release_signing_contract.py
```

## ۱۱. هویت منتخب و آرشیو previewها

ده preview ImageGen در `docs/codex/.../assets/brand-concepts-v1/` آرشیو تصمیم هستند، نه فرم انتخاب تازه و نه proof implementation. جهت قبلاً انتخاب شده، ولی identifier آن در artifactهای قابل‌بازیابی موجود نیست.

تا بازیابی همان مرجع:

- هیچ انتخاب جدید، blend خودسرانه یا حدس سلیقه انجام نمی‌شود.
- foundation فنی، حلقه‌های بازی، accessibility، reward truth و performance ادامه دارد.
- فقط mascot family، palette و badge art نهایی، icon identity، world object و screenshot-diff هویت‌محور متوقف‌اند.
- نخستین کار پس از بازیابی، ثبت identifier در state، preservation contract و asset manifest است تا continuity دوباره از دست نرود.

## ۱۲. چهار گیت قطعی انتشار

### P0 — credential remediation

credentialهای تاریخی در commitهای قبلی باید revoke/rotate شوند و history، cache، clone و artifactهای منتشرشده بررسی و پاک‌سازی شوند. حذف secret از HEAD به‌تنهایی remediation نیست.

### P0 — gateway و auth production

gateway عملیاتی، issuer/session کوتاه‌عمر، rate limiting، abuse control، secret manager و observability امن باید خارج از APK deploy و اثبات شوند.

### P0 — signing lineage و install-update proof

release keystore continuity، package identity، channel، versioning و install/update واقعی با artifact امضاشده و قابل‌ردیابی لازم‌اند. debug/benchmark APK proof انتشار نیست.

### P0 — بازبینی تحریری/بالینی corpus

تا وقتی ۸ فایل، ۷۰۱ item و ۲۴۳ entry توسط فرایند مسئول و مستند تأیید نشده‌اند، manifest نباید به وضعیت approved ارتقا یابد.

## ۱۳. نقشهٔ رسیدن به سوپراپ، بدون بادکردن مصنوعی کد

| موج | کار | چرا ارزش سوپراپی می‌سازد | معیار پذیرش |
|---|---|---|---|
| P0 بیرونی | چهار گیت انتشار | اعتماد، امنیت و تداوم واقعی | evidence مستقل، قابل‌ردیابی و release-signed |
| P1 product | Campaign pack چند‌هفته‌ای روی route engine v1 | بازگشت بلندمدت فراتر از نخستین انتخاب دوشاخه | episode manifest، زمان‌بندی، پایان معنی‌دار، save/restore و migration نسخهٔ بعد |
| P1 gamification | collection و cosmetic economy غیرقابل‌خرید با تقلب | مالکیت و انگیزه بدون pay-to-win | ledger-backed unlock، sink/source budget، replay test |
| P1 learning | برنامهٔ شخصی از weakness و SRS | هر روز انتخاب معنادار، نه feed تصادفی | explainable recommendation و opt-out |
| P1 content | بسته‌های تخصصی و مسیرهای سطح‌بندی‌شده | عمق واقعی library | provenance، editorial approval و migration-safe manifest |
| P1 identity | پیاده‌سازی کانسپت منتخب قبلی | جهان برند، mascot و حافظهٔ عاطفی | canonical ID، assets project-bound، semantics و screenshot diff |
| P1 accessibility | keyboard/screen-reader/contrast audit کامل | قابل‌استفاده برای طیف واقعی کاربر | automated + manual matrix و Reduce motion proof |
| P2 performance | Macrobenchmark روی دستگاه هدف | تجربهٔ واقعی cold/warm و jank | p50/p95، frame timing، thermal و low-memory run |
| P2 backend | sync، conflict resolution و backup امن | چنددستگاه و تداوم داده | offline-first contract، encryption و recovery drill |
| P2 social | challengeهای consent-based و ضدتقلب | انگیزش اجتماعی بدون leaderboard جعلی | server authority، privacy، moderation و abuse tests |

اصل توسعه: هر capability تازه فقط وقتی «اضافه‌شده» محسوب می‌شود که data contract، state machine، persistence، failure/retry، accessibility، telemetry امن و proof واقعی داشته باشد.

## محدودیت‌های صادقانه

- این checkpoint پایان rebuild یا ادعای «سوپراپ کامل» نیست.
- Campaign route v1 یک branch واقعی و persisted است، اما هنوز pack چند‌هفته‌ای، episodeهای محتوایی و پایان روایی ندارد.
- backend، auth issuer و gateway production در این checkout وجود ندارند.
- release artifact امضاشده و install-update proof موجود نیست.
- هویت منتخب وجود دارد، ولی identifier canonical آن در state قابل‌بازیابی نیست.
- previewها داخل APK نیستند و proof fidelity محسوب نمی‌شوند.
- performance روی emulator benchmark اندازه‌گیری شده، نه دستگاه فیزیکی release target.
- validator ساختاری، کیفیت علمی/تحریری corpus را تضمین نمی‌کند.
- ۳۰ warning lint مانع build نیستند، اما باید در چرخهٔ dependency/quality بعدی دسته‌بندی و کاهش یابند.

## تصمیم عملی این checkpoint

settlement اتمیک Quest/Achievement، Mission Pulse، Journey v1، Campaign route v1، claim یک‌باره، replay guard، migration v4، Baseline Profile و CI verifier دیگر گیت‌های باز داخلی این موج نیستند؛ هم در تست و هم در runtime شواهد دارند. توسعه باید route engine را به campaign pack چند‌هفته‌ای و episodic گسترش دهد و collection، personalization، accessibility و زیرساخت release را عمیق کند. کار identity-bearing فقط تا بازیابی **همان انتخاب قبلی** متوقف می‌ماند و انتخاب دوباره درخواست نمی‌شود. انتشار عمومی تا رفع چهار گیت P0 همچنان **BLOCK** است.

</div>
