-- Module 03: User & Role Management.
--
-- Seeds the initial system roles. These are global roles (ORGANIZATION_ID =
-- NULL, IS_SYSTEM_ROLE = TRUE) available to every organization, which is why
-- they can be provisioned once here without targeting any tenant.
--
-- Flyway executes each migration exactly once, so this script cannot create
-- duplicate rows. The UK_ROLES_SYSTEM_NAME partial unique index additionally
-- guards the global role namespace against accidental duplicates.
--
-- IDs are intentionally left to the BIGSERIAL sequence: organizations created
-- at runtime receive their own copies only when the system-role model is
-- extended, so deterministic identifiers are not required.

INSERT INTO ROLES (CREATED_ON, UPDATED_ON, IS_ACTIVE, IS_DELETED, NAME, DESCRIPTION, IS_SYSTEM_ROLE, ORGANIZATION_ID)
VALUES
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE, FALSE, 'AGENCY_ADMIN',
     'Full access to the organization''s recruitment platform.',
     TRUE, NULL),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE, FALSE, 'RECRUITMENT_MANAGER',
     'Manage recruiters, jobs, candidates and recruitment workflows.',
     TRUE, NULL),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE, FALSE, 'RECRUITER',
     'Manage candidates, jobs and applications assigned to them.',
     TRUE, NULL),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE, FALSE, 'HIRING_MANAGER',
     'Review jobs, candidates and applications.',
     TRUE, NULL),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE, FALSE, 'INTERVIEWER',
     'Access assigned interviews and candidate interview information.',
     TRUE, NULL),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE, FALSE, 'VIEWER',
     'Read-only access to permitted recruitment data.',
     TRUE, NULL);