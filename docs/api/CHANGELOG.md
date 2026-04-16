# API specification changelog

## 1.1.0 — 2026-04-12

- Documented URL versioning policy and API governance in OpenAPI `info.description`.
- Added optional `ExportRequest.callback_url` and `ExportJobCallback` schema.
- Declared OpenAPI **callbacks** for export job completion with `X-Hub-Signature-256` verification contract.

## 1.0.0 — prior

- Initial REST surface for mood, groups, analytics, exports, clinical.
