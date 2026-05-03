# 📄 SRS Support Notes (Complementar)

## Produto
**MindSignal**

---

## Objetivo deste Documento

Este documento reúne decisões, diretrizes e nuances que **não fazem parte do Vision Document nem do Pré-SRS**, mas são essenciais para orientar a criação de um SRS consistente, evitando ambiguidades e retrabalho.

---

## 1. Princípios de Produto (Devem Guiar o SRS)

### 1.1 Simplicidade acima de tudo
- Qualquer funcionalidade que aumente atrito deve ser evitada
- Priorizar interações de 1–2 toques
- Evitar campos abertos sempre que possível

### 1.2 Baixa carga cognitiva
- Usuário não deve precisar “pensar” para usar
- Decisões complexas devem ser evitadas

### 1.3 Não responsabilização clínica
- Sistema é **informativo**, não prescritivo
- Nenhuma funcionalidade deve implicar obrigação do médico

### 1.4 Controle do usuário (paciente)
- Paciente é dono dos dados
- Compartilhamento sempre explícito e reversível

### 1.5 Identidade do utilizador (conta única)
- **Todo registo na aplicação é**, em primeiro lugar, **paciente** (quem regista humor, sono, tags e controla a partilha).
- A mesma conta pode **também** ter capacidade de **médico** (`is_doctor`): não se modela “ou paciente ou médico” como tipos excludentes.
- Autorização de rotas de profissional deve assentar em **flag/capacidade médica**, não num `type` mutuamente exclusivo com paciente.

### 1.6 Tags por paciente (multi‑médico, API)

- Persistência atual: **`user_tag_assignments`** — no máximo **uma** entrada por terno (paciente, tag de catálogo, médico atribuídor); inclui **quem atribuiu** e **quando**. Dois médicos **podem** referir a mesma tag de catálogo para o mesmo paciente (duas linhas).
- Ligação/desligação: **`POST /api/v1/users/{patientId}/tag-assignments`** (corpo `assignedByDoctorId`, `tagId`) para adicionar; **`DELETE …/tag-assignments/{tagId}?assignedByDoctorId=`** para remover apenas a linha desse médico. O **`PUT /api/v1/users/{id}`** é só perfil (**code**, **doctor**, demographics), **sem** campo de tags.
- Até **cinco** etiquetas de catálogo **distintas** por **médico** por paciente; tentar uma sétima etiqueta nova nesse slice → erro de negócio (`TAG_ASSIGNMENT_SLICE_FULL`). Remover com DELETE por tag/médico; outras linhas sob outros médicos permanecem.
- O paciente e as leituras agregadas devem assumir a **união deduplicada** por `tag_id` sobre todas as linhas.
- O campo **`assignedByDoctorId`** exige conta com **`is_doctor = true`**; conta só paciente (**não** médico) não pode actuar como atribuídor (`ASSIGNING_ACTOR_NOT_DOCTOR`). O utilizador-alvo (`patientId`) pode ter ou não **`is_doctor`** — sempre paciente primeiro; médico é capacidade acrescida, não tipo excludente.

---

## 2. Diretrizes de UX (Alto Nível)

### 2.1 Loop diário mínimo
- Notificação → ação rápida → fim
- Tempo ideal de interação: < 5 segundos

### 2.2 Registro de humor
- Deve ser direto (escala simples)
- Sem perguntas adicionais

### 2.3 Registro de tags
- Deve ser rápido (lista + seleção)
- Evitar múltiplas etapas

### 2.4 Notificações
- Devem ser leves e não invasivas
- Evitar linguagem alarmista

---

## 3. Diretrizes de Alertas

### 3.1 Natureza dos alertas
- Sempre informativos
- Nunca exigem ação

### 3.2 Linguagem
- Simples e direta
- Exemplo:
  - “Paciente registrou episódio de ansiedade”

### 3.3 Configurabilidade
- Médico controla:
  - Quais tags monitorar
  - Frequência de notificação

### 3.4 Evitar fadiga
- Sistema deve priorizar redução de ruído

---

## 4. Estratégia de Engajamento

### 4.1 Princípio central
> O sucesso depende da recorrência, não da profundidade

### 4.2 Evitar
- Gamificação complexa
- Inputs longos
- Questionários

### 4.3 Priorizar
- Consistência
- Facilidade
- Baixo esforço

---

## 5. Considerações Legais (Alto Nível)

- Deixar claro nos termos:
  - Sistema não substitui acompanhamento médico
  - Alertas são informativos
- Consentimento explícito para compartilhamento de dados
- Transparência sobre uso de dados sensíveis

---

## 6. Escopo Futuro (Não incluir no SRS atual)

- Agendamento de consultas
- Emissão de notas fiscais
- Integração com planos de saúde
- Inteligência clínica avançada

---

## 7. Anti-Objetivos (Muito Importante)

O sistema NÃO deve:
- Diagnosticar
- Sugerir tratamento
- Classificar gravidade clínica automaticamente
- Substituir decisão médica

---

## 8. Pontos de Atenção para o SRS

- Diferenciar claramente:
  - Evento vs Notificação
- Garantir flexibilidade na configuração
- Evitar dependência de inputs complexos
- Manter foco no MVP

---

## 9. Critérios de Qualidade do SRS

O SRS deve ser:
- Claro
- Não ambíguo
- Testável
- Coerente com simplicidade do produto

---

## 10. Perguntas que o SRS deve responder

- Quando exatamente um alerta é gerado?
- Como eventos são armazenados?
- Como funciona a relação paciente-médico?
- Como evitar excesso de notificações?
- Qual o fluxo mínimo do usuário?

---

## 11. Observação Final

Se o SRS começar a incluir complexidade excessiva, isso indica desvio da proposta original.

> Regra prática: se não melhora adesão ou valor clínico direto, provavelmente não deve entrar no MVP.

