"""Generate Cinnamon's current Persian RTL perfection review as a styled PDF."""

from __future__ import annotations

from html import escape
from pathlib import Path
import subprocess

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "output" / "pdf" / "Cinnamon_Perfection_Review_2026-07-22_FA.pdf"
TEMP = ROOT / "tmp" / "pdfs"
HTML_OUTPUT = TEMP / "cinnamon-perfection-review-2026-07-22-fa.html"
CHROME = Path(r"C:\Program Files\Google\Chrome\Application\chrome.exe")

EVIDENCE = {
    "home": ROOT / "output" / "cinnamon-v5-final-fresh-home.png",
    "upgrade": ROOT / "output" / "cinnamon-v5-final-upgrade-home.png",
    "profile": ROOT / "output" / "cinnamon-v5-final-profile-cabinet.png",
    "questboard": ROOT / "output" / "cinnamon-v6-quests.png",
    "sprint": ROOT / "output" / "cinnamon-v5-final-language-sprint.png",
    "pairs": ROOT / "output" / "cinnamon-v5-final-vocab-pairs.png",
    "home360": ROOT / "output" / "cinnamon-v5-final-home-360dp.png",
    "profile360": ROOT / "output" / "cinnamon-v5-final-profile-cabinet-360dp-full.png",
    "quest360": ROOT / "output" / "cinnamon-v6-quests-360dp.png",
}


def validate_inputs() -> dict[str, str]:
    missing = [path for path in EVIDENCE.values() if not path.is_file()]
    if missing:
        raise RuntimeError("Missing runtime evidence: " + ", ".join(map(str, missing)))
    for path in EVIDENCE.values():
        with Image.open(path) as image:
            if image.width < 700 or image.height < 1200:
                raise RuntimeError(f"Evidence image is unexpectedly small: {path} {image.size}")
    return {key: path.as_uri() for key, path in EVIDENCE.items()}


def bullets(items: list[str], css_class: str = "") -> str:
    class_attr = f' class="{css_class}"' if css_class else ""
    return f"<ul{class_attr}>" + "".join(
        f"<li>{escape(item)}</li>" for item in items
    ) + "</ul>"


def table(headers: list[str], rows: list[tuple[str, ...]], css_class: str = "") -> str:
    class_attr = f' class="{css_class}"' if css_class else ""
    head = "".join(f"<th>{escape(item)}</th>" for item in headers)
    body = "".join(
        "<tr>" + "".join(f"<td>{escape(item)}</td>" for item in row) + "</tr>"
        for row in rows
    )
    return f"<table{class_attr}><thead><tr>{head}</tr></thead><tbody>{body}</tbody></table>"


def page(number: int, content: str, css_class: str = "") -> str:
    classes = f"page page-{number} {css_class}".strip()
    return (
        f'<section class="{classes}">{content}'
        '<footer><span>CINNAMON · PERFECTION REVIEW</span>'
        f"<span>{number:02d}</span></footer></section>"
    )


def figure(uri: str, caption: str, css_class: str = "") -> str:
    return (
        f'<figure class="{css_class}"><img src="{uri}" alt="{escape(caption)}" />'
        f"<figcaption>{escape(caption)}</figcaption></figure>"
    )


