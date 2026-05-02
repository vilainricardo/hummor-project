# 📄 Vision Document

## Produto  
**MindSignal**

Plataforma Mobile de Monitoramento Contínuo em Saúde Mental com Alertas Informativos para Apoio ao Tratamento Psiquiátrico

---

## 1. Problema

Pacientes em tratamento psiquiátrico apresentam dificuldade em relatar com precisão sua evolução emocional entre consultas, dependendo de memória subjetiva.

Psiquiatras, por sua vez, não possuem visibilidade contínua sobre o estado do paciente, especialmente em momentos críticos, como crises de ansiedade, recaídas ou ideação suicida.

Isso resulta em:
- Decisões clínicas baseadas em dados incompletos  
- Falta de visibilidade sobre eventos relevantes entre sessões  
- Baixo engajamento do paciente no acompanhamento do próprio estado mental  

---

## 2. Público-Alvo

### Primário  
- Pacientes em acompanhamento psiquiátrico contínuo  

### Secundário  
- Psiquiatras que realizam acompanhamento longitudinal  

**Nota (conta de utilizador):** no modelo de produto, **toda conta é paciente**; o psiquiatra corresponde à **mesma noção de utilizador** com **capacidade médica adicional** (ex.: `is_doctor`), e não a um “tipo” exclusivo em alternativa a paciente.

---

## 3. Stakeholders

- Pacientes  
- Psiquiatras  
- Clínicas de saúde mental  

---

## 4. Proposta de Valor

Converter registros simples do paciente em **sinais clínicos informativos**, permitindo:

- Acompanhamento contínuo entre consultas  
- Visibilidade sobre eventos relevantes em tempo real  
- Maior contexto para decisões clínicas  
- Maior consciência do paciente sobre seu próprio estado  

---

## 5. Visão do Produto

Uma aplicação mobile com interação mínima, onde o paciente registra rapidamente seu estado diário (sono e humor) e eventos relevantes por meio de marcações simples.

O sistema organiza esses dados e, quando configurado pelo médico, envia **notificações informativas em tempo real** sobre eventos críticos.

O médico mantém controle total sobre essas notificações, podendo ativá-las, desativá-las por paciente ou globalmente.

A plataforma atua como uma extensão leve do acompanhamento clínico, sem impor obrigações operacionais ao profissional.

---

## 6. Funcionalidades Principais (Alto Nível)

### Para pacientes
- Registro simples de humor (escala numérica)
- Registro de sono via notificação
- Adição rápida de eventos e pensamentos relevantes
- Seleção de tags pré-definidas (ex: ansiedade, pânico, ideação suicida)
- Controle de quais médicos têm acesso aos seus dados

### Para psiquiatras
- Visualização consolidada da evolução do paciente
- Configuração de monitoramento individual por paciente
- Controle total de notificações:
  - Ativar/desativar por paciente  
  - Desativar globalmente (ex: indisponibilidade)  

### Sistema
- Envio de **alertas informativos em tempo real**, como:
  - “Paciente X registrou episódio de pânico”
  - “Paciente Y indicou risco de recaída”
  - “Paciente Z registrou ideação suicida”
- Organização de histórico e tendências ao longo do tempo

---

## 7. Diferencial Competitivo

- Foco em simplicidade extrema para garantir adesão  
- Alertas informativos sem impor responsabilidade clínica direta  
- Controle granular de notificações pelo médico  
- Compartilhamento de dados controlado pelo paciente  
- Integração leve no fluxo de trabalho do profissional  

---

## 8. Objetivos de Negócio

- Maximizar adesão e retenção de pacientes  
- Validar valor percebido por psiquiatras  
- Adquirir médicos via oferta gratuita inicial  
- Converter médicos para plano pago após período de uso  
- Evoluir para uma plataforma mais ampla de suporte clínico  

---

## 9. Métricas de Sucesso

- Frequência de registros (humor/sono)  
- Retenção de pacientes ao longo do tempo  
- Número médio de pacientes ativos por médico  
- Frequência de acesso dos médicos  
- Taxa de ativação e manutenção de alertas  
- Conversão de médicos para plano pago  

---

## 10. Monetização

### Estratégia inicial
- Gratuito para psiquiatras (6–12 meses)  
- Gratuito para pacientes  

### Estratégia futura
- Assinatura para psiquiatras baseada em uso e valor percebido  

---

## 11. Restrições

- Conformidade com LGPD (dados sensíveis)  
- Natureza informativa dos alertas (sem obrigação de ação)  
- Segurança e privacidade de dados  
- Gestão de expectativas clínicas  

---

## 12. Assumptions

- Interações mínimas aumentam adesão  
- Pacientes respondem melhor a inputs simples  
- Médicos valorizam visibilidade passiva  
- Controle de notificações reduz resistência  
- Pacientes aceitarão compartilhar dados com múltiplos médicos  

---

## 13. Riscos

- Baixo engajamento ao longo do tempo  
- Alertas serem ignorados  
- Excesso de notificações gerar fadiga  
- Dificuldade em demonstrar valor para médicos  
- Sensibilidade legal em torno de dados críticos  

---

## 14. Glossário

- **Tag:** marcação de um estado mental específico  
- **Alerta informativo:** notificação enviada ao médico sem exigir ação  
- **Adesão:** frequência de uso pelo paciente  
- **Monitoramento contínuo:** acompanhamento fora das consultas  
- **Sinal clínico:** informação relevante para contexto médico  

---

## 15. Ambiguidades e Próximos Passos

- Definição de critérios refinados para disparo de alertas  
- Estratégia para evitar fadiga de notificações  
- Apresentação clara de valor para médicos  
- Estratégia inicial de aquisição  
- Balanceamento entre simplicidade e utilidade clínica  

---

## 16. Perguntas para Próxima Iteração

- Quais tipos de eventos devem gerar alertas por padrão?  
- O médico poderá configurar categorias de alertas ou apenas selecionar tags?  
- Haverá algum tipo de resumo periódico para o médico?  
- Como reforçar valor para o paciente sem aumentar complexidade?  

