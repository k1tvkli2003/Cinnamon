# قرارداد حفظ داده، رفتار و محتوا — Room v6

- Snapshot date: 2026-07-23
- Scope: whole UI base rebuild
- Rule: additive, reversible, migration-tested
- Forbidden: reset/clean/uninstall/`pm clear`/destructive migration/whole-file corpus deletion

## Room و دادهٔ کاربر

| قرارداد | baseline قفل‌شده | سیاست تغییر | اثبات لازم |
|---|---|---|---|
| Room schema | v6؛ ۱۸ table، ۱۸۳ column، ۵۰ index، ۱۱ foreign-key declaration | migration افزایشی؛ بدون `fallbackToDestructiveMigration` | verifier همهٔ v1-v5→v6، instrumentation و `adb install -r` |
| schema artifact v5 | `app/schemas/com.cinnamon.app.data.local.AppDatabase/5.json`؛ SHA-256 `8D13191D5DC40422D7EB394217350444ADC303EE83162BDE1E040F4080035E9F` | baseline migration و critical؛ نباید حذف شود | schema diff و migration test |
| schema artifact v6 | `app/schemas/com.cinnamon.app.data.local.AppDatabase/6.json`؛ SHA-256 `A619925A0A23C67FD5CA726B1DCA7C76E844E66F4EA7FBFACA90198937A5AE57` | authority جاری | Room export + v5→v6 conservation |
| populated QA DB | integrity `ok`، صفر FK violation | exact conservation | pre/post counts و logical orphan checks |
| gamification data | ۷ event، ۴ transaction، ۷ summary، ۲ journey، ۷ stage، ۱ focus selection، ۳۴ XP transaction/balance | append/idempotent only | equality، uniqueness و replay proof |
| DataStore | `cinnamon_progress` | keyهای قبلی حفظ و فقط adapter افزوده شود | force-stop/relaunch و preference tests |

در baseline موجود، logical orphan و duplicate مشاهده نشد. Android Auto Backup/device transfer برای دادهٔ app-owned غیرفعال است؛ بنابراین uninstall مسیر recovery نیست.

## Lexicon corpus

- Dataset: `cinnamon.lexicon.core@2026.07.17`
- Origin: `project-bundled-curated`
- Review gate: `editorial-review-required-before-release`
- Total: ۷۰۱ record
- Entries: ۲۴۳ = ۱۲۶ clinical + ۱۱۷ general
- سایر مجموعه‌ها: ۱۶۰ morpheme، ۹۵ abbreviation، ۷۸ phrase، ۴۵ confusable pair، ۸۰ sentence

هش‌های canonical ثبت‌شده در manifest:

| فایل | count | SHA-256 |
|---|---:|---|
| `abbreviations.json` | 95 | `fdf3fe10ceb76d08433562b127999fd3171e060f61fbc73ac99fc3e782a0d374` |
| `clinical_1.json` | 71 | `55b193da28494839e689dd16ffd3fa6aa70861f636946f48057eb81d8704f1f2` |
| `clinical_2.json` | 55 | `a6aeef3e960745da5f0927b8639a0e44cd7814861adede3113a779e0c7e39a8b` |
| `confusables.json` | 45 | `d6aa0c5cd7b77dd7cfd0d319a465aefa73e4692cd82bb2ea8287b9335cd1ccda` |
| `general.json` | 117 | `2abb0b5a6d5c1aeff8f18ab14a7ae3d2e07d62dab4814c38e4172f0d2d4b5be0` |
| `morphemes.json` | 160 | `060d377bb5d65af3c5661f236d0ba43ca6208a62632fae697c1bff438bb62bf5` |
| `phrases.json` | 78 | `044ca4cdae0a190cfbb0b17c8c49b1a017f23b0e7db61e1efc947594ab77877c` |
| `sentences.json` | 80 | `374fe97b8ac3fcc1dc9ae88ec94e3473cf04ba1cd0596deacbe9ad5b5b6d3990` |

`manifest.json` byte SHA-256:
`44882CC1611FB7248621D4BCFAFC955AD53D829211E595E89093D01065EF085D`

JSON entryها ID صریح ندارند؛ seed ID از فایل و ترتیب مشتق می‌شود و در eventها و `entry/{id}` مصرف شده است. بنابراین تغییر order/bytes تنها با migration صریح ID مجاز است.

## NLM MeSH Reference Atlas

