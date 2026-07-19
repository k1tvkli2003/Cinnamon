"""Build Cinnamon's evidence-led Persian RTL perfection report with Chrome."""

from __future__ import annotations

from html import escape
from pathlib import Path
import subprocess

from PIL import Image as PilImage


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "output" / "pdf" / "Cinnamon_Perfection_Gamification_Visual_Direction_FA.pdf"
TEMP = ROOT / "tmp" / "pdfs"
HTML_OUTPUT = TEMP / "cinnamon-perfection-report-fa.html"
CONCEPTS = (
    ROOT
    / "docs"
    / "codex"
    / "2026-07-18-cinnamon-perfection-gamification-and-visual-direction"
    / "assets"
    / "brand-concepts-v1"
)
CHROME = Path(r"C:\Program Files\Google\Chrome\Application\chrome.exe")
RUNTIME_IMAGES = [
    ROOT / "output" / "cinnamon-runtime-final-quest-assigned-20260718.png",
    ROOT / "output" / "cinnamon-runtime-final-quest-complete-20260718.png",
    ROOT / "output" / "cinnamon-runtime-final-quest-claimed-20260718.png",
    ROOT / "output" / "cinnamon-runtime-final-quest-replay-guard-20260718.png",
    ROOT / "output" / "cinnamon-runtime-journey-home-active-v3-20260718.png",
    ROOT / "output" / "cinnamon-runtime-journey-map-v3-20260718.png",
    ROOT / "output" / "cinnamon-runtime-journey-replay-guard-v3-20260718.png",
    ROOT / "output" / "cinnamon-runtime-v4-campaign-choice-20260719.png",
    ROOT / "output" / "cinnamon-runtime-v4-campaign-confirm-20260719.png",
    ROOT / "output" / "cinnamon-runtime-v4-campaign-cold-restored-20260719.png",
]


