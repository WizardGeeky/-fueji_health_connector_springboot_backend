package com.healthconnector.app.utils;

import com.healthconnector.app.exception.AESException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256/GCM/NoPadding deterministic encryption utility.
 *
 * <p><strong>Determinism guarantee:</strong>
 * The IV (nonce) is derived deterministically from
 * {@code SHA-256(secretKey || plainText)[0..11]}.
 * This means the <em>same plaintext + same key always produces the same ciphertext</em>,
 * enabling reliable decrypt() calls on stored values.
 *
 * <p><strong>Security note:</strong>
 * A deterministic IV is safe here because each (key, plaintext) pair produces a unique IV,
 * so GCM nonce reuse across distinct plaintexts is avoided. The secret key must be
 * kept confidential — store it in environment variables, never in source control.
 *
 * <p>Key format: base64-encoded 32-byte (256-bit) value from {@code application.yml → aes.secret-key}.
 */
@Component
@Slf4j
public class AESUtil {

    private static final String ALGORITHM      = "AES/GCM/NoPadding";
    private static final int    GCM_IV_LENGTH  = 12;   // 96 bits recommended for GCM
    private static final int    GCM_TAG_LENGTH = 128;  // bits

    private final SecretKey secretKey;
    private final byte[]    rawKeyBytes;

    public AESUtil(@Value("${aes.secret-key}") String base64Key) {
        this.rawKeyBytes = Base64.getDecoder().decode(base64Key);
        if (rawKeyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "AES key must be exactly 32 bytes (256 bits). Provided: " + rawKeyBytes.length + " bytes.");
        }
        this.secretKey = new SecretKeySpec(rawKeyBytes, "AES");
    }

    /**
     * Encrypts {@code plainText} using AES-256/GCM with a <strong>deterministic IV</strong>.
     *
     * <p>The IV is derived as:
     * <pre>iv = SHA-256(rawKeyBytes || utf8(plainText))[0..11]</pre>
     *
     * <p>Storage format (base64): {@code base64(iv[12] || cipherText[n] || gcmTag[16])}
     *
     * @param plainText value to encrypt; {@code null} returns {@code null}
     * @return base64-encoded ciphertext (IV + ciphertext + GCM tag)
     * @throws AESException on any crypto failure
     */
    public String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            byte[] iv = deriveIv(plainText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Prepend IV so we can extract it during decryption
            byte[] output = new byte[GCM_IV_LENGTH + cipherBytes.length];
            System.arraycopy(iv,          0, output, 0,              GCM_IV_LENGTH);
            System.arraycopy(cipherBytes, 0, output, GCM_IV_LENGTH,  cipherBytes.length);

            return Base64.getEncoder().encodeToString(output);
        } catch (Exception e) {
            throw new AESException("Failed to encrypt sensitive data", e);
        }
    }

    /**
     * Decrypts a value previously produced by {@link #encrypt(String)}.
     *
     * @param encryptedText base64-encoded string (IV + ciphertext + GCM tag)
     * @return decrypted plaintext; {@code null} if input is {@code null}
     * @throws AESException on any crypto failure
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            if (decoded.length < GCM_IV_LENGTH + 16) {
                throw new AESException("Encrypted data is too short to be valid");
            }

            byte[] iv         = Arrays.copyOfRange(decoded, 0, GCM_IV_LENGTH);
            byte[] cipherBytes = Arrays.copyOfRange(decoded, GCM_IV_LENGTH, decoded.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] plainBytes = cipher.doFinal(cipherBytes);

            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (AESException e) {
            throw e;
        } catch (Exception e) {
            throw new AESException("Failed to decrypt sensitive data", e);
        }
    }

    /**
     * Returns {@code true} if the given string is already AES-encrypted
     * (i.e. it can be successfully decrypted with the current key).
     */
    public boolean isEncrypted(String value) {
        if (value == null) return false;
        try {
            decrypt(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Private Helpers ────────────────────────────────────────────────

    /**
     * Deterministically derives a 12-byte GCM IV from the secret key and plaintext.
     *
     * <pre>iv = SHA-256(keyBytes || utf8(plainText))[0..11]</pre>
     */
    private byte[] deriveIv(String plainText) throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        sha256.update(rawKeyBytes);
        sha256.update(plainText.getBytes(StandardCharsets.UTF_8));
        byte[] hash = sha256.digest();
        return Arrays.copyOfRange(hash, 0, GCM_IV_LENGTH);
    }
}