- Dataset: `cinnamon.mesh.reference@2026.1`
- Source: NLM MeSH 2026 production descriptors
- Selection: شاخه‌های `A/C/E/F/G/N` با حداقل یک tree number در عمق حداکثر ۳
- Descriptor count: ۵٬۷۴۲
- Payload: `mesh-reference-2026.jsonl`، ۲٬۶۴۲٬۰۲۶ byte
- Payload SHA-256:
  `63203108BB9F5EB5A20F1A9CC7F9E2451FF140F0DA5B0CD044A2E6A89BDCDF7E`
- Source archive SHA-256:
  `9FE35B3170652376A592DAF69E91A80D6C693ECAF9C571CEB701D04204CB357D`
- Attribution: `Courtesy of the U.S. National Library of Medicine`
- NLM endorsement: `false`
- Staleness disclosure: این bundle بازتاب MeSH 2026 است و updateهای بعدی را تضمین نمی‌کند.

عضویت دسته‌ها overlap دارد:

| root | membership |
|---|---:|
| Anatomy (`A`) | 697 |
| Diseases (`C`) | 1,853 |
| Techniques (`E`) | 1,534 |
| Psychiatry (`F`) | 493 |
| Phenomena (`G`) | 1,167 |
| Health Care (`N`) | 632 |

`meshUi` کلید canonical است و برخلاف lexicon core به ترتیب فایل وابسته نیست.
جدول `mesh_reference` learner state ندارد. seed آن حق تغییر `lexicon`، SRS،
bookmark، event یا reward را ندارد.
در update نسخه، snapshot قبلی و جدید فقط داخل یک transaction تعویض می‌شوند؛
هر failure باید کل replacement را rollback کند. این import پس از critical
startup و در پس‌زمینه اجرا می‌شود و failure آن حق قفل‌کردن مسیر یادگیری را ندارد.

## کاتالوگ گیمیفیکیشن

| artifact | baseline |
|---|---|
| `catalog-v1.json` | SHA-256 `EC5AF859E04D654EDCB0F69FFF59ACD5091AC2E9ABD49C143013E1F26F0BD60A` |
| `copy-en-v1.json` | SHA-256 `F8ABE48BBDEBDC7C6B8F0B70ACE1274A2E9CAF4FA5E9D56A5247BD904890C72C` |
| Catalog shape | ۴ rarity، ۱۵ reward، ۵ presentation، ۶ level، ۱۲ achievement، ۱۲ quest |
| Locale contract | ۹۱/۹۱ key حاضر |
| Criteria | ۲۴ criterion با outcome قابل‌اثبات |

هیچ ID موجود rename/delete نمی‌شود. نسخهٔ جدید باید additive و با retired-ID reservation باشد. reward، achievement یا quest فقط از event/transaction/receipt معتبر نمایش داده می‌شود.

## محتوای تألیفی embedded در Kotlin

قبل از split یا بازنویسی فایل‌های UI زیر باید corpus آن‌ها verbatim استخراج یا با consumer test قفل شود:

- ۴ scenario
- ۱۵ clinical lab
- ۱۷ fluency tab/exercise
- Language Sprint prompts
- transcript clues
- offline/fallback corpora
- jargon mappings
- Journey و learning-focus catalogs

تغییر تایپی `ectatic` در `NativeFluencyPlaygroundScreen.kt` بدون editorial review انجام نمی‌شود؛ baseline حفظ و به backlog محتوا منتقل می‌شود.

## مرز AI و انتشار

- Android فقط public `AI_GATEWAY_BASE_URL` را می‌شناسد.
- `AiGatewayClient` بدون product session معتبر fail-closed است.
- `ProductSessionTokenProvider.currentToken()` فعلاً `null` است؛ live gateway path اثبات نشده.
- provider secret، API key، keystore یا credential نباید وارد APK، log، CI یا سند شود.
- release عمومی تا signing continuity، signed upgrade، gateway/session و content approval مسدود است.

## rollback

1. presentation جدید در کنار legacy route نگه داشته می‌شود.
2. هر migration دارای pre/post identity و rollback plan است.
3. corpus core و Reference Atlas هرکدام از manifest/version/hash مستقل بازیابی می‌شوند.
4. Room history rewrite نمی‌شود.
5. shell قدیمی تا parity کامل حذف نمی‌شود.
6. دادهٔ دستگاه فقط با `adb install -r` حفظ می‌شود؛ uninstall ممنوع است.
