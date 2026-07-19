# راستی‌آزمایی

## خلاصه

- Result: `passed for current internal checkpoint / release blocked`
- آخرین full checkpoint: 2026-07-19، پس از Campaign route v1، migration v4، runtime upgrade/commit/relaunch و ProfileInstaller.
- کاربر کانسپت را قبلاً انتخاب کرده؛ identifier canonical در state قابل‌بازیابی نیست و انتخاب مجدد درخواست نمی‌شود.

## checkهای گذرانده

| بررسی | فرمان / روش | نتیجه | شاهد و مرز |
|---|---|---|---|
| full JVM + lint + assemble | `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleBenchmark` | passed | ۱۰۴ test در ۲۶ suite؛ ۰ failure/error/skip؛ lint ۰ error و ۳۰ warning |
| Android instrumentation | `:app:connectedDebugAndroidTest` | passed | ۳/۳ روی `Codex_API35 (AVD)`، Android 15 |
| atomic settlement | DAO integration + planner suites | passed | unlock، rollback collision، duplicate، claim/replay/forgery، پنج سناریوی Journey و چهار Campaign |
| Mission copy/state | pure copy tests + ViewModel projection | passed | unassigned، active، complete و claimed |
| exact-final Mission runtime | ADB + UIAutomator + screenshot | passed | سه review، `24/80 → 34/80 XP`، replay بدون XP/receipt تازه |
| Journey projection/runtime | ADB + UIAutomator + screenshot | passed | Home pulse و Journey Map؛ فصل ۱ secured، فصل ۲ active، فصل‌های ۳/۴ locked |
| Room v2→v3 upgrade | `adb install -r` + DB/WAL snapshot + SQLite query | passed | app data محفوظ؛ `user_version=3`، `34→44 XP` و یک journey transaction |
| Journey cold-relaunch replay | force-stop/cold start + query | passed | event count=۱، transaction count=۱، balance=۴۴؛ receipt تازه ساخته نشد |
| Campaign projection | pure projector tests | passed | Locked/Choose/Ready/Recovery و version mismatch بدون state جعلی |
| Room v3→v4 upgrade | old APK + restored v3 DB/WAL + `adb install -r` | passed | Foundation و ۴۴ XP حفظ؛ choice/event قبل از commit صفر |
| Campaign commitment | UIAutomator + SQLite query | passed | Precision 0/3؛ summary=۰، transaction=۰، balance=۴۴، route دوم materialize نشد |
| Campaign cold restore | force-stop/cold start + UI/DB | passed | choice=۱، event=۱، route instance=۱، stage=۳، balance=۴۴ |
| migration matrix | `verify_room_migration.py` | passed | v1→v2، v2→v3، v3→v4 و chain v1→v4؛ ۴۳ statement افزایشی |
| feedback preference persistence | ADB/UIAutomator + force-stop/relaunch | passed | changed state پس از relaunch حفظ و سپس restore شد |
| typed gateway failures | targeted JVM tests | passed | status، request ID، Retry-After و delivery state |
| benchmark build | `:app:assembleBenchmark` | passed | non-debuggable، non-minified؛ debug signing فقط برای measurement |
| Baseline Profile source | verifier مستقل | passed | ۲٬۶۴۵ rule متعلق به Cinnamon، بدون duplicate/foreign owner |
| Baseline Profile packaging | ZIP inspection + verifier | passed | `assets/dexopt/baseline.prof` و `baseline.profm` موجود |
| startup same-build comparison | `measure_startup.ps1`، دو مجموعهٔ پنج‌run | measured | p50 TotalTime: ۲۳۰۱ms none در برابر ۲۱۲۷ms speed-profile؛ emulator فقط |
| lexicon provenance | runtime contract + verifier | passed structurally | ۸ فایل، ۷۰۱ item، ۲۴۳ entry؛ editorial review هنوز لازم است |
| secret/data/signing verifiers | scriptهای مستقل | passed locally | credential history و release signing lineage بیرونی را رفع نمی‌کند |
| PDF نهایی | Chrome RTL + Poppler render + visual inspection | passed | ۱۲ صفحه A4 Tagged؛ همهٔ صفحات inspect؛ clipping عنوان صفحهٔ ۱۱ اصلاح و re-render شد |

## artifact checkpoint

- debug APK: ۲۱٬۸۲۵٬۳۷۵ byte؛ SHA-256 `58EA0223E14B2B2FF01CBD301A9A7C2298872E51A3A4BDA7FB87962C4FF95A6F`.
- benchmark APK: ۱۴٬۴۶۵٬۷۵۴ byte؛ SHA-256 `7B6C074D11DC4B85E3E76D2934BF22BF46BC8D9D2F938580B2ED8CCDA1359504`.
- focused profile: ۲٬۶۴۵ rule؛ compiled profile ۱۲٬۳۱۴ byte و metadata ۶۳۷ byte.
- PDF RTL: ۳٬۸۶۴٬۴۱۲ byte؛ SHA-256 `9FC34F600DC489EBBD5078319B8D3BF955C72FBB0D46CCE5709E1CA30E51AA3C`.

## runtime artifacts نهایی

- `output/cinnamon-runtime-final-home-20260718.png`
- `output/cinnamon-runtime-final-quest-assigned-20260718.png`
- `output/cinnamon-runtime-final-review-three-20260718.png`
- `output/cinnamon-runtime-final-quest-complete-20260718.png`
- `output/cinnamon-runtime-final-quest-claimed-20260718.png`
- `output/cinnamon-runtime-final-quest-replay-guard-20260718.png`
- `output/cinnamon-runtime-journey-home-active-v3-20260718.png`
- `output/cinnamon-runtime-journey-map-v3-20260718.png`
- `output/cinnamon-runtime-journey-replay-guard-v3-20260718.png`
- `output/cinnamon-runtime-v4-campaign-choice-20260719.png`
- `output/cinnamon-runtime-v4-campaign-confirm-20260719.png`
- `output/cinnamon-runtime-v4-campaign-active-20260719.png`
- `output/cinnamon-runtime-v4-campaign-cold-restored-20260719.png`

## گیت‌های باز

- credential rotation/revocation و history remediation.
- gateway/auth/session/rate-limit/abuse/observability production.
- signing lineage و install-update واقعی.
- editorial/clinical approval داده.
- identity screenshot-diff پس از بازیابی identifier انتخاب قبلی.
- performance release روی دستگاه فیزیکی هدف و jank budget.

## حکم

checkpoint داخلی برای settlement، Mission، Journey v1، Campaign route v1، migration v4، build/test و Baseline Profile سبز است. release عمومی همچنان BLOCK است و هیچ benchmark emulator به‌عنوان proof دستگاه release معرفی نمی‌شود.
