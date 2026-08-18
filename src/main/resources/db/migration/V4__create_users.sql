-- Module 03: User & Role Management.
--
-- Creates the USERS table. Every user belongs to exactly one organization
-- and can never be moved to another one. The ORGANIZATION_ID foreign key
-- makes the owning tenant the hard isolation boundary for the table.
--
-- Email uniqueness is deliberately enforced at the application layer
-- (see UserService) with an ORGANIZATION_ID + EMAIL scoped lookup instead
-- of a database UNIQUE constraint so that:
--   1. Two different organizations may register the same email.
--   2. A soft-deleted user does not block reuse of their email address.
--
-- One CREATE INDEX per lookup strategy used by the HQL / JPQL queries.

CREATE TABLE USERS (
    ID                BIGSERIAL     PRIMARY KEY,
    CREATED_ON        TIMESTAMP(6)  NOT NULL,
    UPDATED_ON        TIMESTAMP(6)  NOT NULL,
    IS_ACTIVE         BOOLEAN       NOT NULL,
    IS_DELETED        BOOLEAN       NOT NULL,
    FIRST_NAME        VARCHAR(100)  NOT NULL,
    LAST_NAME         VARCHAR(100)  NOT NULL,
    EMAIL             VARCHAR(255)  NOT NULL,
    PHONE             VARCHAR(30),
    PASSWORD_HASH     VARCHAR(255)  NOT NULL,
    PROFILE_IMAGE_URL VARCHAR(500),
    JOB_TITLE         VARCHAR(100),
    LAST_LOGIN_AT     TIMESTAMP(6),
    ORGANIZATION_ID   BIGINT        NOT NULL,
    CONSTRAINT FK_USERS_ORGANIZATION
        FOREIGN KEY (ORGANIZATION_ID) REFERENCES ORGANIZATIONS (ID)
);

CREATE INDEX IDX_USERS_ORGANIZATION_ID ON USERS (ORGANIZATION_ID);
CREATE INDEX IDX_USERS_EMAIL ON USERS (EMAIL);
CREATE INDEX IDX_USERS_ORGANIZATION_EMAIL ON USERS (ORGANIZATION_ID, EMAIL);
CREATE INDEX IDX_USERS_ACTIVE_DELETED ON USERS (IS_ACTIVE, IS_DELETED);
CREATE INDEX IDX_USERS_ORGANIZATION_ACTIVE_DELETED ON USERS (ORGANIZATION_ID, IS_ACTIVE, IS_DELETED);