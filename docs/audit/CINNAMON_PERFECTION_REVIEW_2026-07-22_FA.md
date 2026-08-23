# نقد، بررسی و نقشهٔ پرفکشن Cinnamon

**نسخهٔ گزارش:** ۲۰۲۶-۰۷-۲۲  
**دامنه:** `C:\Users\K1\Desktop\Projects\Cinnamon`  
**نوع ارزیابی:** source + معماری + داده + UX + گیمیفیکیشن + build + APK + AVD ایزوله + مهاجرت واقعی + performance  
**حکم انتشار عمومی:** **BLOCK**  
**حکم هستهٔ داخلی فعلی:** **عبور مشروط؛ پایدار و قابل‌اثبات، اما هنوز پرفکشن کامل نیست**

## خلاصهٔ مدیریتی

Cinnamon دیگر یک نمونهٔ سه‌هزارخطی یا مجموعه‌ای از cardهای نمایشی نیست. شمارش زندهٔ workspace فعلی، بدون build/cache/output، **۲۷٬۰۱۶ خط Kotlin** و **۵۰٬۱۱۰ خط مهندسی و دادهٔ تألیفی** را نشان می‌دهد. مهم‌تر از عدد، هستهٔ واقعی محصول است: Room v5، event ledger تغییرناپذیر، reward settlement اتمیک، quest و achievement نسخه‌دار، Foundation Plan چهارمرحله‌ای، Learning Focus پایدار، scheduler روزانه و هفتگی، offline-first lexicon، Recovery Checkpoint شش‌مرحله‌ای، Repair Loop لینک‌شده و ضد‌farm، خطای AI صادقانه و APKهایی که روی AVD ایزوله آزموده شده‌اند. هم چرخهٔ Review تا Claim و هم انتخاب یک‌بارهٔ Learning Focus اکنون پس از force-stop و با PID تازه اثبات شده‌اند.

اما «پرفکشن» با تعداد خط یا سبزشدن تست‌ها برابر نیست. نتیجهٔ دقیق این ممیزی دوگانه است:

- هستهٔ داده، مهاجرت و ضدتقلب در سطح بسیار قوی قرار دارد.
- ظاهر فعلی گرم، متمایز، قابل‌خواندن و انگیزه‌بخش است؛ Home، Profile و Questboard در ۵۴۰dp و ۳۶۰dp اثبات شده‌اند.
- گیمیفیکیشن به عمل یادگیری معتبر وصل است و صرف بازکردن صفحه XP نمی‌دهد؛ Delayed Recall، Gentle Return و «ذخیره و مرور» همگی فقط از evidence زمانی و رابطه‌ایِ اثبات‌شده در ledger ساخته می‌شوند.
- backend زنده، session محصول، sync/restore، signing lineage، هویت نهایی mascot و چند evidence producer هنوز بسته نشده‌اند.
- بنابراین APK فعلی یک checkpoint داخلی جدی است، نه نسخهٔ آمادهٔ انتشار عمومی.

## اصلاح دامنه: Cinnamon هیچ Campaign فعالی ندارد

اصطلاح Campaign به محصول Cinnamon تعلق ندارد. معماری فعال اکنون از این واژگان استفاده می‌کند:

- `Foundation Plan`
- `Learning Focus`
- `Language Precision`
- `Recall Range`
- `Learning Questboard`
- `Language Sprint`
- `Vocab Pairs`

موارد قدیمی فقط در چهار مرز مجاز باقی مانده‌اند: SQL فریز‌شدهٔ مهاجرت، adapter سازگاری محدود، fixtureهای تست و snapshot تاریخی. این داده‌ها برای حفظ کاربر v4 لازم‌اند و در UI، accessibility، API فعال، event جدید یا مدل دامنهٔ جدید دیده نمی‌شوند.

نتیجهٔ جست‌وجوی منبع فعال:

- هیچ Campaign Catalog، Campaign Planner یا Campaign Projection فعالی وجود ندارد.
- جدول قدیمی در مهاجرت v5 حذف می‌شود.
- انتخاب قدیمی بدون دست‌کاری مقدار به `learning_focus_selections` منتقل می‌شود.
- نصب تازه هیچ شناسهٔ قدیمی تولید نمی‌کند.

## حکم امتیازی

این امتیازها judgment ممیزی هستند، نه عدد بازاریابی.

| حوزه | امتیاز از ۱۰ | حکم |
|---|---:|---|
| صحت دامنه و IA | ۹٫۳ | مدل فعال با محصول یادگیری هم‌راستا شده است |
| داده و مهاجرت | ۹٫۶ | حفظ دقیق v4، rollback و cold restore اثبات شده‌اند |
| Reward Engine و ضدتقلب | ۹٫۴ | ledger، idempotency، cap، assignment snapshot و receipt authority قوی‌اند |
| UX بصری و هویت جاری | ۸٫۵ | گرم، متمایز و responsive؛ mascot نهایی هنوز continuity می‌خواهد |
| گیمیفیکیشن قابل‌استفاده | ۹٫۲ | Foundation/Focus/Cabinet، Turnaround، Context Builder، Delayed Recall، Gentle Return و Kept and Learned واقعی‌اند؛ breadth catalog هنوز کامل نیست |
| accessibility و responsive | ۸٫۸ | ۳۶۰dp و targetهای ۴۸dp اثبات شده‌اند؛ ماتریس TalkBack کامل نیست |
| خطا، offline و صداقت AI | ۸٫۶ | fail-closed و delivery certainty مناسب است |
| performance | ۷٫۶ | harness مرحله‌ای و Profile واقعی‌اند؛ Macrobenchmark دستگاه فیزیکی هنوز لازم است |
| پوشش آزمون | ۹٫۷ | ۱۵۱ unit پاس؛ ۱۰ instrumentation ایزولهٔ پیشین و چهار lifecycle test جدیدِ کامپایل‌شده برای Context Builder، Delayed Recall، Gentle Return و Kept and Learned؛ اجرای تازهٔ آن‌ها وابسته به AVD اختصاصی آزاد است |
| backend و release | ۴٫۰ | وابسته به auth/gateway/signing/owner gates بیرونی |

**امتیاز هستهٔ داخلی:** حدود **۹٫۰ از ۱۰**  
**آمادگی انتشار عمومی:** **BLOCK** تا بسته‌شدن P0های بیرونی

