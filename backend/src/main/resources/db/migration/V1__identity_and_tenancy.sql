-- Fase 1: identidad, empresas y auditoría.
-- Las tablas de automatizaciones, documentos y consumo de IA no se crean todavía:
-- existirían sin el código que las protege. Su diseño está en docs/arquitectura.md.
--
-- Aislamiento: las políticas de abajo son una red de seguridad. La autorización
-- principal sigue estando en los servicios. Un rol superusuario o con BYPASSRLS
-- ignoraría estas políticas; la aplicación rechaza arrancar con un rol así.

ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO neiver_app;

CREATE TABLE companies (
  id uuid PRIMARY KEY,
  legal_name varchar(200) NOT NULL,
  trade_name varchar(200) NOT NULL,
  tax_id varchar(12) NOT NULL,
  email varchar(320) NOT NULL,
  phone varchar(30),
  address_line varchar(200),
  city varchar(120),
  province varchar(120),
  postal_code varchar(12),
  country varchar(2) NOT NULL DEFAULT 'ES',
  status varchar(32) NOT NULL DEFAULT 'ACTIVE',
  suspension_reason varchar(500),
  plan_label varchar(40) NOT NULL,
  monthly_ai_budget_eur numeric(12, 2),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT uq_companies_tax_id UNIQUE (tax_id),
  CONSTRAINT ck_companies_status CHECK (status IN ('ACTIVE', 'SUSPENDED')),
  CONSTRAINT ck_companies_budget CHECK (monthly_ai_budget_eur IS NULL OR monthly_ai_budget_eur >= 0)
);

CREATE TABLE users (
  id uuid PRIMARY KEY,
  email varchar(320) NOT NULL,
  password_hash varchar(255) NOT NULL,
  first_name varchar(80) NOT NULL,
  last_name varchar(120) NOT NULL,
  status varchar(32) NOT NULL DEFAULT 'ACTIVE',
  password_change_required boolean NOT NULL DEFAULT true,
  failed_login_count integer NOT NULL DEFAULT 0,
  locked_until timestamptz,
  last_login_at timestamptz,
  session_version integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT uq_users_email UNIQUE (email),
  CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'DISABLED')),
  CONSTRAINT ck_users_failed_login CHECK (failed_login_count >= 0),
  CONSTRAINT ck_users_session_version CHECK (session_version >= 0)
);

-- Un usuario pertenece a una sola empresa en esta versión.
-- SUPER_ADMIN no tiene empresa. El índice único sobre user_id deja el modelo
-- preparado para revisar esa decisión cuando un mismo correo deba entrar en varias.
CREATE TABLE memberships (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES users (id),
  company_id uuid REFERENCES companies (id),
  role varchar(32) NOT NULL,
  created_at timestamptz NOT NULL,
  CONSTRAINT uq_memberships_user UNIQUE (user_id),
  CONSTRAINT ck_membership_role CHECK (role IN ('SUPER_ADMIN', 'COMPANY_ADMIN', 'COMPANY_USER')),
  CONSTRAINT ck_membership_role_company CHECK (
    (role = 'SUPER_ADMIN' AND company_id IS NULL)
    OR (role <> 'SUPER_ADMIN' AND company_id IS NOT NULL)
  )
);

CREATE INDEX ix_memberships_company ON memberships (company_id);

CREATE TABLE refresh_tokens (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES users (id),
  token_hash varchar(64) NOT NULL,
  family_id uuid NOT NULL,
  expires_at timestamptz NOT NULL,
  revoked_at timestamptz,
  replaced_by uuid,
  created_at timestamptz NOT NULL,
  created_by_ip varchar(64),
  user_agent varchar(300),
  CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX ix_refresh_tokens_family ON refresh_tokens (family_id);

CREATE TABLE audit_logs (
  id uuid PRIMARY KEY,
  occurred_at timestamptz NOT NULL,
  actor_user_id uuid,
  company_id uuid,
  action varchar(80) NOT NULL,
  resource_type varchar(80) NOT NULL,
  resource_id uuid,
  ip varchar(64),
  user_agent varchar(300),
  details jsonb NOT NULL
);

CREATE INDEX ix_audit_company_time ON audit_logs (company_id, occurred_at DESC);
CREATE INDEX ix_audit_time ON audit_logs (occurred_at DESC);

-- Sin membresía no hay usuario visible para una empresa. El trigger es diferido
-- para permitir insertar la fila de usuario y, en la misma transacción, su membresía.
CREATE FUNCTION user_must_have_membership() RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM memberships m WHERE m.user_id = NEW.id) THEN
    RAISE EXCEPTION 'user_without_membership';
  END IF;
  RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_user_must_have_membership
AFTER INSERT ON users
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION user_must_have_membership();

