-- One role + one database per service.

CREATE ROLE enrollment_user LOGIN PASSWORD 'enrollment_pw';
CREATE DATABASE enrollment OWNER enrollment_user;
REVOKE CONNECT ON DATABASE enrollment FROM PUBLIC;

CREATE ROLE sessions_user LOGIN PASSWORD 'sessions_pw';
CREATE DATABASE sessions OWNER sessions_user;
REVOKE CONNECT ON DATABASE sessions FROM PUBLIC;

CREATE ROLE claims_user LOGIN PASSWORD 'claims_pw';
CREATE DATABASE claims OWNER claims_user;
REVOKE CONNECT ON DATABASE claims FROM PUBLIC;

CREATE ROLE fraud_user LOGIN PASSWORD 'fraud_pw';
CREATE DATABASE fraud OWNER fraud_user;
REVOKE CONNECT ON DATABASE fraud FROM PUBLIC;
