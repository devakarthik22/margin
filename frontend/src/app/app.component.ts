import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'mg-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <div class="app">
      <header class="top">
        <div class="brand">
          <span class="mark" aria-hidden="true">
            <span class="rule"></span><span class="dot"></span>
          </span>
          <div>
            <div class="name">Margin</div>
            <div class="tag">an agent that reads your diff and writes back</div>
          </div>
        </div>
      </header>
      <router-outlet />
    </div>
  `,
  styles: [`
    .app { height:100vh; overflow:hidden; padding:22px; display:flex; flex-direction:column; gap:18px; }
    .top { display:flex; align-items:center; justify-content:space-between; }
    .brand { display:flex; align-items:center; gap:13px; }
    .mark { position:relative; width:26px; height:30px; flex:none; }
    .rule { position:absolute; left:7px; top:0; bottom:0; width:2px; background:var(--line); }
    .dot { position:absolute; left:4px; top:9px; width:8px; height:8px; border-radius:50%;
      background:var(--accent); box-shadow:0 0 14px 1px rgba(124,140,248,.6); }
    .name { font-weight:700; font-size:18px; letter-spacing:-.02em; }
    .tag { font-size:12px; color:var(--muted); }
    @media (max-width:880px) {
      /* Stacked layout scrolls the whole page instead of pinning to the viewport. */
      .app { height:auto; min-height:100vh; overflow:visible; padding:16px; }
    }
  `],
})
export class AppComponent {}