## آنچه اکنون واقعاً ساخته شده است

### ۱. حلقهٔ انگیزش واقعی

جریان authority به‌شکل زیر است:

`عمل معتبر یادگیری → event پایدار → settlement اتمیک Room → projection خواندنی → receipt یک‌بارمصرف`

ویژگی‌های مهم:

- بازکردن صفحه، انتخاب tab یا اجرای animation پاداش نمی‌دهد.
- review، mastery و practice فقط پس از outcome معتبر ثبت می‌شوند.
- event تکراری transaction یا balance تازه ایجاد نمی‌کند.
- daily cap رفتارهای farmable را محدود می‌کند.
- milestoneهای بلندمدت از cap روزانه معاف‌اند، اما idempotent باقی می‌مانند.
- receipt فقط presentation است و acknowledge آن balance را تغییر نمی‌دهد.
- state قفل‌شده evidence زودرس را مصرف نمی‌کند.

### ۲. Foundation Plan

Foundation Plan چهار milestone ترتیبی دارد و از Room تغذیه می‌شود. نصب تازه شناسهٔ canonical زیر را می‌سازد:

`journey.foundation.plan@1`

کاربر قدیمی با wire identity سابق بدون rewrite یا duplicate شدن پشتیبانی می‌شود. UI هر دو حالت را با عنوان واحد Foundation Plan نمایش می‌دهد.

### ۳. Learning Focus

پس از evidence پیش‌نیاز، دو Focus هم‌ارزش باز می‌شوند:

| Focus | هدف | milestoneهای اصلی |
|---|---|---|
| Language Precision | دقت واژگان و کاربرد | مرور متنوع، practice چندفرمتی، active-day evidence |
| Recall Range | وسعت recall و تنوع تمرین | مرور بیشتر، practice گسترده‌تر، ریتم چندروزه |

انتخاب:

- یک‌بار و اتمیک است.
- هیچ XP مستقیمی برای «انتخاب کردن» نمی‌دهد.
- conflict باعث rollback کامل می‌شود.
- نصب تازه IDهای canonical می‌سازد.
- مهاجرت، تمام IDها و timestampهای legacy را دقیق نگه می‌دارد.

چرخهٔ دستگاهی این قرارداد نیز بسته شده است: سه Review واقعی milestone پیش‌نیاز را باز می‌کنند، کاربر از UI خود Questboard گزینهٔ `Language Precision` را انتخاب می‌کند، XP انتخاب دقیقاً صفر می‌ماند، تلاش دوم برای تغییر Focus fail-closed است و همان انتخاب پس از recreation و process تازه از Room بازیابی می‌شود.

### ۴. Achievement Cabinet

کابینت Profile اکنون از projection واقعی استفاده می‌کند:

- stateهای `UNLOCKED` و `IN PROGRESS`
- مقدار evidence واقعی
- progress bar قابل‌خواندن
- توضیح ضدتقلب: badge با بازکردن صفحه ساخته نمی‌شود
- رندر صحیح در ۵۴۰dp و ۳۶۰dp

کاتالوگ کامل شامل موارد زیر است:

| نوع | تعداد |
|---|---:|
| Achievement definition | ۱۲ |
| Quest definition | ۱۲ |
| Reward definition | ۱۵ |
| Rarity | ۴ |
| Progression level | ۶ |
| Presentation family | ۵ |

در نسخهٔ فعلی نه achievement غیرمخفی در کابینت ظاهر می‌شوند، چون نه معیار دارای producer و evidence resolver کامل‌اند: delayed mastery، rhythm week، content breadth، repair loop، Confusable Precision، Context Builder، Delayed Recall، Gentle Return و Kept and Learned. پنهان‌ماندن سه مورد دیگر تصمیم درستی است؛ نشان‌دادن progress حدسی خلاف قرارداد محصول بود.

#### Turnaround: اصلاح اشتباه، پیشرفت واقعی

چرخهٔ `achievement.correction.repair_loop` صرفاً یک badge نمایشی نیست. پاسخ Again یک event صفر-XP با `repairCandidate` ثبت می‌کند؛ مرور موفقِ بعدی فقط وقتی correction می‌سازد که موعد همان واژه رسیده باشد و Room بتواند یک mistake قبلیِ هنوز اصلاح‌نشده را برای همان subject پیدا کند. correction، شناسهٔ event قبلی، `repairLinkPresent` و تقدم زمانی را در metadata تغییرناپذیر نگه می‌دارد.

- resolver DAO خود metadata و row لینک‌شده را دوباره query می‌کند؛ event type مشابه، metadata ناقص، subject ناهماهنگ یا timestamp معکوس evidence نمی‌سازد.
- شمارش `COUNT_DISTINCT(subjectId)` است؛ یک واژه هرگز repair XP یا evidence تکراری نمی‌سازد.
- event خام اشتباه از Activity feed پنهان است و روز مطالعه یا streak را بالا نمی‌برد؛ فقط «Mistake repaired» به‌عنوان بازخورد انسانی دیده می‌شود.
- proof دستگاهی با پنج واژهٔ متمایز، پنج correction موعددار، بازشدن Turnaround، ۴۰ XP دقیقِ repair+achievement، تکمیل `Daily Repair One`، claim دقیق ۱۰ XP و replay/claim تکراریِ ردشده بسته شده است.

#### Confusable Precision: دیدن تفاوت، اثبات در بازگشت

بازی `Vocab Pairs` اکنون از ۴۵ جفت واقعی corpus محلی استفاده می‌کند، نه تعریف‌های عمومیِ match-card. هر دور یک مثال واقعی می‌دهد، فقط انتخاب اول را می‌پذیرد و پس از پاسخ، `howToTell` همان جفت را توضیح می‌دهد. پاسخ درست امروز فقط `confusable_pair_attempted` می‌سازد؛ پاسخ درستِ همان جفت پس از حداقل ۲۴ ساعت، فقط از طریق لینک immutable به آن تلاش، `confusable_pair_resolved` تولید می‌کند.

