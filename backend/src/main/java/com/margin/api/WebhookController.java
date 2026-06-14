package com.margin.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.margin.domain.model.PullRequestRef;
import com.margin.pipeline.ReviewRoute;
import com.margin.pipeline.ReviewRoutePayload;
import com.margin.scm.ScmProviderType;
import com.margin.scm.github.GitHubPullRequestEvent;
import com.margin.scm.github.GitHubSignatureVerifier;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Receives GitHub pull_request webhooks. Every request is signature-verified
 * against the shared secret before anything else happens; only then is a job
 * dropped onto the async review route. Returns immediately so GitHub's delivery
 * isn't blocked on the review.
 */
@RestController
@RequestMapping("/api/webhooks/github")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final ProducerTemplate producer;
    private final GitHubSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;

    public WebhookController(ProducerTemplate producer,
                             GitHubSignatureVerifier signatureVerifier,
                             ObjectMapper objectMapper) {
        this.producer = producer;
        this.signatureVerifier = signatureVerifier;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<Void> onPullRequest(
            @RequestHeader(value = "X-GitHub-Event", required = false) String event,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody byte[] rawBody) throws Exception {

        if (!signatureVerifier.isValid(rawBody, signature)) {
            log.warn("Rejected GitHub webhook with invalid signature.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!"pull_request".equals(event)) {
            return ResponseEntity.accepted().build();
        }

        GitHubPullRequestEvent payload = objectMapper.readValue(rawBody, GitHubPullRequestEvent.class);
        if (!("opened".equals(payload.action()) || "synchronize".equals(payload.action()))) {
            return ResponseEntity.accepted().build();
        }

        PullRequestRef ref = new PullRequestRef(
                payload.repository().owner().login(),
                payload.repository().name(),
                payload.pullRequest().number(),
                payload.pullRequest().head().sha());

        producer.sendBody(ReviewRoute.ENTRY, new ReviewRoutePayload(ref, ScmProviderType.GITHUB));
        return ResponseEntity.accepted().build();
    }
}
