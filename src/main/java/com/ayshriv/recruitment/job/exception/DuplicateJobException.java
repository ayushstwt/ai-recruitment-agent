package com.ayshriv.recruitment.job.exception;

import com.ayshriv.recruitment.common.exception.DuplicateResourceException;

/**
 * Thrown when a job would be created or updated with a job code that is
 * already used by another job of the same organization.
 */
public class DuplicateJobException extends DuplicateResourceException {

    /**
     * Create a duplicate job exception.
     *
     * @param message human readable conflict message
     */
    public DuplicateJobException(String message) {
        super(message, "JOB_ALREADY_EXISTS");
    }

    /**
     * Create a duplicate job exception for a job code allocation conflict.
     */
    public static DuplicateJobException forJobCodeConflict() {
        return new DuplicateJobException(
                "A unique job code could not be allocated, please retry");
    }
}
