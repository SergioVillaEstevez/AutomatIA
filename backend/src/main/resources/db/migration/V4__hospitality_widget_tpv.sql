-- Hostelería-2: booking público, plano de sala, TPV de caja y plantillas WhatsApp.
-- El TPV registra cobros en caja; no procesa tarjetas ni pasarelas.

ALTER TABLE hospitality_resources
  ADD COLUMN pos_x integer NOT NULL DEFAULT 40,
  ADD COLUMN pos_y integer NOT NULL DEFAULT 40,
  ADD COLUMN width integer NOT NULL DEFAULT 96,
  ADD COLUMN height integer NOT NULL DEFAULT 72,
  ADD CONSTRAINT ck_hosp_resource_pos CHECK (pos_x >= 0 AND pos_y >= 0 AND width >= 40 AND height >= 40);

CREATE TABLE hospitality_venue_settings (
  company_id uuid PRIMARY KEY REFERENCES companies (id),
  public_slug varchar(80) NOT NULL,
  public_booking_enabled boolean NOT NULL DEFAULT false,
  public_booking_note varchar(500),
  floor_width integer NOT NULL DEFAULT 900,
  floor_height integer NOT NULL DEFAULT 600,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT uq_hosp_venue_slug UNIQUE (public_slug),
  CONSTRAINT ck_hosp_slug CHECK (public_slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
  CONSTRAINT ck_hosp_floor CHECK (floor_width >= 400 AND floor_height >= 300)
);

CREATE TABLE hospitality_whatsapp_templates (
  id uuid PRIMARY KEY,
  company_id uuid NOT NULL REFERENCES companies (id),
  code varchar(60) NOT NULL,
  title varchar(120) NOT NULL,
  body varchar(1000) NOT NULL,
  meta_template_name varchar(120),
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT uq_hosp_wa_template UNIQUE (company_id, code)
);

CREATE TABLE hospitality_tickets (
  id uuid PRIMARY KEY,
  company_id uuid NOT NULL REFERENCES companies (id),
  reservation_id uuid REFERENCES hospitality_reservations (id),
  resource_id uuid REFERENCES hospitality_resources (id),
  label varchar(120) NOT NULL,
  status varchar(32) NOT NULL,
  payment_method varchar(32),
  notes varchar(500),
  opened_by uuid REFERENCES users (id),
  closed_by uuid REFERENCES users (id),
  opened_at timestamptz NOT NULL,
  closed_at timestamptz,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT ck_hosp_ticket_status CHECK (status IN ('OPEN', 'PAID', 'CANCELLED')),
  CONSTRAINT ck_hosp_ticket_payment CHECK (
    payment_method IS NULL OR payment_method IN ('CASH', 'CARD_TERMINAL', 'OTHER')
  )
);

CREATE INDEX ix_hosp_tickets_company_status ON hospitality_tickets (company_id, status, opened_at DESC);

CREATE TABLE hospitality_ticket_lines (
  id uuid PRIMARY KEY,
  ticket_id uuid NOT NULL REFERENCES hospitality_tickets (id) ON DELETE CASCADE,
  company_id uuid NOT NULL REFERENCES companies (id),
  description varchar(200) NOT NULL,
  quantity numeric(10, 2) NOT NULL,
  unit_price_eur numeric(12, 2) NOT NULL,
  created_at timestamptz NOT NULL,
  CONSTRAINT ck_hosp_line_qty CHECK (quantity > 0),
  CONSTRAINT ck_hosp_line_price CHECK (unit_price_eur >= 0)
);

CREATE INDEX ix_hosp_ticket_lines_ticket ON hospitality_ticket_lines (ticket_id);

ALTER TABLE hospitality_venue_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE hospitality_venue_settings FORCE ROW LEVEL SECURITY;
ALTER TABLE hospitality_whatsapp_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE hospitality_whatsapp_templates FORCE ROW LEVEL SECURITY;
ALTER TABLE hospitality_tickets ENABLE ROW LEVEL SECURITY;
ALTER TABLE hospitality_tickets FORCE ROW LEVEL SECURITY;
ALTER TABLE hospitality_ticket_lines ENABLE ROW LEVEL SECURITY;
ALTER TABLE hospitality_ticket_lines FORCE ROW LEVEL SECURITY;

CREATE POLICY hosp_venue_select ON hospitality_venue_settings
  FOR SELECT USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id::text = current_setting('app.tenant_id', true)
    OR (
      current_setting('app.public_booking', true) = 'on'
      AND public_booking_enabled = true
      AND company_id::text = current_setting('app.tenant_id', true)
    )
  );
CREATE POLICY hosp_venue_write ON hospitality_venue_settings
  FOR ALL USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id::text = current_setting('app.tenant_id', true)
  )
  WITH CHECK (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id::text = current_setting('app.tenant_id', true)
  );

CREATE POLICY hosp_wa_tpl_select ON hospitality_whatsapp_templates
  FOR SELECT USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id::text = current_setting('app.tenant_id', true)
  );
CREATE POLICY hosp_wa_tpl_write ON hospitality_whatsapp_templates
  FOR ALL USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id::text = current_setting('app.tenant_id', true)
  )
  WITH CHECK (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id::text = current_setting('app.tenant_id', true)
  );

CREATE POLICY hosp_tickets_select ON hospitality_tickets
  FOR SELECT USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id::text = current_setting('app.tenant_id', true)
  );
CREATE POLICY hosp_tickets_write ON hospitality_tickets
  FOR ALL USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id::text = current_setting('app.tenant_id', true)
  )
  WITH CHECK (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id::text = current_setting('app.tenant_id', true)
  );

CREATE POLICY hosp_ticket_lines_select ON hospitality_ticket_lines
  FOR SELECT USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id::text = current_setting('app.tenant_id', true)
  );
CREATE POLICY hosp_ticket_lines_write ON hospitality_ticket_lines
  FOR ALL USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id::text = current_setting('app.tenant_id', true)
  )
  WITH CHECK (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id::text = current_setting('app.tenant_id', true)
  );

-- Lectura pública de recursos activos y alta de reservas vía servicio con bypass controlado.
CREATE POLICY hosp_resources_public_select ON hospitality_resources
  FOR SELECT USING (
    current_setting('app.public_booking', true) = 'on' AND active = true
  );

CREATE POLICY hosp_reservations_public_insert ON hospitality_reservations
  FOR INSERT WITH CHECK (
    current_setting('app.public_booking', true) = 'on'
    AND company_id::text = current_setting('app.tenant_id', true)
  );

GRANT SELECT, INSERT, UPDATE, DELETE
  ON hospitality_venue_settings, hospitality_whatsapp_templates, hospitality_tickets, hospitality_ticket_lines
  TO neiver_app;

COMMENT ON TABLE hospitality_venue_settings IS 'Slug público y plano del local. public_booking_enabled controla el widget.';
COMMENT ON TABLE hospitality_tickets IS 'Tickets de caja. PAID = cobrado en local, no pasarela.';
COMMENT ON TABLE hospitality_whatsapp_templates IS 'Plantillas locales; meta_template_name opcional para Cloud API.';
