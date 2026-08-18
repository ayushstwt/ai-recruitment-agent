package com.ayshriv.recruitment.common.security;

import com.ayshriv.recruitment.common.config.AppProperties;
import com.ayshriv.recruitment.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates passwords against the configurable {@code app.security.password-policy}
 * requirements before they are hashed and stored.
 *
 * <p>The policy is externalized through {@link AppProperties} so operators can
 * relax or tighten requirements without code changes. Violations surface as
 * {@code 400} responses carrying the {@code INVALID_PASSWORD} code.</p>
 */
@Component
@RequiredArgsConstructor
public class PasswordPolicyValidator {

    /**
     * Machine readable code returned when a password does not satisfy the
     * configured policy or does not match during a password change.
     */
    public static final String INVALID_PASSWORD = "INVALID_PASSWORD";

    private final AppProperties appProperties;

    /**
     * Validate a candidate password against the configured policy.
     *
     * @param password candidate password
     * @throws BadRequestException when the password violates the policy
     */
    public void validate(String password) {
        AppProperties.PasswordPolicy policy = appProperties.getSecurity().getPasswordPolicy();

        List<String> errors = new ArrayList<>();

        if (password == null || password.isBlank()) {
            errors.add("Password is required");
        } else {
            if (password.length() < policy.getMinLength()) {
                errors.add("Password must be at least " + policy.getMinLength() + " characters");
            }
            if (policy.isRequireUppercase() && password.chars().noneMatch(Character::isUpperCase)) {
                errors.add("Password must contain at least one uppercase letter");
            }
            if (policy.isRequireLowercase() && password.chars().noneMatch(Character::isLowerCase)) {
                errors.add("Password must contain at least one lowercase letter");
            }
            if (policy.isRequireDigit() && password.chars().noneMatch(Character::isDigit)) {
                errors.add("Password must contain at least one digit");
            }
            if (policy.isRequireSpecialCharacter() && password.chars().noneMatch(c -> !Character.isLetterOrDigit(c))) {
                errors.add("Password must contain at least one special character");
            }
        }

        if (!errors.isEmpty()) {
            throw new BadRequestException(String.join("; ", errors), INVALID_PASSWORD);
        }
    }
}