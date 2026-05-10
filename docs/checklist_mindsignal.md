# Checklist MindSignal — alinhamento docs ↔ código

Este ficheiro consolida requisitos dos documentos em `docs/` e o estado observado no repositório backend (Java/Spring Boot). Não há app Flutter nem serviços de notificação neste workspace.

**Legenda**

- `[x]` — implementado no backend/repo actual
- `[ ]` — pendente ou apenas especificado na documentação
- `[~]` — parcial ou sem verificação automática no repo

**Última revisão:** 2026-05-10 — alinhado a `mind_signal_srs.md`, SAD, código em `src/main/java` e migrações Flyway.

---

## 1. SRS — Requisitos funcionais

| ID | Item | Estado |
|----|------|--------|
| FR-001 | Código de utilizador único (público) | [~] Código no registo; unicidade e `GET /by-code/{code}`; geração automática pelo servidor não exposta como feature isolada |
| FR-002 | Vínculo bilateral (ambos adicionam código) | [x] `MutualDoctorPatientLinkService` + `POST …/mutual-links/doctor-code` (com `accessStartDate`) e `…/patient-code` |
| FR-003 | Desvinculação com estados (ativo / com acesso / sem acesso) | [~] Campo `DoctorPatientLinkStatus` em `doctor_patients` (`DoctorPatientAssociation`); **falta** API/serviço explícito para transições de estado |
| FR-004 | Data de início de acesso (paciente define partilha) | [x] `access_start_date` no vínculo mútuo e na lista `doctor_patients`; leituras médico filtram `created_at` ≥ consentimento (`DoctorPatientScaleDataService`) |
| FR-005 | Registo de humor | [~] Entidade `MoodEntry` + tabela `mood_entries` (escala 0–10 produto); **falta** endpoint paciente para criar registo |
| FR-006 | Registo de sono | [~] Entidade `SleepEntry` + tabela `sleep_entries` (0–10); **falta** endpoint paciente para criar registo |
| FR-007 | Registo de evento com 1 tag (união `user_tag_assignments`) | [ ] Sem entidade/evento TAG nem API paciente |
| FR-008 | Configuração de tags (POST/DELETE, ≤5 por médico, etc.) | [x] Migrações + `UserService` + `UserController` + testes |
| FR-009 | Tags críticas | [ ] `is_critical` ainda não no esquema (SAD FLW-013) |
| FR-010 | Evento de alerta para tag crítica | [ ] Depende de FR-009 + pipeline |
| FR-011 | Cooldown entre notificações | [ ] |
| FR-012 | Supressão dentro do cooldown | [ ] |
| FR-013 | Contador de eventos críticos | [ ] |
| FR-014 | Lista de pacientes com indicadores | [~] Roster médico–paciente existe; leitura de humor/sono com período/consentimento; **falta** lista agregada com indicadores FR-014 |
| FR-015 | Filtro ativos/inativos | [ ] |
| FR-016 | Detalhe do paciente (gráficos, histórico) | [~] GET mood/sleep/scale-history para médico com `from`/`to`; gráficos/UI fora do backend |
| FR-017 | Reset de contador ao abrir detalhe | [ ] |
| FR-018 | Tendência (média 3 vs 3) | [ ] |
| FR-019 | Exibição de tendência (UI) | [ ] Cliente mobile fora do repo |

---

## 2. SRS — Requisitos não funcionais

| ID | Item | Estado |
|----|------|--------|
| NFR-001 | Resposta ≤ 3 s | [~] Sem testes de carga/SLA |
| NFR-002 | ≤ 2 interações (usabilidade) | [ ] Produto/UI |
| NFR-003 | Sem busca aberta; dados protegidos | [~] Dados clínicos só com vínculo + consentimento + estado; **sem** autenticação JWT/OAuth no código |

---

## 3. Vision document — funcionalidades de alto nível