def make_contact_sheets() -> list[Path]:
    """Create deterministic project-bound sheets from the ten archived previews."""
    TEMP.mkdir(parents=True, exist_ok=True)
    files = sorted(CONCEPTS.glob("*.png"))
    if len(files) != 10:
        raise RuntimeError(f"Expected 10 concept images, found {len(files)} in {CONCEPTS}")

    cell_width, cell_height = 660, 490
    sheets: list[Path] = []
    for sheet_index, group in enumerate((files[:6], files[6:]), start=1):
        rows = (len(group) + 1) // 2
        sheet = PilImage.new("RGB", (cell_width * 2, cell_height * rows), "#17110e")
        for index, file_path in enumerate(group):
            source = PilImage.open(file_path).convert("RGB")
            source.thumbnail((cell_width - 24, cell_height - 24), PilImage.Resampling.LANCZOS)
            x = (index % 2) * cell_width + (cell_width - source.width) // 2
            y = (index // 2) * cell_height + (cell_height - source.height) // 2
            sheet.paste(source, (x, y))

        target = TEMP / f"cinnamon-brand-concepts-v1-contact-sheet-{sheet_index}.jpg"
        sheet.save(target, quality=88, optimize=True)
        sheets.append(target)
    return sheets


def validate_runtime_images() -> list[str]:
    missing = [path for path in RUNTIME_IMAGES if not path.is_file()]
    if missing:
        raise RuntimeError("Missing runtime evidence: " + ", ".join(map(str, missing)))
    return [path.as_uri() for path in RUNTIME_IMAGES]


def paragraph(value: str, css_class: str = "body") -> str:
    return f'<p class="{css_class}">{escape(value)}</p>'


def bullets(items: list[str], css_class: str = "") -> str:
    class_attribute = f' class="{css_class}"' if css_class else ""
    return f"<ul{class_attribute}>" + "".join(
        f"<li>{escape(item)}</li>" for item in items
    ) + "</ul>"


def html_table(headers: list[str], rows: list[tuple[str, ...]], css_class: str = "") -> str:
    class_attribute = f' class="{css_class}"' if css_class else ""
    head = "".join(f"<th>{escape(value)}</th>" for value in headers)
    body = "".join(
        "<tr>" + "".join(f"<td>{escape(value)}</td>" for value in row) + "</tr>"
        for row in rows
    )
    return f"<table{class_attribute}><thead><tr>{head}</tr></thead><tbody>{body}</tbody></table>"


def scorecard() -> str:
    return html_table(
        ["حوزه", "وضعیت", "حکم"],
        [
            ("تسویهٔ Quest/Achievement", "گذرانده", "اتمیک، idempotent و دارای replay guard"),
            ("Mission Pulse", "گذرانده", "چهار state واقعی و proof روی APK نهایی"),
            ("Journey v1", "گذرانده", "Room v3، چهار فصل persisted و runtime upgrade proof"),
            ("Campaign route v1", "گذرانده", "Room v4، انتخاب zero-XP و cold restore"),
            ("Build و آزمون", "گذرانده", "۱۰۴ JVM + سه instrumentation + lint بدون خطا"),
            ("Baseline Profile", "گذراندهٔ داخلی", "۲٬۶۴۵ rule متمرکز و بسته‌بندی‌شده"),
            ("هویت بصری", "continuity لازم", "انتخاب قبلی محفوظ؛ شناسهٔ canonical باید بازیابی شود"),
            ("انتشار عمومی", "مسدود", "چهار گیت امنیت، زیرساخت، امضا و محتوا بازند"),
        ],
        "scorecard",
    )


def loc_table() -> str:
    return html_table(
        ["بخش", "فایل", "خط فیزیکی"],
        [
            ("Kotlin/Java و build logic", "۹۷", "۲۳٬۹۳۶"),
            ("script/config/resource/data تألیفی", "۳۳", "۹٬۲۹۳"),
            ("تألیفی؛ بدون schema generated", "۱۳۰", "۳۳٬۲۲۹"),
            ("schemaهای generated Room", "۳", "۵٬۱۱۴"),
            ("جمع مهندسی و داده", "۱۳۳", "۳۸٬۳۴۳"),
            ("همراه Markdown", "۱۶۹", "۴۰٬۱۷۳"),
        ],
        "compact",
    )


def page(number: int, content: str, class_name: str = "") -> str:
    classes = f"page page-{number} {class_name}".strip()
    return (
        f'<section class="{classes}">{content}'
        '<footer><span>گزارش پرفکشن Cinnamon</span>'
        f"<span>{number}</span></footer></section>"
    )


def build_html(contact_sheets: list[Path], runtime_images: list[str]) -> str:
    sheets = [sheet.as_uri() for sheet in contact_sheets]
    (
        assigned,
        complete,
        claimed,
        replay,
        journey_home,
        journey_map,
        journey_replay,
        campaign_choice,
        campaign_confirm,
        campaign_restored,
    ) = runtime_images
    pages = [
        page(1, """
            <div class="cover-orbit orbit-one"></div>
            <div class="cover-orbit orbit-two"></div>
            <div class="cover-kicker">CINNAMON · PRODUCT CHECKPOINT</div>
            <h1 class="cover-title">گزارش پرفکشن سینامون</h1>
            <p class="cover-subtitle">از XP نمایشی تا Mission، Journey و Campaign نسخه‌دار با proof واقعی</p>
            <div class="cover-callout">
                <span class="pill success">هستهٔ داخلی: اثبات‌شده</span>
                <span class="pill danger">انتشار عمومی: BLOCK</span>
                <p>مسیر روزانه، Journey و Campaign دوشاخه روی APK نصب‌شده و دادهٔ ارتقایافته اجرا شده‌اند؛ اما امنیت بیرونی، زیرساخت، امضای release و تأیید محتوا همچنان گیت قطعی‌اند.</p>
            </div>
            <p class="cover-date">۲۸ تیر ۱۴۰۵ · ۱۹ ژوئیهٔ ۲۰۲۶</p>
            <div class="cover-bottom">گزارش بر پایهٔ source، Room integration، build، آزمون Android، trace کارایی، digest artifact و تصویر اجرای واقعی است. هیچ preview مفهومی به‌عنوان implementation جا زده نشده است.</div>
        """, "cover"),
        page(2, """
            <div class="page-kicker">01 · حکم و مقیاس</div>
            <h1>پروژه ۳هزار خط نیست؛ اما خط‌کد هم هدف نهایی نیست</h1>
            <p class="lead">شمارش filesystem با حذف build، cache، output و temp نشان می‌دهد حتی با کنارگذاشتن schemaهای generated، ۳۳٬۲۲۹ خط مهندسی و دادهٔ تألیفی داریم. جمع مهندسی/داده با schemaها ۳۸٬۳۴۳ و همراه مستندات ۴۰٬۱۷۳ خط است؛ Baseline Profile نیز جداگانه ۲٬۶۴۵ rule دارد.</p>
        """ + loc_table() + """
            <div class="insight">قانون مقیاس: هر قابلیت فقط وقتی واقعی است که state، persistence، failure/retry، accessibility، امنیت و proof داشته باشد؛ صفحه و decoration به‌تنهایی عمق سوپراپ نیست.</div>
            <h2>کارت وضعیت</h2>
        """ + scorecard()),
        page(3, """
            <div class="page-kicker">02 · موتور انگیزش</div>
            <h1>پاداش از یادگیری می‌آید، نه از لمس و انیمیشن</h1>
            <div class="flow"><b>عمل معتبر</b><span>←</span><b>LearningEvent</b><span>←</span><b>Room transaction</b><span>←</span><b>Quest / Journey / Campaign</b><span>←</span><b>Receipt</b></div>
            <div class="two-column">
                <div class="panel berry">
                    <h2>Settlement اتمیک</h2>
        """ + bullets([
            "event، XP، balance، summary، Quest/Achievement، Journey stage و receipt در یک transaction.",
            "collision یا payload متناقض کل عملیات را rollback می‌کند.",
            "unlock، settlement، Journey reconciliation و claim تکراری XP دوباره تولید نمی‌کنند.",
            "UI فقط projection و receipt را مصرف می‌کند؛ authority نوشتن XP ندارد.",
        ]) + """
                </div>
                <div class="panel teal">
                    <h2>Evidence واقعی</h2>
        """ + bullets([
            "مرور موفق distinct، itemهای due، mastery، study day و breadth از queryهای واقعی می‌آیند.",
            "Again مرور موفق، streak یا Quest progress جعل نمی‌کند.",
            "Quest فقط پس از سه مرور معتبر complete می‌شود.",
            "claim دستی تنها پس از completion و فقط یک بار پذیرفته می‌شود؛ stage قفل‌شده evidence زودرس را مصرف نمی‌کند.",
        ]) + """
                </div>
            </div>
            <h2>Mission Pulse در Home</h2>
            <div class="state-row"><span>Unassigned</span><span>Active</span><span>Complete</span><span>Claimed</span></div>
            <p>کارت مأموریت checkbox تزئینی نیست. عنوان، توضیح، XP، progress، CTA و قابلیت کلیک از state ذخیره‌شده می‌آیند و کنترل‌ها semantics و اندازهٔ لمس مناسب دارند.</p>
            <div class="test-strip"><b>آزمون‌های سخت:</b> atomic unlock · collision rollback · duplicate · one-time claim · Journey replay · zero-XP campaign choice · route conflict</div>
        """),
        page(4, f"""
            <div class="page-kicker">03 · proof اجرای واقعی</div>
            <h1>یک حلقهٔ کامل، روی همان APK نهایی</h1>
            <p>چهار تصویر زیر stateهای مستقل نیستند؛ یک مسیر پیوسته‌اند: مأموریت از due queue ساخته شد، سه مرور ثبت شد، claim ده XP افزود و تکرار دوباره هیچ XP یا receipt تازه‌ای نساخت.</p>
            <div class="phone-grid">
                <figure><img src="{assigned}" alt="مأموریت فعال" /><figcaption>فعال · ۰ از ۳</figcaption></figure>
                <figure><img src="{complete}" alt="مأموریت کامل" /><figcaption>کامل · ۲۴ از ۸۰ XP</figcaption></figure>
                <figure><img src="{claimed}" alt="رسید پاداش" /><figcaption>دریافت · ۳۴ از ۸۰ XP</figcaption></figure>
                <figure><img src="{replay}" alt="محافظ بازپخش" /><figcaption>تکرار · بدون XP تازه</figcaption></figure>
            </div>
            <div class="proof-grid">
                <div><b>Session</b><span>۳ / ۱۶ مرور</span></div>
                <div><b>Quest</b><span>۳ / ۳ verified</span></div>
                <div><b>Balance</b><span>۲۴ ← ۳۴ XP</span></div>
                <div><b>Replay</b><span>۳۴ XP ثابت</span></div>
            </div>
        """),
        page(5, f"""
            <div class="page-kicker">04 · Journey نسخه‌دار</div>
            <h1>از مأموریت روزانه تا مسیر ماندگار چهار‌فصلی</h1>
            <p>Foundation Expedition روی Room v3 ذخیره می‌شود. فصل‌ها sequential هستند، UI فقط projection واقعی را می‌بیند و milestone XP در همان transaction ledger تسویه می‌شود.</p>
            <div class="journey-grid">
                <figure><img src="{journey_home}" alt="Journey Pulse در Home" /><figcaption>Home · فصل ۲ فعال</figcaption></figure>
                <figure><img src="{journey_map}" alt="Journey Map در Questboard" /><figcaption>Map · secured / active / locked</figcaption></figure>
                <figure><img src="{journey_replay}" alt="Journey replay guard" /><figcaption>Cold relaunch · بدون پاداش تازه</figcaption></figure>
            </div>
            <div class="proof-grid journey-proof">
                <div><b>Schema</b><span>Room v3</span></div>
                <div><b>Progress</b><span>فصل ۱ از ۴ secured</span></div>
                <div><b>Balance</b><span>۳۴ ← ۴۴ XP</span></div>
                <div><b>Replay</b><span>۱ event · ۱ transaction</span></div>
            </div>
            <div class="insight">Proof ارتقا با adb install -r و بدون reset داده انجام شد. یک defect واقعی bootstrap کشف شد، reconciliation Journey از catalog جدا شد و همان داده دوباره با cold start و SQLite query تأیید شد.</div>
        """),
        page(6, f"""
            <div class="page-kicker">05 · Campaign route v1</div>
            <h1>یک انتخاب واقعی، ذخیره‌شده و بدون XP لمسی</h1>
            <p>پس از secure شدن فصل نخست Foundation، کاربر بین Precision Trail و Momentum Circuit انتخاب می‌کند. هر route سه فصل و ۱۱۰ XP دارد؛ اما خود commitment دقیقاً صفر XP است و route دوم هرگز materialize نمی‌شود.</p>
            <div class="campaign-grid">
                <figure><img src="{campaign_choice}" alt="انتخاب دو مسیر Campaign" /><figcaption>Choose · دو route هم‌ارزش</figcaption></figure>
                <figure><img src="{campaign_confirm}" alt="هشدار commitment مسیر" /><figcaption>Confirm · بدون XP و غیرقابل‌تعویض</figcaption></figure>
                <figure><img src="{campaign_restored}" alt="بازیابی مسیر پس از cold relaunch" /><figcaption>Restored · Precision 0/3</figcaption></figure>
            </div>
            <div class="proof-grid journey-proof">
                <div><b>Schema</b><span>Room v4</span></div>
                <div><b>Choice</b><span>۱ route immutable</span></div>
                <div><b>Reward</b><span>۰ transaction</span></div>
                <div><b>Balance</b><span>۴۴ XP ثابت</span></div>
            </div>
            <div class="insight">ارتقای v3→v4 با adb install -r روی snapshot واقعی انجام شد. Foundation و ۴۴ XP حفظ شدند؛ commit یک event، یک choice، یک route Journey و سه stage ساخت. cold relaunch همین countها را بدون پاداش تازه نگه داشت.</div>
        """),
        page(7, """
            <div class="page-kicker">06 · startup و سرعت</div>
            <h1>Baseline Profile واقعی، با مرز ادعای روشن</h1>
            <p class="lead">Profile از benchmark build غیرdebug، غیرminified و API 35 جمع‌آوری شد؛ سپس فقط ruleهای متعلق به Cinnamon نگه داشته شدند.</p>
        """ + html_table(
            ["سنجه", "بدون compilation", "Focused profile", "نتیجهٔ p50"],
            [
                ("TotalTime p50 / p95", "۲۳۰۱ / ۲۴۶۸ ms", "۲۱۲۷ / ۲۲۲۶ ms", "حدود ۷٫۶٪ بهتر"),
                ("Prepare p50 / p95", "۷۴۸ / ۸۵۱ ms", "۶۵۱ / ۷۹۵ ms", "حدود ۱۳٪ بهتر"),
                ("Reconciliation", "۲۴ / ۳۷ ms", "۳۱ / ۴۰ ms", "noise؛ بدون claim"),
                ("Overlap", "۲۰۸ / ۳۲۷ ms", "۱۷۷ / ۲۶۹ ms", "diagnostic"),
            ],
            "performance",
        ) + """
            <div class="metric-cards">
                <div><strong>۲٬۶۴۵</strong><span>rule متمرکز</span></div>
                <div><strong>۱۲٬۳۱۴ B</strong><span>baseline.prof</span></div>
                <div><strong>۶۳۷ B</strong><span>baseline.profm</span></div>
                <div><strong>۵ / ۵</strong><span>trace موفق هر حالت</span></div>
            </div>
            <h2>گیت دائمی</h2>
        """ + bullets([
            "verifier تعداد rule، duplicate و owner خارجی را رد می‌کند.",
            "CI benchmark APK را می‌سازد و وجود profile و metadata را داخل ZIP می‌سنجد.",
            "startup بعد از دو frame واقعی آغاز می‌شود و reconciliation مسیر fast-path دارد.",
            "این اندازه‌گیری emulator است؛ proof release فقط با دستگاه فیزیکی هدف و jank budget معتبر می‌شود.",
        ]) + """
            <div class="source-note">مرجع روش: Android Baseline Profiles و manual collection رسمی. نسخهٔ کتابخانهٔ ۱٫۴٫۰ برای این build تک‌پردازه بسته‌بندی شده است.</div>
        """),
        page(8, """
            <div class="page-kicker">07 · اعتماد و انتشار</div>
            <h1>هسته سبز است؛ release هنوز عمداً قرمز می‌ماند</h1>
            <div class="gate-grid">
                <div><span>P0</span><h2>Credential</h2><p>کلیدهای تاریخی revoke/rotate و history، cache، clone و artifactها remediation شوند.</p></div>
                <div><span>P0</span><h2>Gateway / Auth</h2><p>session کوتاه‌عمر، rate limit، abuse control، secret manager و observability امن deploy شوند.</p></div>
                <div><span>P0</span><h2>Signing / Update</h2><p>lineage امضای release و install-update واقعی با artifact قابل‌ردیابی اثبات شود.</p></div>
                <div><span>P0</span><h2>Content approval</h2><p>۸ فایل، ۷۰۱ item و ۲۴۳ entry بازبینی تحریری/بالینی ثبت‌شده بگیرند.</p></div>
            </div>
            <h2>مرز AI</h2>
        """ + bullets([
            "Android فقط gateway HTTPS محصول را می‌پذیرد؛ provider key یا token ثابت داخل APK نیست.",
            "failureهای 400/401/403/409/429/5xx همراه request ID، Retry-After و delivery state حفظ می‌شوند.",
            "NotSent، ResponseReceived و UnknownAfterSend به UI اجازه می‌دهند retry و copy صادقانه داشته باشد.",
            "بدون session معتبر، پیام بیرون نمی‌رود و guided practice محلی جایگزین می‌شود.",
        ]) + """
            <div class="warning">حذف secret از HEAD به‌تنهایی credential remediation نیست؛ debug یا benchmark APK نیز proof انتشار محسوب نمی‌شود.</div>
        """),
        page(9, """
            <div class="page-kicker">08 · نقشهٔ سوپراپ</div>
            <h1>رشد بعدی باید عمق بسازد، نه فقط خط و صفحه</h1>
        """ + html_table(
            ["موج", "قابلیت", "تعریف Done"],
            [
                ("P1", "Campaign pack چند‌هفته‌ای", "episode manifest، زمان‌بندی، save/restore و پایان روایی"),
                ("P1", "Collection و cosmetic economy", "ledger-backed unlock، source/sink budget و anti-replay"),
                ("P1", "Personal plan از SRS", "recommendation قابل‌توضیح، opt-out و عدم feed تصادفی"),
                ("P1", "Content packs تخصصی", "manifest نسخه‌دار، provenance و editorial approval"),
                ("P1", "هویت و mascot منتخب", "canonical ID، asset واقعی، semantics و screenshot diff"),
                ("P1", "Accessibility matrix", "screen reader، contrast، focus و Reduce motion proof"),
                ("P2", "Sync و recovery چنددستگاهی", "offline-first conflict، encryption و recovery drill"),
                ("P2", "Social challenge امن", "server authority، consent، privacy، moderation و ضدتقلب"),
            ],
            "roadmap",
        ) + """
            <div class="identity-callout">
                <h2>هویت قبلاً انتخاب شده است</h2>
                <p>شناسهٔ canonical آن در artifactهای قابل‌بازیابی گم شده؛ از کاربر انتخاب دوباره خواسته نمی‌شود. تا بازیابی مرجع، فقط mascot، palette، badge art، icon و screenshot-diff هویت‌محور متوقف‌اند.</p>
            </div>
            <p class="formula">Capability = Data contract + State machine + Persistence + Failure path + Accessibility + Secure telemetry + Runtime proof</p>
        """),
        page(10, f"""
            <div class="page-kicker">09 · آرشیو تصمیم بصری</div>
            <h1>پیش‌نمایش‌های ۱ تا ۶ — مرجع پیوستگی، نه فرم انتخاب</h1>
            <p>این تصویرها آرشیو ایده‌پردازی ImageGen هستند. انتخاب قبلی کاربر معتبر است، اما identifier آن باید از artifact canonical بازیابی شود. هیچ‌کدام به‌تنهایی proof پیاده‌سازی یا mascot production-ready نیستند.</p>
            <img class="contact-sheet first" src="{sheets[0]}" alt="پیش‌نمایش کانسپت‌های یک تا شش" />
            <p class="caption">Questlands · Morph Lab · Clinical Comicverse · Care City · Pulse Jam · Cinnamon Orbit</p>
        """),
        page(11, f"""
            <div class="page-kicker">10 · آرشیو تصمیم بصری</div>
            <h1 class="archive-title"><span>پیش‌نمایش‌های ۷ تا ۱۰</span><small>فقط برای بازیابی مرجع منتخب</small></h1>
            <p>پس از بازیابی شناسه، همان جهت به design token، mascot family، badge system، world object و screenهای واقعی تجزیه می‌شود. انتخاب تازه یا blend خودسرانه مجاز نیست.</p>
            <img class="contact-sheet second" src="{sheets[1]}" alt="پیش‌نمایش کانسپت‌های هفت تا ده" />
            <p class="caption">Cinnamon Forge · Case Noir · Shift Theatre · Living Lexicon Wilds</p>
            <div class="archive-note">Preview در docs می‌ماند و داخل APK بسته‌بندی نشده است؛ asset نهایی باید project-bound، بهینه، semantic و قابل سنجش در runtime باشد.</div>
        """),
        page(12, """
            <div class="page-kicker">11 · شواهد نهایی</div>
            <h1>گیت داخلی سبز است؛ ادعای آمادگی انتشار ممنوع</h1>
            <div class="metric-cards final-metrics">
                <div><strong>۱۰۴</strong><span>JVM test</span></div>
                <div><strong>۲۶</strong><span>test suite</span></div>
                <div><strong>۳ / ۳</strong><span>Android test</span></div>
                <div><strong>۰</strong><span>lint error</span></div>
            </div>
        """ + html_table(
            ["Artifact", "Evidence"],
            [
                ("Debug APK", "۲۱٬۸۲۵٬۳۷۵ byte · SHA-256: 58EA02…5A6F"),
                ("Benchmark APK", "۱۴٬۴۶۵٬۷۵۴ byte · SHA-256: 7B6C07…9504"),
                ("Mission runtime", "assign → سه review → complete → claim → replay guard"),
                ("Journey runtime", "v2→v3 · فصل ۱ secured · ۳۴→۴۴ XP · replay guard"),
                ("Campaign runtime", "v3→v4 · Precision 0/3 · zero XP · cold restore"),
                ("Baseline Profile", "۲٬۶۴۵ rule · source و packaging verify شده"),
                ("Lexicon", "۸ فایل · ۷۰۱ item · ۲۴۳ entry · approval لازم"),
            ],
            "evidence",
        ) + """
            <h2>حکم نهایی این checkpoint</h2>
            <div class="final-verdict">Settlement، Mission Pulse، Journey v1، Campaign route v1، migration v4، replay guard، build/test و Baseline Profile دیگر گیت داخلی باز این موج نیستند. موج بعد باید campaign pack چند‌هفته‌ای، collection، personalization، accessibility و زیرساخت release را عمیق کند. انتشار تا رفع چهار P0 همچنان BLOCK است.</div>
            <p class="fine-print">محدودیت: benchmark روی emulator است؛ محتوا approval بیرونی ندارد؛ backend و signing production در checkout نیستند؛ identifier هویت منتخب هنوز بازیابی نشده است.</p>
        """),
    ]
    return f"""<!doctype html>
<html lang="fa" dir="rtl">
<head>
<meta charset="utf-8" />
<title>گزارش پرفکشن Cinnamon</title>
<style>
@page {{ size: A4; margin: 0; }}
* {{ box-sizing: border-box; }}
html, body {{ margin: 0; padding: 0; direction: rtl; background: #fffaf4; color: #2a1d19; font-family: Tahoma, Arial, sans-serif; }}
body {{ -webkit-print-color-adjust: exact; print-color-adjust: exact; }}
.page {{ width: 210mm; height: 297mm; position: relative; overflow: hidden; padding: 17mm 15mm 19mm; page-break-after: always; break-after: page; background: linear-gradient(145deg, #fffdf9 0%, #fbf3e9 100%); }}
.page:last-child {{ page-break-after: auto; break-after: auto; }}
.page::after {{ content: ""; position: absolute; width: 72mm; height: 72mm; border-radius: 50%; left: -39mm; top: -39mm; border: 18mm solid rgba(137, 61, 88, .035); pointer-events: none; }}
.page-kicker {{ color: #9a6d5a; font-size: 10px; letter-spacing: .7px; margin-bottom: 5px; font-weight: 700; }}
h1 {{ color: #7e304e; font-size: 22px; line-height: 1.45; margin: 0 0 9px; font-weight: 800; }}
h2 {{ color: #176a60; font-size: 16px; line-height: 1.45; margin: 12px 0 6px; font-weight: 800; }}
p {{ font-size: 12.7px; line-height: 1.85; margin: 0 0 7px; text-align: right; }}
.lead {{ font-size: 13.7px; color: #49352d; }}
ul {{ list-style: none; padding: 0; margin: 0; }}
li {{ font-size: 12.2px; line-height: 1.75; position: relative; padding-right: 17px; margin: 3px 0; text-align: right; }}
li::before {{ content: "◆"; color: #ee8f5b; font-size: 7px; position: absolute; right: 1px; top: 6px; }}
table {{ border-collapse: separate; border-spacing: 0; width: 100%; table-layout: fixed; direction: rtl; overflow: hidden; border-radius: 10px; border: 1px solid #d8c4b6; }}
th {{ background: #176a60; color: #fff; font-size: 11.2px; font-weight: 800; padding: 7px; text-align: center; border-left: 1px solid rgba(255,255,255,.18); }}
td {{ background: rgba(255,255,255,.78); font-size: 10.8px; line-height: 1.45; padding: 7px; text-align: right; border-left: 1px solid #e2d2c7; border-top: 1px solid #e2d2c7; vertical-align: middle; }}
tr:first-child td {{ border-top: 0; }} td:last-child, th:last-child {{ border-left: 0; }}
.scorecard {{ margin-top: 3px; }} .scorecard th:nth-child(1) {{ width: 29%; }} .scorecard th:nth-child(2) {{ width: 21%; }}
.compact {{ margin-bottom: 10px; }} .compact th:nth-child(1) {{ width: 52%; }} .compact th:nth-child(2) {{ width: 18%; }}
.performance th:nth-child(1) {{ width: 27%; }} .performance td {{ font-size: 10.4px; }}
.roadmap td {{ font-size: 10.3px; padding: 6px; }} .roadmap th:nth-child(1) {{ width: 10%; }} .roadmap th:nth-child(2) {{ width: 34%; }}
.evidence th:nth-child(1) {{ width: 27%; }}
.insight, .warning, .source-note, .archive-note, .test-strip {{ margin: 9px 0; padding: 10px 12px; border-radius: 10px; font-size: 11.8px; line-height: 1.75; }}
.insight {{ background: #fce0d0; border-right: 4px solid #ee8f5b; color: #6b3421; }}
.warning {{ background: #f7dbe4; border-right: 4px solid #9a3b5c; color: #65263d; font-weight: 700; }}
.source-note, .archive-note {{ background: #e2f2ec; border-right: 4px solid #2d8a79; color: #174d45; }}
.test-strip {{ background: #2b201c; color: #fff5e9; border-right: 4px solid #eea15f; }}
.flow {{ display: flex; align-items: center; justify-content: space-between; direction: rtl; margin: 6px 0 13px; padding: 13px; border-radius: 12px; background: #2b201c; color: #fff4e9; font-size: 11px; }}
.flow span {{ color: #f5a565; font-size: 17px; }}
.two-column {{ display: grid; grid-template-columns: 1fr 1fr; gap: 9px; direction: rtl; }}
.panel {{ padding: 8px 11px 11px; border-radius: 13px; min-height: 91mm; }} .panel h2 {{ margin-top: 3px; }}
.panel.berry {{ background: #f5dbe4; border: 1px solid #d89bb1; }} .panel.teal {{ background: #ddf1eb; border: 1px solid #8cc7ba; }}
.state-row {{ display: grid; grid-template-columns: repeat(4, 1fr); gap: 7px; margin: 7px 0 9px; direction: ltr; }}
.state-row span {{ background: #8b3d58; color: white; border-radius: 999px; text-align: center; padding: 7px 2px; font-size: 10.5px; font-weight: 700; }}
.phone-grid {{ display: grid; grid-template-columns: repeat(4, 1fr); gap: 7px; margin-top: 8px; direction: ltr; }}
.phone-grid figure {{ margin: 0; padding: 4px; border-radius: 13px; background: #241814; box-shadow: 0 5px 14px rgba(61,34,26,.15); }}
.phone-grid img {{ display: block; width: 100%; height: auto; object-fit: contain; border-radius: 9px; background: #17110e; }}
.phone-grid figcaption {{ color: #f9dbc9; font-size: 9px; text-align: center; padding: 5px 1px 3px; direction: rtl; }}
.journey-grid {{ display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-top: 8px; direction: ltr; }}
.journey-grid figure {{ margin: 0; padding: 4px; border-radius: 13px; background: #241814; box-shadow: 0 5px 14px rgba(61,34,26,.15); }}
.journey-grid img {{ display: block; width: 100%; height: auto; max-height: 118mm; object-fit: contain; border-radius: 9px; background: #17110e; }}
.journey-grid figcaption {{ color: #f9dbc9; font-size: 8.7px; text-align: center; padding: 5px 1px 3px; direction: rtl; }}
.campaign-grid {{ display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-top: 8px; direction: ltr; }}
.campaign-grid figure {{ margin: 0; padding: 4px; border-radius: 13px; background: #241814; box-shadow: 0 5px 14px rgba(61,34,26,.15); }}
.campaign-grid img {{ display: block; width: 100%; height: auto; max-height: 116mm; object-fit: contain; border-radius: 9px; background: #17110e; }}
.campaign-grid figcaption {{ color: #f9dbc9; font-size: 8.7px; text-align: center; padding: 5px 1px 3px; direction: rtl; }}
.journey-proof {{ margin-top: 8px; }}
.proof-grid, .metric-cards {{ display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-top: 11px; direction: rtl; }}
.proof-grid div, .metric-cards div {{ background: #fff; border: 1px solid #d9c6b9; border-radius: 11px; padding: 9px 5px; text-align: center; }}
.proof-grid b, .proof-grid span, .metric-cards strong, .metric-cards span {{ display: block; }}
.proof-grid b {{ color: #7e304e; font-size: 10px; }} .proof-grid span {{ font-size: 10.5px; margin-top: 3px; }}
.metric-cards strong {{ color: #7e304e; font-size: 18px; }} .metric-cards span {{ color: #5e4a41; font-size: 9.5px; margin-top: 3px; }}
.gate-grid {{ display: grid; grid-template-columns: 1fr 1fr; gap: 10px; direction: rtl; }}
.gate-grid div {{ background: #fff; border: 1px solid #d9b7c4; border-radius: 14px; padding: 10px 12px; min-height: 43mm; position: relative; }}
.gate-grid div > span {{ position: absolute; left: 9px; top: 9px; background: #8b3d58; color: white; border-radius: 999px; padding: 3px 8px; font-size: 9px; font-weight: 700; }}
.gate-grid h2 {{ margin-top: 1px; }} .gate-grid p {{ font-size: 11px; }}
.identity-callout {{ margin-top: 12px; background: linear-gradient(135deg, #8b3d58, #61263d); color: white; padding: 11px 14px; border-radius: 14px; }}
.identity-callout h2 {{ color: #ffd9c1; margin-top: 0; }} .identity-callout p {{ margin-bottom: 0; }}
.formula {{ margin-top: 11px; direction: ltr; text-align: center; color: #176a60; background: #dff1eb; border-radius: 10px; padding: 10px; font-size: 10.8px; font-weight: 700; }}
.contact-sheet {{ display: block; width: 100%; object-fit: contain; background: #17110e; border-radius: 14px; margin: 8px auto 0; box-shadow: 0 7px 20px rgba(48,31,24,.18); }}
.contact-sheet.first {{ max-height: 198mm; }} .contact-sheet.second {{ max-height: 155mm; }}
.caption {{ color: #80695e; font-size: 9.5px; text-align: center; margin-top: 6px; direction: ltr; }}
.archive-title span, .archive-title small {{ display: block; }}
.archive-title small {{ color: #176a60; font-size: 18px; line-height: 1.55; margin-top: 1px; }}
.final-metrics {{ margin: 12px 0; }} .final-metrics div {{ background: #2b201c; border-color: #2b201c; }} .final-metrics strong {{ color: #f7a263; }} .final-metrics span {{ color: #f9e9dc; }}
.final-verdict {{ margin-top: 8px; padding: 14px 16px; border-radius: 14px; background: linear-gradient(135deg, #dcefe9, #f7dfd0); color: #24463f; font-size: 13px; font-weight: 700; line-height: 1.9; border: 1px solid #acd0c6; }}
.fine-print {{ margin-top: 12px; color: #745f54; font-size: 10.5px; }}
.cover {{ background: radial-gradient(circle at 12% 12%, rgba(243,155,96,.24), transparent 28%), linear-gradient(155deg, #201511 0%, #3a211c 54%, #642a42 100%); color: #fff7ef; }}
.cover::after {{ display: none; }} .cover-kicker {{ margin-top: 26mm; color: #f2a168; font-size: 11px; letter-spacing: 2px; direction: ltr; text-align: center; }}
.cover-title {{ color: #fff6ed; font-size: 35px; line-height: 1.35; text-align: center; margin: 9px 0 2px; }}
.cover-subtitle {{ color: #e9cfc0; text-align: center; font-size: 15px; line-height: 1.9; margin: 0 auto 22px; max-width: 150mm; }}
.cover-callout {{ position: relative; z-index: 2; background: rgba(255,255,255,.08); border: 1px solid rgba(255,255,255,.20); border-radius: 18px; padding: 16px 18px; backdrop-filter: blur(5px); }}
.cover-callout p {{ margin: 13px 0 0; color: #fff4e8; font-size: 14px; line-height: 2; }}
.pill {{ display: inline-block; border-radius: 999px; padding: 6px 11px; margin-left: 5px; font-size: 10px; font-weight: 800; }} .pill.success {{ background: #7fc8b5; color: #123d35; }} .pill.danger {{ background: #efa3b9; color: #5d2035; }}
.cover-date {{ color: #f3aa72; text-align: center; font-size: 14px; margin-top: 19px; }}
.cover-bottom {{ position: absolute; bottom: 34mm; left: 18mm; right: 18mm; color: #d6bdb0; text-align: center; font-size: 11.5px; line-height: 2; }}
.cover-orbit {{ position: absolute; border-radius: 50%; border: 1px solid rgba(255,255,255,.12); }} .orbit-one {{ width: 110mm; height: 110mm; left: -47mm; top: -40mm; }} .orbit-two {{ width: 72mm; height: 72mm; right: -33mm; bottom: 15mm; }}
footer {{ position: absolute; left: 15mm; right: 15mm; bottom: 7mm; border-top: 1px solid #d7c6ba; padding-top: 4px; display: flex; direction: rtl; justify-content: space-between; color: #79645a; font-size: 9px; }}
.cover footer {{ border-color: rgba(255,255,255,.18); color: #c9aea1; }}
</style>
</head>
<body>{''.join(pages)}</body>
</html>"""


def build_pdf() -> Path:
    if not CHROME.is_file():
        raise RuntimeError(f"Chrome was not found at {CHROME}")
    contact_sheets = make_contact_sheets()
    runtime_images = validate_runtime_images()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    HTML_OUTPUT.write_text(build_html(contact_sheets, runtime_images), encoding="utf-8")
    command = [
        str(CHROME),
        "--headless=new",
        "--disable-gpu",
        "--no-pdf-header-footer",
        "--allow-file-access-from-files",
        f"--print-to-pdf={OUTPUT}",
        HTML_OUTPUT.as_uri(),
    ]
    result = subprocess.run(command, capture_output=True, text=True, timeout=120, check=False)
    if result.returncode != 0 or not OUTPUT.is_file() or OUTPUT.stat().st_size < 20_000:
        raise RuntimeError(f"Chrome PDF rendering failed: {result.stderr.strip()}")
    return OUTPUT


if __name__ == "__main__":
    print(build_pdf())
