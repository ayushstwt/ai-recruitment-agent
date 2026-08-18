package com.ayshriv.recruitment.job.service;

import com.ayshriv.recruitment.common.exception.BadRequestException;
import com.ayshriv.recruitment.job.entity.Job;
import com.ayshriv.recruitment.job.entity.JobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobStatusTransitionServiceTest {

    private JobStatusTransitionService transitionService;

    @BeforeEach
    void setUp() {
        transitionService = new JobStatusTransitionService();
    }

    private Job job(JobStatus status) {
        Job job = new Job();
        job.setStatus(status);
        job.setActive(true);
        return job;
    }

    @Test
    void publishMovesDraftToOpenAndSetsPublishedOn() {
        Job job = job(JobStatus.DRAFT);

        transitionService.apply(job, JobStatus.OPEN);

        assertThat(job.getStatus()).isEqualTo(JobStatus.OPEN);
        assertThat(job.getPublishedOn()).isNotNull();
        assertThat(job.isActive()).isTrue();
    }

    @Test
    void holdMovesOpenToOnHoldAndKeepsActive() {
        Job job = job(JobStatus.OPEN);

        transitionService.apply(job, JobStatus.ON_HOLD);

        assertThat(job.getStatus()).isEqualTo(JobStatus.ON_HOLD);
        assertThat(job.isActive()).isTrue();
    }

    @Test
    void closeDeactivatesOpenJob() {
        Job job = job(JobStatus.OPEN);

        transitionService.apply(job, JobStatus.CLOSED);

        assertThat(job.getStatus()).isEqualTo(JobStatus.CLOSED);
        assertThat(job.isActive()).isFalse();
    }

    @Test
    void cancelDeactivatesOnHoldJob() {
        Job job = job(JobStatus.ON_HOLD);

        transitionService.apply(job, JobStatus.CANCELLED);

        assertThat(job.getStatus()).isEqualTo(JobStatus.CANCELLED);
        assertThat(job.isActive()).isFalse();
    }

    @Test
    void reopenMovesOnHoldToOpen() {
        Job job = job(JobStatus.ON_HOLD);

        transitionService.apply(job, JobStatus.OPEN);

        assertThat(job.getStatus()).isEqualTo(JobStatus.OPEN);
        assertThat(job.getPublishedOn()).isNotNull();
        assertThat(job.isActive()).isTrue();
    }

    @Test
    void draftCannotBeClosed() {
        Job job = job(JobStatus.DRAFT);

        assertThatThrownBy(() -> transitionService.apply(job, JobStatus.CLOSED))
                .isInstanceOf(BadRequestException.class)
                .extracting(ex -> ((BadRequestException) ex).getCode())
                .isEqualTo("INVALID_STATUS_TRANSITION");
        assertThat(job.getStatus()).isEqualTo(JobStatus.DRAFT);
    }

    @Test
    void closedJobIsTerminal() {
        Job job = job(JobStatus.CLOSED);

        assertThatThrownBy(() -> transitionService.apply(job, JobStatus.OPEN))
                .isInstanceOf(BadRequestException.class)
                .extracting(ex -> ((BadRequestException) ex).getCode())
                .isEqualTo("INVALID_STATUS_TRANSITION");
    }

    @Test
    void cancelledJobIsTerminal() {
        Job job = job(JobStatus.CANCELLED);

        assertThatThrownBy(() -> transitionService.apply(job, JobStatus.ON_HOLD))
                .isInstanceOf(BadRequestException.class)
                .extracting(ex -> ((BadRequestException) ex).getCode())
                .isEqualTo("INVALID_STATUS_TRANSITION");
    }

    @Test
    void openJobCannotBePublishedAgain() {
        Job job = job(JobStatus.OPEN);

        assertThatThrownBy(() -> transitionService.apply(job, JobStatus.OPEN))
                .isInstanceOf(BadRequestException.class)
                .extracting(ex -> ((BadRequestException) ex).getCode())
                .isEqualTo("INVALID_STATUS_TRANSITION");
    }

    @Test
    void draftCanBeCancelled() {
        Job job = job(JobStatus.DRAFT);

        transitionService.apply(job, JobStatus.CANCELLED);

        assertThat(job.getStatus()).isEqualTo(JobStatus.CANCELLED);
        assertThat(job.isActive()).isFalse();
    }

    @Test
    void onHoldCanBeClosed() {
        Job job = job(JobStatus.ON_HOLD);

        transitionService.apply(job, JobStatus.CLOSED);

        assertThat(job.getStatus()).isEqualTo(JobStatus.CLOSED);
        assertThat(job.isActive()).isFalse();
    }
}
