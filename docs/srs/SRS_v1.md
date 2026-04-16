# Software Requirements Specification (SRS)

**Product:** Hub de Controle de Humor  
**Version:** v1  
**Date:** 2026-04-12  
**Status:** Draft for validation  

---

## 1. Purpose and scope

### 1.1 Purpose

This SRS defines functional and non-functional requirements for a multi-tenant web platform that lets natural persons (CPF) log mood over time, organize in groups (including CNPJ-led organizations), run privacy-preserving aggregates and exports, correlate actions with mood, and support optional clinical practitioner–patient links under LGPD.

### 1.2 In scope (MVP)

- Authentication and authorization via external **hub** (JWT; roles in token).
- Mood CRUD for self.
- Groups with three visibility models; invites; membership roles.
- k-anonymity-gated aggregates and anonymized export jobs.
- Group actions registry for correlation analysis UI (future insight engine out of scope).
- Clinical link lifecycle with explicit consent artifact references.
- Audit logging for sensitive admin reads.

### 1.3 Out of scope

- Hub implementation, payment, Instagram OAuth/content sync (integration hooks only in backlog).
- Native mobile apps (responsive web only for MVP).
- ML/IA insight generation beyond simple charts described here.

---

## 2. Personas

| ID | Persona | Goals | Pain today |
| --- | --- | --- | --- |
| P-CPF | Individual user | Log mood, see own history, join groups safely | No structured emotional timeline |
| P-CNPJ | Org admin | Create groups, review aggregates, evidence well-being programs | No compliant cohort view |
| P-GRP | Group admin | Manage members, invites, exports | Manual spreadsheets |
| P-CLIN | Licensed clinician (where applicable) | View linked patient aggregates with consent | Fragmented tools |
| P-AUD | Security / DPO reviewer | Prove k-gates, audit trails, retention | Ambiguous specs |

---

## 3. Prioritization legend

- **Must:** MVP blocker; release not allowed without it.
- **Should:** Strong value; acceptable deferral only with documented waiver.
- **Could:** Nice-to-have within MVP window if capacity allows.

---

## 4. Business KPIs and success metrics

| KPI | Definition | Target (MVP) | Measurement |
| --- | --- | --- | --- |
| Mood logging stickiness | % of registered users with ≥1 mood/week | Should: 40% at 8 weeks | Analytics DB |
| Aggregate trust | Zero incidents of below-k disclosure | Must: 0 | Automated policy tests + audit |
| Export success time | p95 job completion after cohort passes k | Should: < 5 min | Job metrics |
| Clinical consent integrity | % clinical links with ≥1 consent artifact | Must: 100% | DB constraint + API validation |
| Availability | API monthly uptime | Must: 99.5% | Synthetic probes |

---

## 5. UX-level requirements

| ID | Priority | Requirement |
| --- | --- | --- |
| UX-01 | Must | Primary mood capture reachable in ≤2 taps from home (mobile-first). |
| UX-02 | Must | Distinct visual treatment for **hidden** vs **visible_to_admins** contribution in INDIVIDUAL groups. |
| UX-03 | Should | Charts default to weekly buckets; user can switch to daily when density allows. |
| UX-04 | Must | When aggregate is blocked (below k), UI shows neutral explanation (no numeric leak). |
| UX-05 | Could | Dark mode parity with light mode contrast (WCAG 2.1 AA for text). |

---

## 6. Usability constraints

- All error surfaces that affect privacy MUST use plain language (PT-BR primary locale).
- Time zones: display in user locale; server stores UTC.
- Maximum note length 2000 characters (aligned with API).

---

## 7. Functional requirements

### FR-01 — Hub-authenticated access

| Field | Content |
| --- | --- |
| Priority | Must |
| Statement | Every API call (except documented health) SHALL require a valid hub-issued JWT. |
| Acceptance criteria | (1) Missing token → 401. (2) Expired token → 401. (3) Roles enforced server-side per FR-02. |

### FR-02 — Role model

