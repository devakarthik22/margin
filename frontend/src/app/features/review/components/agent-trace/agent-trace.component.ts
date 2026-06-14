import { Component, Input } from '@angular/core';
import { TraceStep } from '../../../../core/models/trace-step.model';

/** Renders the agent's live reasoning stream. The signature element. */
@Component({
  selector: 'mg-agent-trace',
  standalone: true,
  template: `
    <div class="trace">
      @for (step of steps; track $index) {
        <div class="line" [class]="step.kind">
          <span class="prefix">{{ prefix(step.kind) }}</span>{{ step.text }}
        </div>
      }
      @if (active) {
        <div class="line"><span class="prefix">·</span><span class="blink">▍</span></div>
      }
    </div>
  `,
  styles: [`
    :host { display:flex; flex-direction:column; flex:1; min-height:0; }
    .trace { flex:1; min-height:0; overflow-y:auto; background:var(--canvas); border:1px solid var(--line);
      border-radius:10px; padding:14px; font-family:var(--mono); font-size:12.5px; line-height:1.95; }
    .line { display:flex; gap:9px; color:var(--muted); }
    .prefix { color:var(--faint); width:10px; flex:none; text-align:center; }
    .tool { color:var(--accent); }
    .ret { color:#4EC8A0; }
    .done { color:var(--text); font-weight:600; }
    .blink { color:var(--accent); animation:blink 1s steps(2) infinite; }
    @keyframes blink { 50% { opacity:0; } }
    @media (prefers-reduced-motion: reduce) { .blink { animation:none; } }
  `],
})
export class AgentTraceComponent {
  @Input() steps: TraceStep[] = [];
  @Input() active = false;

  prefix(kind: string): string {
    return kind === 'tool' ? '→' : kind === 'ret' ? '←' : kind === 'done' ? '✓' : '·';
  }
}