- تلاش زودهنگام، لینک گم‌شده، subject ناهماهنگ و تکرار همان جفت evidence نمی‌سازند.
- شمارش از `COUNT_DISTINCT(subjectId)` و DAO revalidation می‌آید؛ UI یا metadata خام اختیار achievement ندارد.
- `achievement.mastery.confusable_precision` و `quest.daily.confusable_pair` اکنون producer/resolver واقعی دارند. پاسخ لحظه‌ای XP مستقیم ندارد؛ پاداش فقط از settlementِ catalog/quest می‌آید.

#### Context Builder: واژه در جمله، نه فقط در کارت

در Cloze Clinic، پاسخ صحیحِ اول برای یک sentence authored اکنون فقط از طریق `recordVerifiedContextApplication` می‌تواند evidence بسازد. repository خود entry را در lexicon محلی بازخوانی می‌کند؛ call عمومی برای این event ممنوع است و DAO فقط event type محافظت‌شده با `applicationVerified=true` را می‌شمارد. بنابراین tap، پایان round، practice session عمومی یا metadata ناقص، Context Builder را جلو نمی‌برد.

- پنج entry متمایز، `achievement.application.context_builder` را باز می‌کنند؛ یک entry تکراری صرفاً evidence تکراری است، نه XP یا progress تازه.
- `quest.daily.context_use` با یک کاربرد متمایز کامل می‌شود و claim snapshot‌شدهٔ ۱۰ XP دارد.
- lifecycle test دستگاهی برای مسیر repository → Room → Quest claim → Profile به source افزوده و build شده است؛ اجرای تازه‌اش منتظر AVD اختصاصیِ آزاد مانده و هنوز به‌عنوان proof اجراشده ادعا نمی‌شود.

#### Delayed Recall: بازگشت بعد از فاصله، نه XP برای حضور

هر `review_completed` موفق، پیش از ساختن evidence جدید در Room با یک مرور موفقِ پیشین برای همان `lexicon_entry` تطبیق داده می‌شود. فقط اگر آن event واقعاً دست‌کم ۷۲ ساعت قدیمی‌تر باشد، `delayed_recall_succeeded` نوشته می‌شود. event عمومی حق تولید این outcome را ندارد و یک واژه فقط یک evidence از این نوع می‌گیرد.

- DAO هنگام شمارش، هم metadata (`priorReviewEventId`، `delayedRecallLinkPresent` و مرز ۷۲ ساعت) و هم row مرجعِ همان subject را دوباره اعتبارسنجی می‌کند؛ JSON دست‌ساز، لینک مفقود، subject ناهماهنگ و فاصلهٔ حتی یک میلی‌ثانیه کوتاه‌تر، progress نمی‌سازند.
- `achievement.challenge.delayed_recall` با سه واژهٔ متمایز باز می‌شود. این event XP مستقیم ندارد؛ تنها settlement کاتالوگ می‌تواند پاداش tier بدهد.
- unit proof مرز دقیق ۷۲ ساعت، replay و لینک گم‌شده را پوشش می‌دهد. lifecycle test واقعیِ `Review → Room → achievement → Profile` نیز compile شده، اما تا آزادشدن AVD اختصاصی هنوز proof اجراشده نیست.

#### Gentle Return: بازگشت آرام، نه تنبیه برای غیبت

یک کاربر تازه یا یک session عادی هرگز «بازگشت» نمی‌گیرد. فقط پایانِ یک game session با حداقل سه action معتبر، بعد از آنکه Room آخرین فعالیت معتبر او را حداقل هفت روز پیش پیدا کند، outcome محافظت‌شدهٔ `comeback_session_completed` می‌سازد. هیچ call عمومی این event را نمی‌نویسد؛ retry همان occurrenceKey نیز فقط نتیجهٔ canonical قبلی را برمی‌گرداند.

- DAO هم لینک immutable به activity قبلی، هم مرز دقیق ۷ روز و هم `meaningfulActionCount >= 3` را دوباره query می‌کند؛ metadata دست‌ساز، action کمتر از سه یا فاصلهٔ کوتاه‌تر از ۷ روز evidence نمی‌سازد.
- `achievement.comeback.gentle_return` فقط از همین evidence unlock می‌شود و event بازگشت XP مستقیم ندارد؛ پاداش ۳۰ XP صرفاً از settlement کاتالوگِ unlock جدید می‌آید.
- unit proof مرز ۷ روز، لینک گم‌شده و session کوچک را fail-closed می‌سنجد. lifecycle test `Activity → practice producer → Room → Profile` نیز compile شده، اما مانند دو lifecycle تازهٔ دیگر هنوز AVD اختصاصیِ آزاد لازم دارد.

#### Kept and Learned: ذخیره، بازگشت و مرور واقعی

bookmark دیگر صرفاً یک icon تزئینی نیست. وقتی کاربر واژه‌ای را ذخیره می‌کند، یک event بدون XP برای همان `lexicon_entry` ثبت می‌شود. فقط مرور موفقِ همان واژه در حالی که هنوز ذخیره شده و دست‌کم یک ساعت از bookmark معتبر گذشته است، outcome محافظت‌شدهٔ `saved_item_reviewed` تولید می‌کند. سپس `achievement.collection.learned_not_saved` برای ده واژهٔ متمایز پیش می‌رود.

- call عمومی برای outcome محافظت‌شده رد می‌شود؛ یک bookmark یا review تکراری، progress دوباره نمی‌سازد.
- DAO علاوه بر metadata، row دقیق bookmark، subject یکسان، فاصلهٔ حداقل یک ساعت و `successfulReviewCount=1` را دوباره query می‌کند؛ JSON دست‌ساز، bookmark گم‌شده، فاصلهٔ کوتاه یا subject ناهماهنگ evidence نیست.
- هر واژه فقط یک بار `COUNT_DISTINCT(subjectId)` حساب می‌شود. bookmark مستقیم XP، streak یا activity معنی‌دار نمی‌سازد؛ unlock فقط از settlement کاتالوگ می‌آید.
- در خود Entry Detail یک cue زندهٔ «Kept and Learned is active» و توضیح چرخهٔ یک‌ساعته دیده می‌شود؛ accessibility دکمهٔ bookmark هم همین مسیر را بیان می‌کند.
- unit proof مرز دقیق یک ساعت و revalidation DAO را پوشش می‌دهد. lifecycle test چهارم، injection عمومی، ده bookmark/review متمایز، unlock، replay ردشده و Profile را compile می‌کند؛ اجرای دستگاهی تازه همچنان به AVD اختصاصی Cinnamon نیاز دارد.

