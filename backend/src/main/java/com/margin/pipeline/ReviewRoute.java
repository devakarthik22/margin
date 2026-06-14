package com.margin.pipeline;

import com.margin.review.ReviewService;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 * Apache Camel route for asynchronous reviews (e.g. triggered by a webhook).
 * Camel owns the integration concerns here — queueing, redelivery, and dead-letter
 * handling — while the business step stays a single call into {@link ReviewService}.
 * Synchronous callers use ReviewService directly; this route is the async path.
 */
@Component
public class ReviewRoute extends RouteBuilder {

    public static final String ENTRY = "seda:reviews?concurrentConsumers=4";

    private final ReviewService reviewService;

    public ReviewRoute(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Override
    public void configure() {
        onException(Exception.class)
                .maximumRedeliveries(2)
                .redeliveryDelay(2000)
                .handled(true)
                .log("Review failed for ${body.ref().slug()}: ${exception.message}");

        from(ENTRY)
                .routeId("pr-review")
                .log("Reviewing ${body.ref().slug()}")
                .process(exchange -> {
                    ReviewRoutePayload p = exchange.getIn().getBody(ReviewRoutePayload.class);
                    reviewService.reviewPullRequest(p.ref(), p.provider());
                })
                .log("Completed review for ${body.ref().slug()}");
    }
}
