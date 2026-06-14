import { Component, Input } from '@angular/core';
import { Finding } from '../../../../core/models/finding.model';
import { SeverityBadgeComponent } from '../../../../shared/severity-badge/severity-badge.component';

/**
 * Renders findings as notes against a gutter rule — the product metaphor made
 * literal: comments in the margin, each pinned to its line.
 */
@Component({
  selector: 'mg-finding-list',
  standalone: true,
  imports: [SeverityBadgeComponent],
  template: `
    @if (findings.length === 0) {
      <div class="clean">No issues found. The agent read it and had nothing to flag.</div>
    } @else {
      <div class="findings">
        @for (f of findings; track $index) {
          <article class="finding" [style.animation-delay.ms]="$index * 70">
            <div class="gutter"><span class="ln">{{ f.line != null ? 'L' + f.line : '—' }}</span></div>
            <div class="note">
              <div class="note-head">
                <span class="cat">{{ f.category }}</span>
                <mg-severity-badge [severity]="f.severity" />
              </div>
              <h3 class="title">{{ f.title }}</h3>
              <p class="expl">{{ f.explanation }}</p>
              @if (f.suggestion) {
                <div class="fix">
                  <span class="fix-label">suggested fix</span>
                  <code>{{ f.suggestion }}</code>
                </div>
              }
            </div>
          </article>
        }
      </div>
    }
  `,
  styles: [`
    .clean { font-size:14px; color:#4EC8A0; background:rgba(78,200,160,.08);
      border:1px solid rgba(78,200,160,.25); border-radius:10px; padding:14px; }
    .findings { display:flex; flex-direction:column; }
    .finding { display:grid; grid-template-columns:52px 1fr; animation:rise .4s ease both; }
    .gutter { border-right:1px solid var(--line); padding-right:12px;
      display:flex; justify-content:flex-end; padding-top:16px; }
    .finding:first-child .gutter { padding-top:0; }
    .ln { font-family:var(--mono); font-size:11px; color:var(--faint); }
    .note { padding:0 0 18px 16px; }
    .finding:not(:first-child) .note { padding-top:16px; }
    .note-head { display:flex; align-items:center; gap:10px; margin-bottom:7px; }
    .cat { font-family:var(--mono); font-size:10px; letter-spacing:.12em; text-transform:uppercase;
      color:var(--muted); border:1px solid var(--line); border-radius:5px; padding:2px 7px; }
    .title { font-size:14.5px; font-weight:600; margin-bottom:5px; line-height:1.35; }
    .expl { font-size:13px; color:var(--muted); line-height:1.6; }
    .fix { margin-top:10px; background:var(--canvas); border:1px solid var(--line);
      border-left:2px solid var(--accent); border-radius:8px; padding:9px 11px;
      display:flex; flex-direction:column; gap:5px; }
    .fix-label { font-family:var(--mono); font-size:9.5px; letter-spacing:.14em;
      text-transform:uppercase; color:var(--faint); }
    .fix code { font-family:var(--mono); font-size:12px; color:var(--text);
      line-height:1.55; white-space:pre-wrap; }
    @keyframes rise { from { opacity:0; transform:translateY(8px); } to { opacity:1; transform:none; } }
    @media (prefers-reduced-motion: reduce) { .finding { animation:none; } }
  `],
})
export class FindingListComponent {
  @Input() findings: Finding[] = [];
}
