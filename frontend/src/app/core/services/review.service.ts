import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ReviewRequest, ReviewResponse } from '../models/review.model';
import { TraceStep } from '../models/trace-step.model';
import { PrFiles } from '../models/pr-files.model';

export type StreamEvent =
  | { type: 'trace'; step: TraceStep }
  | { type: 'result'; data: ReviewResponse };

export interface PublishReviewRequest {
  prUrl: string;
  summary: string;
  comments: { path: string | null; line: number | null; body: string }[];
}

/**
 * Single gateway to the review API. Components depend on this service, not on
 * HttpClient directly, so the transport can change without touching the UI.
 */
@Injectable({ providedIn: 'root' })
export class ReviewService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/reviews';

  /** Review a pull request by reference (triggers fetch + publish on the server). */
  reviewPullRequest(request: ReviewRequest): Observable<ReviewResponse> {
    return this.http.post<ReviewResponse>(this.baseUrl, request);
  }

  /** Fetches a public PR's parsed file list + diffs for the browser view. */
  fetchPrFiles(prUrl: string): Observable<PrFiles> {
    return this.http.post<PrFiles>(`${this.baseUrl}/github/files`, { prUrl });
  }

  /** Posts user-edited comments to a GitHub PR (requires a write-scoped token on the server). */
  publishReview(req: PublishReviewRequest): Observable<{ posted: number }> {
    return this.http.post<{ posted: number }>(`${this.baseUrl}/github/publish`, req);
  }

  /**
   * Streams agent trace events and the final review result over SSE.
   * Uses fetch + ReadableStream since EventSource does not support POST.
   */
  streamDiff(rawDiff: string): Observable<StreamEvent> {
    return this.streamSse(`${this.baseUrl}/diff/stream`, { rawDiff });
  }

  /**
   * Reviews a public GitHub PR by URL (read-only — nothing is posted back).
   * Same SSE contract as {@link streamDiff}.
   */
  streamGithub(prUrl: string): Observable<StreamEvent> {
    return this.streamSse(`${this.baseUrl}/github/stream`, { prUrl });
  }

  /** Shared SSE transport: POST a JSON body, parse the event/data stream. */
  private streamSse(url: string, body: Record<string, string>): Observable<StreamEvent> {
    return new Observable<StreamEvent>(subscriber => {
      const controller = new AbortController();

      fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
        signal: controller.signal,
      }).then(async response => {
        if (!response.ok) {
          subscriber.error(new Error(`HTTP ${response.status}`));
          return;
        }
        const reader = response.body!.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        let currentEvent = '';

        while (true) {
          const { done, value } = await reader.read();
          if (done) { subscriber.complete(); break; }
          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split('\n');
          buffer = lines.pop()!;

          for (const line of lines) {
            if (line.startsWith('event:')) {
              currentEvent = line.slice(6).trim();
            } else if (line.startsWith('data:') && line.length > 5) {
              const data = JSON.parse(line.slice(5).trim());
              if (currentEvent === 'trace') subscriber.next({ type: 'trace', step: data as TraceStep });
              else if (currentEvent === 'result') subscriber.next({ type: 'result', data: data as ReviewResponse });
              else if (currentEvent === 'error') subscriber.error(new Error(data?.message ?? 'Review failed'));
            }
          }
        }
      }).catch(err => { if (err.name !== 'AbortError') subscriber.error(err); });

      return () => controller.abort();
    });
  }
}
