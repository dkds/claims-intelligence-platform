-- Master-data shadow copies (populated by consuming cip.enrollment.v1)

CREATE TABLE local_clinic
(
    id         UUID         NOT NULL,
    name       VARCHAR(255) NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_local_clinic PRIMARY KEY (id)
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

-- Claim aggregate

CREATE TABLE claim
(
    id                    UUID           NOT NULL,
    clinic_id             UUID           NOT NULL,
    pet_id                UUID           NOT NULL,
    policy_id             UUID           NOT NULL,
    origin                VARCHAR(20)    NOT NULL
        CHECK (origin IN ('SESSION', 'MANUAL')),
    source_session_id     UUID,
    submitted_by          UUID           NOT NULL,
    status                VARCHAR(30)    NOT NULL DEFAULT 'ASSEMBLED'
        CHECK (status IN ('ASSEMBLED', 'PENDING_REVIEW', 'ADJUDICATED', 'REJECTED', 'READY_FOR_SUBMISSION')),
    adjudication_decision VARCHAR(20)
        CHECK (adjudication_decision IN ('APPROVED', 'PARTIALLY_APPROVED')),
    total_requested       NUMERIC(12, 2) NOT NULL,
    approved_amount       NUMERIC(12, 2),
    created_at            TIMESTAMPTZ    NOT NULL,
    updated_at            TIMESTAMPTZ,
    CONSTRAINT pk_claim PRIMARY KEY (id)
);

CREATE TABLE claim_line
(
    id               UUID           NOT NULL,
    claim_id         UUID           NOT NULL,
    procedure_code   VARCHAR(50)    NOT NULL,
    quantity         INT            NOT NULL,
    requested_amount NUMERIC(12, 2) NOT NULL,
    approved_amount  NUMERIC(12, 2),
    CONSTRAINT pk_claim_line PRIMARY KEY (id),
    CONSTRAINT fk_claim_line_claim FOREIGN KEY (claim_id) REFERENCES claim (id)
);

CREATE TABLE claim_transition
(
    id          UUID        NOT NULL,
    claim_id    UUID        NOT NULL,
    from_status VARCHAR(30),
    to_status   VARCHAR(30) NOT NULL,
    actor       VARCHAR(100),
    reason      VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_claim_transition PRIMARY KEY (id),
    CONSTRAINT fk_claim_transition_claim FOREIGN KEY (claim_id) REFERENCES claim (id)
);

-- Indices

CREATE INDEX idx_local_pet_clinic ON local_pet (clinic_id);
CREATE INDEX idx_local_policy_pet ON local_policy (pet_id);
CREATE INDEX idx_claim_clinic ON claim (clinic_id);
CREATE INDEX idx_claim_pet ON claim (pet_id);
CREATE INDEX idx_claim_policy ON claim (policy_id);
CREATE INDEX idx_claim_status ON claim (status);
CREATE INDEX idx_claim_line_claim ON claim_line (claim_id);
CREATE INDEX idx_claim_transition_claim ON claim_transition (claim_id);
