package com.ayshriv.recruitment.job.exception;

import com.ayshriv.recruitment.common.exception.ResourceNotFoundException;

/**
 * Thrown when a job cannot be found because it does not exist or has been
 * soft deleted.
 */
public class JobNotFoundException extends ResourceNotFoundException {

    /**
     * Create a not found exception for a job.
     *
     * @param id job primary key
     */
    public JobNotFoundException(Long id) {
        super("Job not found with id: " + id, "JOB_NOT_FOUND");
    }
}