ALTER TABLE companies ENABLE ROW LEVEL SECURITY;
ALTER TABLE companies FORCE ROW LEVEL SECURITY;
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;
ALTER TABLE memberships ENABLE ROW LEVEL SECURITY;
ALTER TABLE memberships FORCE ROW LEVEL SECURITY;
ALTER TABLE refresh_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE refresh_tokens FORCE ROW LEVEL SECURITY;
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs FORCE ROW LEVEL SECURITY;

-- companies: una empresa solo ve la suya. El alta de empresas es del administrador general.
CREATE POLICY companies_select ON companies
  FOR SELECT USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR id::text = current_setting('app.tenant_id', true)
  );

CREATE POLICY companies_insert ON companies
  FOR INSERT WITH CHECK (
    current_setting('app.bypass_rls', true) = 'on'
  );

CREATE POLICY companies_update ON companies
  FOR UPDATE USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR id::text = current_setting('app.tenant_id', true)
  ) WITH CHECK (
    current_setting('app.bypass_rls', true) = 'on'
    OR id::text = current_setting('app.tenant_id', true)
  );

-- users: el login busca por correo antes de conocer el identificador.
-- app.login_lookup solo lo activa el servicio de autenticación durante esa búsqueda.
CREATE POLICY users_select ON users
  FOR SELECT USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR current_setting('app.login_lookup', true) = 'on'
    OR id::text = current_setting('app.user_id', true)
    OR EXISTS (
      SELECT 1 FROM memberships m
      WHERE m.user_id = users.id
        AND m.company_id::text = current_setting('app.tenant_id', true)
    )
  );

CREATE POLICY users_insert ON users
  FOR INSERT WITH CHECK (
    current_setting('app.bypass_rls', true) = 'on'
    OR current_setting('app.tenant_id', true) <> ''
  );

CREATE POLICY users_update ON users
  FOR UPDATE USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR id::text = current_setting('app.user_id', true)
    OR EXISTS (
      SELECT 1 FROM memberships m
      WHERE m.user_id = users.id
        AND m.company_id::text = current_setting('app.tenant_id', true)
    )
  ) WITH CHECK (
    current_setting('app.bypass_rls', true) = 'on'
    OR id::text = current_setting('app.user_id', true)
    OR EXISTS (
      SELECT 1 FROM memberships m
      WHERE m.user_id = users.id
        AND m.company_id::text = current_setting('app.tenant_id', true)
    )
  );

CREATE POLICY memberships_select ON memberships
  FOR SELECT USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR user_id::text = current_setting('app.user_id', true)
    OR company_id::text = current_setting('app.tenant_id', true)
  );

CREATE POLICY memberships_insert ON memberships
  FOR INSERT WITH CHECK (
    current_setting('app.bypass_rls', true) = 'on'
    OR user_id::text = current_setting('app.user_id', true)
    OR company_id::text = current_setting('app.tenant_id', true)
  );

CREATE POLICY memberships_update ON memberships
  FOR UPDATE USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR user_id::text = current_setting('app.user_id', true)
    OR company_id::text = current_setting('app.tenant_id', true)
  ) WITH CHECK (
    current_setting('app.bypass_rls', true) = 'on'
    OR user_id::text = current_setting('app.user_id', true)
    OR company_id::text = current_setting('app.tenant_id', true)
  );

-- El token de renovación se busca por su hash, antes de saber de qué usuario es.
CREATE POLICY refresh_tokens_select ON refresh_tokens
  FOR SELECT USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR current_setting('app.token_lookup', true) = 'on'
    OR user_id::text = current_setting('app.user_id', true)
  );

CREATE POLICY refresh_tokens_insert ON refresh_tokens
  FOR INSERT WITH CHECK (
    current_setting('app.bypass_rls', true) = 'on'
    OR current_setting('app.token_lookup', true) = 'on'
    OR user_id::text = current_setting('app.user_id', true)
  );

CREATE POLICY refresh_tokens_update ON refresh_tokens
  FOR UPDATE USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR current_setting('app.token_lookup', true) = 'on'
    OR user_id::text = current_setting('app.user_id', true)
  ) WITH CHECK (
    current_setting('app.bypass_rls', true) = 'on'
    OR current_setting('app.token_lookup', true) = 'on'
    OR user_id::text = current_setting('app.user_id', true)
  );

CREATE POLICY audit_select ON audit_logs
  FOR SELECT USING (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id::text = current_setting('app.tenant_id', true)
  );

-- Un fallo de login de un correo desconocido no tiene empresa.
-- Solo se permite esa fila mientras el servicio está en la búsqueda de login.
CREATE POLICY audit_insert ON audit_logs
  FOR INSERT WITH CHECK (
    current_setting('app.bypass_rls', true) = 'on'
    OR company_id::text = current_setting('app.tenant_id', true)
    OR (
      company_id IS NULL
      AND current_setting('app.login_lookup', true) = 'on'
    )
  );

GRANT USAGE ON SCHEMA public TO neiver_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON companies, users, memberships, refresh_tokens, audit_logs TO neiver_app;
GRANT EXECUTE ON FUNCTION user_must_have_membership() TO neiver_app;
