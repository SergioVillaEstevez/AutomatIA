-- Vertical hostelería (fase hostelería-1): mesas/recursos, reservas y avisos WhatsApp.
-- Un local = la empresa. No se inventan reservas de demostración.

CREATE TABLE hospitality_resources (
  id uuid PRIMARY KEY,
  company_id uuid NOT NULL REFERENCES companies (id),
  name varchar(80) NOT NULL,
  seats integer NOT NULL,
  active boolean NOT NULL DEFAULT true,
  sort_order integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT ck_hosp_resource_seats CHECK (seats > 0),
  CONSTRAINT uq_hosp_resource_name UNIQUE (company_id, name)
);

CREATE INDEX ix_hosp_resources_company ON hospitality_resources (company_id, active, sort_order);

CREATE TABLE hospitality_reservations (
  id uuid PRIMARY KEY,
  company_id uuid NOT NULL REFERENCES companies (id),
  resource_id uuid REFERENCES hospitality_resources (id),
  guest_name varchar(120) NOT NULL,
  guest_phone varchar(30),
  party_size integer NOT NULL,
  starts_at timestamptz NOT NULL,
  ends_at timestamptz NOT NULL,
  status varchar(32) NOT NULL,
  notes varchar(500),
  created_by uuid REFERENCES users (id),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT ck_hosp_party_size CHECK (party_size > 0),
  CONSTRAINT ck_hosp_window CHECK (ends_at > starts_at),
  CONSTRAINT ck_hosp_status CHECK (status IN (
    'PENDING', 'CONFIRMED', 'SEATED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'
  ))
);

CREATE INDEX ix_hosp_reservations_company_time
  ON hospitality_reservations (company_id, starts_at);
CREATE INDEX ix_hosp_reservations_resource_time
  ON hospitality_reservations (resource_id, starts_at)
  WHERE resource_id IS NOT NULL;

CREATE TABLE hospitality_whatsapp_messages (
  id uuid PRIMARY KEY,
  company_id uuid NOT NULL REFERENCES companies (id),
  reservation_id uuid REFERENCES hospitality_reservations (id),
  phone varchar(30) NOT NULL,
  body varchar(1000) NOT NULL,
  status varchar(32) NOT NULL,
  provider_message_id varchar(120),
  error_code varchar(80),
  error_message varchar(500),
  created_at timestamptz NOT NULL,
  CONSTRAINT ck_hosp_wa_status CHECK (status IN ('SENT', 'FAILED', 'SKIPPED'))
);

CREATE INDEX ix_hosp_wa_company_time ON hospitality_whatsapp_messages (company_id, created_at DESC);

ALTER TABLE hospitality_resources ENABLE ROW LEVEL SECURITY;
ALTER TABLE hospitality_resources FORCE ROW LEVEL SECURITY;
ALTER TABLE hospitality_reservations ENABLE ROW LEVEL SECURITY;
ALTER TABLE hospitality_reservations FORCE ROW LEVEL SECURITY;
ALTER TABLE hospitality_whatsapp_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE hospitality_whatsapp_messages FORCE ROW LEVEL SECURITY;

CREATE POLICY hosp_resources_select ON hospitality_resources
  FOR SELECT USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id::text = current_setting('app.tenant_id', true)
  );
CREATE POLICY hosp_resources_write ON hospitality_resources
  FOR ALL USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id::text = current_setting('app.tenant_id', true)
  )
  WITH CHECK (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id::text = current_setting('app.tenant_id', true)
  );

CREATE POLICY hosp_reservations_select ON hospitality_reservations
  FOR SELECT USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id::text = current_setting('app.tenant_id', true)
  );
CREATE POLICY hosp_reservations_write ON hospitality_reservations
  FOR ALL USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id::text = current_setting('app.tenant_id', true)
  )
  WITH CHECK (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id::text = current_setting('app.tenant_id', true)
  );

CREATE POLICY hosp_wa_select ON hospitality_whatsapp_messages
  FOR SELECT USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id::text = current_setting('app.tenant_id', true)
  );
CREATE POLICY hosp_wa_insert ON hospitality_whatsapp_messages
  FOR INSERT WITH CHECK (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id::text = current_setting('app.tenant_id', true)
  );

GRANT SELECT, INSERT, UPDATE, DELETE
  ON hospitality_resources, hospitality_reservations, hospitality_whatsapp_messages
  TO neiver_app;

COMMENT ON TABLE hospitality_resources IS 'Mesas u otros recursos reservables del local (empresa).';
COMMENT ON TABLE hospitality_reservations IS 'Reservas de comensales. Sin filas de demostración.';
COMMENT ON TABLE hospitality_whatsapp_messages IS 'Intentos reales de aviso por WhatsApp Cloud API.';
