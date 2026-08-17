package com.ayshriv.recruitment.common.entity;

import com.ayshriv.recruitment.organization.entity.Organization;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityLifecycleTest {

    private BaseEntity newEntity() {
        return new Organization("Acme Recruitment");
    }

    @Test
    void onCreateInitializesLifecycleFields() {
        BaseEntity entity = newEntity();

        entity.onCreate();

        assertThat(entity.getId()).isNull();
        assertThat(entity.getCreatedOn()).isNotNull();
        assertThat(entity.getUpdatedOn()).isNotNull();
        assertThat(entity.isActive()).isTrue();
        assertThat(entity.isDeleted()).isFalse();
        assertThat(entity.isNew()).isTrue();
    }

    @Test
    void onUpdateRefreshesUpdatedTimestamp() {
        BaseEntity entity = newEntity();
        entity.onCreate();

        entity.onUpdate();

        assertThat(entity.getUpdatedOn()).isNotNull();
        assertThat(entity.getCreatedOn()).isNotNull();
    }

    @Test
    void activateSetsActiveAndClearsDeleted() {
        BaseEntity entity = newEntity();
        entity.onCreate();
        entity.softDelete();

        entity.activate();

        assertThat(entity.isActive()).isTrue();
        assertThat(entity.isDeleted()).isFalse();
    }

    @Test
    void deactivateDisablesActiveWithoutDeleting() {
        BaseEntity entity = newEntity();
        entity.onCreate();

        entity.deactivate();

        assertThat(entity.isActive()).isFalse();
        assertThat(entity.isDeleted()).isFalse();
    }

    @Test
    void softDeleteDisablesAndMarksDeleted() {
        BaseEntity entity = newEntity();
        entity.onCreate();

        entity.softDelete();

        assertThat(entity.isActive()).isFalse();
        assertThat(entity.isDeleted()).isTrue();
    }

    @Test
    void idBasedConstructorMarksEntityAsManaged() {
        BaseEntity entity = new Organization(42L);

        assertThat(entity.getId()).isEqualTo(42L);
        assertThat(entity.isNew()).isFalse();
    }
}