| Field | Content |
| --- | --- |
| Priority | Must |
| Statement | System SHALL support roles `owner`, `admin`, `member` on group membership and SHALL map JWT `roles[]` to tenant-level capabilities as defined in §10 business rules. |
| Acceptance criteria | Admin-only operations return 403 for `member` in automated tests for each protected endpoint. |

### FR-03 — User profile

| Field | Content |
| --- | --- |
| Priority | Must |
| Statement | User SHALL read and update allowed profile fields (`display_name` MVP). |
| Acceptance criteria | GET/PUT `/v1/users/me` behaviors per OpenAPI; validation errors return RFC 9457 problem+json. |

### FR-04 — Mood logging

| Field | Content |
| --- | --- |
| Priority | Must |
| Statement | User SHALL create mood entries with integer value 1–5, optional note, optional `recorded_at` with server authority if skew > 5 minutes. |
| Acceptance criteria | (1) Values outside range rejected. (2) List supports cursor pagination + optional `from`/`to` filters. |

### FR-05 — Mood listing

| Field | Content |
| --- | --- |
| Priority | Must |
| Statement | User SHALL list only own mood entries. |
| Acceptance criteria | Cross-user access impossible (403/404 policy per security design). |

### FR-06 — Mood hard delete

| Field | Content |
| --- | --- |
| Priority | Must |
| Statement | User SHALL hard-delete own mood entry by id; idempotent delete semantics documented. |
| Acceptance criteria | Second delete returns consistent non-leaking response (404 or idempotent 204 per API contract). |

### FR-07 — Group creation

| Field | Content |
| --- | --- |
| Priority | Must |
| Statement | Authenticated user SHALL create a group with `type ∈ {INDIVIDUAL_PUBLIC, INDIVIDUAL, GROUPED}` and metadata. |
| Acceptance criteria | Created group is bound to tenant from JWT; type immutable after create (unless future SRS adds migration). |

### FR-08 — Invites

| Field | Content |
| --- | --- |
| Priority | Must |
| Statement | Admin/owner SHALL invite users by `invitee_user_id`; invitee SHALL accept via dedicated endpoint. |
| Acceptance criteria | State machine matches §14.3; duplicate invites handled without data corruption. |

### FR-09 — Group read

| Field | Content |
| --- | --- |
| Priority | Must |
| Statement | Member SHALL read group metadata per visibility rules in §10. |
| Acceptance criteria | 403 when user not in group; 404 when group not in tenant; response shape per OpenAPI `Group`. |

### FR-10 — k-gated mood aggregate

| Field | Content |
| --- | --- |
| Priority | Must |
| Statement | Authorized user SHALL request mood aggregate for `[from,to]` only if distinct active contributors in window ≥ k (default 5). |
| Acceptance criteria | Below threshold → 409 with stable `INSUFFICIENT_COHORT` code; never returns per-user breakdown below k. |

### FR-11 — Group actions

| Field | Content |
| --- | --- |
| Priority | Should |
| Statement | Admin/owner SHALL register an action (title, occurred_at, optional description) for correlation views. |
| Acceptance criteria | Members cannot create actions unless policy extended (MVP: admin/owner only). |

### FR-12 — Rate limiting

| Field | Content |
| --- | --- |
| Priority | Must |
| Statement | System SHALL expose rate-limit headers on list/aggregate/export paths and enforce limits per tenant+user. |
| Acceptance criteria | 429 with `Retry-After` on exhaustion (per API). |

### FR-13 — Export job

| Field | Content |
| --- | --- |
| Priority | Must |
| Statement | Authorized user SHALL start anonymized export job for group + window; poll job; receive time-limited download URL on success. |
| Acceptance criteria | Same k rules as aggregates; failed jobs retain `hub_error_code`; URLs expire ≤7 days. |

### FR-14 — Export polling / async notification

| Field | Content |
| --- | --- |
| Priority | Should (Could for webhooks in first release — see OpenAPI evolution) |
| Statement | Client SHALL poll job status; optional webhook callback MAY be configured per tenant (future FR linkage). |

### FR-15 — Clinical link create

