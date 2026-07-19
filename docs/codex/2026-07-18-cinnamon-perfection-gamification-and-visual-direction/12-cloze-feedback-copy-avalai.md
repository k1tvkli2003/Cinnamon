# AvalAI working material: Cloze feedback

- Model: `gemini-flash-latest`
- Endpoint: `gemini`
- Temperature: `0.2`
- Thinking budget: `1536`
- Maximum output tokens: `5000`
- Request ID: `019f75a2-8a5e-7d42-8f94-1af08053ac8e`

This is advisory working material. Product truth and final wording remain locally owned.

## Response

1. `Correct.`
2. `Not quite. The correct term is {answer}. {cue}`
3. `Selected incorrect option. Correct answer is {answer}.`
4. `This round did not meet the requirements to count as a completed practice session. You can try this session again.`

The final implementation uses the bundled plain-language definition as `{cue}` and shortens the summary helper while preserving the same truthful meaning.
