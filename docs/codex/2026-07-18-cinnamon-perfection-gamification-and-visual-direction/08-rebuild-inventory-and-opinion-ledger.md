# موجودی بازسازی و دفتر رأی تجربه

## محصول و مرز

- وعدهٔ محصول: تمرین انگلیسی پزشکی و fluency با پیشرفت قابل‌اثبات، نه یک بازیِ امتیازمحورِ نمایشی.
- مخاطب: یادگیرنده‌ای که به تمرین کوتاه، بازخورد قابل اعتماد، واژگان بالینی و حس پیشرفت نیاز دارد.
- مرز بازسازی: پوسته، مسیرها، hierarchy، assetها، motion و stateهای رابط Android. Room، DataStore، catalog، seed، API contract و signing در این مرحله بازنویسی نمی‌شوند.

## موجودی عملکرد و وضعیت‌ها

| سطح | کار اصلی | داده/قرارداد | حالت‌های لازم |
| --- | --- | --- | --- |
| Home | دیدن next action، review و moduleها | startup state، progress و lexicon | آماده‌سازی، خطا با retry، empty، ready |
| Lexicon | جست‌وجو/مشاهدهٔ entry | Room lexicon و `entry/{id}` | loading، empty، missing ID، detail |
| Practice | انتخاب فعالیت و completion | Room ledger و progress | شروع، paused، completed، replay بدون XP، error/retry |
| Games | حل تمرین واقعی و دیدن رسید | event + transaction + receipt | پاسخ نادرست، focus، success، duplicate/replay، cap/error |
| Fluency/clinical | تمرین راهنمای زبان | scenario، offline guide و gateway boundary | not configured، not authenticated، delivery unconfirmed، local guide |
| Profile | فهم progression و preference | Room projection + DataStore | empty/new learner، milestone، long history، settings |
| Shell | حرکت بین چهار job اصلی | Navigation Compose | selected، back، state restoration، narrow/medium/expanded |

## دفتر رأی پنج‌گانهٔ سطح‌ها

| سطح | رأی | دلیل و عملکرد حفظ‌شده | implication اجرای بعدی | اثبات لازم |
| --- | --- | --- | --- | --- |
| مدل داده و ledger | نگه‌دار | قرارداد پاداش واقعی و migration v1→v2 منبع حقیقت است | UI فقط از repository/view model می‌خواند | unit + Room integration + runtime receipt |
| seed و catalog | نگه‌دار | manifest، hash و ID پایدار دارد | asset/copy جدید به‌صورت versioned اضافه می‌شود | validator + clean install |
| Home | بازطراحی | شروع کار اکنون کاربردی اما جهان برند و next-action storytelling ضعیف است | Today به یک stage زنده با action اصلی تبدیل شود | preview approved + startup/error screenshots |
| Navigation shell | بازطراحی | چهار job درست‌اند، اما bottom bar و hierarchy هنوز generic هستند | Today / Library / Play / Journey با mapping افزایشی | route parity + narrow/medium screenshot |
| Lexicon و Entry | بهبود | محتوای حقیقی و IDها درست‌اند، presentation خشک است | discovery، collection و context بدون تغییر مدل داده | long text، entry ID، search و RTL/LTR proof |
| Practice hub | بازطراحی | فعالیت‌ها معتبرند اما اولویت و انگیزه به‌اندازهٔ کافی داستان‌دار نیست | typeهای تمرین از طریق نقشه/atelier/mission نشان داده شوند | action-to-ledger trace + error/replay proof |
| Gamification hub | بازطراحی | receipt/quest authority درست است؛ brand/mascot/badge language منتخب پس از بازیابی identifier canonical قابل اجراست | reward presentation فقط view روی receipt باشد | idempotency + reduced-motion + receipt screenshot |
| Clinical / fluency copy | بهبود | مرز safety درست شده ولی زبان باید گرم‌تر و خودآگاه‌تر شود | copy truthful و non-diagnostic می‌ماند | failure/degraded screenshots و copy audit |
| Roleplay AI | بهبود | gateway-only و fail-closed درست است؛ backend خارجی هنوز موجود نیست | local guide به‌عنوان حالت degraded مشخص می‌ماند | request contract + no-sent copy proof |
| Audio / WearOS mockها | حذف | caller واقعی نداشتند و قابلیت نادرست القا می‌کردند | بازگشت فقط با contract، permission و runtime proof | manifest/lint/runtime proof |
| App icon و splash | افزودن | launcher فعلی هویت نهایی ندارد | adaptive + monochrome + splash از هویت تصویری منتخب | real launcher/splash screenshots |
| Motion و celebration | افزودن | اقتصاد reward آماده است اما Motion Bible و asset family منتخب ندارد | event-to-presentation contract، reduced-motion و semantic fallback | animatic + runtime proof |
| Release/signing | خارج از scope رابط | مالک محصول و زیرساخت بیرونی لازم است | به‌عنوان P0 بیرونی باقی می‌ماند | key rotation/auth/gateway/signing/install-update |

## قراردادهای canonical و اثرهای آن‌ها

| مفهوم | مالک canonical | مصرف‌کننده | invariant |
| --- | --- | --- | --- |
| completion یادگیری | `GamificationRepository.record` + Room transaction | games، review، hub، profile | هر رخداد meaningful فقط یک grant/receipt معتبر دارد |
| موجودی XP | `reward_balances` و transactionهای Room | Home، Profile، hub | UI نمی‌تواند XP را مستقیماً بنویسد |
| اثر نمایشی پاداش | `reward_presentation_receipts` | `UserProgressViewModel` و snackbar/celebration | نمایش نمی‌تواند grant ایجاد یا تکرار کند |
| content identity | lexicon manifest و stable ID | seed، search، entry detail، practice | entryها با ID و manifest سازگار می‌مانند |
| gateway status | `AiGatewayClient` | roleplay / offline guide | بدون session معتبر، پیام بیرون نمی‌رود و UI این را صادقانه می‌گوید |
| startup readiness | `AppStartupCoordinator` | Home و entry point | failure به loading بی‌پایان تبدیل نمی‌شود |

## مسیر منتخب برای بازسازی پس از بازیابی identifier

1. routeهای موجود نگه داشته می‌شوند و فقط shell جدید به آن‌ها map می‌شود.
2. یک world-object و شخصیت راهنما از preview منتخب به assetهای project-bound و live semantic UI تجزیه می‌شود.
3. Today، Play و Journey ابتدا با همان repository/view model موجود بازسازی می‌شوند؛ سپس Library و Profile.
4. برای هر سطح، حالت‌های loading/error/empty/replay/reduced-motion همزمان وارد زبان جدید می‌شوند.
5. حذف shell قدیمی فقط پس از screenshot-diff، route parity و rollback proof خواهد بود.
