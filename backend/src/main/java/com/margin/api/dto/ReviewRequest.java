package com.margin.api.dto;

import com.margin.scm.ScmProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** Request to review a pull request on demand. */
public record ReviewRequest(
        @NotBlank String owner,
        @NotBlank String repo,
        @Positive int number,
        @NotBlank String headSha,
        ScmProviderType provider) {

    public ScmProviderType providerOrDefault() {
        return provider == null ? ScmProviderType.GITHUB : provider;
    }
}
