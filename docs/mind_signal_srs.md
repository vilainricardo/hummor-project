# SOFTWARE REQUIREMENTS SPECIFICATION (SRS) — MindSignal v2.0

## 1. Introdução

### 1.1 Propósito
Documento completo de requisitos para desenvolvimento, testes e arquitetura.

### 1.2 Premissas
- Tempo máximo de interação por sessão: ≤ 60 segundos
- Sistema informativo, não clínico
- Controle total de dados pelo paciente

---

## 2. Requisitos Funcionais

### FR-001 — Geração de Código de Usuário
Descrição: Gerar identificador único público.
Prioridade: Must
Validação:
- Critério: unicidade
- Método: teste de base
Dependências: —

---

### FR-002 — Vinculação Bilateral
Descrição: Vínculo criado apenas após ambos adicionarem código.
Prioridade: Must
Validação:
- Critério: vínculo somente após dupla confirmação
- Método: teste de fluxo
Dependências: FR-001

---

### FR-003 — Desvinculação
Descrição: Permitir alteração do estado do vínculo.
Estados:
- Ativo
- Desvinculado com acesso
- Desvinculado sem acesso
Regras:
- Com acesso → mantém histórico
- Sem acesso → remove acesso
Prioridade: Must
Validação:
- Critério: estado e acesso corretos
- Método: teste funcional + autorização
Dependências: FR-002, FR-004

---

### FR-004 — Data de Acesso
Descrição: Paciente define início de compartilhamento.
Prioridade: Must
Validação:
- Critério: filtro correto
- Método: teste funcional
Dependências: —

---

### FR-005 — Registro de Humor
Descrição: Registro em escala 1–5.
Regras: 1 toque
Prioridade: Must
Validação:
- Critério: valores válidos
- Método: teste funcional + UX
Dependências: —

---

### FR-006 — Registro de Sono
Descrição: Escala 1–5.
Prioridade: Must
Validação:
- Critério: valores válidos
- Método: teste funcional
Dependências: —

---

### FR-007 — Registro de Tags
Descrição: Seleção de 1 tag por evento.
Regras:
- Apenas tags configuradas
Prioridade: Must
Validação:
- Critério: restrição respeitada
- Método: teste funcional
Dependências: FR-008

---

### FR-008 — Configuração de Tags
Descrição: Médico define até 5 tags.
Prioridade: Must
Validação:
- Critério: limite respeitado
- Método: teste funcional
Dependências: —

---

### FR-009 — Tags Críticas
Descrição: Médico define tags críticas.
Prioridade: Must
Validação:
- Critério: marcação correta
- Método: teste funcional
Dependências: FR-008

---

### FR-010 — Evento de Alerta
Descrição: Gerar evento para tag crítica.
Prioridade: Must
Validação:
- Critério: evento correto
- Método: teste integrado
Dependências: FR-009, FR-011

---

### FR-011 — Cooldown
Descrição: Intervalo mínimo entre notificações.
Prioridade: Must
Validação:
- Critério: bloqueio correto
- Método: teste temporal
Dependências: —

---

### FR-012 — Supressão
Descrição: Não notificar dentro do cooldown.
Prioridade: Must
Validação:
- Critério: supressão correta
- Método: teste integrado
Dependências: FR-011

---

### FR-013 — Contador
Descrição: Contar eventos críticos.
Prioridade: Must
Validação:
- Critério: incremento correto
- Método: teste funcional
Dependências: FR-010

---

### FR-014 — Lista de Pacientes
Descrição: Exibir lista com indicadores.
Prioridade: Must
Validação:
- Critério: elementos exibidos
- Método: teste UI
Dependências: FR-018

---

### FR-015 — Filtro
Descrição: Alternar ativos/inativos.
Prioridade: Must
Validação:
- Critério: filtro correto
- Método: teste funcional
Dependências: —

---

### FR-016 — Detalhe do Paciente
Descrição: Exibir gráficos e histórico.
Prioridade: Must
Validação:
- Critério: dados corretos
- Método: teste funcional
Dependências: FR-004

---

### FR-017 — Reset de Contador
Descrição: Zerar ao abrir detalhe.
Prioridade: Must
Validação:
- Critério: reset correto
- Método: teste funcional
Dependências: FR-013, FR-016

---

### FR-018 — Tendência
Descrição: Detectar piora (média últimos 3 < anteriores).
Prioridade: Must
Validação:
- Critério: cálculo correto
- Método: teste matemático
Dependências: FR-005, FR-006

---

### FR-019 — Exibição de Tendência
Descrição: Exibir indicador visual.
Prioridade: Must
Validação:
- Critério: indicador correto
- Método: teste UI
Dependências: FR-018

---

## 3. Requisitos Não Funcionais

### NFR-001 Performance
≤ 3 segundos

### NFR-002 Usabilidade
≤ 2 interações

### NFR-003 Segurança
- Sem busca aberta
- Dados protegidos

---

## 4. Assumptions
- Usuário aceita código

---

## 5. Glossário
- Tag, Evento, Alerta, Cooldown

---

# SAD SUPPORT DOCUMENT

## Decisões
- Código fixo
- Append-only

## Riscos
- Notificações

## Pendências
- Evolução de tendência