### ۵. Questboard و بازی‌ها

Questboard از tabهای زیر تشکیل شده است:

- Learning Plan
- Quests
- Language Sprint
- Vocab Pairs
- Transcript Escape
- Case Study
- Study Log

اصلاح نام‌ها دقیق است:

- فعالیت تکرارشونده دیگر «Weekly Challenge» نام ندارد.
- بازی pair matching دیگر «Match-3» نامیده نمی‌شود.
- label، mechanics و accessibility با هم سازگارند.

همهٔ tab targetهای اندازه‌گیری‌شده دقیقاً ۴۸dp هستند و selected state در UIAutomator ثبت شده است.

Quest اجرایی دیگر فقط daily نیست. scheduler فعلی پنج قرارداد قابل‌اجرا دارد:

| Quest | cadence | evidence دقیق | پاداش snapshot‌شده |
|---|---|---|---:|
| `quest.daily.due_review` | روزانه | سه subject مرور due و متمایز در پنجرهٔ روز | ۱۰ XP |
| `quest.daily.repair_one` | روزانه | یک repair لینک‌شده و تأییدشده در پنجرهٔ روز | ۱۰ XP |
| `quest.daily.context_use` | روزانه | یک کاربردِ تأییدشدهٔ واژهٔ corpus در جملهٔ authored | ۱۰ XP |
| `quest.daily.confusable_pair` | روزانه | دو جفت متمایز با تلاش لینک‌شده و بازگشت حداقل ۲۴ساعته | ۱۰ XP |
| `quest.weekly.durable_mastery` | هفتگی | هشت subject با delayed mastery در پنجرهٔ دوشنبه تا دوشنبه | ۳۰ XP |

هر assignment، timezone، شروع/پایان evidence، expiry، reward grant و presentation policy را همان لحظه در Room فریز می‌کند. نتیجه:

- تغییر timezone، Quest فعال را دوباره نمی‌سازد و پنجرهٔ evidence را جابه‌جا نمی‌کند.
- replay دیررس بعد از grace نمی‌تواند Quest منقضی را زنده یا تکمیل کند.
- Quest کامل اما claim‌نشده بعد از expiry همچنان برای کاربر قابل‌claim می‌ماند.
- claim از snapshot همان assignment خوانده می‌شود، نه از مقدار قابل‌تغییر catalog امروز.
- reward type ناسازگار به‌جای crash کردن event settlement، fail-closed و inert می‌شود.
- UI واقعی در API 35 و عرض ۳۶۰dp، Quest هفتگی «Make eight memories stick» را از projection ذخیره‌شده نشان می‌دهد.

### ۶. هویت بصری فعلی

هویت موجود از این مواد واقعی استفاده می‌کند:

- Toasted Dark به‌عنوان تم پیش‌فرض
- Warm Cream و Midnight AMOLED
- رنگ spice orange، mint و berry
- گوشه‌های نرم، cardهای عمیق و progressهای واضح
- cinnamon-roll mark پروژه در Home و Profile
- لحن انگیزه‌بخش بدون streak-shaming

این ظاهر خشک یا template-first نیست. بااین‌حال، exact identifier کانسپت برند و خانوادهٔ mascot انتخاب‌شده در workspace فعلی ثبت نشده است. برای جلوگیری از جعل تصمیم کاربر، mascot نهایی تازه‌ای اختراع نشد.

## داده و مهاجرت

### Room v5

جدول canonical جدید:

`learning_focus_selections`

گیت‌های اعمال‌شده:

- `v1/v2/v3/v4 → v5`
- دیتابیس خالی و پر
- foreign-key clean
- integrity clean
- unique collision rollback
- orphan reference rollback
- حفظ دقیق event، reward، balance، journey و selection
- schema export نسخهٔ ۵

validator مستقل ۵۲ statement را بررسی می‌کند.

### اثبات واقعی v4 → v5

snapshot واقعی با DB/WAL/SHM روی AVD ایزوله restore شد. نتیجه:

| شاخص | v4 | v5 | cold restart |
|---|---:|---:|---:|
| event | ۷ | ۷ | ۷ |
| reward transaction | ۴ | ۴ | ۴ |
| journey | ۲ | ۲ | ۲ |
| journey stage | ۷ | ۷ | ۷ |
| selection | ۱ | ۱ | ۱ |
| XP | ۳۴ | ۳۴ | ۳۴ |
| schema | ۴ | ۵ | ۵ |

هیچ crash، reset یا duplicate Foundation ثبت نشد.

### نصب پاک v5

- یک Foundation canonical
- صفر Foundation legacy
- صفر selection ازپیش‌ساخته
- صفر reward transaction
- صفر achievement جعلی
- سه audit event idempotent startup و بدون reward

## backend و AI

### نقاط قوی

- اپ provider key نمی‌پذیرد.
- URL gateway فقط HTTPS و بدون user-info/query/fragment قبول می‌شود.
- idempotency key و request ID جدا هستند.
- error body محدود به ۱۶KB خوانده می‌شود.
- متن خام provider به UI نشت نمی‌کند.
- delivery certainty سه state دارد: ارسال‌نشده، پاسخ‌گرفته، نامطمئن پس از ارسال.
- fallback آفلاین دقیقاً می‌گوید پیام ارسال نشده یا تحویلش تأیید نشده است.

### محدودیت قطعی

`ProductSessionTokenProvider` عمداً token برنمی‌گرداند. بنابراین live coaching در artifact فعلی fail-closed است. این رفتار امن است، اما قابلیت live کامل نیست.

برای production لازم است:

1. account/session محصول با token کوتاه‌عمر؛
2. gateway مالک محصول؛
3. rate limit و abuse control؛
4. secret manager؛
5. redacted observability و request correlation؛
6. retention/privacy policy برای متن کاربر؛
7. آزمون delivery ambiguity و retry در محیط staging.

## خطا و resilience

نقاط مثبت:

