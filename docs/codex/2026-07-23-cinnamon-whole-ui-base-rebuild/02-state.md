# وضعیت و دفتر تصمیم‌ها

- Current status: `active — canonical preview gate`
- Last updated: 2026-07-23
- Owner: Codex

## وضعیت فعلی

ممیزی مستقل، موجودی route/state، موجودی داده و بررسی شواهد بصری تکمیل شده‌اند. هیچ پیاده‌سازی مربوط به **whole UI base rebuild جدید** آغاز نشده است. worktree از قبل تغییرات گستردهٔ متعلق به کاربر/کار قبلی دارد و باید بدون reset، clean یا overwrite حفظ شود.

Data plane اکنون یک لایهٔ مرجع جدا نیز دارد:
`cinnamon.mesh.reference@2026.1` با ۵٬۷۴۲ descriptor از MeSH 2026.
این لایه جایگزین ۲۴۳ واژهٔ عمیق یادگیری نیست، learner state ندارد و با
کلید پایدار `meshUi` در Room v6 ذخیره می‌شود.
critical startup آن را مسدود نمی‌کند: در آخرین اجرای managed device، core در
۱٬۲۵۳ms آماده شد و Atlas در ۸٬۰۹۶ms به‌صورت پس‌زمینه آماده بود.

## تصمیم‌ها

| تصمیم | وضعیت | دلیل |
|---|---|---|
| `Cinnamon Questlands` جهت کاری جدید است | accepted by Automate authority | نزدیک‌ترین پیوند به spiral motif، Journey، mastery map و انرژی موردنظر |
| این تصمیم recovery انتخاب قبلی نیست | locked | شواهد قابل‌اثبات شناسهٔ قبلی را ثبت نکرده‌اند |
| preview قدیمی شمارهٔ ۱ فقط reference/mock است | locked | board آن production-ready، responsive یا state-complete نیست |
| هیچ Campaign domain در بازسازی وجود ندارد | locked | محصول Medical English Learning OS است |
| شروع mutation رابط پس از preview approval | hard gate | قرارداد Rebuild/Modernize/Style/Anatomy/Mascot |
| architecture افزایشی و reversible | locked | حفاظت از داده، route و dirty worktree |
| routeها typed می‌شوند و stringهای فعلی alias می‌مانند | proposed | not-found/recovery و refactor امن |
| world art مکمل live semantic UI است | locked | کنترل، داده، accessibility و متن حساس باید زنده بمانند |

## مانع جاری preview

ImageGen داخلی سه بار امتحان شد:

- دو فراخوانی با تصویر مرجع: network error
- یک فراخوانی مستقل: `401 Unauthorized` با `token_revoked`

طبق قرارداد ImageGen، fallback به CLI/API فقط پس از مجوز صریح کاربر و وجود `OPENAI_API_KEY` مجاز است. تا رفع OAuth یا صدور آن مجوز، ساخت preview canonical متوقف است و به همین دلیل UI جدید پیاده‌سازی نمی‌شود.

## ممیزی مکمل Jules

- Session: `sessions/10587640236032519966`
- URL: <https://jules.google.com/session/10587640236032519966>
- Scope: read-only architecture/theme/screens/state/coupling report
- PR: none
- Plan approval activity ثبت شده است، اما API در آخرین مشاهده هنوز `AWAITING_PLAN_APPROVAL` گزارش می‌کرد؛ نتیجهٔ Jules هنوز مبنای ادعای تکمیل نیست.

## Perfect cycle

| چرخه | نقص غالب | بهبود انتخاب‌شده | شاهد |
|---|---|---|---|
| ۱ | بازسازی می‌تواند داده/کورپوس واقعی را حذف کند | ممیزی فریز + قرارداد Room v5/corpus | validatorها، hashها و PDF ۲۰ صفحه‌ای |
| ۲ | هویت منتخب canonical قابل اثبات نیست | تصمیم کاری جدید Questlands با برچسب صریح | دفتر تصمیم و preview ledger |
| ۳ | preview specialist unavailable | توقف در گیت و حفظ scope غیرمخرب | خطای OAuth ثبت‌شده |
| ۴ | افزودن مستقیم هزاران واژه می‌توانست ID و SRS را جابه‌جا کند | Reference Atlas مستقل با stable MeSH UI و Room v6 | ۵٬۷۴۲ رکورد، validator، migration و tests |
| ۵ | Atlas چندثانیه‌ای می‌توانست کل اپ را در first run قفل کند | انتقال warmup به مسیر اختیاری پس از critical Ready | ۱٬۲۵۳ms critical Ready، دو تست Android 35 بدون failure |

## ممیزی دیتاست Jules

- Session: `sessions/15306253539687638977`
- URL: <https://jules.google.com/session/15306253539687638977>
- Scope: ممیزی read-only معماری، provenance، coverage، migration و performance توسعهٔ دیتاست
- State در آخرین مشاهده: `IN_PROGRESS`
- PR: none
