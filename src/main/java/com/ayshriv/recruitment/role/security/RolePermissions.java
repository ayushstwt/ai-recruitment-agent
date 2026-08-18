package com.ayshriv.recruitment.role.security;

/**
 * Placeholder permission catalog for the future fine-grained permission
 * system.
 *
 * <p>Permissions are not implemented yet. This constant holder is the clean
 * extension point the authorization architecture will build on: once the
 * permission module lands, roles can be mapped to these machine readable
 * permissions and enforced centrally instead of sprinkling string literals
 * through controllers.</p>
 */
public final class RolePermissions {

    public static final String USER_READ = "USER_READ";
    public static final String USER_CREATE = "USER_CREATE";
    public static final String USER_UPDATE = "USER_UPDATE";
    public static final String USER_DELETE = "USER_DELETE";

    public static final String CANDIDATE_READ = "CANDIDATE_READ";
    public static final String CANDIDATE_CREATE = "CANDIDATE_CREATE";
    public static final String CANDIDATE_UPDATE = "CANDIDATE_UPDATE";
    public static final String CANDIDATE_DELETE = "CANDIDATE_DELETE";

    public static final String JOB_READ = "JOB_READ";
    public static final String JOB_CREATE = "JOB_CREATE";
    public static final String JOB_UPDATE = "JOB_UPDATE";
    public static final String JOB_DELETE = "JOB_DELETE";

    public static final String APPLICATION_READ = "APPLICATION_READ";
    public static final String APPLICATION_UPDATE = "APPLICATION_UPDATE";

    public static final String INTERVIEW_READ = "INTERVIEW_READ";
    public static final String INTERVIEW_MANAGE = "INTERVIEW_MANAGE";

    public static final String AI_AGENT_RUN = "AI_AGENT_RUN";
    public static final String AI_AGENT_CONFIGURE = "AI_AGENT_CONFIGURE";

    public static final String ANALYTICS_READ = "ANALYTICS_READ";

    private RolePermissions() {
    }
}