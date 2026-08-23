# دفتر preview و گیت تأیید

## وضعیت canonical

| مورد | نوع | وضعیت | معیار استفاده |
|---|---|---|---|
| `01-cinnamon-questlands.png` قدیمی | Mock Preview / reference | موجود، تأییدنشده | فقط mood و motif |
| Learning Focus preview v1 | feature-only preview | موجود، غیرcanonical | فقط همان feature؛ نه کل اپ |
| Questlands whole-app board v2 | ImageGen canonical candidate | تولیدنشده | نیازمند OAuth سالم |
| Mascot family/model sheet | ImageGen canonical candidate | تولیدنشده | نیازمند OAuth سالم |
| Reward/achievement world assets | ImageGen canonical candidate | تولیدنشده | نیازمند OAuth سالم |
| Motion/reduced-motion storyboard | preview candidate | تولیدنشده | پس از art direction |

مرجع قدیمی:
`../2026-07-18-cinnamon-perfection-gamification-and-visual-direction/assets/brand-concepts-v1/01-cinnamon-questlands.png`

## boardهای لازم

### QL-01 — Whole-app world

- compact Home/Today، Library، Practice، Journey و Profile
- toasted-dark base با spice orange، mint، cyan و berry
- نقشهٔ mastery با مسیرهای ملموس و negative space کنترل‌شده
- کارت فقط برای object/contract واقعی؛ نه container پیش‌فرض همه‌چیز
- live controls و دادهٔ حساس خارج از artwork

### QL-02 — Responsive/state matrix

- compact، medium و expanded
- loading، empty، error، offline، success، replay و NotFound
- stable navigation و context preservation
- long medical terms، large font و LTR/RTL stress states

### QL-03 — Mascot studio

- cinnamon-spiral guide با silhouette متمایز
- turnaround و facial system
- idle، invite، thinking، gentle correction، success، streak-risk، offline و reduced-motion poses
- پرهیز از شباهت به mascotهای مشهور و پرهیز از infantile styling

### QL-04 — Reward and achievement language

- badge families برای mastery، consistency، recovery، exploration و care
- rarity بدون pay-to-win language
- celebration intensity متناسب با event
- semantic/static alternative برای reduced motion

## خطای ImageGen

- attempt 1: reference input → network error
- attempt 2: reference retry → network error
- attempt 3: clean generation → `401 Unauthorized`, `token_revoked`

fallback به CLI/API بدون مجوز صریح کاربر ممنوع است. هیچ UI code نباید صرفاً با تکیه بر board قدیمی ساخته شود.

## approval checklist

- [ ] جهان کلی جذاب و غیرخشک است.
- [ ] Home فهرست کارت‌ها نیست و next action فوری دیده می‌شود.
- [ ] mascot شخصیت و نقش محصولی روشن دارد.
- [ ] رنگ‌ها در سه theme معنایی و accessibility-safe هستند.
- [ ] همهٔ route families و failure states قابل تصورند.
- [ ] artwork، live UI و semantic fallback از هم تفکیک شده‌اند.
- [ ] کاربر preview canonical را صریحاً تأیید کرده است.

