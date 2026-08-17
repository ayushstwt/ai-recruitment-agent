-- Module 02: Organization & Multi-Tenant Management.
--
-- Extends the ORGANIZATIONS table created by V1 with the fields introduced
-- by the Organization feature. Existing migrations are left untouched.
--
-- Email uniqueness is deliberately enforced at the application layer
-- (see OrganizationService) instead of a database UNIQUE constraint so that
-- a soft-deleted organization does not prevent reuse of its email address
-- by a newly provisioned organization.
--
-- One ALTER TABLE per column keeps the script compatible with both
-- PostgreSQL and the H2 (PostgreSQL mode) test database.

ALTER TABLE ORGANIZATIONS ADD COLUMN LEGAL_NAME   VARCHAR(200);
ALTER TABLE ORGANIZATIONS ADD COLUMN EMAIL        VARCHAR(255) NOT NULL;
ALTER TABLE ORGANIZATIONS ADD COLUMN PHONE        VARCHAR(30);
ALTER TABLE ORGANIZATIONS ADD COLUMN WEBSITE      VARCHAR(255);
ALTER TABLE ORGANIZATIONS ADD COLUMN DESCRIPTION  VARCHAR(1000);
ALTER TABLE ORGANIZATIONS ADD COLUMN INDUSTRY     VARCHAR(100);
ALTER TABLE ORGANIZATIONS ADD COLUMN COUNTRY      VARCHAR(100);
ALTER TABLE ORGANIZATIONS ADD COLUMN STATE        VARCHAR(100);
ALTER TABLE ORGANIZATIONS ADD COLUMN CITY         VARCHAR(100);
ALTER TABLE ORGANIZATIONS ADD COLUMN TIMEZONE     VARCHAR(50);

-- Lookup indexes for the most common filters and searches. Composite index
-- matches the soft-delete filter used by nearly every query.

CREATE INDEX IDX_ORGANIZATIONS_NAME ON ORGANIZATIONS (NAME);
CREATE INDEX IDX_ORGANIZATIONS_EMAIL ON ORGANIZATIONS (EMAIL);
CREATE INDEX IDX_ORGANIZATIONS_ACTIVE_DELETED ON ORGANIZATIONS (IS_ACTIVE, IS_DELETED);
