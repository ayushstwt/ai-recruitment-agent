package com.ayshriv.recruitment.common.security;

import com.ayshriv.recruitment.common.config.AppProperties;
import com.ayshriv.recruitment.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyValidatorTest {

    private final AppProperties appProperties = new AppProperties();

    private final PasswordPolicyValidator validator = new PasswordPolicyValidator(appProperties);

    @Test
    void acceptsPasswordMeetingDefaultPolicy() {
        assertThatCode(() -> validator.validate("TemporaryPassword123!"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsBlankPassword() {
        assertThatThrownBy(() -> validator.validate("   "))
                .isInstanceOf(BadRequestException.class)
                .extracting(ex -> ((BadRequestException) ex).getCode())
                .isEqualTo("INVALID_PASSWORD");
    }

    @Test
    void rejectsTooShortPassword() {
        assertThatThrownBy(() -> validator.validate("Short1!"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least 8 characters");
    }

    @Test
    void rejectsPasswordWithoutUppercase() {
        assertThatThrownBy(() -> validator.validate("password123!"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("uppercase");
    }

    @Test
    void rejectsPasswordWithoutDigitOrSpecialCharacter() {
        assertThatThrownBy(() -> validator.validate("Passwordonly"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("digit")
                .hasMessageContaining("special character");
    }

    @Test
    void policyIsConfigurable() {
        AppProperties.PasswordPolicy policy = appProperties.getSecurity().getPasswordPolicy();
        policy.setMinLength(12);
        policy.setRequireUppercase(false);
        policy.setRequireLowercase(false);
        policy.setRequireDigit(false);
        policy.setRequireSpecialCharacter(false);

        assertThatThrownBy(() -> validator.validate("short1!"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least 12 characters");
        assertThatCode(() -> validator.validate("longenoughwithoutrules"))
                .doesNotThrowAnyException();
        assertThat(policy.getMinLength()).isEqualTo(12);
    }
}