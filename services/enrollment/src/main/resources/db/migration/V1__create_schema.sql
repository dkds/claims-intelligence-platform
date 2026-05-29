CREATE TABLE clinic
(
    id            UUID         NOT NULL,
    name          VARCHAR(255) NOT NULL,
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city          VARCHAR(100),
    postcode      VARCHAR(20),
    country_code  VARCHAR(2),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    registered_at TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ,
    CONSTRAINT pk_clinic PRIMARY KEY (id)
);

CREATE TABLE owner
(
    id         UUID         NOT NULL,
    clinic_id  UUID         NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(255),
    phone      VARCHAR(50),
    created_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_owner PRIMARY KEY (id),
    CONSTRAINT fk_owner_clinic FOREIGN KEY (clinic_id) REFERENCES clinic (id)
);

CREATE TABLE vet
(
    id               UUID         NOT NULL,
    clinic_id        UUID         NOT NULL,
    first_name       VARCHAR(100) NOT NULL,
    last_name        VARCHAR(100) NOT NULL,
    email            VARCHAR(255),
    license_number   VARCHAR(100) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    rejection_reason VARCHAR(500),
    registered_at    TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ,
    CONSTRAINT pk_vet PRIMARY KEY (id),
    CONSTRAINT fk_vet_clinic FOREIGN KEY (clinic_id) REFERENCES clinic (id)
);

CREATE TABLE pet
(
    id               UUID         NOT NULL,
    clinic_id        UUID         NOT NULL,
    owner_id         UUID         NOT NULL,
    name             VARCHAR(100) NOT NULL,
    species          VARCHAR(50)  NOT NULL,
    breed            VARCHAR(100),
    date_of_birth    DATE,
    microchip_number VARCHAR(50),
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE')),
    enrolled_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_pet PRIMARY KEY (id),
    CONSTRAINT fk_pet_clinic FOREIGN KEY (clinic_id) REFERENCES clinic (id),
    CONSTRAINT fk_pet_owner FOREIGN KEY (owner_id) REFERENCES owner (id)
);

CREATE TABLE catalogue_item
(
    id                 UUID           NOT NULL,
    code               VARCHAR(50)    NOT NULL,
    description        VARCHAR(500)   NOT NULL,
    reimbursement_rate NUMERIC(10, 2) NOT NULL,
    active             BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ    NOT NULL,
    updated_at         TIMESTAMPTZ,
    CONSTRAINT pk_catalogue_item PRIMARY KEY (id),
    CONSTRAINT uq_catalogue_item_code UNIQUE (code)
);

CREATE TABLE policy
(
    id            UUID        NOT NULL,
    pet_id        UUID        NOT NULL,
    coverage_type VARCHAR(20) NOT NULL
        CHECK (coverage_type IN ('BASIC', 'STANDARD', 'PREMIUM')),
    start_date    DATE        NOT NULL,
    end_date      DATE        NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED')),
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ,
    CONSTRAINT pk_policy PRIMARY KEY (id),
    CONSTRAINT fk_policy_pet FOREIGN KEY (pet_id) REFERENCES pet (id)
);

CREATE INDEX idx_owner_clinic ON owner (clinic_id);
CREATE INDEX idx_vet_clinic ON vet (clinic_id);
CREATE INDEX idx_pet_clinic ON pet (clinic_id);
CREATE INDEX idx_pet_owner ON pet (owner_id);
CREATE INDEX idx_policy_pet ON policy (pet_id);
