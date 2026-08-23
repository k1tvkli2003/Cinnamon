# ImageGen prompt pack — Cinnamon Questlands v2

این promptها پس از رفع OAuth با تصویر مرجع
`01-cinnamon-questlands.png`
استفاده می‌شوند. تصویر مرجع mood/source است، نه layoutی که باید کورکورانه کپی شود.

## قواعد مشترک

- original product design؛ بدون تقلید از Duolingo، Headspace یا mascot معروف
- medical-English learning world برای دانشجوی پزشکی/کارآموز بالینی؛ بالغ، گرم و بازیگوش، نه کودکانه
- حفظ cinnamon spiral، toasted navy، cream، spice orange، mint، cyan و berry
- حذف currency/gem جعلی و حذف reward numberهای بی‌منبع
- کنترل‌ها، live data و متن accessibility-critical باید در implementation زنده باشند
- متن داخل mockup کم، دقیق و انگلیسی باشد؛ از lorem ipsum و gibberish جلوگیری شود
- فضا، z-order، crop، object scale و navigation برای Android واقعی باورپذیر باشند
- کارت تنها وقتی استفاده شود که یک object واقعی مثل receipt، ticket یا field note را نمایش می‌دهد

## QL-01 — Whole-app world board

```text
Create a premium 16:9 product design board for an Android medical-English learning app named “Cinnamon Questlands”. Use the supplied older Questlands concept only as a visual ancestor: retain its warm cinnamon spiral motif, inviting illustrated world, toasted charcoal background, cream parchment, spice-orange, mint, cyan and berry accents, but redesign the entire app to feel more sophisticated, contemporary, spatial and credible for adult medical learners.

Show five complete compact-phone screens at readable scale: Today, Field Atlas, Practice Studio, Quest Map, and Cabinet. Today is the hero screen: a living expedition base with one unmistakable next action, a short review trail, a calm cinnamon-spiral guide character, and progress landmarks integrated into the scene rather than stacked dashboard cards. Field Atlas is a rich lexicon discovery space with live search, saved items and one detailed medical term specimen. Practice Studio presents evidence-based activities as physical training stations. Quest Map shows authentic mastery progression with locked states explained by prerequisites, not decorative stars. Cabinet shows earned achievements, learning focus, themes and accessibility preferences.

Use a coherent five-item navigation system. Make controls look implementable in Jetpack Compose, with clear 48dp touch targets, strong hierarchy, generous negative space, and a premium editorial mix of a warm humanist serif for display words plus a clean sans serif for UI. Integrate subtle medical-English cues—dialogue notes, anatomy field sketches, pronunciation waveform, terminology tabs—without hospital cliché imagery.

Include a small design-token strip: palette swatches, type pairing, surface materials, icon language and three semantic themes. Include a small note that reward visuals are receipt-driven, but do not display fake XP, gems, coins or impossible numbers. No generic hero-plus-cards layout, no glassmorphism, no neon gaming HUD, no childish toy town, no illegible tiny text, no famous-character resemblance. Render as a polished high-fidelity product visualization with crisp UI, authored illustration, believable Android proportions and consistent lighting.
```

## QL-02 — Responsive and state matrix

```text
Create a highly detailed 16:9 responsive UX board for “Cinnamon Questlands”, a premium gamified medical-English Android app. Continue the exact visual identity of the approved Questlands v2 whole-app direction. Show the same Today experience transformed across compact phone, medium foldable/tablet and expanded landscape tablet. Do not merely stretch cards: on phone use a focused vertical stage; on medium use navigation rail plus a two-zone expedition scene; on expanded use a persistent world map/context pane and a readable learning workspace.

Along the bottom of the board show eight compact but legible state specimens: startup preparing, empty review queue, offline local guide, invalid entry Not Found, recoverable quest claim error with the correct retry action, successful evidence receipt, replay with no duplicate reward, and reduced-motion celebration. Each specimen must share the same component language and make the state unmistakable without relying only on color.

Use real-looking short labels only: “Today”, “Field Atlas”, “Practice”, “Journey”, “Cabinet”, “Continue review”, “Not found”, “Try again”, “Saved offline”, “Already counted”. Maintain adult warmth, medical-English specificity, 48dp controls, large-type resilience, LTR content readiness and no clipping. Avoid modal overload, endless spinners, silent fallback, generic cards, decorative fake metrics, blurred glass and neon. Present it like an implementation-ready responsive design review board with measurements, alignment guides and safe-area awareness.
```

