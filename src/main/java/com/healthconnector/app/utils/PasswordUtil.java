package com.healthconnector.app.utils;

import com.healthconnector.app.constants.AppConstants;
import com.healthconnector.app.exception.BusinessException;
import com.healthconnector.app.constants.ErrorCodes;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Password utility for BCrypt operations and policy validation.
 */
@Component
public class PasswordUtil {

    /**
     * Password policy: min 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special char.
     */
    private static final Pattern PASSWORD_POLICY = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_#^])[A-Za-z\\d@$!%*?&_#^]{8,}$"
    );

    /**
     * Validates a plain-text password against the platform's password policy.
     *
     * @param password the plain-text password to validate
     * @throws BusinessException if the password does not meet policy requirements
     */
    public void validatePasswordPolicy(String password) {
        if (password == null || !PASSWORD_POLICY.matcher(password).matches()) {
            throw new BusinessException(
                    ErrorCodes.PASSWORD_POLICY_VIOLATED,
                    AppConstants.MSG_PASSWORD_POLICY
            );
        }
    }

    /**
     * Returns {@code true} if the given password meets policy requirements.
     */
    public boolean meetsPolicy(String password) {
        return password != null && PASSWORD_POLICY.matcher(password).matches();
    }
}