- startup یک state machine قابل‌بازیابی دارد.
- failure stage مشخص می‌شود و exception data در logcat serialize نمی‌شود.
- migration قبل از import و seed تفکیک شده است.
- cancellation صحیح دوباره throw می‌شود.
- AI failure به fallback کاربردی تبدیل می‌شود.
- Room collisionها fail-closed هستند.
- هر شش stage اکنون Recovery Checkpoint اختصاصی با عنوان، توضیح و CTA متفاوت دارند؛ خطای Lexicon دیگر به پیام generic سقوط نمی‌کند.
- پیام حفظ XP/history، CTA حداقل ۴۸dp و `liveRegion` برای اعلام تغییر state پیاده شده‌اند.
- ماتریس شش‌مرحله‌ای روی Compose واقعی در AVD پاس شده و preview مستقل ۳۶۰dp نیز از خود component ثبت شده است.

شکاف‌ها:

- fault injection واقعی به دیتابیس خراب یا asset خراب هنوز از Activity تولید نشده است؛ ماتریس فعلی خود سطح presentation و action را کامل می‌سنجد.
- delivery ambiguity هوش مصنوعی در staging واقعی هنوز E2E نشده است.
- observability production وجود ندارد.
- restart و retry matrix روی دستگاه‌های چندسازنده اجرا نشده است.
- export/restore کاربر وجود ندارد؛ `allowBackup=false` حریم خصوصی را حفظ می‌کند ولی ریسک از‌دست‌رفتن داده در uninstall/device loss را بالا می‌برد.

## Performance

### نتیجهٔ زنده روی AVD ایزوله

| build | run | TotalTime p50 | TotalTime p95 | prepare p50 | prepare p95 |
|---|---:|---:|---:|---:|---:|
| debug | ۵ | ۱۲٬۷۵۸ms | ۱۳٬۱۱۳ms | ۲٬۸۴۴ms | ۳٬۱۱۳ms |
| benchmark non-debuggable | ۵ | ۳٬۶۸۷ms | ۳٬۷۸۳ms | ۹۵۸ms | ۹۹۳ms |

پس از slice هفتگی، یک diagnostic تازه روی AVD گرم فعلی اجرا شد. این اعداد به‌دلیل تفاوت وضعیت AVD با baseline بالا، مقایسهٔ مستقیم یا ادعای regression/improvement نیستند:

| سناریوی v6 | run | TotalTime p50 | TotalTime p95 | reconciliation p50 | reconciliation p95 |
|---|---:|---:|---:|---:|---:|
| process-cold، دادهٔ موجود | ۵/۵ | ۱٬۲۱۱ms | ۱٬۳۳۰ms | ۹ms | ۳۵ms |
| fresh app data در هر run | ۳/۳ | ۱٬۱۹۶ms | ۱٬۳۰۸ms | ۲۵ms | ۲۸ms |

تفسیر:

- debug برای ارزیابی تجربهٔ release معیار خوبی نیست و حدود چهار برابر کندتر است.
- benchmark هنوز به سطح «فوری» نمی‌رسد.
- parse کاتالوگ در benchmark حدود ۶۲۷ تا ۷۰۱ms است و بهترین هدف داخلی بعدی است.
- startup pipeline فعلی migration/import/seed را درست تفکیک می‌کند.
- Baseline Profile شامل ۲٬۶۴۵ rule است و profile واقعاً داخل APK بنچمارک بسته‌بندی شده است.

در همین ممیزی یک bug در summary harness پیدا و رفع شد: آرایهٔ percentile تمام-null باعث parameter-binding failure می‌شد. smoke و پنج run بعد از اصلاح پاس شدند.

این اعداد فقط emulator evidence هستند. پذیرش production به Macrobenchmark/profileable build و حداقل یک دستگاه فیزیکی میان‌رده نیاز دارد.

## Accessibility و responsive

اثبات‌شده:

- Home، Profile و Questboard در ۵۴۰dp
- Home، Achievement Cabinet و Questboard در ۳۶۰dp
- tab، badge و کارت Quest هفتگی در ۳۶۰dp با bounds داخل viewport
- tab target برابر ۴۸dp
- selected semantics
- labelهای زنده برای progress
- Reduce Motion، sound و haptic preference
- contrast contract test
- RTL support در Manifest

باقی‌مانده:

- walkthrough کامل TalkBack
- font scale ۱٫۳ و ۲٫۰
- switch access
- landscape/tablet matrix
- reduced-motion screenshot/runtime matrix
- semantic assertions برای تمام بازی‌ها و modalها

## Actions، امنیت build و release

### وضعیت خوب

- workflow فقط verification است.
- permission سراسری `contents: read` است.
- actionها با commit SHA pin شده‌اند.
- secret boundary، lexicon، Room، signing contract و Baseline Profile در CI بررسی می‌شوند.
- job مستقل `instrumentation` بعد از verify، تست‌ها را روی Gradle Managed Device API 35 اجرا می‌کند؛ دیگر فقط AndroidTest را compile نمی‌کند.
- task واقعی `pixel2Api35DebugAndroidTest` در model محلی Gradle ثبت و workflow با parser مستقل YAML تأیید شد.
- `gradle-wrapper.jar` و distribution هر دو به Gradle 9.3.1 هم‌نسخه شدند و checksum رسمی دارند.
- release signing fail-closed است.
- benchmark با debug key امضا می‌شود و release fallback نیست.
- endpoint عمومی از credential جداست.
- backup/export اپ به‌صورت صریح بسته است.

### P0های بیرونی انتشار

1. revoke/rotate credentialهای قبلی و remediation تاریخچهٔ Git؛
2. تعیین new product یا update و حفظ applicationId/signing lineage در صورت update؛
3. upload/app-signing fingerprint و backup امن؛
4. gateway/auth/rate limit production؛
5. master icon و هویت نهایی تأییدشده؛
6. privacy/legal/content approval؛
7. versionCode/versionName و کانال انتشار؛
8. update test واقعی از artifact منتشرشدهٔ قبلی.

## پوشش آزمون

### نتیجهٔ فعلی

