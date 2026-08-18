-- Module 03: User & Role Management.
--
-- Creates the ROLES table.
--
-- Roles follow a dual model:
--   * SYSTEM roles  (IS_SYSTEM_ROLE = TRUE, ORGANIZATION_ID = NULL) are
--     global application-level roles shared by every organization.
--   * ORGANIZATION roles (IS_SYSTEM_ROLE = FALSE, ORGANIZATION_ID set) are
--     tenant scoped and can never be used by another organization.
--
-- A user of organization A can therefore use a role of organization A or any
-- system role, but never a role of organization B.
--
-- Name uniqueness:
--   * System role names must be unique in the global namespace.
--   * Organization role names must be unique per organization.
--
-- Uniqueness is enforced at the application layer (see RoleService) because
-- H2 - used in tests - does not support PostgreSQL partial indexes
-- (WHERE clause on CREATE INDEX) and the two dialects place the
-- NULLS NOT DISTINCT clause differently. The composite index below backs the
-- per-scope lookups and the application-level uniqueness checks.

CREATE TABLE ROLES (
    ID              BIGSERIAL     PRIMARY KEY,
    CREATED_ON      TIMESTAMP(6)  NOT NULL,
    UPDATED_ON      TIMESTAMP(6)  NOT NULL,
    IS_ACTIVE       BOOLEAN       NOT NULL,
    IS_DELETED      BOOLEAN       NOT NULL,
    NAME            VARCHAR(100)  NOT NULL,
    DESCRIPTION     VARCHAR(1000),
    IS_SYSTEM_ROLE  BOOLEAN       NOT NULL,
    ORGANIZATION_ID BIGINT,
    CONSTRAINT FK_ROLES_ORGANIZATION
        FOREIGN KEY (ORGANIZATION_ID) REFERENCES ORGANIZATIONS (ID)
);

CREATE INDEX IDX_ROLES_NAME ON ROLES (NAME);
CREATE INDEX IDX_ROLES_ACTIVE_DELETED ON ROLES (IS_ACTIVE, IS_DELETED);
CREATE INDEX IDX_ROLES_SYSTEM_ACTIVE_DELETED ON ROLES (IS_SYSTEM_ROLE, IS_ACTIVE, IS_DELETED);

CREATE INDEX IDX_ROLES_NAME_SCOPE ON ROLES (ORGANIZATION_ID, NAME);