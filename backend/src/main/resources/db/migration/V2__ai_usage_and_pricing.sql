-- Fase 2: precios de modelos, configuración de proveedor y registro de consumo.
-- Los precios semilla salen de la tarifa pública estándar de OpenAI
-- (https://developers.openai.com/api/docs/pricing) consultada el 2026-11-10.
-- Hay que actualizarlos cuando OpenAI cambie la tarifa; no están hardcodeados en Java.
-- Las semillas se insertan ANTES de FORCE RLS: el migrador tampoco puede saltársela.

CREATE TABLE ai_model_pricing (
  id uuid PRIMARY KEY,
  provider_code varchar(40) NOT NULL,
  model_code varchar(80) NOT NULL,
  input_usd_per_million numeric(12, 6) NOT NULL,
  output_usd_per_million numeric(12, 6) NOT NULL,
  currency varchar(3) NOT NULL DEFAULT 'USD',
  effective_from timestamptz NOT NULL,
  effective_to timestamptz,
  source_note varchar(300) NOT NULL,
  created_at timestamptz NOT NULL,
  CONSTRAINT ck_ai_pricing_input CHECK (input_usd_per_million >= 0),
  CONSTRAINT ck_ai_pricing_output CHECK (output_usd_per_million >= 0),
  CONSTRAINT ck_ai_pricing_window CHECK (effective_to IS NULL OR effective_to > effective_from)
);

CREATE INDEX ix_ai_pricing_lookup
  ON ai_model_pricing (provider_code, model_code, effective_from DESC);

CREATE TABLE ai_provider_configuration (
  id uuid PRIMARY KEY,
  company_id uuid REFERENCES companies (id),
  task_type varchar(60) NOT NULL,
  provider_code varchar(40) NOT NULL,
  model_code varchar(80) NOT NULL,
  enabled boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT uq_ai_provider_config UNIQUE (company_id, task_type)
);

CREATE TABLE ai_usage (
  id uuid PRIMARY KEY,
  company_id uuid NOT NULL REFERENCES companies (id),
  user_id uuid REFERENCES users (id),
  automation_id uuid,
  provider_code varchar(40) NOT NULL,
  model_code varchar(80) NOT NULL,
  task_type varchar(60) NOT NULL,
  input_tokens integer NOT NULL DEFAULT 0,
  output_tokens integer NOT NULL DEFAULT 0,
  estimated_cost_eur numeric(14, 6),
  estimated_cost_usd numeric(14, 6),
  status varchar(32) NOT NULL,
  error_code varchar(80),
  error_message varchar(500),
  occurred_at timestamptz NOT NULL,
  CONSTRAINT ck_ai_usage_input CHECK (input_tokens >= 0),
  CONSTRAINT ck_ai_usage_output CHECK (output_tokens >= 0),
  CONSTRAINT ck_ai_usage_status CHECK (status IN ('SUCCESS', 'FAILED', 'BLOCKED'))
);

CREATE INDEX ix_ai_usage_company_time ON ai_usage (company_id, occurred_at DESC);
CREATE INDEX ix_ai_usage_company_month ON ai_usage (company_id, occurred_at);

INSERT INTO ai_model_pricing (
  id, provider_code, model_code, input_usd_per_million, output_usd_per_million,
  currency, effective_from, effective_to, source_note, created_at
) VALUES
(
  '11111111-1111-4111-8111-111111111101',
  'openai',
  'gpt-4.1-mini',
  0.400000,
  1.600000,
  'USD',
  TIMESTAMPTZ '2025-04-14 00:00:00+00',
  NULL,
  'Tarifa estándar OpenAI (developers.openai.com/api/docs/pricing), consultada 2026-11-10',
  TIMESTAMPTZ '2026-11-10 00:00:00+00'
),
(
  '11111111-1111-4111-8111-111111111102',
  'openai',
  'gpt-4o-mini',
  0.150000,
  0.600000,
  'USD',
  TIMESTAMPTZ '2024-07-18 00:00:00+00',
  NULL,
  'Tarifa estándar OpenAI (developers.openai.com/api/docs/pricing), consultada 2026-11-10',
  TIMESTAMPTZ '2026-11-10 00:00:00+00'
);

INSERT INTO ai_provider_configuration (
  id, company_id, task_type, provider_code, model_code, enabled, created_at, updated_at
) VALUES (
  '22222222-2222-4222-8222-222222222201',
  NULL,
  'DEFAULT',
  'openai',
  'gpt-4.1-mini',
  true,
  TIMESTAMPTZ '2026-11-10 00:00:00+00',
  TIMESTAMPTZ '2026-11-10 00:00:00+00'
);

ALTER TABLE ai_model_pricing ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_model_pricing FORCE ROW LEVEL SECURITY;
ALTER TABLE ai_provider_configuration ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_provider_configuration FORCE ROW LEVEL SECURITY;
ALTER TABLE ai_usage ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_usage FORCE ROW LEVEL SECURITY;

CREATE POLICY ai_pricing_select ON ai_model_pricing
  FOR SELECT USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR current_setting('app.tenant_id', true) <> ''
    OR current_setting('app.user_id', true) <> ''
  );

CREATE POLICY ai_pricing_insert ON ai_model_pricing
  FOR INSERT WITH CHECK (current_setting('app.bypass_rls', true) = 'on');

CREATE POLICY ai_pricing_update ON ai_model_pricing
  FOR UPDATE USING (current_setting('app.bypass_rls', true) = 'on')
  WITH CHECK (current_setting('app.bypass_rls', true) = 'on');

CREATE POLICY ai_provider_config_select ON ai_provider_configuration
  FOR SELECT USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id IS NULL
    OR company_id::text = current_setting('app.tenant_id', true)
  );

CREATE POLICY ai_provider_config_write ON ai_provider_configuration
  FOR ALL USING (current_setting('app.bypass_rls', true) = 'on')
  WITH CHECK (current_setting('app.bypass_rls', true) = 'on');

CREATE POLICY ai_usage_select ON ai_usage
  FOR SELECT USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id::text = current_setting('app.tenant_id', true)
  );

CREATE POLICY ai_usage_insert ON ai_usage
  FOR INSERT WITH CHECK (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id::text = current_setting('app.tenant_id', true)
  );

GRANT SELECT, INSERT, UPDATE, DELETE ON ai_model_pricing, ai_provider_configuration, ai_usage TO neiver_app;

COMMENT ON TABLE ai_model_pricing IS 'Precios vigentes por modelo. Actualizar filas al cambiar la tarifa del proveedor.';
COMMENT ON TABLE ai_usage IS 'Registro de consumo de IA por empresa. No inventar filas de demostración.';
