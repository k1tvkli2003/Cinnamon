# AvalAI working material: catalog rewards

- Model: `gemini-flash-latest`
- Endpoint: `gemini`
- Temperature: `0.2`
- Thinking budget: `768`
- Maximum output tokens: `2200`
- Request ID: `019f75d8-abbb-71e3-8b46-1b770f5120cb`

This is advisory working material. Product truth and final wording remain locally owned.

## Response

1. Button: `CLAIM EARNED REWARD`
2. Claimed state: `REWARD CLAIMED · XP ADDED`
3. Quest notice title: `Quest Reward Secured`
4. Quest notice detail: `+N XP added to your balance from this completed checkpoint.`
5. Milestone title: `Milestone Unlocked`
6. Milestone detail: `+N XP awarded from your learning progress. This milestone remains unlocked.`
7. Failure: `Quest progress is safe, but the reward failed to claim. Please try again.`

The final implementation adopts the clearer balance/milestone language, adds the exact claimable XP to the button, and keeps the retry message explicit that no claim occurred.

