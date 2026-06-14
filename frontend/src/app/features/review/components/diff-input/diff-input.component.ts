import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ReviewService } from '../../../../core/services/review.service';
import { PrFile, PrFiles } from '../../../../core/models/pr-files.model';

const SAMPLE = `diff --git a/PaymentService.java b/PaymentService.java
--- a/PaymentService.java
+++ b/PaymentService.java
@@ -10,6 +10,9 @@ public class PaymentService {
     public BigDecimal settle(String accountId, double amount) {
         Account acc = repo.find(accountId);
+        String sql = "SELECT * FROM ledger WHERE acct = '" + accountId + "'";
+        Statement st = conn.createStatement();
+        ResultSet rs = st.executeQuery(sql);
+        double fee = amount * 0.02;
+        return acc.getBalance().subtract(BigDecimal.valueOf(amount + fee));
     }`;

/**
 * Left panel: two tabs. "Pull Request" loads a public PR's file tree + diffs
 * (GitHub-style) and triggers a read-only review; "Paste Diff" reviews a pasted
 * unified diff. Emits the chosen review action up; owns its own browse state.
 */
@Component({
  selector: 'mg-diff-input',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="panel">
      <div class="tabs">
        <button class="tab" [class.on]="tab() === 'pr'" (click)="tab.set('pr')">Pull Request</button>
        <button class="tab" [class.on]="tab() === 'diff'" (click)="tab.set('diff')">Paste Diff</button>
      </div>

      @if (tab() === 'pr') {
        <div class="pr-bar">
          <input class="pr-url" type="url" spellcheck="false" [(ngModel)]="prUrl"
                 (keyup.enter)="loadPr()"
                 placeholder="https://github.com/owner/repo/pull/123" />
          <button class="ghost" (click)="loadPr()" [disabled]="prLoading() || !prUrl.trim()">
            {{ prLoading() ? 'Loading…' : 'Load' }}
          </button>
        </div>

        @if (prError()) { <div class="pr-err">{{ prError() }}</div> }

        @if (prFiles(); as pr) {
          <div class="pr-head">
            <span class="pr-title">{{ pr.title || (pr.owner + '/' + pr.repo + ' #' + pr.number) }}</span>
            <span class="pr-count">{{ pr.files.length }} file{{ pr.files.length === 1 ? '' : 's' }}</span>
          </div>

          <div class="filelist">
            @for (f of pr.files; track f.path) {
              <button class="file" [class.sel]="selected() === f" (click)="selected.set(f)">
                <span class="fpath">{{ f.path }}</span>
                <span class="fstat"><span class="add">+{{ f.additions }}</span><span class="del">−{{ f.deletions }}</span></span>
              </button>
            }
          </div>

          <div class="diffview">
            @if (selected(); as f) {
              @for (l of f.lines; track $index) {
                <div class="dl {{ l.type }}">
                  <span class="ln">{{ l.line ?? '' }}</span>
                  <span class="lc">{{ glyph(l.type) }}{{ l.content }}</span>
                </div>
              }
            }
          </div>

          <div class="actions">
            <button class="run" [disabled]="loading" (click)="runGithub.emit(prUrl)">
              {{ loading ? 'Reviewing…' : 'Review PR' }}
            </button>
          </div>
        } @else if (!prLoading()) {
          <div class="pr-empty">Paste a public GitHub PR link and load it to browse the changes.</div>
        }
      }

      @if (tab() === 'diff') {
        <textarea class="code" spellcheck="false" [(ngModel)]="diff"
                  placeholder="Paste a unified diff…"></textarea>
        <div class="actions">
          <button class="ghost" (click)="diff = sample">Load sample</button>
          <button class="run" [disabled]="loading || !diff.trim()" (click)="run.emit(diff)">
            {{ loading ? 'Reviewing…' : 'Run review' }}
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    :host { display:flex; flex-direction:column; min-height:0; height:100%; }
    .panel { gap:12px; }

    .tabs { display:flex; gap:4px; flex:none; background:var(--canvas);
      border:1px solid var(--line); border-radius:9px; padding:4px; }
    .tab { flex:1; font-size:13px; font-weight:600; color:var(--muted);
      background:transparent; border:none; border-radius:6px; padding:8px 12px; cursor:pointer; transition:.15s; }
    .tab:hover { color:var(--text); }
    .tab.on { background:var(--surface2); color:var(--text); }

    .pr-bar { display:flex; gap:8px; flex:none; }
    .pr-url { flex:1; font-family:var(--mono); font-size:13px; color:var(--text);
      background:var(--canvas); border:1px solid var(--line); border-radius:8px; padding:9px 11px; outline:none; }
    .pr-url:focus { border-color:var(--accent); }
    .pr-err { flex:none; font-size:13px; color:#E8826E; background:rgba(232,130,110,.08);
      border:1px solid rgba(232,130,110,.25); border-radius:8px; padding:10px 12px; }
    .pr-empty { flex:1; display:flex; align-items:center; justify-content:center; text-align:center;
      color:var(--muted); font-size:13.5px; line-height:1.6; padding:24px; }

    .pr-head { flex:none; display:flex; align-items:center; justify-content:space-between; gap:10px; }
    .pr-title { font-size:13.5px; font-weight:600; color:var(--text); overflow:hidden;
      text-overflow:ellipsis; white-space:nowrap; }
    .pr-count { flex:none; font-family:var(--mono); font-size:11px; color:var(--faint); }

    .filelist { flex:none; max-height:30%; overflow-y:auto; display:flex; flex-direction:column;
      gap:2px; background:var(--canvas); border:1px solid var(--line); border-radius:8px; padding:6px; }
    .file { display:flex; align-items:center; justify-content:space-between; gap:10px;
      background:transparent; border:none; border-radius:6px; padding:6px 8px; cursor:pointer; text-align:left; }
    .file:hover { background:var(--surface2); }
    .file.sel { background:var(--surface2); }
    .fpath { font-family:var(--mono); font-size:12px; color:var(--text); overflow:hidden;
      text-overflow:ellipsis; white-space:nowrap; direction:rtl; }
    .fstat { flex:none; font-family:var(--mono); font-size:11px; display:flex; gap:7px; }
    .fstat .add { color:#4EC8A0; } .fstat .del { color:#E8826E; }

    .diffview { flex:1; min-height:0; overflow:auto; background:var(--canvas);
      border:1px solid var(--line); border-radius:8px; padding:8px 0;
      font-family:var(--mono); font-size:12px; line-height:1.65; }
    .dl { display:flex; gap:0; white-space:pre; }
    .dl .ln { flex:none; width:46px; text-align:right; padding:0 10px 0 6px; color:var(--faint);
      user-select:none; }
    .dl .lc { flex:1; padding-right:10px; }
    .dl.add { background:rgba(78,200,160,.10); } .dl.add .lc { color:#bfe9d8; }
    .dl.del { background:rgba(232,130,110,.10); } .dl.del .lc { color:#f0c4b9; }
    .dl.ctx .lc { color:var(--muted); }
    .dl.hunk { background:rgba(124,140,248,.08); } .dl.hunk .lc { color:var(--accent); }
    .dl.hunk .ln { color:transparent; }
  `],
  styleUrl: '../../../../shared/panel.css',
})
export class DiffInputComponent {
  private readonly reviewService = inject(ReviewService);

  @Input() loading = false;
  @Output() run = new EventEmitter<string>();
  @Output() runGithub = new EventEmitter<string>();
  @Output() prLoaded = new EventEmitter<PrFiles | null>();

  protected readonly sample = SAMPLE;
  protected readonly tab = signal<'pr' | 'diff'>('pr');
  protected readonly prLoading = signal(false);
  protected readonly prError = signal('');
  protected readonly prFiles = signal<PrFiles | null>(null);
  protected readonly selected = signal<PrFile | null>(null);

  diff = SAMPLE;
  prUrl = '';

  protected loadPr(): void {
    const url = this.prUrl.trim();
    if (!url || this.prLoading()) return;
    this.prLoading.set(true);
    this.prError.set('');
    this.prFiles.set(null);
    this.selected.set(null);

    this.reviewService.fetchPrFiles(url).subscribe({
      next: pr => {
        this.prFiles.set(pr);
        this.selected.set(pr.files[0] ?? null);
        this.prLoading.set(false);
        this.prLoaded.emit(pr);
      },
      error: () => {
        this.prError.set("Couldn't load that PR. Check it's a public GitHub PR link and try again.");
        this.prLoading.set(false);
      },
    });
  }

  protected glyph(type: string): string {
    return type === 'add' ? '+' : type === 'del' ? '-' : type === 'hunk' ? '' : ' ';
  }
}
