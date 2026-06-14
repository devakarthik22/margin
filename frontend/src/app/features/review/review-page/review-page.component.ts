import { Component, inject, signal } from '@angular/core';
import { ReviewService } from '../../../core/services/review.service';
import { ReviewResponse } from '../../../core/models/review.model';
import { TraceStep } from '../../../core/models/trace-step.model';
import { PrFiles } from '../../../core/models/pr-files.model';
import { VERDICT_META } from '../../../core/models/review.enums';
import { DiffInputComponent } from '../components/diff-input/diff-input.component';
import { AgentTraceComponent } from '../components/agent-trace/agent-trace.component';
import { ReviewSummaryComponent } from '../components/review-summary/review-summary.component';
import { CommentComposerComponent } from '../components/comment-composer/comment-composer.component';

type ViewState = 'idle' | 'reviewing' | 'done' | 'error';

/**
 * Smart component: thin state machine (idle → reviewing → done/error).
 * Subscribes to the SSE stream from ReviewService — trace steps arrive in
 * real time as the agent runs, followed by the final result event.
 */
@Component({
  selector: 'mg-review-page',
  standalone: true,
  imports: [DiffInputComponent, AgentTraceComponent, ReviewSummaryComponent, CommentComposerComponent],
  templateUrl: './review-page.component.html',
  styleUrl: './review-page.component.css',
})
export class ReviewPageComponent {
  private readonly reviewService = inject(ReviewService);

  protected readonly state = signal<ViewState>('idle');
  protected readonly steps = signal<TraceStep[]>([]);
  protected readonly result = signal<ReviewResponse | null>(null);
  protected readonly errorMsg = signal<string>('');
  protected readonly reviewedPrUrl = signal<string | null>(null);
  protected readonly prFiles = signal<PrFiles | null>(null);

  protected get verdictMeta() {
    const r = this.result();
    return r ? VERDICT_META[r.verdict] : null;
  }

  run(rawDiff: string): void {
    this.reviewedPrUrl.set(null);
    this.prFiles.set(null);
    this.consume(this.reviewService.streamDiff(rawDiff),
      "The review didn't complete. Check the diff and run it again.");
  }

  runPr(prUrl: string): void {
    this.reviewedPrUrl.set(prUrl);
    this.consume(this.reviewService.streamGithub(prUrl),
      "Couldn't review that PR. Check the link is a public GitHub PR and try again.");
  }

  private consume(stream$: ReturnType<ReviewService['streamDiff']>, errorMsg: string): void {
    this.state.set('reviewing');
    this.steps.set([]);
    this.result.set(null);

    stream$.subscribe({
      next: event => {
        if (event.type === 'trace') {
          this.steps.update(cur => [...cur, event.step]);
        } else if (event.type === 'result') {
          this.finish(event.data);
        }
      },
      error: () => {
        this.state.set('error');
        this.errorMsg.set(errorMsg);
      },
    });
  }

  private finish(res: ReviewResponse): void {
    const n = res.findings.length;
    this.steps.update(cur => [...cur, { kind: 'done', text: `done · ${n} note${n === 1 ? '' : 's'}` }]);
    this.result.set(res);
    this.state.set('done');
  }
}