| Field | Content |
| --- | --- |
| Priority | Must |
| Statement | Professional SHALL propose clinical link to patient with `consent_artifact_ids` array (min 1 UUID). |
| Acceptance criteria | Missing artifacts → 400; duplicates rejected; audit record created. |

### FR-16 — Clinical link states

| Field | Content |
| --- | --- |
| Priority | Must |
| Statement | Link SHALL pass through `pending → active → revoked`. |
| Acceptance criteria | Transitions in §14.2; invalid transitions return 409/403 as specified. |

### FR-17 — Admin read audit

| Field | Content |
| --- | --- |
| Priority | Must |
| Statement | Any successful admin-level read of member-level mood data in INDIVIDUAL / INDIVIDUAL_PUBLIC groups SHALL append immutable audit event. |
| Acceptance criteria | Audit includes actor, target, group, timestamp, correlation id; tamper-evidence strategy in NFR. |

### FR-18 — Visibility contribution

| Field | Content |
| --- | --- |
| Priority | Must |
| Statement | Member in `INDIVIDUAL` group SHALL set `visibility_contribution ∈ {hidden, visible_to_admins}`. |
| Acceptance criteria | Hidden contributions excluded from admin-visible member analytics paths; k-aggregate math still counts distinct members per policy table. |

### FR-19 — Clinical revoke

| Field | Content |
| --- | --- |
| Priority | Must |
| Statement | Patient SHALL revoke clinical link; processing stops for professional views within SLA. |
| Acceptance criteria | Revoke is idempotent; active sessions invalidated ≤15 minutes (NFR). |

### FR-20 — Health check

| Field | Content |
| --- | --- |
| Priority | Must |
| Statement | System SHALL expose unauthenticated liveness endpoint for orchestration. |
| Acceptance criteria | Returns 200 when process up; does not leak tenant data. |

---

## 8. Non-functional requirements

| ID | Category | Requirement | Validation |
| --- | --- | --- | --- |
| NFR-01 | Latency | p95 read APIs < 300 ms under nominal load test | k6/Grafana |
| NFR-02 | Latency | p95 aggregate compute < 800 ms for 90-day window ≤10k members | Load test |
| NFR-03 | Availability | 99.5% monthly API | Uptime probes |
| NFR-04 | RPO/RTO | RPO ≤ 24h; RTO ≤ 4h for regional failure | DR drill |
| NFR-05 | Backup | Encrypted backups; mood hard-delete reflected within 30 days in backup sets | Backup audit |
| NFR-06 | Security | TLS 1.2+ everywhere; JWT signature validation against hub JWKS | Pen test checklist |
| NFR-07 | Observability | Structured logs with `trace_id`; RED metrics per service | Dashboard review |
| NFR-08 | Compliance | LGPD Art. 11 sensitive processing defaults | Legal review + DPIA gate for workplace |

---

## 9. Business rules (formal)

### 9.1 Rule list

1. **BR-k:** Default k=5 for aggregates/exports; system MUST NOT allow tenant to lower k below 5 in MVP.
2. **BR-visibility-IND:** In `INDIVIDUAL` groups, admins only see per-member series where `visibility_contribution = visible_to_admins`.
3. **BR-visibility-PUB:** In `INDIVIDUAL_PUBLIC`, member-to-member visibility is forbidden; only admins see individual series.
4. **BR-GROUPED:** In `GROUPED`, all members see group-level mood contributions subject to member opt-in flags (same enum).
5. **BR-export:** Export SHALL use same cohort definition as aggregate k-gate.

### 9.2 Decision table — can admin view member mood detail?

| Group type | visibility_contribution | Admin view individual time series? |
| --- | --- | --- |
| INDIVIDUAL_PUBLIC | any | Yes (admin only) |
| INDIVIDUAL | hidden | No (only k-aggregate) |
| INDIVIDUAL | visible_to_admins | Yes |
| GROUPED | hidden | No (peer/aggregate views per UI) |
| GROUPED | visible_to_admins | Yes for peers per UX-02 |

*Exact peer rules MUST follow same table for non-admin members in GROUPED.*

