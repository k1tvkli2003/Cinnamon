# Cinnamon Cloze feedback copy brief

Review concise English feedback for an adult medical-English cloze game. Return one recommended line per slot, with at most one short rationale. This game checks authored word matches only; it does not assess clinical competence or fluency.

Constraints: calm, mature, non-shaming, plain international English, short enough for a screen reader, no casino language, and no hidden implementation terms.

Slots:

1. Correct answer live announcement.
2. Incorrect answer panel using `{answer}` and optional short plain-language cue `{cue}`.
3. Screen-reader state for the selected incorrect option after the answer is revealed.
4. Summary helper when fewer than three answers were correct, explaining neutrally that the round did not count as a completed practice session and can be tried again.
