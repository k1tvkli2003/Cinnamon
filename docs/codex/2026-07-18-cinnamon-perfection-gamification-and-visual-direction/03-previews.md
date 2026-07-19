# پیش‌نمایش‌ها

## Mock Preview: Cinnamon brand concepts v1

همهٔ تصویرهای ImageGen در این پرونده **Mock Preview** و آرشیو تصمیم‌اند. کاربر کانسپت را قبلاً انتخاب کرده است، اما identifier canonical انتخاب در state قابل‌بازیابی نیست. تا بازیابی همان identifier و screenshot-diff مقابل implementation واقعی، هیچ preview اثبات fidelity محسوب نمی‌شود و انتخاب تازه‌ای درخواست نمی‌شود.

Label: Mock Preview  
Source: ImageGen  
Assumptions: این ده تصویر برای مقایسهٔ جهان، رنگ‌مایه، انرژی و composition اولیه تولید شده‌اند؛ تصمیم قبلاً گرفته شده است.  
Limitations: متن، data، کنترل‌های تعاملی، accessibility و mascot production-ready را اثبات نمی‌کنند.  
Verified: خیر؛ جهت مفهومی است و تا بازیابی identifier انتخاب قبلی، implementation/fidelity هویت‌محور ندارد.  
Asset: `assets/brand-concepts-v1/`

## ledger پیش‌نمایش

| نام | نوع | منبع | تأیید شده؟ | asset | یادداشت |
|---|---|---|---|---|---|
| Cinnamon Questlands | Mock Preview | ImageGen | خیر | [01](assets/brand-concepts-v1/01-cinnamon-questlands.png) | نقشهٔ سفر و progression |
| Morph Lab | Mock Preview | ImageGen | خیر | [02](assets/brand-concepts-v1/02-morph-lab.png) | آزمایشگاه واژه‌سازی |
| Clinical Comicverse | Mock Preview | ImageGen | خیر | [03](assets/brand-concepts-v1/03-clinical-comicverse.png) | روایت اپیزودیک، language-first |
| Cinnamon Care City | Mock Preview | ImageGen | خیر | [04](assets/brand-concepts-v1/04-cinnamon-care-city.png) | شهر مهارت‌ها |
| Pulse Jam | Mock Preview | ImageGen | خیر | [05](assets/brand-concepts-v1/05-pulse-jam.png) | ریتم و fluency |
| Cinnamon Orbit | Mock Preview | ImageGen | خیر | [06](assets/brand-concepts-v1/06-cinnamon-orbit.png) | mastery map منظومه‌ای |
| Cinnamon Forge | Mock Preview | ImageGen | خیر | [07](assets/brand-concepts-v1/07-cinnamon-forge.png) | ساختن و صیقل‌دادن زبان |
| Case Noir | Mock Preview | ImageGen | خیر | [08](assets/brand-concepts-v1/08-case-noir.png) | پرونده و معما |
| Shift Theatre | Mock Preview | ImageGen | خیر | [09](assets/brand-concepts-v1/09-shift-theatre.png) | rehearsal و roleplay |
| Living Lexicon Wilds | Mock Preview | ImageGen | خیر | [10](assets/brand-concepts-v1/10-living-lexicon-wilds.png) | واژه‌های زنده و collection |

## محدودیت‌ها

- previewها text/live-data/accessibility semantics واقعی ندارند.
- previewها نباید به‌جای mascot system یا asset pipeline نهایی استفاده شوند.
- بازیابی و ثبت identifier انتخاب قبلی، شرط شروع screenshot-diff و Copy validation هویت‌محور است؛ از کاربر انتخاب مجدد خواسته نمی‌شود.

## screenshotهای runtime واقعی

این‌ها **Mock Preview نیستند**؛ از اپ اجراشده روی emulator گرفته شده‌اند و فقط همان state دیده‌شده را اثبات می‌کنند:

- `app/build/runtime/scenario-language-sprint-complete.png` - Sprint کامل و receipt XP.
- `app/build/runtime/transcript-escape-complete.png` - Escape کامل و receipt XP.
- `app/build/runtime/transcript-escape-focus-cleared.png` - پایان Escape بدون keyboard مزاحم.
- `output/cinnamon-runtime-word-match-finish-20260718.png` - کارت پایدار reward و receipt واقعی.
- `output/cinnamon-runtime-profile-settings-20260718.png` - preferenceهای feedback.
- `output/cinnamon-runtime-profile-settings-changed-20260718.png` - Sound/Haptic خاموش و Reduce motion روشن.