## QL-03 — Mascot model and emotional system

```text
Design an original character system sheet for the “Cinnamon Questlands” medical-English learning app. The mascot is “Cinna”, a warm cinnamon-spiral expedition guide inspired by a rolled cinnamon ribbon and field notebook, not an animal and not similar to any existing famous learning mascot. The character must feel intelligent, calm, gently humorous and suitable for adult medical students—not babyish, plush-toy cute or hyperactive.

Show: front, three-quarter, side and back turnaround; silhouette tests at 24px, 48px and 96px; facial-expression system; hand/arm gesture vocabulary; material and color callouts; and ten product poses: welcoming return, pointing to next action, focused listening, thinking, gentle correction, celebrating verified progress, streak at risk without shame, offline/local-guide state, recoverable error, and static reduced-motion state.

Include three outfit/accessory modes that do not imply clinical authority: field guide satchel, lexicon bookmark kit, and practice headphones. Do not add a white coat, medical license badge, stethoscope as authority symbol, weapon, currency bag, gem or crown. Use toasted caramel and cream with mint/cyan/berry accents, clear dark outlines, subtle paper-and-enamel texture, excellent silhouette recognition and production-friendly separated shapes. The board should look like a professional animation and UI asset model sheet with transparent-background asset thumbnails and consistent proportions.
```

## QL-04 — Reward and achievement asset language

```text
Create a premium game-economy presentation board for “Cinnamon Questlands”, an adult medical-English learning app. Continue the approved Questlands v2 identity and Cinna mascot. Design a coherent achievement and reward language driven by verified learning evidence, with five families: Mastery, Consistency, Recovery, Exploration and Careful Communication.

Show 20 original badge concepts organized by those five families, each with a distinctive silhouette that remains recognizable at 24px. Show four rarity treatments that change material and framing without implying pay-to-win value. Show five receipt-style reward moments: first verified review, difficult-term recovery, seven-day consistent practice, completed clinical-language lab, and mastered lexicon cluster. Include compact, medium and large presentation variants plus static reduced-motion alternatives.

Use cinnamon enamel, parchment, oxidized mint, cyan ceramic, berry wax seal and restrained gold only for the rarest earned milestones. Avoid coins, gems, loot chests, casino sparkle, fake XP numbers, crowns, military medals and manipulative streak shame. Make every asset production-ready: clean silhouette, consistent perspective, separated foreground/background, clear safe area and asset naming callouts. Add a small motion strip showing anticipation, reveal and settle, with intensity proportional to the event.
```

## QL-05 — Motion storyboard

```text
Create a 16:9 motion-design storyboard for the approved “Cinnamon Questlands” Android UI. Show six sequences in 4–6 clear frames each: app startup world assembling, Today next-action invitation, transition from Field Atlas term to Review, verified reward receipt, Quest Map node unlock, and calm error recovery. Each sequence must also show a reduced-motion alternative using opacity, color and static pose changes only.

Use short timing labels, easing curves and layer names. Motion should feel warm, tactile and restrained: cinnamon-ribbon curl, paper ticket settle, map beacon pulse, gentle parallax under 8dp equivalent, no camera shake, no infinite bounce, no confetti storm. Preserve context across transitions and keep interactive targets stable. Avoid any reward animation before persistence confirmation. Present as a professional implementation storyboard for Jetpack Compose animation APIs.
```