- ۳۵ suite واحد
- ۱۵۱ تست واحد
- صفر failure و صفر error
- ۱۰/۱۰ instrumentation واقعی روی AVD ایزوله API 35؛ صفر failure، error و skip
- چهار lifecycle test تازهٔ Context Builder، Delayed Recall، Gentle Return و Kept and Learned با `assembleDebugAndroidTest` کامپایل شده‌اند؛ اجرای دستگاهی تازه‌شان عمداً تا آزادشدن AVD اختصاصی Cinnamon به تأخیر افتاده است.
- lint با صفر error و ۳۲ warning: ۳۱ هشدار freshness نسخه و یک هشدار `OldTargetApi` ناشی از compileSdk 36.1 در کنار target API سالانهٔ 36
- build موفق debug و benchmark

پوشش واحد در reward، AI contract، catalog، persistence و migration قوی است. سه instrumentation قراردادهای context/startup/catalog را می‌سنجند؛ تست مسیر Activity از Home تا Practice، Learning Questboard، tab `Quests` و projection هفتگی عبور می‌کند؛ دو تست ترتیبی Quest lifecycle هشت کارت واقعی را در آستانهٔ mastery از UI Review ثبت می‌کنند، Quest را به ۸/۸ می‌رسانند، Claim را از خود Questboard می‌زنند، افزایش XP را دقیق می‌سنجند و دوباره‌Claim را بدون افزایش balance رد می‌کنند؛ دو تست Learning Focus با Review واقعی milestone را باز، انتخاب canonical را از UI ثبت و تغییر دوباره را بدون XP یا corruption رد می‌کنند؛ Repair Loop با پنج mistake/correction متمایز، ۴۰ XP exact، metadata link، تکمیل `Daily Repair One`، claim دقیق ۱۰ XP و replay/claim تکراری ردشده را از خود Activity تا Profile می‌سنجد؛ تست query Confusable Precision delay دقیق ۲۴ ساعت، link مفقود، subject mismatch و replay هم‌جفت را fail-closed اثبات می‌کند. Context Builder، Delayed Recall، Gentle Return و Kept and Learned هر چهار اکنون producer محدودشده، query revalidation، projection و lifecycle device test کامپایل‌شده دارند. برای Kept and Learned مرز دقیق یک ساعت، bookmark مفقود، subject ناهماهنگ، replay و injection عمومی unit/runtime-contract شده‌اند. اجرای device این چهار مسیر به AVD آزاد نیاز دارد و هنوز به‌عنوان proof اجراشده ادعا نمی‌شود؛ و تست دهم هر شش failure stage را روی کارت Compose واقعی با متن اختصاصی و action قابل‌کلیک می‌سنجد. اجرای کامل پیشین با runner مستقیم روی `emulator-5582` انجام شد؛ run قدیمیِ shared emulator به‌دلیل foreground شدن app نامرتبط به‌عنوان evidence استفاده نشده است.

اثبات process restart جدا از اجرای معمول suite برای هر دو جریان انجام شد. در Quest، PID از `7534` به `7643` تغییر کرد و متد دوم همان Claim و balance را از دیسک خواند. در Learning Focus، PID از `7321` به `7415` تغییر کرد و انتخاب immutable، XP بدون تغییر و conflict protection بازیابی شدند. هر چهار invocation مستقل ۱/۱ پاس شدند. شاهد خام در `qa/runtime-proof/quest-process-restart-20260722-135016.txt` و `qa/runtime-proof/learning-focus-process-restart-20260722-134918.txt` ثبت است.

## نقد معماری و maintainability

چند فایل بسیار بزرگ هستند:

| فایل | خط تقریبی |
|---|---:|
| `GamificationHubScreen.kt` | ۱٬۸۸۰ |
| `GamificationDao.kt` | ۲٬۰۶۶ |
| `ClinicalSimLabsScreen.kt` | ۱٬۶۱۰ |
| `NativeFluencyPlaygroundScreen.kt` | ۱٬۵۰۶ |
| `RoleplayChatScreen.kt` | ۱٬۱۱۸ |
| `HomeScreen.kt` | ۱٬۱۱۶ |

این فایل‌ها الزاماً bug نیستند، اما هزینهٔ review، preview، ownership و regression را بالا می‌برند. refactor پیشنهادی باید behavior-preserving و مرحله‌ای باشد:

- هر Questboard component در فایل مستقل؛
- DAO به query interfaceهای ledger/catalog/journey تفکیک شود، بدون شکستن transaction owner؛
- state holderهای صفحه از composition جدا شوند؛
- preview و semantics test کنار هر feature قرار گیرد؛
- stringهای محصول از composableهای حجیم بیرون کشیده شوند.

## P0، P1 و P2

### P0 - پیش از انتشار عمومی

| مالک | اقدام | گیت پذیرش |
|---|---|---|
| Owner/Security | rotation و history remediation | کلید قدیمی invalid و secret scan پاک |
| Backend | gateway + auth + rate limit | staging E2E با token کوتاه‌عمر و retry امن |
| Release | signing lineage و version policy | upgrade واقعی با signature تأییدشده |
| Product/Legal | privacy و clinical/editorial review | متن و policy امضاشده |
| Brand | master icon و identity package | asset canonical ثبت‌شده در repo |

### P1 - رسیدن از ۸٫۸ به حدود ۹٫۳

1. producer و resolver واقعی برای ۸ achievement باقی‌مانده؛
2. scheduler و persistence برای Questهای هفتگی باقی‌مانده، weekend، comeback و monthly؛
3. Macrobenchmark و بهینه‌سازی parse کاتالوگ؛
4. fault injection واقعی برای database/asset corruption و AI delivery ambiguity؛
5. export رمزنگاری‌شده یا account-bound sync/restore؛
6. refactor فایل‌های ۱۰۰۰+ خطی بدون تغییر رفتار؛
7. registry یکپارچهٔ string و تست copy؛
8. diagnostics و support bundle پاک‌سازی‌شده برای recovery، بدون دادهٔ حساس؛
9. ماتریس TalkBack/font scale/landscape/tablet؛
10. بازیابی artifact کانسپت انتخاب‌شده و اجرای mascot/celebration family همان کانسپت.

### P2 - رساندن تجربه به سطح flagship

- celebrationهای context-aware و قابل‌خاموش‌کردن؛
- artifact collection برای milestoneهای واقعی؛
- adaptive quest deck براساس due queue و history؛
- seasonهای شخصی بدون leaderboard جعلی یا FOMO؛
- Study Log تصویری با evidence قابل‌فهم؛
- widget و notificationهای humane و قابل‌کنترل؛
- downloadable content packs با provenance؛
- offline encrypted export و migration preview؛
- performance budget در CI؛
- visual regression matrix برای هر سه theme.

