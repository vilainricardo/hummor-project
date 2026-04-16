# API review — Hub de Controle de Humor

**Artifact:** `docs/api/openapi_v1.yaml`  
**SRS baseline:** `docs/srs/SRS_v1.md`  
**SAD baseline:** `docs/sad/SAD_v1.md`  
**Review version:** v1  
**Date:** 2026-04-12  

---

## Checks

| Topic | Result | Finding |
| --- | --- | --- |
| Endpoint completeness vs SRS | Pass | Core FR paths present (mood, groups, aggregate, export, clinical, profile, health). |
| Schema correctness | Pass | Types, enums, idempotency header on unsafe operations. |
| Error consistency | Pass | Shared `Problem` schema + domain codes. |
| Versioning strategy | Fail | `info.version` only; no URL policy / deprecation contract documented. |
| Async callbacks / webhooks | Fail | SRS FR-14 allows async notification; spec has poll-only with no callback contract. |
| API governance | Fail | No explicit naming, pagination, idempotency, error code governance section in spec. |
| Doc drift | Fail | Description still references **SRS v2**; must match `SRS_v1.md`. |

---

## Verdict

**NEEDS REVISION**

---

## Required actions (blocking)

1. Align `info.description` with `SRS_v1` + add an **API governance** block (naming, pagination, idempotency replay semantics, error taxonomy maintenance).
2. Document **versioning** (URL `/v1` freeze, additive changes only for PATCH minors, breaking → `/v2`).
3. Add **async callback** for export completion: optional `callback_url` on `ExportRequest` and OpenAPI **callbacks** entry describing signed webhook POST body (job id, state, timestamps).
