# Vision Document — Hub de Controle de Humor

## 1. Introdução

### 1.1 Propósito

Este documento descreve a visão do sistema **Hub de Controle de Humor**, uma aplicação digital destinada ao monitoramento emocional de indivíduos e grupos, com foco em análise temporal e impacto de ações sobre o bem-estar.

### 1.2 Escopo

O sistema permitirá:

- Registro de humor individual (CPF)
- Organização em grupos (CPF e CNPJ)
- Tipos de grupo com diferentes regras de visibilidade
- Análise gráfica de evolução emocional
- Correlação com ações/intervenções
- Compartilhamento de dados (com controle de privacidade)
- Integração com redes sociais (Instagram)

O sistema será parte de um **hub de aplicações com autenticação unificada (SSO)**.

---

## 2. Problema de Negócio

Atualmente:

- Não há ferramentas simples para acompanhamento contínuo de humor
- Empresas (CNPJ) não conseguem medir impacto emocional de ações internas
- Indivíduos não conseguem correlacionar hábitos com seu estado emocional
- Falta visualização clara e histórica de evolução emocional

Além disso:

- Profissionais da saúde (psicólogos, terapeutas, etc.) não possuem ferramentas práticas para acompanhar a evolução emocional de seus pacientes ao longo do tempo
- Pacientes não possuem uma forma estruturada e visual de acompanhar seu próprio progresso emocional e os efeitos de tratamentos ou hábitos

O sistema propõe resolver essas lacunas oferecendo uma plataforma estruturada de acompanhamento individual e coletivo.

---

## 3. Objetivos do Produto

### 3.1 Objetivos principais

- Permitir auto-monitoramento emocional
- Oferecer análise visual clara e temporal
- Permitir gestão de grupos (empresas, comunidades, profissionais de saúde e pacientes)
- Correlacionar ações com mudanças de humor
- Garantir controle de privacidade

### 3.2 Objetivos secundários

- Incentivar engajamento contínuo
- Permitir compartilhamento social
- Criar base para futuros insights (ex: IA)

---

## 4. Stakeholders

| Stakeholder | Interesse |
|------------|----------|
| Usuário CPF | Monitorar humor |
| Usuário CNPJ | Analisar grupos |
| Administrador de grupo | Gestão e análise |
| Profissionais de saúde | Acompanhar pacientes |
| Produto | Engajamento |
| Engenharia | Escalabilidade e segurança |

---

## 5. Personas

### 5.1 Pessoa Física (CPF)

- Registra humor regularmente
- Quer entender padrões
- Pode participar de grupos
- Pode criar grupos

### 5.2 Empresa (CNPJ)

- Cria grupos (ex: funcionários)
- Avalia impacto de ações
- Define líderes e admins

---

## 6. Tipos de Grupos e Regras de Visibilidade

O sistema suportará diferentes tipos de grupos, cada um com regras específicas de visualização e privacidade:

### 6.1 Membros Individuais Públicos

- Dados dos membros são sempre visíveis para administradores
- Exportações são sempre anonimizadas
- Membros não conseguem visualizar dados uns dos outros

### 6.2 Membros Individuais

- Apenas administradores podem visualizar os dados individuais
- Usuários podem definir seus dados como públicos ou ocultos
- Regras de anonimização são aplicadas conforme configuração dos membros

### 6.3 Membros Agrupados

- Todos os membros podem visualizar os dados do grupo
- Usuários podem definir seus dados como públicos ou ocultos
- Regras de anonimização são aplicadas conforme configuração dos membros

---

## 7. Proposta de Valor

### Para CPF:

- Autoconhecimento emocional
- Visualização clara de evolução
- Correlação com hábitos

### Para CNPJ:

- Insights sobre bem-estar coletivo
- Avaliação de iniciativas internas
- Gestão de grupos

---

## 8. Diferenciais

- Controle de anonimato dinâmico
- Correlação com ações
- Multi-tenant com hub integrado
- Modelo híbrido (individual + organizacional)

---

## 9. Restrições

- LGPD (dados sensíveis)
- Integração com Instagram
- Escalabilidade de gráficos
- Multi-tenant

---

## 10. Premissas

- Usuários autenticados via JWT (hub)
- Sistema web mobile-first
- Backend via API
- Dados altamente sensíveis

---

## 11. Riscos já identificados

- Anonimização incorreta
- Reidentificação indireta
- Complexidade de grupos
- Crescimento de dados temporais