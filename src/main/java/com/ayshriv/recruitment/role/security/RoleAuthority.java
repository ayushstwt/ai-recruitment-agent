package com.ayshriv.recruitment.role.security;

/**
 * Spring Security authority names for the application's role model.
 *
 * <p>This is the extension point for the future user authentication flow:
 * a {@code UserPrincipal} will expose these authorities so that
 * {@code @PreAuthorize} or a centralized authorization service can enforce
 * access. The constants mirror the default system roles and must not be
 * hardcoded inline across the code base.</p>
 */
public final class RoleAuthority {

    public static final String ROLE_AGENCY_ADMIN = "ROLE_AGENCY_ADMIN";
    public static final String ROLE_RECRUITMENT_MANAGER = "ROLE_RECRUITMENT_MANAGER";
    public static final String ROLE_RECRUITER = "ROLE_RECRUITER";
    public static final String ROLE_HIRING_MANAGER = "ROLE_HIRING_MANAGER";
    public static final String ROLE_INTERVIEWER = "ROLE_INTERVIEWER";
    public static final String ROLE_VIEWER = "ROLE_VIEWER";

    private RoleAuthority() {
    }
}