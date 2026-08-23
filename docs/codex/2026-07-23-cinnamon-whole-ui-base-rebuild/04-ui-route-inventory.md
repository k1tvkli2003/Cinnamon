# موجودی UI، route و state

## startup و shell فعلی

`MainActivity` edge-to-edge را فعال می‌کند، preferenceها را اعمال و `AppNavigation` را mount می‌کند. coordinator چهار حالت دارد:

- `NotStarted`
- `Preparing`
- `Ready`
- `Failed(stage)`

گیت startup در کل NavHost سراسری نیست؛ Home checkpoint دارد، اما destination بازیابی‌شدهٔ غیرHome ممکن است پیش از reconciliation رندر شود.

## مسیرهای حفاظت‌شده

| خانواده | routeهای فعلی | رفتار لازم در shell جدید |
|---|---|---|
| tabs | `home`، `lexicon`، `practice`، `profile` | aliasهای پایدار؛ انتخاب و back stack حفظ شود |
| content | `entry/{id}`، `review`، `cloze` | typed args، NotFound و retry صریح |
| games | `vocab_match`، `unscramble`، `gamification_hub` | settlement/replay parity |
| clinical | `clinical_sim_labs`، `scenario_select`، `patient_chat/{scenarioId}` | invalid scenario نباید silent fallback کند |
| fluency | `native_fluency_playground/{tab}`، `make_it_native` | stable tab ID؛ label نمایشی route argument نباشد |

## نقص‌های route/state

1. invalid `entry/{id}` می‌تواند spinner بی‌نهایت نشان دهد.
2. invalid `patient_chat/{scenarioId}` بی‌صدا به chest-pain fallback می‌کند.
3. invalid fluency tab بی‌صدا Tone Slider را باز می‌کند.
4. نام قابل‌نمایش tab به‌عنوان route argument استفاده می‌شود.
5. deep-link، not-found، typed-argument و back regression test کافی وجود ندارد.
6. toolbar back در Clinical با system back contract یکسان نیست.
7. همهٔ `ProgressErrorEvent`ها recovery سراسری `retryLastPracticeSession` را اجرا می‌کنند؛ quest-claim failure هم ممکن است command اشتباه بگیرد.
8. `ChatBubble` مستقیم به Room DAO دسترسی دارد.

## coupling و فایل‌های بزرگ

| فایل/سطح | تقریب خطوط | رأی |
|---|---:|---|
| Gamification Hub | 1955 | split by feature registry + screen contract |
| Clinical Sim Labs | 1729 | extract authored labs before split |
| Native Fluency Playground | 1617 | stable tab registry + corpus extraction |
| Roleplay Chat | 1169 | gateway/read-model boundary |
| Home | 1149 | stage composition + startup contract |
| User Progress ViewModel | 990 | typed commands/read models |
| Profile | 642 | cabinet/progress/settings sections |
| Lexicon | 552 | search/list/detail contracts |

## معماری route پیشنهادی

- sealed `AppRoute` با codec و typed argument
- mapping همهٔ stringهای فعلی به legacy alias
- `StartupGate` بیرون NavHost
- `MainTab` مستقل از label نمایشی
- screen-specific read model و command
- recovery action مخصوص همان failure
- explicit state family:
  `Loading / Empty / Ready / NotFound / Offline / RecoverableError / FatalError`
- feature registry برای game/fluency/clinical content
- repository facade میان presentation و Room

## journeyهای پذیرش بحرانی

1. cold start → Preparing → Ready → restored destination
2. Home next action → practice → evidence → receipt → replay بدون grant
3. Lexicon search → entry ID → review queue → saved progress
4. quest assignment → progress → complete → claim → relaunch
5. learning focus انتخاب → persistence → restore
6. preference تغییر → force-stop → relaunch
7. invalid route arg → typed NotFound → parent recovery
8. AI unavailable → local guide / not-sent truth → retry مناسب
9. compact/medium/expanded با حفظ context و بدون clipping
10. reduced motion و TalkBack semantics برای celebration/reward

