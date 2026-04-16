# Evolution trace — Hub de Controle de Humor

**Bundle version:** v1  
**Date:** 2026-04-12  

| Document | Path | Version |
| --- | --- | --- |
| SRS | `docs/srs/SRS_v1.md` | v1 |
| SAD | `docs/sad/SAD_v1.md` | v1 |
| OpenAPI | `docs/api/openapi_v1.yaml` | `info.version` 1.1.0 |
| Compliance | `docs/compliance/compliance_v1.md` | v1 |

---

## 1. Version comparison (initial baseline)

This is the first tracked evolution bundle in-repo. Prior state: Vision document + partial OpenAPI/compliance without SRS/SAD on disk.

| Delta | From | To | Summary |
| --- | --- | --- | --- |
| SRS introduced | — | `SRS_v1.md` | Full FR/NFR/state/open questions. |
| SAD introduced | — | `SAD_v1.md` | C4, sequences, security, observability, SRS mapping. |
| OpenAPI | 1.0.0 | 1.1.0 | Governance text, export callback optional field + callbacks; SRS ref fix. |
| Compliance | partial | enriched v1 | SRS baseline fix, mermaid flow, DPA/subprocessor/crypto/PbD. |

---

## 2. Cross-document consistency

| Link | Status | Notes |
| --- | --- | --- |
| SRS ↔ SAD | Consistent | SAD §14 maps FR-01–FR-20. |
| SAD ↔ API | Consistent | Policy/k/export/clinical flows match components. |
| API ↔ Compliance | Consistent | Webhook signing referenced in compliance §11; export URL TTL in both. |
| SRS ↔ Compliance | Consistent | Legal basis rows cite FR ids present in SRS. |

---

## 3. Impact analysis (material changes)

### 3.1 OpenAPI 1.0.0 → 1.1.0

| What | Why | Architecture | API | Data | Compliance | Backward compatibility |
| --- | --- | --- | --- | --- | --- | --- |
| Governance block | `api_review_v1` finding | Doc only | Clarified policy | — | Aligns contracting language | **Low risk** — additive |
| `callback_url` + callbacks | SRS FR-14 / SAD async | Worker must call HTTPS outbound | New optional field | Webhook audit logs | DPA may list webhook endpoint as processing | **Medium** — tenants opt-in; default unchanged |

**Linked review:** `docs/review/api_review_v1.md`, `docs/review/api_review_v2.md`

### 3.2 Compliance baseline correction

| What | Why | Impact |
| --- | --- | --- |
| SRS `v2` → `v1` reference | Remove ghost baseline | Low — documentation accuracy |

**Linked review:** `docs/review/compliance_review_v1.md`

---

## 4. Drift detection

| Check | Finding |
| --- | --- |
| Requirement without API | FR-14 webhook optional path now covered when `callback_url` set; pure poll still valid. |
| API without architecture | Export callback egress documented in evolution impact (SAD worker should add outbound allowlist — **Should** track as implementation task). |
| Compliance missing for feature | None blocking; workplace DPIA remains process gate §15. |

---

## 5. Risk of change

| Change | Risk class | Rationale |
| --- | --- | --- |
| Webhook delivery | **Medium** | Secret management, retry storms, tenant verification bugs |
| Doc-only governance | **Low** | No runtime change |

---

## 6. Migration impact

- **Data migration:** none for docs-only releases.
- **API versioning:** clients remain on `/v1`; optional `callback_url` ignored by old clients.
- **Client impact:** tenants adopting callbacks must expose verified HTTPS endpoint; otherwise no change.

---

## 7. Evolution validator verdict

**Status:** APPROVED  
**Reviewer role:** Senior Engineering Auditor  
**Date:** 2026-04-12  

Cross-document checks passed; remaining items are implementation follow-ups (worker egress allowlist), not specification contradictions.
