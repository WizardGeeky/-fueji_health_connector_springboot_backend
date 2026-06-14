package com.healthconnector.app.utils;

import com.healthconnector.app.constants.AppConstants;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates cryptographically secure temporary passwords that meet the platform's policy.
 */
@Component
public class TemporaryPasswordGenerator {

    private static final String UPPERCASE  = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE  = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS     = "0123456789";
    private static final String SPECIALS   = "@$!%*?&_#^";
    private static final String ALL_CHARS  = UPPERCASE + LOWERCASE + DIGITS + SPECIALS;

    private final SecureRandom random = new SecureRandom();

    /**
     * Generates a temporary password guaranteed to meet password policy:
     * at least 1 uppercase, 1 lowercase, 1 digit, 1 special character.
     *
     * @return a secure temporary password of length {@link AppConstants#TEMP_PASSWORD_LENGTH}
     */
    public String generate() {
        char[] password = new char[AppConstants.TEMP_PASSWORD_LENGTH];
        // Guarantee at least one of each required category
        password[0] = UPPERCASE.charAt(random.nextInt(UPPERCASE.length()));
        password[1] = LOWERCASE.charAt(random.nextInt(LOWERCASE.length()));
        password[2] = DIGITS.charAt(random.nextInt(DIGITS.length()));
        password[3] = SPECIALS.charAt(random.nextInt(SPECIALS.length()));
        // Fill remaining positions with random characters from all categories
        for (int i = 4; i < AppConstants.TEMP_PASSWORD_LENGTH; i++) {
            password[i] = ALL_CHARS.charAt(random.nextInt(ALL_CHARS.length()));
        }
        // Shuffle to avoid predictable position pattern
        for (int i = password.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = password[i];
            password[i] = password[j];
            password[j] = tmp;
        }
        return new String(password);
    }
}
