package com.margin.pipeline;

import com.margin.domain.model.PullRequestRef;
import com.margin.scm.ScmProviderType;

/** Message passed onto the Camel review route. */
public record ReviewRoutePayload(PullRequestRef ref, ScmProviderType provider) {
}
