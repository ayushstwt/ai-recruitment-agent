package com.ayshriv.recruitment.job.exception;

import com.ayshriv.recruitment.common.exception.ForbiddenException;

/**
 * Thrown when an authenticated organization attempts to access a job that
 * belongs to another organization or to reassign a job to a client of another
 * organization.
 */
public class JobAccessDeniedException extends ForbiddenException {

    /**
     * Create a job access denied exception.
     */
    public JobAccessDeniedException() {
        super("Access denied to the requested job", "JOB_ACCESS_DENIED");
    }
}
