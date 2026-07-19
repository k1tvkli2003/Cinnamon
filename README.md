# Cinnamon! 🤎

**Cinnamon** is a personal English-perfection companion built for one demanding reader: an advanced (above-B2) medical student who already knows the medicine and now wants the *language* — pronunciation, natural usage, collocations, register, and the difference between "concerning for" and "consistent with."

It is not a beginner app. Nothing in it is basic.

## What's inside

- **A dictionary-grade lexicon** — 240+ headwords across clinical English (cardiology, pulmonology, nephrology, neurology, pathophysiology, and more) and high-level general English (precision verbs, nuanced adjectives, academic register, idiom). Every entry carries IPA, a precise definition, a plain-language gloss, etymology, morpheme breakdown, synonyms/antonyms, real collocations, a ward example, an everyday example, and a usage note on the traps.
- **Word building** — ~160 medical prefixes, roots, and suffixes with meanings and worked examples.
- **Ward abbreviations** — ~95 abbreviations an extern must *read and say* correctly, each with how it's actually pronounced and used on rounds.
- **Communication phrases** — presenting on rounds, breaking bad news, colleague handovers, academic writing, and natural conversation.
- **Mix-ups** — the confusable pairs that change a chart note (palpation vs palpitation, ileum vs ilium, dysphagia vs dysphasia…).
- **Spaced repetition** — every word is an SM-2 flashcard; the app schedules reviews so vocabulary actually sticks.
- **Word games** — Cloze Clinic (fill the blank in real sentences), Word Match, and Sentence Unscramble, all drawing from the same lexicon.
- **Guided practice** — standardized-patient roleplay, clinical sim labs (SBAR, SPIKES, morning report), and a "Make It Native" rewriter. Live coaching is available only through a product-owned, authenticated gateway; otherwise these routes stay in honest on-device guided-practice mode.

## Design

Cinnamon wears a warm spice palette — toasted dark by default, with a warm-cream light mode and a true-black AMOLED mode. Soft corners, springy motion, a serif for headwords, and full edge-to-edge layout.

## Build & run

**Prerequisites:** [Android Studio](https://developer.android.com/studio)

1. Open the project in Android Studio and let it sync.
2. (Optional, for live coaching) configure a non-secret `AI_GATEWAY_BASE_URL` Gradle property in `local.properties` or pass it with `-PAI_GATEWAY_BASE_URL=https://your-gateway.example/`. The Android app never accepts provider API keys. Gateway credentials belong only in the gateway's secret manager.
3. Run on an emulator or device. The full lexicon seeds into a local Room database on first launch.

The dictionary data lives as plain JSON under `app/src/main/assets/lexicon/` and is easy to extend.
