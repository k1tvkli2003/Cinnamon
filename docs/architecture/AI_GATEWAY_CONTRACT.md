# قرارداد gateway برای Live Coaching

## مرز اعتماد

Android فقط با gateway متعلق به محصول صحبت می‌کند. provider API key، model name، system prompt، retry policy سروری و telemetry نباید در APK وجود داشته باشند.

```text
Android → product session → POST /v1/ai/chat → gateway → provider
```

تا وقتی این gateway و یک product session کوتاه‌عمر وجود ندارد، Android باید در حالت `offline_guided_practice` بماند و هیچ request خارجی برای AI نفرستد.

## Request Android

```http
POST /v1/ai/chat
Authorization: Bearer <short-lived-product-session>
Idempotency-Key: <UUID>
X-Request-ID: <UUID>
Content-Type: application/json
```

```json
{
  "mode": "native_coach",
  "scenarioId": null,
  "message": "User-authored text only"
}
```

مقادیر مجاز `mode` فقط `native_coach` و `standardized_patient` هستند. `scenarioId` باید یک catalog ID محدودشده باشد؛ متن system prompt، model، endpoint یا provider authorization هرگز از Android پذیرفته نمی‌شود.

## Response و خطا

```json
{
  "reply": "Validated assistant response",
  "requestId": "req_..."
}
```

همهٔ خطاها باید ساختار امن زیر داشته باشند و هیچ provider message، stack trace، credential یا محتوای کامل درخواست را برنگردانند:

```json
{
  "error": {
    "code": "gateway_unavailable",
    "requestId": "req_..."
  }
}
```

کدهای لازم: `invalid_request`, `unauthenticated`, `forbidden`, `rate_limited`, `idempotency_conflict`, `gateway_unavailable`, `upstream_unavailable`.

## مسئولیت‌های اجباری gateway

- احراز هویت product session و authorization پیش از تماس با provider؛
- validate طول پیام، mode و scenarioId در boundary؛
- rate limit per account/device و quota روزانه؛
- idempotency durable بر پایهٔ subject + `Idempotency-Key`؛ retry mutation فقط پس از reconcile؛
- prompt policy سروری و promptهای versioned؛
- timeout محدود، بدون retry بی‌نهایت، و circuit-break/degraded state؛
- structured log شامل request ID، mode، status و latency؛ بدون Authorization header، credential یا متن خام کاربر؛
- نگه‌داری/حذف محتوای مکالمه بر اساس policy محصول و exclusion از backup/export پیش از persistence.

## اقدام لازم بیرون از مخزن

این repository هنوز auth issuer، hosting gateway، rate-limit store، secret manager یا credential جدید ندارد. قبل از فعال‌کردن `AI_GATEWAY_BASE_URL` در release باید مالک محصول این چهار مرز را فراهم کند و gateway را با credentialهای rotate‌شده deploy کند. این محدودیت عمدی است: یک token ثابت در APK جایگزین امنی برای credential provider نیست.
