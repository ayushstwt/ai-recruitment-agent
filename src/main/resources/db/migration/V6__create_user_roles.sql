-- Module 03: User & Role Management.
--
-- Creates the USER_ROLES join table linking users to roles. The composite
-- primary key doubles as the unique constraint guaranteeing a user can never
-- hold the same role twice.

CREATE TABLE USER_ROLES (
    USER_ID BIGINT NOT NULL,
    ROLE_ID BIGINT NOT NULL,
    CONSTRAINT PK_USER_ROLES PRIMARY KEY (USER_ID, ROLE_ID),
    CONSTRAINT FK_USER_ROLES_USER
        FOREIGN KEY (USER_ID) REFERENCES USERS (ID),
    CONSTRAINT FK_USER_ROLES_ROLE
        FOREIGN KEY (ROLE_ID) REFERENCES ROLES (ID)
);

CREATE INDEX IDX_USER_ROLES_ROLE_ID ON USER_ROLES (ROLE_ID);