## ایده‌های گیمیفیکیشن دقیق، بدون خشک‌شدن یا فریب

### ۱. Cabinet به‌عنوان فضای زنده

هر achievement باید یک artifact تصویری از evidence واقعی بگیرد: نه badge تخت، بلکه شیء کوچک در قفسهٔ Cinnamon. artifact تا زمان unlock به‌صورت silhouette قابل‌فهم است و rarity با شکل/قاب، نه فقط رنگ، بیان می‌شود.

### ۲. Momentum بدون streak-shaming

به‌جای «زنجیره را نشکن»، ریتم‌های انعطاف‌پذیر نمایش داده شود: سه روز معنادار در هفته، comeback کوچک، و recovery بدون تنبیه. کاتالوگ فعلی پایهٔ این مدل را دارد.

### ۳. Focus identity

Language Precision و Recall Range باید در milestoneهای بعدی، texture، illustration و microcopy متمایز داشته باشند؛ نه صرفاً رنگ متفاوت. این هویت نباید wire ID یا migration contract را تغییر دهد.

### ۴. Reward receipt با معنا

هر receipt باید سه چیز را بگوید: «چه evidenceی ثبت شد»، «چرا پاداش معتبر است» و «قدم بعدی چیست». confetti بدون توضیح حذف شود.

### ۵. Quest deck تطبیقی

Quest assignment باید از due count، content availability، absence window و recent practice mix تغذیه شود. هر replacement باید دلیل شفاف داشته باشد و حداکثر شمارش آن محدود بماند.

### ۶. Failure به‌عنوان بخشی از بازی

اصلاح اشتباه باید مادهٔ achievement باشد، نه شکست. Repair Loop اکنون producer، repair-link اتمیک، ضد‌farm و proof دستگاهی دارد؛ Confusable Precision نیز همین قراردادِ delayed-link را دارد. calibrated confidence هنوز باید به این معیار سخت‌گیرانه برسد.

### ۷. Mascot به‌عنوان coach، نه decoration

پس از بازیابی کانسپت منتخب، mascot فقط در لحظه‌های کمکی ظاهر شود: شروع Focus، repair موفق، comeback و milestone. در clinical practice، reduced motion یا وضعیت تمرکز باید آرام یا غایب باشد.

## دفترچهٔ انطباق با پروتکل اسکیل‌ها

این جدول claim تشریفاتی نیست؛ هر ردیف به تصمیم یا شاهد همین workspace متصل است.

| اسکیل | نحوهٔ اعمال در Cinnamon | وضعیت |
|---|---|---|
| `copy` | اسکرین‌شات Questboard موجود به‌عنوان reference binding حفظ شد؛ row geometry و زبان بصری عوض نشد | پاس |
| `perfect` | سه چرخهٔ adversarial، ابتدا timezone/evidence، سپس reward drift، سپس claimability/UI backlog را پیدا و اصلاح کرد | پاس برای slice فعلی |
| `orchestrator` | ترتیب authority: data contract → scheduler → projection → runtime → CI → report حفظ شد | پاس |
| `gamify` | Quest و Repair Loop فقط با outcome معتبر پیش می‌روند؛ tap، animation و raw mistake هیچ XP نمی‌سازند | پاس |
| `gamify-reward-engine` | event ledger، repair-link تراکنشی، transaction idempotent، reward snapshot و claim یک‌بارمصرف | پاس |
| `gamify-achievement-catalog` | نه criterion با producer/resolver دقیق فعال‌اند؛ DAO metadata و link Repair Loop، Delayed Recall، Gentle Return و Kept and Learned را دوباره اثبات می‌کند | پاس محدود و صادقانه |
| `gamify-mascot-studio` | چون artifact canonical کانسپت منتخب در workspace بازیابی نشد، mascot تازه جعل نشد | gate باز |
| `style` | reference screenshot، viewport ۵۴۰dp/۳۶۰dp، bounds و visual check؛ Recovery Checkpoint نیز در ۳۶۰dp بازبینی شد | پاس برای سطح تغییرکرده |
| `anatomy` | Quest روزانه و هفتگی در tab واحد `Quests` قرار گرفتند؛ IA دیگر cadence را اشتباه محدود نمی‌کند | پاس |
| `modernize` | label خشک و نادرست `Daily Quests` به `Quests` تبدیل شد، بدون تغییر هویت پذیرفته‌شده | پاس |
| `dataman` | Room v5، schema export، migration verifier، exact evidence window، conservation و query رابطه‌ای repair/Delayed Recall/Gentle Return/Kept and Learned | پاس |
| `backend` | authority مالی در Room transaction باقی ماند؛ catalog/UI اختیار grant ندارند | پاس محلی؛ backend زنده باز |
| `actions` | permissions حداقلی، SHA pin، Gradle checksum، signing fail-closed و Android Managed Device واقعی؛ release-doc audit تازه هم پاس شد | config پاس؛ اجرای remote پس از push |
| `errors` | snapshot خراب fail-closed است؛ هر شش startup stage اکنون recovery اختصاصی، live-region و action آزموده‌شده دارند | پاس محلی؛ fault injection واقعی باز |
| `function` | Quest، Focus، Repair Loop و ماتریس شش‌مرحله‌ای Recovery Checkpoint روی Compose واقعی و AVD ایزوله سنجیده شدند؛ lifecycle Kept and Learned برای device آماده است | پاس محدود |
| `performance` | ۵ process-cold و ۳ fresh-data run؛ stage timing جدا برای reconciliation | پاس emulator؛ دستگاه فیزیکی باز |
| `string` | label جدید با مدل `gemini-flash-latest` در AvalAI نقد شد و `Quests` انتخاب شد | پاس |
| `automate` | patch، تست، build، install، navigation، force-stop، دو PID proof، screenshot، logcat و report بدون handoff نیمه‌کاره اجرا شد | پاس برای slice فعلی |
| `multi-agent` | ممیزی‌های مستقل scheduler، reward و release به تصمیم نهایی وارد شدند و ownership نوشتن تداخل نداشت | پاس |

### منابع فنی زنجیرهٔ ساخت