def build_html(images: dict[str, str]) -> str:
    pages = [
        page(1, """
            <div class="cover-grid"></div>
            <div class="cover-mark">C</div>
            <div class="eyebrow">CINNAMON · EVIDENCE-LED PRODUCT REVIEW</div>
            <h1 class="cover-title">نقد و نقشهٔ پرفکشن<br/>Cinnamon</h1>
            <p class="cover-lead">گیمیفیکیشن واقعی، هویت گرم و اثبات فنی - بدون XP جعلی و بدون نسبت‌دادن Campaign به محصول</p>
            <div class="cover-verdicts">
                <div><b>هستهٔ داخلی</b><strong>۹٫۰ / ۱۰</strong><span>عبور مشروط</span></div>
                <div class="blocked"><b>انتشار عمومی</b><strong>BLOCK</strong><span>گیت‌های بیرونی باز</span></div>
            </div>
            <div class="cover-proof">Room v5 · ۱۵۱/۱۵۱ unit · ۱۰/۱۰ AVD پیشین · Context + Delayed + Gentle + Kept lifecycle build · Quest + Focus restart</div>
            <p class="cover-date">۳۱ تیر ۱۴۰۵ · ۲۲ ژوئیهٔ ۲۰۲۶</p>
        """, "cover"),
        page(2, """
            <div class="kicker">01 · حکم دقیق</div>
            <h1>از نمونهٔ ساده عبور کرده؛ از پرفکشن کامل هنوز نه</h1>
            <p class="lead">Cinnamon اکنون یک اپ یادگیری واقعی با persistence، ledger، مهاجرت، Focus، Quest و Cabinet است. عدد خط‌کد دیگر ۳هزار نیست؛ بااین‌حال عمق محصول با proof سنجیده می‌شود، نه حجم فایل.</p>
            <div class="metric-grid">
                <div><strong>۲۷٬۰۱۶</strong><span>خط Kotlin</span></div>
                <div><strong>۵۰٬۱۱۰</strong><span>خط مهندسی و داده</span></div>
                <div><strong>۱۵۱</strong><span>unit پاس</span></div>
                <div><strong>۱۰</strong><span>device test پاس</span></div>
            </div>
        """ + table(
            ["حوزه", "امتیاز", "حکم"],
            [
                ("داده و مهاجرت", "۹٫۶", "قوی و اثبات‌شده"),
                ("Reward Engine", "۹٫۴", "اتمیک، snapshot و ضدتقلب"),
                ("UX و هویت جاری", "۸٫۵", "گرم و متمایز"),
                ("گیمیفیکیشن قابل‌استفاده", "۹٫۲", "Repair، Context، Delayed، Gentle و Kept واقعی؛ breadth ناقص"),
                ("Performance", "۷٫۶", "قابل‌بهبود"),
                ("Backend و Release", "۴٫۰", "مسدود"),
            ],
            "score",
        ) + """
            <div class="callout amber"><b>حکم:</b> checkpoint داخلی جدی است؛ release public هنوز مجاز نیست.</div>
        """),
        page(3, """
            <div class="kicker">02 · اصلاح دامنه و معماری</div>
            <h1>مدل فعال، یادگیری است - نه Campaign</h1>
            <div class="two-col">
                <div class="panel teal"><h2>واژگان canonical</h2>""" + bullets([
                    "Foundation Plan",
                    "Learning Focus",
                    "Language Precision",
                    "Recall Range",
                    "Learning Questboard",
                    "Language Sprint و Vocab Pairs",
                ]) + """</div>
                <div class="panel rose"><h2>مرز legacy</h2>""" + bullets([
                    "فقط SQL فریز‌شدهٔ مهاجرت",
                    "adapter محدود سازگاری",
                    "fixture و snapshot تاریخی",
                    "صفر label یا event جدید قدیمی",
                    "صفر مدل دامنهٔ فعال قدیمی",
                ]) + """</div>
            </div>
            <h2>جریان authority</h2>
            <div class="flow"><span>عمل معتبر</span><i>←</i><span>Event ledger</span><i>←</i><span>Room transaction</span><i>←</i><span>Projection</span><i>←</i><span>Receipt</span></div>
            <div class="callout mint">UI نمی‌تواند XP بنویسد. بازکردن صفحه و animation هیچ پاداشی تولید نمی‌کند.</div>
            <h2>Learning Focus</h2>
        """ + table(
            ["Focus", "هویت", "قرارداد"],
            [
                ("Language Precision", "دقت و کاربرد", "انتخاب یک‌بار، بدون XP انتخاب"),
                ("Recall Range", "وسعت recall", "انتخاب اتمیک، conflict rollback"),
            ],
        )),
        page(4, f"""
            <div class="kicker">03 · اجرای واقعی</div>
            <h1>هویت و Foundation روی APK نهایی</h1>
            <div class="phone-pair">
                {figure(images['home'], 'نصب پاک v5 · Cinnamon و Foundation canonical')}
                {figure(images['upgrade'], 'مهاجرت v4 · ۳۴ XP و Foundation حفظ‌شده')}
            </div>
            <div class="proof-row"><span>Fresh: ۰ transaction</span><span>Upgrade: ۷ event</span><span>Cold restart: ثابت</span></div>
        """),
        page(5, f"""
            <div class="kicker">04 · گیمیفیکیشن دیداری</div>
            <h1>Cabinet و Questboard خشک نیستند</h1>
            <div class="phone-pair">
                {figure(images['profile'], 'Achievement Cabinet · evidence واقعی')}
                {figure(images['questboard'], 'Quests · daily/weekly از projection ذخیره‌شده')}
            </div>
            <div class="proof-row"><span>بدون badge جعلی</span><span>selected semantics</span><span>target دقیق ۴۸dp</span></div>
        """),
        page(6, f"""
            <div class="kicker">05 · بازی و nomenclature</div>
            <h1>اسم، مکانیک و وعده یکسان‌اند</h1>
            <div class="phone-pair compact-phones">
                {figure(images['sprint'], 'Language Sprint · سه prompt در یک round')}
                {figure(images['pairs'], 'Vocab Pairs · pair matching واقعی')}
            </div>
            <div class="callout teal"><b>اصلاح مهم:</b> Weekly Challenge تکرارشونده و Match-3 غیرواقعی حذف شدند. tab navigation هیچ transaction، achievement یا receipt نساخت.</div>
            <div class="catalog-strip"><span>۱۲ Achievement</span><span>۱۲ Quest</span><span>۱۵ Reward</span><span>۴ Rarity</span><span>۶ Level</span></div>
        """),
        page(7, """
            <div class="kicker">06 · Room v5 و حفظ داده</div>
            <h1>مهاجرت باید دقیق باشد، نه فقط موفق</h1>
            <p class="lead">DB، WAL و SHM واقعی v4 restore شدند. APK نهایی آن‌ها را باز کرد، cold restart شد و هر مقدار حساس دوباره query شد.</p>
        """ + table(
            ["شاخص", "v4", "v5", "cold"],
            [
                ("event", "۷", "۷", "۷"),
                ("transaction", "۴", "۴", "۴"),
                ("journey", "۲", "۲", "۲"),
                ("stage", "۷", "۷", "۷"),
                ("selection", "۱", "۱", "۱"),
                ("XP", "۳۴", "۳۴", "۳۴"),
                ("schema", "۴", "۵", "۵"),
            ],
            "migration",
        ) + """
            <div class="two-col short">
                <div class="panel mint"><h2>نصب تازه</h2>""" + bullets([
                    "یک journey.foundation.plan",
                    "صفر identity قدیمی",
                    "صفر selection جعلی",
                    "صفر reward transaction",
                ]) + """</div>
                <div class="panel amber"><h2>مهاجرت</h2>""" + bullets([
                    "legacy identity حفظ شد",
                    "canonical duplicate ساخته نشد",
                    "جدول قبلی حذف شد",
                    "integrity و FK سالم",
                ]) + """</div>
            </div>
        """),
        page(8, """
            <div class="kicker">07 · Reward Engine</div>
            <h1>انگیزش از evidence می‌آید</h1>
            <div class="two-col">
                <div class="panel rose"><h2>ضدتقلب</h2>""" + bullets([
                    "idempotency در event و transaction",
                    "daily cap برای رفتار farmable",
                    "collision rollback کامل",
                    "receipt بدون اختیار مالی",
                    "reward و evidence window هنگام assignment فریز می‌شوند",
                ]) + """</div>
                <div class="panel teal"><h2>صداقت catalog</h2>""" + bullets([
                    "۱۲ تعریف achievement موجود است",
                    "۹ معیار producer و resolver کامل دارند",
                    "۳ مورد بدون evidence پنهان می‌مانند",
                    "Repair، Delayed، Gentle و Kept دوباره query می‌شوند",
                    "XP هرگز معیار mastery نیست",
                    "rarity فقط با رنگ بیان نمی‌شود",
                ]) + """</div>
            </div>
            <div class="callout amber"><b>P1:</b> producer واقعی برای calibration و etymology اضافه شود؛ نه progress حدسی.</div>
            <h2>Quest breadth</h2>
            <p>پنج قرارداد Quest اجرایی اکنون فعال‌اند: daily due-review، Daily Repair One، Context Builder، Confusable Precision و weekly durable mastery. Delayed Recall فقط مرور موفقِ همان واژه پس از ۷۲ ساعت را می‌پذیرد؛ Gentle Return نیز فقط پایان یک game session سه‌اقدامی پس از هفت روز سکوت واقعی را به achievement تبدیل می‌کند. Kept and Learned نیز bookmark همان واژه، یک ساعت فاصله و مرور موفق را با query رابطه‌ای دوباره اثبات می‌کند. Context فقط از پاسخ صحیح Cloze به entry واقعی ساخته می‌شود و Confusable فقط با لینک دقیقِ تلاش و بازگشت ۲۴ساعته کامل می‌شود.</p>
        """),
        page(9, """
            <div class="kicker">08 · Backend، خطا و حریم خصوصی</div>
            <h1>Fail-closed درست است؛ recovery هم انسانی شد</h1>
            <div class="two-col short">
                <div class="panel teal"><h2>کلاینت امن</h2>""" + bullets([
                    "فقط gateway HTTPS مالک محصول",
                    "بدون provider key در APK",
                    "request ID و idempotency key",
                    "error body محدود به ۱۶KB",
                    "متن خام provider وارد UI نمی‌شود",
                    "Recovery Checkpoint برای هر ۶ stage",
                ]) + """</div>
                <div class="panel rose"><h2>گیت production</h2>""" + bullets([
                    "session token کوتاه‌عمر",
                    "auth و rate limit",
                    "abuse control و secret manager",
                    "observability redacted",
                    "privacy/retention policy",
                    "staging E2E برای retry ambiguity",
                ]) + """</div>
            </div>
            <div class="callout mint">Lexicon دیگر پیام generic ندارد؛ متن، CTA، حفظ progress، live-region و ماتریس Compose روی AVD و ۳۶۰dp اثبات شدند.</div>
            <div class="callout amber">ProductSessionTokenProvider عمداً null است؛ live coaching فعلی غیرفعال و fallback آفلاین صادقانه است.</div>
            <p><b>Data recovery:</b> allowBackup=false حریم خصوصی را حفظ می‌کند، اما uninstall یا device loss می‌تواند progress را از بین ببرد. export رمزنگاری‌شده یا account-bound restore یک P1 واقعی است.</p>
        """),
        page(10, """
            <div class="kicker">09 · Performance واقعی</div>
            <h1>debug کند است؛ benchmark قابل‌قبول اما ممتاز نیست</h1>
        """ + table(
            ["Build", "Runs", "Total p50", "Total p95", "Prepare p50", "Prepare p95"],
            [
                ("debug", "۵", "۱۲٬۷۵۸ms", "۱۳٬۱۱۳ms", "۲٬۸۴۴ms", "۳٬۱۱۳ms"),
                ("benchmark", "۵", "۳٬۶۸۷ms", "۳٬۷۸۳ms", "۹۵۸ms", "۹۹۳ms"),
            ],
            "performance",
        ) + """
            <div class="bar-chart">
                <div><b>Debug p50</b><span style="width:100%">۱۲٫۷۶s</span></div>
                <div><b>Benchmark p50</b><span style="width:29%">۳٫۶۹s</span></div>
                <div><b>Prepare p50</b><span style="width:8%">۰٫۹۶s</span></div>
            </div>
            <div class="callout mint">Baseline Profile: ۲٬۶۴۵ rule و profile بسته‌بندی‌شده داخل APK بنچمارک.</div>
            <div class="callout amber"><b>P1:</b> parse کاتالوگ حدود ۶۲۷ تا ۷۰۱ms؛ cache/compile و Macrobenchmark روی دستگاه فیزیکی لازم است.</div>
            <p><b>Diagnostic v6:</b> ۵/۵ process-cold با p50=۱٬۲۱۱ms و reconciliation p50=۹ms؛ ۳/۳ fresh-data با p50=۱٬۱۹۶ms و reconciliation p50=۲۵ms. این run با baseline بالا هم‌شرایط نیست و ادعای بهبود مستقیم محسوب نمی‌شود.</p>
        """),
        page(11, f"""
            <div class="kicker">10 · Responsive و Accessibility</div>
            <h1>عرض ۳۶۰dp فقط ادعا نیست</h1>
            <div class="phone-trio">
                {figure(images['home360'], 'Home · ۳۶۰dp')}
                {figure(images['profile360'], 'Cabinet · ۳۶۰dp')}
                {figure(images['quest360'], 'Quest هفتگی · ۳۶۰dp')}
            </div>
            <div class="proof-row"><span>بدون overlap بحرانی</span><span>۴۸dp targets</span><span>selected=true</span></div>
            <p class="small"><b>باقی‌مانده:</b> TalkBack walkthrough، font scale ۲٫۰، switch access، landscape/tablet و reduced-motion matrix.</p>
        """),
        page(12, """
            <div class="kicker">11 · Build، test و maintainability</div>
            <h1>هستهٔ آزموده قوی است؛ device proof چهار مسیر تازه باقی مانده</h1>
            <div class="metric-grid">
                <div><strong>۳۵</strong><span>unit suite</span></div>
                <div><strong>۱۵۱/۱۵۱</strong><span>unit pass</span></div>
                <div><strong>۱۰/۱۰</strong><span>AVD ایزوله</span></div>
                <div><strong>0</strong><span>lint error</span></div>
            </div>
        """ + table(
            ["فایل بزرگ", "خط"],
            [
                ("GamificationHubScreen.kt", "۱٬۸۸۰"),
                ("GamificationDao.kt", "۲٬۰۶۶"),
                ("ClinicalSimLabsScreen.kt", "۱٬۶۱۰"),
                ("NativeFluencyPlaygroundScreen.kt", "۱٬۵۰۶"),
                ("RoleplayChatScreen.kt", "۱٬۱۱۸"),
                ("HomeScreen.kt", "۱٬۱۱۶"),
            ],
        ) + """
            <div class="callout mint">Quest با PID تازه از ۷۵۳۴ به ۷۶۴۳، Claim و XP را حفظ کرد؛ Focus نیز از ۷۳۲۱ به ۷۴۱۵، انتخاب immutable و XP صفر را بازیابی کرد. Daily Repair One هم با پنج اصلاح متمایز، ۴۰ XP repair/achievement، claim دقیق ۱۰ XP و duplicate-claim ردشده روی AVD ایزوله بسته شد. چهار lifecycle Context، Delayed، Gentle و Kept compile شده‌اند؛ اجرای جدیدشان device proof محسوب نمی‌شود.</div>
            <div class="callout amber"><b>P1:</b> refactor مرحله‌ای و behavior-preserving؛ transaction owner شکسته نشود. fault injection واقعی برای corruption و AI ambiguity هنوز باز است.</div>
        """),
        page(13, """
            <div class="kicker">12 · نقشهٔ رسیدن به ۱۰/۱۰</div>
            <h1>P0 بیرونی، سپس P1 محصولی</h1>
            <h2>P0 - پیش از release</h2>
        """ + table(
            ["مالک", "گیت"],
            [
                ("Security", "rotation و history remediation"),
                ("Backend", "gateway، auth، rate limit، privacy"),
                ("Release", "signing lineage، version و upgrade test"),
                ("Brand", "master icon و identity canonical"),
                ("Product/Legal", "clinical/editorial و policy approval"),
            ],
        ) + """
            <h2>P1 - جهش کیفیت</h2>
        """ + bullets([
            "مسیرهای evidence و Questهای باقی‌مانده، پس از Daily Repair، Confusable، Context، Delayed، Gentle و Kept",
            "Macrobenchmark و بهینه‌سازی parse کاتالوگ",
            "fault injection برای corruption و AI delivery ambiguity",
            "sync/restore یا export رمزنگاری‌شده",
            "refactor فایل‌های ۱۰۰۰+ خطی و registry یکپارچهٔ string",
            "TalkBack/font-scale/tablet matrix",
            "بازیابی کانسپت منتخب و اجرای mascot family همان کانسپت",
        ], "roadmap") + """
            <div class="callout rose">افزایش خط‌کد کور هدف نیست؛ هر قابلیت جدید باید state، failure، accessibility و proof داشته باشد.</div>
        """),
        page(14, """
            <div class="kicker">13 · جهت خلاقه</div>
            <h1>چطور جذاب‌تر شود، بدون تبدیل‌شدن به اسباب‌بازی</h1>
            <div class="idea-grid">
                <div><b>Cabinet زنده</b><span>artifact واقعی به‌جای badge تخت</span></div>
                <div><b>Momentum انسانی</b><span>ریتم هفتگی و comeback، نه streak-shaming</span></div>
                <div><b>Focus identity</b><span>texture و microcopy متمایز برای دو Focus</span></div>
                <div><b>Receipt معنادار</b><span>evidence، چرایی پاداش و قدم بعد</span></div>
                <div><b>Quest deck تطبیقی</b><span>due queue، availability و history</span></div>
                <div><b>Repair as play</b><span>اشتباه به مادهٔ پیشرفت تبدیل شود</span></div>
            </div>
            <div class="mascot-note"><b>Mascot:</b> باید coach باشد، نه decoration. exact concept منتخب در workspace فعلی ثبت نشده؛ تا بازیابی continuity، هویت تازه‌ای جعل نمی‌شود.</div>
        """),
        page(15, """
            <div class="kicker">14 · انطباق اسکیل‌ها</div>
            <h1>پروتکل‌ها به تصمیم تبدیل شدند</h1>
            <div class="protocol-grid">
                <div class="panel teal"><h2>Fidelity و IA</h2>""" + bullets([
                    "copy: reference binding به Questboard موجود",
                    "style: screenshot و bounds در ۵۴۰/۳۶۰dp",
                    "anatomy + modernize: tab واحد Quests",
                    "string: نقد AvalAI و انتخاب label نهایی",
                ]) + """</div>
                <div class="panel rose"><h2>Authority و داده</h2>""" + bullets([
                    "gamify + reward-engine: event و claim اتمیک",
                    "achievement-catalog: فقط criterion قابل‌اثبات",
                    "dataman: Room v5 و evidence window دقیق",
                    "backend + errors + function: fail-closed end-to-end",
                ]) + """</div>
                <div class="panel amber"><h2>Execution و proof</h2>""" + bullets([
                    "perfect: سه چرخهٔ adversarial مادی",
                    "orchestrator + automate: مسیر کامل تا runtime",
                    "multi-agent: ممیزی مستقل بدون تداخل نوشتن",
                    "actions + performance: CI، checksum و timing",
                ]) + """</div>
                <div class="panel mint"><h2>Continuity gate</h2>""" + bullets([
                    "gamify-mascot-studio: هویت تازه جعل نشد",
                    "artifact کانسپت منتخب باید canonical بازیابی شود",
                    "backend/auth/signing همچنان گیت بیرونی release است",
                    "گزارش کامل در Markdown، با evidence path دقیق",
                ]) + """</div>
            </div>
        """),
        page(16, """
            <div class="final-mark">✓</div>
            <div class="kicker">حکم نهایی</div>
            <h1>یک محصول جدی، با مسیر روشن تا flagship</h1>
            <p class="final-lead">Cinnamon اکنون گرم، متمایز، گیمیفای‌شده و از نظر داده قابل‌اعتماد است. مهم‌ترین پیروزی این است که انگیزش از evidence واقعی می‌آید و migration کاربر قدیمی را قربانی طراحی تازه نمی‌کند.</p>
            <div class="final-split">
                <div><b>همین حالا پذیرفته</b>Room v5، Reward Engine، Foundation، Focus immutable، Quest claim، Daily Repair لینک‌شده، Context Builder، Confusable Precision ۲۴ساعته، Delayed Recall ۷۲ساعته، Gentle Return هفت‌روزه، Kept and Learned یک‌ساعته، دو process restart، Recovery matrix، Cabinet و ۳۶۰dp</div>
                <div><b>هنوز باز</b>Backend/Auth، signing، restore، breadth catalog، breadth UI test، performance دستگاه واقعی و mascot continuity</div>
            </div>
            <div class="final-verdict">هستهٔ داخلی: ۹٫۰/۱۰ · انتشار عمومی: BLOCK</div>
            <p class="small center">منبع کامل: گزارش Markdown همین release و شواهد runtime Quest / Focus / Repair</p>
        """, "final"),
    ]

    css = r"""
@page { size: A4; margin: 0; }
* { box-sizing: border-box; }
html, body { margin: 0; padding: 0; direction: rtl; background: #140e0c; color: #f7eade; font-family: Tahoma, Arial, sans-serif; }
.page { width: 210mm; height: 297mm; padding: 16mm 15mm 14mm; position: relative; overflow: hidden; page-break-after: always; background: radial-gradient(circle at 90% 8%, rgba(244,162,97,.12), transparent 32%), #17100e; }
.page:last-child { page-break-after: auto; }
footer { position: absolute; bottom: 6mm; left: 15mm; right: 15mm; display: flex; direction: ltr; justify-content: space-between; color: #8f7c71; font-size: 8pt; letter-spacing: .08em; border-top: 1px solid #392a24; padding-top: 3mm; }
h1 { font-size: 25pt; line-height: 1.35; margin: 1.5mm 0 5mm; color: #fff4e8; }
h2 { font-size: 14pt; margin: 5mm 0 3mm; color: #f4a261; }
p, li, td, th { font-size: 10.2pt; line-height: 1.75; }
p { margin: 2mm 0; }
ul { margin: 2mm 0; padding-right: 6mm; }
li { margin: 1mm 0; }
.kicker, .eyebrow { color: #7dd3bd; font-weight: 800; font-size: 9pt; letter-spacing: .06em; }
.lead { color: #ddcbbf; font-size: 12pt; line-height: 1.8; }
.small { font-size: 8.8pt; color: #bca99d; }
.center { text-align: center; }
.cover { padding: 21mm 17mm; background: radial-gradient(circle at 18% 18%, rgba(125,211,189,.18), transparent 30%), radial-gradient(circle at 88% 72%, rgba(139,56,83,.28), transparent 42%), #160e0c; }
.cover-grid { position: absolute; inset: 0; opacity: .08; background-image: linear-gradient(#f4a261 1px, transparent 1px), linear-gradient(90deg,#f4a261 1px,transparent 1px); background-size: 18mm 18mm; transform: rotate(-8deg) scale(1.2); }
.cover-mark { position: relative; width: 34mm; height: 34mm; border-radius: 10mm; display: grid; place-items: center; margin: 8mm 0 16mm auto; background: linear-gradient(145deg,#ffc39a,#f4a261); color: #5e2f13; font-size: 34pt; font-weight: 900; box-shadow: 0 5mm 18mm rgba(244,162,97,.22); }
.cover-title { position: relative; font-size: 38pt; line-height: 1.25; margin-top: 8mm; }
.cover-lead { position: relative; width: 86%; font-size: 15pt; line-height: 1.9; color: #dfc9bb; }
.cover-verdicts { position: relative; display: grid; grid-template-columns: 1fr 1fr; gap: 6mm; margin-top: 16mm; }
.cover-verdicts div { background: #21382f; border: 1px solid #4a8c7b; border-radius: 6mm; padding: 6mm; display: grid; gap: 2mm; }
.cover-verdicts .blocked { background: #3b1d28; border-color: #8b3853; }
.cover-verdicts b { color: #c7b3a7; font-size: 10pt; }
.cover-verdicts strong { font-size: 23pt; color: #fff2e6; direction: ltr; }
.cover-verdicts span { color: #85d8c1; }
.cover-verdicts .blocked span { color: #f4a2bb; }
.cover-proof { position: relative; margin-top: 14mm; padding: 5mm; border: 1px solid #51392e; border-radius: 4mm; color: #f4a261; direction: ltr; text-align: center; font-size: 10pt; }
.cover-date { position: absolute; bottom: 20mm; right: 17mm; color: #9e8a7d; }
.metric-grid { display: grid; grid-template-columns: repeat(4,1fr); gap: 3mm; margin: 5mm 0; }
.metric-grid div { background: #241915; border: 1px solid #3c2b25; border-radius: 4mm; padding: 4mm 2mm; text-align: center; }
.metric-grid strong { display: block; color: #f4a261; font-size: 18pt; direction: ltr; }
.metric-grid span { color: #b9a69a; font-size: 8.5pt; }
table { width: 100%; border-collapse: separate; border-spacing: 0; margin: 4mm 0; border: 1px solid #453129; border-radius: 4mm; overflow: hidden; }
th { background: #2b1d18; color: #7dd3bd; text-align: right; padding: 2.4mm 3mm; }
td { padding: 2.3mm 3mm; color: #ddcbbf; border-top: 1px solid #34251f; }
tr:nth-child(even) td { background: rgba(255,255,255,.018); }
.score td:nth-child(2), .migration td:not(:first-child), .performance td:not(:first-child) { direction: ltr; text-align: center; font-weight: 700; color: #f4a261; }
.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 5mm; margin: 4mm 0; }
.two-col.short .panel { min-height: 55mm; }
.panel { padding: 5mm; border-radius: 5mm; background: #211713; border: 1px solid #3d2b24; min-height: 89mm; }
.panel h2 { margin-top: 0; }
.panel.teal { border-color: #3f7768; background: linear-gradient(145deg,#173028,#201816); }
.panel.rose { border-color: #7c3b50; background: linear-gradient(145deg,#3a1c27,#211513); }
.panel.mint { border-color: #4d7f70; }
.panel.amber { border-color: #8a5728; }
.flow { direction: rtl; display: flex; align-items: center; justify-content: space-between; gap: 2mm; margin: 5mm 0; }
.flow span { flex: 1; padding: 4mm 2mm; text-align: center; border-radius: 4mm; background: #2b211c; border: 1px solid #4b382f; color: #f3d6c1; font-size: 9pt; }
.flow i { color: #f4a261; font-style: normal; }
.callout { padding: 4mm 5mm; border-radius: 4mm; margin: 4mm 0; border-right: 3px solid; font-size: 10pt; line-height: 1.75; }
.callout.amber { background: #3a2717; border-color: #f4a261; }
.callout.mint, .callout.teal { background: #183129; border-color: #7dd3bd; }
.callout.rose { background: #351b24; border-color: #d86f91; }
.phone-pair { direction: ltr; display: grid; grid-template-columns: 1fr 1fr; gap: 6mm; height: 215mm; }
figure { margin: 0; min-width: 0; display: flex; flex-direction: column; align-items: center; }
figure img { width: 100%; height: 198mm; object-fit: contain; object-position: top center; border-radius: 5mm; background: #0f0b09; border: 1px solid #3f2e27; }
figcaption { direction: rtl; margin-top: 2mm; color: #bba79a; font-size: 8.5pt; text-align: center; }
.compact-phones { height: 190mm; }
.compact-phones figure img { height: 174mm; }
.proof-row, .catalog-strip { display: flex; justify-content: space-between; gap: 2mm; margin-top: 4mm; }
.proof-row span, .catalog-strip span { flex: 1; background: #251a16; border: 1px solid #3e2d26; border-radius: 8mm; padding: 2.5mm; text-align: center; color: #7dd3bd; font-size: 8.3pt; }
.catalog-strip span { color: #f4a261; }
.bar-chart { margin: 8mm 0; direction: ltr; }
.bar-chart div { display: grid; grid-template-columns: 32mm 1fr; align-items: center; gap: 3mm; margin: 4mm 0; }
.bar-chart b { color: #bfa99b; font-size: 9pt; }
.bar-chart span { display: block; min-width: 20mm; padding: 2.5mm 3mm; border-radius: 0 4mm 4mm 0; background: linear-gradient(90deg,#8b3853,#f4a261); color: #1b100d; font-weight: 800; }
.phone-trio { direction: ltr; display: grid; grid-template-columns: repeat(3,1fr); gap: 4mm; height: 210mm; }
.phone-trio figure img { height: 193mm; }
.roadmap { columns: 2; column-gap: 8mm; }
.idea-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 5mm; margin-top: 8mm; }
.idea-grid div { min-height: 35mm; padding: 6mm; border-radius: 5mm; background: linear-gradient(145deg,#301e16,#1f1714); border: 1px solid #684525; }
.idea-grid b { display: block; color: #f4a261; font-size: 13pt; margin-bottom: 3mm; }
.idea-grid span { color: #cbb7aa; line-height: 1.7; }
.mascot-note { margin-top: 8mm; padding: 7mm; border-radius: 6mm; background: linear-gradient(135deg,#21382f,#3b1d28); border: 1px solid #639785; line-height: 1.9; }
.protocol-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 4mm; margin-top: 4mm; }
.protocol-grid .panel { min-height: 75mm; padding: 4mm 5mm; }
.protocol-grid .panel h2 { font-size: 12.5pt; margin-bottom: 2mm; }
.protocol-grid .panel li { font-size: 9pt; line-height: 1.55; margin: .7mm 0; }
.final { background: radial-gradient(circle at 50% 26%, rgba(244,162,97,.16), transparent 32%), #160f0d; text-align: center; padding-top: 24mm; }
.final-mark { width: 30mm; height: 30mm; margin: 0 auto 10mm; border-radius: 50%; display: grid; place-items: center; font-size: 28pt; color: #10251e; background: #7dd3bd; box-shadow: 0 0 22mm rgba(125,211,189,.16); }
.final h1 { font-size: 32pt; }
.final-lead { max-width: 155mm; margin: 8mm auto; font-size: 14pt; line-height: 2; color: #dcc8bb; }
.final-split { display: grid; grid-template-columns: 1fr 1fr; gap: 7mm; margin: 14mm 0; text-align: right; }
.final-split div { padding: 7mm; border-radius: 6mm; background: #211713; border: 1px solid #4a352b; line-height: 1.9; color: #cdb9ac; }
.final-split b { display: block; color: #7dd3bd; margin-bottom: 3mm; }
.final-verdict { margin: 10mm auto; padding: 6mm; border-radius: 6mm; background: linear-gradient(90deg,#22382f,#42202c); color: #fff0e4; font-size: 16pt; font-weight: 900; }
"""

    return f"""<!doctype html><html lang="fa" dir="rtl"><head><meta charset="utf-8"><style>{css}</style></head><body>{''.join(pages)}</body></html>"""


def main() -> None:
    if not CHROME.is_file():
        raise RuntimeError(f"Chrome was not found at {CHROME}")
    images = validate_inputs()
    TEMP.mkdir(parents=True, exist_ok=True)
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    HTML_OUTPUT.write_text(build_html(images), encoding="utf-8")
    subprocess.run(
        [
            str(CHROME),
            "--headless=new",
            "--disable-gpu",
            "--no-pdf-header-footer",
            "--allow-file-access-from-files",
            f"--print-to-pdf={OUTPUT}",
            HTML_OUTPUT.as_uri(),
        ],
        check=True,
        cwd=ROOT,
    )
    if not OUTPUT.is_file() or OUTPUT.stat().st_size < 100_000:
        raise RuntimeError(f"PDF output was not created correctly: {OUTPUT}")
    print(OUTPUT)


if __name__ == "__main__":
    main()
