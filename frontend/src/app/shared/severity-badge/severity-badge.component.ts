import { Component, Input } from '@angular/core';
import { Severity, SEVERITY_META } from '../../core/models/review.enums';

/** Small, reusable severity pill. One job: render a severity with its colour. */
@Component({
  selector: 'mg-severity-badge',
  standalone: true,
  template: `
    <span class="badge" [style.--c]="meta.color">
      <span class="dot"></span>{{ meta.label }}
    </span>
  `,
  styles: [`
    .badge { display:inline-flex; align-items:center; gap:5px;
      font-family:var(--mono); font-size:11px; color:var(--c); }
    .dot { width:7px; height:7px; border-radius:50%; background:var(--c);
      box-shadow:0 0 8px var(--c); }
  `],
})
export class SeverityBadgeComponent {
  @Input({ required: true }) severity!: Severity;
  get meta() { return SEVERITY_META[this.severity]; }
}