| Tema | Estado |
|------|--------|
| Registo simples de humor e sono | [~] Modelo e leitura médico; **falta** API paciente para submeter escala |
| Tags pré-definidas e controlo de acesso a médicos | [~] Tags + atribuições + FR-004; eventos tag ainda não |
| Vista consolidada e tendências para o médico | [~] Histórico escala com janela temporal; tendência/c gráficos não |
| Alertas informativos em tempo real | [ ] |
| Controlo fino de notificações pelo médico | [ ] |

---

## 4. SRS Support Notes — princípios e API de tags

| Tema | Estado |
|------|--------|
| Conta “paciente primeiro” + `is_doctor` | [x] `User` / `Doctor` (JOINED) |
| `user_tag_assignments` (≤5, união, POST/DELETE) | [x] |
| Loop diário / UX leve | [ ] Flutter |

---

## 5. SAD — Stack e modelo de dados

| Item | Estado |
|------|--------|
| Java 21 + Spring Boot | [x] |
| PostgreSQL + Flyway | [x] |
| Modelo `users` + endereço estruturado | [x] |
| `user_tag_assignments` | [x] |
| Tabela unificada `events` (MOOD/SLEEP/TAG) | [ ] Modelo actual: `mood_entries` + `sleep_entries` (dedicados) |
| Relação médico–paciente com `status` + data | [~] `doctor_patients` via `DoctorPatientAssociation` (`access_start_date`, `DoctorPatientLinkStatus`), não a tabela `relationships` do SAD literal |
| App Flutter | [ ] Fora do repo |
| Fila + notificação + push | [ ] |
| API Gateway | [ ] |
| EDA / append-only | [~] Inserções de humor/sono são append-only; sem fila |

---

## 6. SAD — Fluxos (amostra)

| Fluxo | Descrição breve | Estado |
|-------|-----------------|--------|
| FLW-011 | Registo de evento TAG | [ ] |
| FLW-012 | Configuração de tags | [x] |
| FLW-013 / FLW-014 | Tag crítica / processamento | [ ] |
| FLW-022–024 | Lista / filtro / detalhe paciente | [~] Leitura escala + consentimento; lista/filtro completos FR-014–015 não |
| FLW-026–030 | Tendência | [ ] |
| FLW-032 | Health (DB + fila) | [~] App sobe; fila não |
| FLW-034 | Audit logging | [ ] |

---

## 7. SRS — apêndice “SAD SUPPORT” (`mind_signal_srs.md`)

| Item | Estado |
|------|--------|
| Código fixo | [~] `code` único; mutável via `PUT` |
| Append-only (eventos humor/sono) | [~] Inserções novas; sem tabela `events` unificada |

---

## 8. Internacionalização (API)

| Item | Estado |
|------|--------|
| Negociação `Accept-Language` + fallback `en-US` | [x] `I18nConfig` (`AcceptHeaderLocaleResolver`) |
| Bundles regionais (`messages_*_*`, `ValidationMessages_*_*`) | [x] e.g. `en_US`, `en_GB`, `pt_BR`, `pt_PT`, `es_ES`, `es_MX` |
| Documentação OpenAPI (cabeçalho descrito globalmente) | [x] `OpenApiConfig` |
| Testes integração idioma | [x] `AcceptLanguageIntTest` |

---

## 9. Resumo executivo

**Já implementado (backend):** utilizadores e perfil SAD; vínculo **mútuo** com **data de partilha** (FR-004) e **estado** de vínculo na lista (FR-003 parcial); **FR-008**; entidades **humor/sono** (escala 0–10) e **API médico** para listar histórico com **período** e **consentimento**; mensagens da API **poliglotas** (en/pt/es regionais) e **testes** de locale.

**Em aberto (SRS/SAD):** submissão **paciente** de humor/sono/tags; **tags críticas** e alertas; **notificações**; lista/indicadores **completos** FR-014–019; **Flutter**, **fila**, **JWT**, auditoria enterprise.

---

## 10. Como atualizar este ficheiro

1. Após implementar um requisito, alterar `[ ]` / `[~]` para `[x]` e, se útil, referência curta (classe, migração, endpoint).
2. Manter coerência com `mind_signal_srs.md` e `SAD.md` quando mudarem.
