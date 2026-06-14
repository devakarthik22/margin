package com.margin.scm.github;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubSignatureVerifierTest {

    private static final String SECRET = "s3cr3t";
    private final GitHubSignatureVerifier verifier =
            new GitHubSignatureVerifier(new GitHubProperties("token", SECRET));

    private String sign(byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
    }

    @Test
    void acceptsAValidSignature() throws Exception {
        byte[] body = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
        assertThat(verifier.isValid(body, sign(body))).isTrue();
    }

    @Test
    void rejectsATamperedBody() throws Exception {
        byte[] original = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
        byte[] tampered = "{\"action\":\"closed\"}".getBytes(StandardCharsets.UTF_8);
        assertThat(verifier.isValid(tampered, sign(original))).isFalse();
    }

    @Test
    void rejectsMissingSignature() {
        assertThat(verifier.isValid("x".getBytes(StandardCharsets.UTF_8), null)).isFalse();
    }
}