- [Android Gradle Managed Devices](https://developer.android.com/studio/test/managed-devices?hl=en)
- [Gradle distribution and wrapper checksums](https://gradle.org/release-checksums/)
- [Android 16 QPR2 SDK setup](https://developer.android.com/about/versions/16/qpr2/setup-sdk)
- [Google Play target API requirements](https://developer.android.com/google/play/requirements/target-sdk)

## شواهد و artifactها

### اسناد

- `docs/codex/2026-07-21-cinnamon-domain-correction/00-learning-focus-contract.md`
- `qa/runtime-proof/2026-07-22-learning-focus-v5-upgrade/README.md`
- `docs/release/RELEASE_BLOCKERS.md`

### شواهد تصویری

- `output/cinnamon-v5-final-fresh-home.png`
- `output/cinnamon-v5-final-upgrade-home.png`
- `output/cinnamon-v5-final-profile-cabinet.png`
- `output/cinnamon-v5-final-questboard.png`
- `output/cinnamon-v5-final-language-sprint.png`
- `output/cinnamon-v5-final-vocab-pairs.png`
- `output/cinnamon-v5-final-home-360dp.png`
- `output/cinnamon-v5-final-profile-cabinet-360dp-full.png`
- `output/cinnamon-v5-final-questboard-360dp.png`
- `output/cinnamon-v6-quests.png`
- `output/cinnamon-v6-quests.xml`
- `output/cinnamon-v6-quests-360dp.png`
- `output/cinnamon-v6-quests-360dp.xml`

### Performance

- `output/performance/startup-20260722-141349.json`
- `output/performance/startup-20260722-141501.json`
- `output/performance/startup-20260722-162125.json`
- `output/performance/startup-20260722-162211.json`

### CI و copy proof

- `.github/workflows/android.yml`
- `gradle/wrapper/gradle-wrapper.properties`
- `app/src/androidTest/java/com/cinnamon/app/ui/QuestboardWeeklyQuestAndroidTest.kt`
- `app/src/androidTest/java/com/cinnamon/app/ui/QuestLifecycleAndroidTest.kt`
- `app/src/androidTest/java/com/cinnamon/app/ui/LearningFocusLifecycleAndroidTest.kt`
- `app/src/androidTest/java/com/cinnamon/app/ui/GamificationDeviceTestState.kt`
- `app/src/androidTest/java/com/cinnamon/app/ui/StartupRecoveryMatrixAndroidTest.kt`
- `app/src/androidTest/java/com/cinnamon/app/ui/RepairLoopLifecycleAndroidTest.kt`
- `app/src/androidTest/java/com/cinnamon/app/ui/SavedItemReviewLifecycleAndroidTest.kt`
- `app/src/test/java/com/cinnamon/app/data/local/GamificationRepairEvidenceQueryTest.kt`
- `app/src/main/java/com/cinnamon/app/ui/screens/home/StartupCheckpointPresentation.kt`
- `app/src/test/java/com/cinnamon/app/ui/screens/home/StartupCheckpointPresentationTest.kt`
- `app/src/test/java/com/cinnamon/app/ui/screens/home/StartupCheckpointScreenshotTest.kt`
- `scripts/verify_gamification_process_restart.ps1`
- `scripts/verify_quest_process_restart.ps1`
- `scripts/verify_learning_focus_process_restart.ps1`
- `qa/runtime-proof/quest-process-restart-20260722-135016.txt`
- `qa/runtime-proof/learning-focus-process-restart-20260722-134918.txt`
- `qa/runtime-proof/repair-loop-isolated-avd-20260722-211211.txt`
- `qa/runtime-proof/daily-repair-quest-isolated-avd-20260722-213000.txt`
- `output/cinnamon-v7-startup-recovery-360dp.png`
- `qa/agent-notes/questboard-scope-label-review-avalai.md`

## نتیجهٔ نهایی

Cinnamon اکنون یک محصول یادگیری واقعی با هستهٔ داده‌ای بسیار سالم، گیمیفیکیشن صادقانه و شخصیت بصری مشخص است. مهم‌ترین پیشرفت این است که انگیزش از evidence می‌آید، نه از صفحه‌آرایی یا XP جعلی. Quest هفتگی اکنون assignment، persistence، progress، expiry، Claim یک‌بارمصرف، UI واقعی و process-restart proof دارد؛ `Daily Repair One` نیز از خطای واقعی آغاز، فقط با repair لینک‌شده کامل و با claim ده XP یک‌بارمصرف تسویه می‌شود. Confusable Precision بازی را از match مکانیکی به تشخیص واقعی و بازگشت ۲۴ساعته تبدیل می‌کند، Context Builder یک واژه را تنها پس از کاربرد صحیح در sentence authored وارد پیشرفت می‌کند، Delayed Recall فقط با مرور موفق پس از ۷۲ ساعت evidence می‌سازد و Gentle Return فقط از یک session واقعی سه‌اقدامی پس از هفت روز سکوتِ ledger خلق می‌شود. Kept and Learned نیز bookmark را به یک وعدهٔ قابل‌پیگیری تبدیل می‌کند: همان واژه باید در حالت ذخیره‌شده، دست‌کم یک ساعت بعد، واقعاً با پاسخ موفق مرور شود. Learning Focus نیز با evidence واقعی باز می‌شود، از UI یک‌بار انتخاب می‌شود، XP جعلی نمی‌دهد و پس از process restart پایدار می‌ماند. Repair Loop نیز اشتباه را از تجربه‌ای خاموش به یک اصلاح موعددار، لینک‌شده، انسانی و غیرقابل‌farm تبدیل می‌کند. Recovery Checkpoint نیز شکست startup را به شش مسیر انسانی، شفاف و قابل‌اقدام تبدیل کرده است. مهاجرت، cold restart، نصب پاک، responsive UI و APK فعلی نیز proof دارند.

بااین‌حال، perfection کامل هنوز اعلام نمی‌شود. بسته‌شدن backend/auth/release P0ها، گسترش evidence producerها، fault injection واقعی برای corruption/AI ambiguity، performance دستگاه واقعی و continuity هویت منتخب لازم است. مسیر درست از اینجا افزایش خط‌کد کور نیست؛ بستن همین شکاف‌ها با proof و بدون تخریب state فعلی است.
