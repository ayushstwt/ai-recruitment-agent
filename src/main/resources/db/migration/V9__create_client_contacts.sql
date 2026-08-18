-- Module 04: Client / Hiring Company Management.
--
-- Creates the CLIENT_CONTACTS table. A recruitment agency may hold multiple
-- contacts inside a client company, so contacts belong to exactly one client.
-- Tenant isolation is inherited through the owning client: every query scopes
-- contacts through client.organization.id and never trusts a bare contact id.
--
-- Email is not globally unique: the same person may be a contact at multiple
-- client companies and the same address may legitimately exist in different
-- organizations, so no unique constraint is placed on EMAIL.
--
-- One CREATE INDEX per lookup strategy used by the HQL / JPQL queries.

CREATE TABLE CLIENT_CONTACTS (
    ID          BIGSERIAL     PRIMARY KEY,
    CREATED_ON  TIMESTAMP(6)  NOT NULL,
    UPDATED_ON  TIMESTAMP(6)  NOT NULL,
    IS_ACTIVE   BOOLEAN       NOT NULL,
    IS_DELETED  BOOLEAN       NOT NULL,
    FIRST_NAME  VARCHAR(100)  NOT NULL,
    LAST_NAME   VARCHAR(100)  NOT NULL,
    EMAIL       VARCHAR(255)  NOT NULL,
    PHONE       VARCHAR(30),
    JOB_TITLE   VARCHAR(100),
    DEPARTMENT  VARCHAR(100),
    NOTES       VARCHAR(2000),
    CLIENT_ID   BIGINT        NOT NULL,
    CONSTRAINT FK_CLIENT_CONTACTS_CLIENT
        FOREIGN KEY (CLIENT_ID) REFERENCES CLIENTS (ID)
);

CREATE INDEX IDX_CLIENT_CONTACTS_CLIENT_ID ON CLIENT_CONTACTS (CLIENT_ID);
CREATE INDEX IDX_CLIENT_CONTACTS_EMAIL ON CLIENT_CONTACTS (EMAIL);
CREATE INDEX IDX_CLIENT_CONTACTS_ACTIVE_DELETED ON CLIENT_CONTACTS (IS_ACTIVE, IS_DELETED);
CREATE INDEX IDX_CLIENT_CONTACTS_CLIENT_ACTIVE_DELETED ON CLIENT_CONTACTS (CLIENT_ID, IS_ACTIVE, IS_DELETED);
CREATE INDEX IDX_CLIENT_CONTACTS_CLIENT_EMAIL ON CLIENT_CONTACTS (CLIENT_ID, EMAIL);