### 9.3 Decision table — aggregate/export eligibility

| Distinct contributors in window | k | Result |
| --- | --- | --- |
| < k | 5 | Deny (409 INSUFFICIENT_COHORT) |
| ≥ k | 5 | Allow |

---

## 10. User flows (narrative + failures)

### 10.1 Mood logging (happy path)

1. User opens app → hub SSO → lands on home.
2. User selects mood value + optional note → save.
3. System persists with server timestamp if skew > 5m.
**Failures:** offline queue (Could) — MVP: show error; hub token expired → redirect SSO.

### 10.2 Invite and accept

1. Admin creates invite for user B.
2. B receives notification (out of band) → accepts.
**Failures:** invite revoked → 403; wrong user → 403.

### 10.3 Export job

1. Admin requests export for window.
2. Policy engine validates k.
3. Worker builds anonymized artifact → job succeeded → URL issued.
**Failures:** below k → 409; worker failure → `failed` state with code.

### 10.4 Clinical link

1. Professional creates link with consent artifacts → pending/active per policy.
2. Patient may revoke.
**Failures:** missing consent → 400; professional tries revoke → 403.

---

## 11. Edge cases and error scenarios

- **Clock skew:** If client `recorded_at` differs >300s from server, ignore client time for compliance ordering.
- **Duplicate idempotency key:** Replays same response without double write.
- **Concurrent role change:** Operations use latest membership snapshot; 409 if mid-flight revoke.
- **Member exits group mid-export:** Export job cohort frozen at job creation time (Must).
- **Hub user merge:** Open question OQ-01 — operational playbook until automated merge exists.

---

## 12. Data definitions (semantic)

### 12.1 UserProfile

| Field | Type | Constraints | Meaning |
| --- | --- | --- | --- |
| user_id | UUID | immutable | Hub subject |
| display_name | string | ≤120 | Non-official name |

### 12.2 MoodEntry

| Field | Type | Constraints | Meaning |
| --- | --- | --- | --- |
| entry_id | UUID | PK | Entry id |
| user_id | UUID | FK | Owner |
| value | int | 1–5 | Ordinal mood scale |
| note | text | ≤2000, optional | Free text sensitive |
| recorded_at | timestamptz | required | Event time |

### 12.3 Group

| Field | Type | Constraints | Meaning |
| --- | --- | --- | --- |
| group_id | UUID | PK | Group |
| tenant_id | UUID | required | Org isolation |
| name | string | ≤200 | Label |
| type | enum | immutable | Visibility model |

### 12.4 Membership

| Field | Type | Constraints | Meaning |
| --- | --- | --- | --- |
| role | enum | owner/admin/member | Permissions |
| visibility_contribution | enum | hidden / visible_to_admins | INDIVIDUAL/GROUPED contribution |

### 12.5 ExportJob

| Field | Meaning |
| --- | --- |
| state | queued / processing / succeeded / failed |
| download_url | Presigned URL nullable until success |

### 12.6 ClinicalLink

| Field | Meaning |
| --- | --- |
| consent_artifact_ids | References to hub-stored consent proofs (content not duplicated) |

---

## 13. State modeling

### 13.1 Group lifecycle

`active → archived` (Could MVP: treat all as active; deletion out of scope — **OQ-02**).

### 13.2 Membership

`pending_invite → active → left` (left is terminal; historical mood retained per retention unless delete rights exercised).

### 13.3 Export job

`queued → processing → succeeded | failed` (no backward transitions; retry creates new job).

### 13.4 Clinical link

`pending → active → revoked`  
Invalid: `revoked → active` without new link creation.

---

## 14. Open questions

| ID | Topic | Impact |
| --- | --- | --- |
| OQ-01 | Hub-driven account merge | Audit attribution |
| OQ-02 | Hard delete group vs archive | Data residency |
| OQ-03 | Instagram integration scope | Future SRS module |

---

## 15. Traceability note

Requirement IDs FR-01–FR-20 align with API annotations where present; gaps SHALL be closed in API governance doc.
