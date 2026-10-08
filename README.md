# Cinnamon

Cinnamon is a personal English-perfection companion for one demanding reader: an advanced (above-B2) medical student who already knows the medicine and wants the language — pronunciation, natural usage, collocations, register, and the difference between "concerning for" and "consistent with." Not a beginner app; nothing in it is basic.

## What's inside

- **Dictionary-grade lexicon** — 240+ headwords across clinical English (cardiology, pulmonology, nephrology, neurology, pathophysiology) and high-level general English (precision verbs, nuanced adjectives, academic register, idiom). Each entry carries IPA, precise definition, plain-language gloss, etymology, morpheme breakdown, synonyms/antonyms, real collocations, a ward example, an everyday example, and a usage note on the traps.
- **Word building** — ~160 medical prefixes, roots, and suffixes with meanings and worked examples.
- **Ward abbreviations** — ~95 abbreviations an extern must read and say correctly, each with actual pronunciation and on-rounds usage.
- **Communication phrases** — round presentations, breaking bad news, handovers, academic writing, natural conversation.
- **Mix-ups** — confusable pairs that change a chart note (palpation vs palpitation, ileum vs ilium, dysphagia vs dysphasia).
- **Spaced repetition** — every word is an SM-2 flashcard; the app schedules reviews so vocabulary sticks.
- **Word games** — Cloze Clinic (fill the blank in real sentences), Word Match, Sentence Unscramble, all drawn from the same lexicon.
- **Guided practice** — standardized-patient roleplay, clinical sim labs (SBAR, SPIKES, morning report), a "Make It Native" rewriter. Live coaching runs only through a product-owned authenticated gateway; otherwise these routes stay in honest on-device guided-practice mode.

The dictionary ships as plain JSON under `app/src/main/assets/lexicon/` and seeds a local Room database on first launch — full offline use, easy to extend.

## Design

Warm spice palette: toasted dark default, warm-cream light mode, true-black AMOLED mode. Soft corners, springy motion, serif headwords, full edge-to-edge layout.

## Tech stack

Native Android (Kotlin, Gradle), Room local database, JSON lexicon bundle, optional AI-gateway backend for live coaching (base URL only — the app never accepts provider API keys; gateway credentials live in the gateway's secret manager).

## Getting started

Prerequisite: Android Studio.

1. Open the project and let Gradle sync.
2. Optional, for live coaching: set a non-secret `AI_GATEWAY_BASE_URL` in `local.properties` or pass `-PAI_GATEWAY_BASE_URL=https://your-gateway.example/`.
3. Run on an emulator or device.

## Status

Active development. Lexicon, games, SRS, and design system are in-tree with docs, QA, and release notes under `docs/`.
