import { Component, Input } from '@angular/core';
import { ReviewResponse } from '../../../../core/models/review.model';
import { SEVERITY_META, Severity } from '../../../../core/models/review.enums';

/** The verdict line, summary text, and per-severity tally. */
@Component({
  selector: 'mg-review-summary',
  standalone: true,
  template: `
    <div class="summary">
      <p class="text">{{ review.summary }}</p>
      <div class="tally">
        @for (s of order; track s) {
          @if (review.countsBySeverity[s]) {
            <span class="pill" [style.--c]="meta(s).color">
              <span class="dot"></span>{{ review.countsBySeverity[s] }} {{ meta(s).label.toLowerCase() }}
            </span>
          }
        }
      </div>
    </div>
  `,
  styles: [`
    .summary { display:flex; flex-direction:column; gap:11px; }
    .text { font-size:14.5px; line-height:1.55; color:var(--text); }
    .tally { display:flex; flex-wrap:wrap; gap:7px; }
    .pill { display:inline-flex; align-items:center; gap:6px; font-family:var(--mono);
      font-size:11px; color:var(--c); border:1px solid var(--line); border-radius:20px; padding:3px 10px; }
    .dot { width:6px; height:6px; border-radius:50%; background:var(--c); }
  `],
})
export class ReviewSummaryComponent {
  @Input({ required: true }) review!: ReviewResponse;
  protected readonly order: Severity[] = ['critical', 'high', 'medium', 'low'];
  meta(s: Severity) { return SEVERITY_META[s]; }
}
