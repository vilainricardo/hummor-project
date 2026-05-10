# Checklist MindSignal — alinhamento docs ↔ código

Este ficheiro consolida requisitos dos documentos em `docs/` e o estado observado no repositório backend (Java/Spring Boot em `hummor-project`). Não há app Flutter nem serviços de notificação neste workspace.

**Legenda**

- `[x]` — implementado no backend/repo atual
- `[ ]` — pendente ou apenas especificado na documentação
- `[~]` — parcial ou sem verificação automática no repo

**Última revisão:** gerada a partir de `mind_signal_srs.md`, `srs_support_notes_mind_signal.md`, `SAD.md`, `vision_document_mind_signal.md` e inventário do código.

---

## 1. SRS — Requisitos funcionais

| ID | Item | Estado |
|----|------|--------|
| FR-001 | Código de utilizador único (público) | [~] Código fornecido no registo; unicidade e `GET /by-code/{code}` existem; geração automática pelo servidor não está isolada no código |
| FR-002 | Vínculo bilateral (ambos adicionam código) | [x] `MutualDoctorPatientLinkService` + `POST …/mutual-links/doctor-code` e `…/patient-code` |
| FR-003 | Desvinculação com estados (ativo / desvinculado com acesso / desvinculado sem acesso) | [ ] Sem modelo `relationships` com `status` nem API equivalente ao SRS |
| FR-004 | Data de início de acesso (paciente define partilha) | [ ] Sem `access_start_date` nem filtro de eventos por data |
| FR-005 | Registo de humor (escala 1–5, 1 toque) | [ ] Sem entidade/API de eventos |
| FR-006 | Registo de sono (escala 1–5) | [ ] Sem entidade/API de eventos |
| FR-007 | Registo de evento com 1 tag (união `user_tag_assignments`) | [ ] Sem pipeline de eventos; catálogo + atribuições existem |
| FR-008 | Configuração de tags (POST/DELETE, ≤5 por médico, `is_doctor` no atribuidor, união deduplicada) | [x] Alinhado ao SRS/Support notes + testes de API |
| FR-009 | Tags críticas | [ ] Documentação: `is_critical` ainda não no esquema (`user_tag_assignments`) |
| FR-010 | Evento de alerta para tag crítica | [ ] Depende de FR-009 + processamento |
| FR-011 | Cooldown entre notificações | [ ] |
| FR-012 | Supressão dentro do cooldown | [ ] |
| FR-013 | Contador de eventos críticos | [ ] |
| FR-014 | Lista de pacientes com indicadores | [ ] Existe roster (`doctor_patients`); falta endpoint “lista médico + indicadores” |
| FR-015 | Filtro ativos/inativos | [ ] |
| FR-016 | Detalhe do paciente (gráficos, histórico) | [ ] |
| FR-017 | Reset de contador ao abrir detalhe | [ ] |
| FR-018 | Tendência (média últimos 3 vs 3 anteriores) | [ ] |
| FR-019 | Exibição de tendência (UI) | [ ] Cliente mobile fora do repo |

---

## 2. SRS — Requisitos não funcionais

| ID | Item | Estado |
|----|------|--------|
| NFR-001 | Resposta ≤ 3 s | [~] Sem testes de carga/SLA no repo |
| NFR-002 | ≤ 2 interações (usabilidade) | [ ] Requisito de produto/UI |
| NFR-003 | Sem busca aberta; dados protegidos | [~] API exposta; sem camada JWT/OAuth encontrada no código |

---

## 3. Vision document — funcionalidades de alto nível

| Tema | Estado |
|------|--------|
| Registo simples de humor e sono | [ ] |
| Tags pré-definidas e controlo de acesso a médicos | [~] Tags e atribuições sim; eventos e partilha fina incompletos |
| Vista consolidada e tendências para o médico | [ ] |
| Alertas informativos em tempo real | [ ] |
| Controlo fino de notificações pelo médico | [ ] |

---

## 4. SRS Support Notes — princípios e API de tags

| Tema | Estado |
|------|--------|
| Conta “paciente primeiro” + capacidade médica (`is_doctor`) | [x] Modelo `User` / `Doctor` (JOINED) |
| `user_tag_assignments`: união deduplicada, ≤5 por médico, POST/DELETE | [x] Migrações + `UserService` + `UserController` |
| Loop diário / UX leve | [ ] Produto Flutter |

---

## 5. SAD — Stack e modelo de dados

| Item | Estado |
|------|--------|
| Java 21 + Spring Boot | [x] |
| PostgreSQL + Flyway | [x] |
| Modelo `users` com endereço estruturado | [x] |
| Tabela `user_tag_assignments` | [x] |
| Tabela `events` (tipos MOOD / SLEEP / TAG) | [ ] |
| Entidade `relationships` com `status` e `access_start_date` (como no SAD) | [ ] Uso de `doctor_patients` + fluxo mútuo, sem estados do SRS |
| App Flutter (camada cliente) | [ ] Fora do repo |
| Fila + serviço de notificação + push | [ ] |
| API Gateway (diagrama SAD) | [ ] |
| Arquitetura orientada a eventos / eventos append-only | [ ] |

---

## 6. SAD — Fluxos (amostra; ver documento completo para FLW-001…)

| Fluxo | Descrição breve | Estado |
|-------|-----------------|--------|
| FLW-011 | Registo de evento TAG | [~] Só configuração de tags (FR-008); registo de evento não |
| FLW-012 | Configuração de tags | [x] |
| FLW-013 | Tag crítica | [ ] |
| FLW-014 | Processamento de evento crítico | [ ] |
| FLW-022–024 | Lista / filtro / detalhe paciente (médico) | [ ] |
| FLW-026–030 | Coleta/cálculo/exibição de tendência | [ ] |
| FLW-032 | Health check (DB + fila) | [~] App sobe; health alinhado ao SAD com fila não |
| FLW-034 | Audit logging imutável | [ ] |
| FLW-035–042 | Retry, cache, backup, CI/CD, etc. | [ ] No âmbito deste repositório |

---

## 7. SRS — apêndice “SAD SUPPORT” (fim de `mind_signal_srs.md`)

| Item | Estado |
|------|--------|
| Código fixo | [~] `code` único; alterável via `PUT` utilizador |
| Append-only (eventos) | [ ] |
| Pendência “evolução de tendência” | [ ] |

---

## 8. Resumo executivo

**Em grande parte feito no backend:** utilizadores (perfil alinhado ao SAD), unicidade de `code`, vínculo **mútuo** paciente–médico, catálogo de tags, **FR-008** (atribuições com limite de cinco por médico, união na leitura, validações de papel médico).

**Ainda por fazer (SRS/SAD):** eventos (humor, sono, tag), relação com **estados** e **data de acesso**, **tags críticas** e persistência, **notificações** (cooldown, supressão, contadores), **API e UI médico** (lista, detalhe, tendência, gráficos), **cliente Flutter**, **mensageria** e derivados da arquitetura “enterprise” (auditoria imutável, filas, etc.).

---

## 9. Como atualizar este ficheiro

1. Após implementar um requisito, alterar `[ ]` ou `[~]` para `[x]` e, se útil, uma nota de uma linha (classe, migração ou endpoint).
2. Manter coerência com `mind_signal_srs.md` e `SAD.md` quando estes mudarem.
