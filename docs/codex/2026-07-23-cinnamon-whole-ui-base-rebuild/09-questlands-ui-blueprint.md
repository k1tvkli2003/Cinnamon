# Questlands UI blueprint

## تجربهٔ محوری

کاربر هر بار وارد یک **expedition base زنده** می‌شود. یک هدف اصلی روشن است؛ جهان اطراف آن شواهد پیشرفت، مسیرهای قابل‌دسترسی و یادآوری‌های کم‌فشار را نشان می‌دهد. gamification از سه لایه ساخته می‌شود:

1. `Action`: یک تمرین واقعی و کوتاه
2. `Evidence`: نتیجهٔ قابل‌ثبت در ledger
3. `Memory`: تغییر ملموس در نقشه، mascot، cabinet یا trail

بدون Evidence، Memory جدید ساخته نمی‌شود.

## shell

| compact | medium | expanded |
|---|---|---|
| bottom navigation پنج‌مقصدی | navigation rail + stage | rail + context/world pane + work pane |
| یک focus در هر viewport | دو zone مرتبط | سه zone با context پایدار |
| scene height حدود 44–52% viewport | scene و action هم‌وزن | world context حدود 30–36% width |

مقصدها:

- `Today` → legacy `home`
- `Field Atlas` → legacy `lexicon`
- `Practice` → legacy `practice`
- `Journey` → `gamification_hub`
- `Cabinet` → legacy `profile`

routeهای فعلی حذف نمی‌شوند؛ shell فقط mapping typed جدید می‌سازد.

## grid و هندسه

- base unit: 8dp
- minimum interactive target: 48dp
- compact horizontal inset: 16dp
- medium inset: 24dp
- expanded content max-width: 1280dp با margin سیال
- primary action width در compact: حداقل 72% و حداکثر 100% فضای محتوا
- radius family: 12 / 20 / 28dp؛ pill فقط برای filter/status
- elevation با outline/occlusion و tonal contrast؛ shadow نمایشی محدود
- map trails و object anchors روی grid هستند، نه placement تصادفی

## Today

### لایه‌های صحنه

1. background world: landscape/room context با contrast آرام
2. progression trail: ۳ تا ۵ landmark نزدیک، نه نقشهٔ بی‌انتها
3. current mission object: بزرگ‌ترین anchor
4. Cinna: guide، نه CTA
5. live action plate: متن و کنترل واقعی Compose
6. evidence strip: review due، focus و last receipt

### hierarchy

- headline: وضعیت قابل‌فهم امروز
- primary action: یک فعل دقیق
- secondary: review trail یا ادامهٔ کار قبلی
- tertiary: world exploration
- هیچ carousel نامحدود یا card grid بالای fold نیست.

## Field Atlas

- search همیشه live و semantic
- واژه‌ها به specimen/field-note object تبدیل می‌شوند، ولی متن واقعی live است
- filterها: All / Clinical / General / Saved
- entry detail از ID پایدار می‌آید
- invalid ID → NotFound با بازگشت به Atlas
- long term/definition و متن bidirectional در همان layout تست می‌شود

## Practice

فعالیت‌ها «ایستگاه» هستند، نه کارت بازاریابی:

- Review Trail
- Cloze Clinic
- Vocab Match
- Unscramble
- Language Sprint
- Fluency Studio
- Clinical Language Labs

هر station باید availability، evidence type، duration تقریبی و failure contract داشته باشد. station قفل‌شده prerequisite را توضیح می‌دهد؛ decoration قفل به‌تنهایی کافی نیست.

## Journey

- مسیر نزدیک کاربر واضح و مسیر دورتر کم‌جزئیات‌تر است.
- node state: available / active / completed / locked / recoverable
- star rating تزئینی حذف می‌شود مگر معیار واقعی و قابل توضیح داشته باشد.
- quest و achievement سطح presentation هستند؛ authority در Room است.
- reward receipt پس از persistence confirmation ظاهر می‌شود.

## Cabinet

- progress، focus، earned badges و preferenceها یک cabinet شخصی می‌سازند.
- achievementهای نگرفته silhouette/requirement روشن دارند؛ shame یا FOMO ندارند.
- theme، Reduce motion، sound و haptic در surface واقعی و قابل دسترس می‌مانند.

## mascot contract

Cinna باید:

- در onboarding و next action راهنما باشد؛
- در خطا blame نکند؛
- در success شدت متناسب داشته باشد؛
- هیچ ادعای تشخیص یا authority بالینی نداشته باشد؛
- در reduced motion pose ثابت و semantic label داشته باشد؛
- در هر صفحه حضور اجباری نداشته باشد.

## theme system

| theme | کاربرد | contrast intention |
|---|---|---|
| Toasted Night | تجربهٔ اصلی و نقشه | dark navy/charcoal با cream متن |
| Parchment Day | مطالعهٔ طولانی و Atlas | warm off-white با ink dark |
| Clinical Clarity | labs و تمرکز بالا | neutral light/dark با cyan/mint محدود |

accentها نقش معنایی دارند:

- orange: action/current progress
- mint: safe success/learned
- cyan: information/listening
- berry: focus/rare emphasis
- amber/gold: فقط milestone نادر و earned
- red: destructive/error، نه reward

## error و recovery

| failure | presentation | action |
|---|---|---|
| startup stage failed | world fragment + stage name | retry همان stage |
| invalid entry ID | empty specimen frame | return to Atlas |
| invalid scenario/tab | typed NotFound | return to parent selector |
| quest claim failure | receipt remains pending | retry claim only |
| AI unavailable | Cinna local-guide pose | use local guide / retry session |
| replay event | calm already-counted receipt | continue without duplicate reward |

## ضدالگوهای ممنوع

- صفحهٔ اصلی با hero و grid کارت‌ها
- container داخل container بدون دلیل
- gem/coin/crown economy
- fake streak urgency یا shame
- mascot تقلیدی/کودکانه
- متن تخت‌شده برای control یا دادهٔ زنده
- silent fallback و spinner بی‌نهایت
- layout یکسانِ کش‌آمده در tablet
- motion قبل از commit داده

