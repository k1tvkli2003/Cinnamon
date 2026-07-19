# AvalAI working material: practice feedback and recap

- Model: `gemini-flash-latest`
- Endpoint: `gemini`
- Temperature: `0.3`
- Thinking budget: `2048`
- Maximum output tokens: `6000`
- Request ID: `019f75a0-5d6e-74b0-9ace-0f15c6af649e`

This is advisory working material. Product truth and final wording remain locally owned.

## Response

1. `Place all the words to complete the sentence.`
2. `That order is not quite right. Try rearranging the words.`
3. `Correct. Sentence {current} of {total} completed.`
4. `Practice Round Complete`
5. `You have successfully reviewed {total} sentences in this session.`
6. `Vocabulary Match Completed`
7. `Time Expired` / `Feel free to try this round again at your own pace.`
8. `Return to Dashboard`

The final implementation shortens a few lines where needed, keeps the factual progress placeholders, and uses `Finish` rather than assuming the destination is always a dashboard.
