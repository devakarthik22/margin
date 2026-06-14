package com.margin.scm.github;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Verifies the {@code X-Hub-Signature-256} header GitHub sends with every webhook.
 * Without this, anyone who learns the endpoint URL could trigger reviews. The
 * comparison is constant-time to avoid leaking the secret via timing.
 */
@Component
public class GitHubSignatureVerifier {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String PREFIX = "sha256=";

    private final byte[] secret;

    public GitHubSignatureVerifier(GitHubProperties props) {
        String s = props.webhookSecret() == null ? "" : props.webhookSecret();
        this.secret = s.getBytes(StandardCharsets.UTF_8);
    }

    /** True if {@code signatureHeader} is a valid HMAC of {@code rawBody}. */
    public boolean isValid(byte[] rawBody, String signatureHeader) {
        if (secret.length == 0 || signatureHeader == null || !signatureHeader.startsWith(PREFIX)) {
            return false;
        }
        String expected = PREFIX + hexHmac(rawBody);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signatureHeader.getBytes(StandardCharsets.UTF_8));
    }

    private String hexHmac(byte[] body) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            byte[] digest = mac.doFinal(body);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute HMAC", e);
        }
    }
}
