-- Local master-data shadow copies (populated by consuming cip.enrollment.v1 in Phase 2b)

CREATE TABLE local_clinic
(
    id         UUID         NOT NULL,
    name       VARCHAR(255) NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_local_clinic PRIMARY KEY (id)
);

CREATE TABLE local_vet
(
    id         UUID         NOT NULL,
    clinic_id  UUID         NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_local_vet PRIMARY KEY (id),
    CONSTRAINT fk_local_vet_clinic FOREIGN KEY (clinic_id) REFERENCES local_clinic (id)
);

CREATE TABLE local_pet
(
    id         UUID         NOT NULL,
    clinic_id  UUID         NOT NULL,
    owner_id   UUID         NOT NULL,
    name       VARCHAR(100) NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE')),
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_local_pet PRIMARY KEY (id),
    CONSTRAINT fk_local_pet_clinic FOREIGN KEY (clinic_id) REFERENCES local_clinic (id)
);

CREATE TABLE local_policy
(
    id            UUID        NOT NULL,
    pet_id        UUID        NOT NULL,
    coverage_type VARCHAR(20) NOT NULL
        CHECK (coverage_type IN ('BASIC', 'STANDARD', 'PREMIUM')),
    start_date    DATE        NOT NULL,
    end_date      DATE        NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED')),
    updated_at    TIMESTAMPTZ,
    CONSTRAINT pk_local_policy PRIMARY KEY (id),
    CONSTRAINT fk_local_policy_pet FOREIGN KEY (pet_id) REFERENCES local_pet (id)
);

CREATE TABLE local_catalogue_item
(
    id                 UUID           NOT NULL,
    code               VARCHAR(50)    NOT NULL,
    description        VARCHAR(500)   NOT NULL,
    reimbursement_rate NUMERIC(10, 2) NOT NULL,
    active             BOOLEAN        NOT NULL DEFAULT TRUE,
    updated_at         TIMESTAMPTZ,
    CONSTRAINT pk_local_catalogue_item PRIMARY KEY (id),
    CONSTRAINT uq_local_catalogue_item_code UNIQUE (code)
);

-- Session aggregate

CREATE TABLE session
(
    id          UUID        NOT NULL,
    clinic_id   UUID        NOT NULL,
    pet_id      UUID        NOT NULL,
    vet_id      UUID        NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'LOGGED'
        CHECK (status IN ('LOGGED', 'VERIFIED', 'CANCELLED')),
    logged_at   TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ,
    verified_by UUID,
    CONSTRAINT pk_session PRIMARY KEY (id),
    CONSTRAINT fk_session_clinic FOREIGN KEY (clinic_id) REFERENCES local_clinic (id),
    CONSTRAINT fk_session_pet FOREIGN KEY (pet_id) REFERENCES local_pet (id),
    CONSTRAINT fk_session_vet FOREIGN KEY (vet_id) REFERENCES local_vet (id)
);

CREATE TABLE session_line
(
    id             UUID        NOT NULL,
    session_id     UUID        NOT NULL,
    procedure_code VARCHAR(50) NOT NULL,
    quantity       INT         NOT NULL,
    notes          VARCHAR(500),
    CONSTRAINT pk_session_line PRIMARY KEY (id),
    CONSTRAINT fk_session_line_session FOREIGN KEY (session_id) REFERENCES session (id)
);

CREATE INDEX idx_local_vet_clinic ON local_vet (clinic_id);
CREATE INDEX idx_local_pet_clinic ON local_pet (clinic_id);
CREATE INDEX idx_local_policy_pet ON local_policy (pet_id);
CREATE INDEX idx_session_clinic ON session (clinic_id);
CREATE INDEX idx_session_pet ON session (pet_id);
CREATE INDEX idx_session_vet ON session (vet_id);
CREATE INDEX idx_session_line_session ON session_line (session_id);
