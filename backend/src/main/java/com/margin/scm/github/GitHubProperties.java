package com.margin.scm.github;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalised GitHub config, bound from {@code margin.github.*}.
 * {@code webhookSecret} is the shared secret used to verify webhook signatures.
 */
@ConfigurationProperties(prefix = "margin.github")
public record GitHubProperties(String token, String webhookSecret) {
}
