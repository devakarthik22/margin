import { Component, Input, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Finding } from '../../../../core/models/finding.model';
import { Category, Severity } from '../../../../core/models/review.enums';
import { PrFiles } from '../../../../core/models/pr-files.model';
import { ReviewService } from '../../../../core/services/review.service';
import { SeverityBadgeComponent } from '../../../../shared/severity-badge/severity-badge.component';

interface SnippetLine { type: string; line: number | null; content: string; target: boolean; }

interface Draft {
  path: string | null;
  line: number | null;
  category: Category;
  severity: Severity;
  body: string;
  include: boolean;
  snippet: SnippetLine[];
}

type PostState = 'idle' | 'posting' | 'done' | 'error';

const NO_FILE = '(no file)';

/**
 * Turns findings into editable draft comments shown GitHub-style — the code line(s)
 * the comment attaches to, with the editable comment beneath. Comments are grouped
 * by file and can be filtered to a single file. Copy as markdown, or post to the PR.
 */
@Component({
  selector: 'mg-comment-composer',
  standalone: true,
  imports: [FormsModule, SeverityBadgeComponent],
  template: `
    @if (drafts().length === 0) {
      <div class="clean">No issues found. The agent read it and had nothing to flag.</div>
    } @else {
      @if (filePaths().length > 1) {
        <div class="filter">
          <button class="chip" [class.on]="fileFilter() === null" (click)="fileFilter.set(null)">
            All · {{ drafts().length }}
          </button>
          @for (p of filePaths(); track p) {
            <button class="chip" [class.on]="fileFilter() === p" (click)="fileFilter.set(p)" [title]="p">
              {{ basename(p) }} · {{ countFor(p) }}
            </button>
          }
        </div>
      }

      <div class="groups">
        @for (g of groups(); track g.path) {
          <div class="group">
            <div class="group-head">{{ g.path }}</div>
            @for (d of g.drafts; track $index) {
              <article class="card" [class.off]="!d.include">
                <div class="card-head">
                  <label class="incl"><input type="checkbox" [(ngModel)]="d.include" /></label>
                  <span class="cat">{{ d.category }}</span>
                  <mg-severity-badge [severity]="d.severity" />
                  <span class="loc">{{ d.line != null ? 'line ' + d.line : 'no line to attach' }}</span>
                </div>

                @if (d.snippet.length) {
                  <div class="snippet">
                    @for (s of d.snippet; track $index) {
                      <div class="sl {{ s.type }}" [class.tgt]="s.target">
                        <span class="sln">{{ s.line }}</span>
                        <span class="slc">{{ glyph(s.type) }}{{ s.content }}</span>
                      </div>
                    }
                  </div>
                }

                <textarea class="edit" [(ngModel)]="d.body" [disabled]="!d.include"></textarea>
              </article>
            }
          </div>
        }
      </div>

      <div class="composer-actions">
        <span class="status" [class.err]="post() === 'error'" [class.ok]="post() === 'done'">{{ statusMsg() }}</span>
        <button class="ghost" (click)="copy()" [disabled]="includedCount() === 0">
          {{ copied() ? 'Copied ✓' : 'Copy markdown' }}
        </button>
        @if (prUrl) {
          <button class="run" (click)="publish()"
                  [disabled]="post() === 'posting' || postableCount() === 0">
            {{ post() === 'posting' ? 'Posting…' : 'Post ' + postableCount() + ' to PR' }}
          </button>
        }
      </div>
    }
  `,
  styles: [`
    :host { display:flex; flex-direction:column; min-height:0; flex:1; gap:12px; }
    .clean { font-size:14px; color:#4EC8A0; background:rgba(78,200,160,.08);
      border:1px solid rgba(78,200,160,.25); border-radius:10px; padding:14px; }

    .filter { flex:none; display:flex; flex-wrap:wrap; gap:6px; }
    .chip { font-family:var(--mono); font-size:11px; color:var(--muted); background:var(--canvas);
      border:1px solid var(--line); border-radius:7px; padding:5px 10px; cursor:pointer; transition:.15s; }
    .chip:hover { color:var(--text); } .chip.on { background:var(--surface2); color:var(--text); border-color:var(--faint); }

    .groups { flex:1; min-height:0; overflow-y:auto; display:flex; flex-direction:column;
      gap:16px; padding-right:6px; }
    .group { display:flex; flex-direction:column; gap:9px; }
    .group-head { font-family:var(--mono); font-size:11.5px; color:var(--text);
      padding:6px 9px; background:var(--canvas); border:1px solid var(--line); border-radius:7px;
      overflow:hidden; text-overflow:ellipsis; white-space:nowrap; position:sticky; top:0; z-index:1; }

    .card { border:1px solid var(--line); border-radius:10px; overflow:hidden;
      background:var(--canvas); display:flex; flex-direction:column; transition:.15s; }
    .card.off { opacity:.5; }
    .card-head { display:flex; align-items:center; gap:9px; flex-wrap:wrap; padding:10px 12px; }
    .incl { display:flex; align-items:center; }
    .incl input { width:15px; height:15px; accent-color:var(--accent); cursor:pointer; }
    .cat { font-family:var(--mono); font-size:10px; letter-spacing:.12em; text-transform:uppercase;
      color:var(--muted); border:1px solid var(--line); border-radius:5px; padding:2px 7px; }
    .loc { font-family:var(--mono); font-size:11px; color:var(--faint); margin-left:auto; }

    .snippet { font-family:var(--mono); font-size:12px; line-height:1.6;
      border-top:1px solid var(--line); border-bottom:1px solid var(--line); background:var(--surface); }
    .sl { display:flex; white-space:pre; }
    .sl .sln { flex:none; width:44px; text-align:right; padding:0 10px 0 6px; color:var(--faint); user-select:none; }
    .sl .slc { flex:1; padding-right:10px; }
    .sl.add { background:rgba(78,200,160,.08); } .sl.add .slc { color:#bfe9d8; }
    .sl.del { background:rgba(232,130,110,.08); } .sl.del .slc { color:#f0c4b9; }
    .sl.ctx .slc { color:var(--muted); }
    .sl.tgt { background:rgba(124,140,248,.14); box-shadow:inset 2px 0 0 var(--accent); }
    .sl.tgt .slc { color:var(--text); }

    .edit { width:100%; resize:vertical; min-height:62px; background:var(--canvas);
      border:none; border-top:1px solid var(--line); padding:10px 12px; color:var(--text);
      font-family:var(--mono); font-size:12.5px; line-height:1.6; outline:none; }
    .edit:focus { background:var(--surface); }
    .edit:disabled { opacity:.6; }

    .composer-actions { flex:none; display:flex; align-items:center; gap:10px; justify-content:flex-end; }
    .status { margin-right:auto; font-size:12px; color:var(--muted); }
    .status.err { color:#E8826E; } .status.ok { color:#4EC8A0; }
    .ghost, .run { font-size:13px; font-weight:600; border-radius:9px; padding:9px 15px;
      cursor:pointer; border:1px solid transparent; transition:.15s; }
    .ghost { background:transparent; color:var(--muted); border-color:var(--line); }
    .ghost:hover { color:var(--text); border-color:var(--faint); }
    .ghost:disabled { opacity:.5; cursor:default; }
    .run { background:var(--accent); color:#0E0F15; box-shadow:0 4px 18px -6px rgba(124,140,248,.7); }
    .run:hover { filter:brightness(1.07); }
    .run:disabled { opacity:.55; cursor:default; box-shadow:none; }
  `],
})
export class CommentComposerComponent {
  private readonly reviewService = inject(ReviewService);

  @Input() summary = '';
  @Input() prUrl: string | null = null;

  protected readonly drafts = signal<Draft[]>([]);
  protected readonly fileFilter = signal<string | null>(null);
  protected readonly post = signal<PostState>('idle');
  protected readonly copied = signal(false);
  private readonly postedMsg = signal('');

  private _files: PrFiles | null = null;

  @Input() set findings(list: Finding[]) {
    this.drafts.set((list ?? []).map(f => ({
      path: f.filePath,
      line: f.line,
      category: f.category,
      severity: f.severity,
      body: this.composeBody(f),
      include: true,
      snippet: this.computeSnippet(f.filePath, f.line),
    })));
    this.fileFilter.set(null);
    this.post.set('idle');
    this.copied.set(false);
    this.postedMsg.set('');
  }

  @Input() set prFiles(files: PrFiles | null) {
    this._files = files;
    // PR data may arrive alongside findings; recompute snippets once we have it.
    this.drafts.update(ds => ds.map(d => ({ ...d, snippet: this.computeSnippet(d.path, d.line) })));
  }

  // --- grouping / filtering -------------------------------------------------

  protected filePaths(): string[] {
    return [...new Set(this.drafts().map(d => d.path ?? NO_FILE))];
  }

  protected countFor(path: string): number {
    return this.drafts().filter(d => (d.path ?? NO_FILE) === path).length;
  }

  protected basename(path: string): string {
    return path.split('/').pop() || path;
  }

  protected groups(): { path: string; drafts: Draft[] }[] {
    const filter = this.fileFilter();
    const map = new Map<string, Draft[]>();
    for (const d of this.drafts()) {
      const key = d.path ?? NO_FILE;
      if (filter != null && key !== filter) continue;
      (map.get(key) ?? map.set(key, []).get(key)!).push(d);
    }
    return [...map.entries()].map(([path, drafts]) => ({ path, drafts }));
  }

  // --- counts (methods, not computed: they react to in-place edits) ---------

  protected includedCount(): number {
    return this.drafts().filter(d => d.include).length;
  }
  protected postableCount(): number {
    return this.drafts().filter(d => d.include && d.path && d.line != null).length;
  }

  protected glyph(type: string): string {
    return type === 'add' ? '+' : type === 'del' ? '-' : ' ';
  }

  protected statusMsg(): string {
    return this.post() === 'posting' ? 'Posting review to GitHub…'
      : this.post() === 'done' || this.post() === 'error' ? this.postedMsg() : '';
  }

  // --- helpers --------------------------------------------------------------

  private composeBody(f: Finding): string {
    let body = `**${f.title}**\n\n${f.explanation}`;
    if (f.suggestion) body += `\n\n_Suggested fix:_ ${f.suggestion}`;
    return body;
  }

  /** Up to 2 lines of preceding context plus the commented line, from the PR diff. */
  private computeSnippet(path: string | null, line: number | null): SnippetLine[] {
    if (!this._files || path == null || line == null) return [];
    const file = this._files.files.find(f => f.path === path);
    if (!file) return [];
    const idx = file.lines.findIndex(l => l.type !== 'hunk' && l.line === line);
    if (idx < 0) return [];
    const out: SnippetLine[] = [];
    for (let i = Math.max(0, idx - 2); i <= idx; i++) {
      const l = file.lines[i];
      if (l.type === 'hunk') continue;
      out.push({ type: l.type, line: l.line, content: l.content, target: i === idx });
    }
    return out;
  }

  // --- actions --------------------------------------------------------------

  protected copy(): void {
    const included = this.drafts().filter(d => d.include);
    const md = [
      this.summary ? `### Margin review\n\n${this.summary}\n` : '### Margin review\n',
      ...included.map(d => {
        const loc = d.path ? `\`${d.path}${d.line != null ? ':' + d.line : ''}\`` : '';
        return `---\n${loc ? loc + '\n\n' : ''}${d.body}`;
      }),
    ].join('\n');
    navigator.clipboard.writeText(md).then(() => {
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    });
  }

  protected publish(): void {
    if (!this.prUrl) return;
    const comments = this.drafts()
      .filter(d => d.include && d.path && d.line != null)
      .map(d => ({ path: d.path, line: d.line, body: d.body }));
    if (comments.length === 0) return;

    this.post.set('posting');
    this.reviewService.publishReview({ prUrl: this.prUrl, summary: this.summary, comments }).subscribe({
      next: res => {
        this.post.set('done');
        this.postedMsg.set(`Posted ${res.posted} comment${res.posted === 1 ? '' : 's'} to the PR ✓`);
      },
      error: err => {
        this.post.set('error');
        const detail = err?.error?.detail ?? err?.error?.message ?? 'Check the token has write access to this repo.';
        this.postedMsg.set(`Couldn't post: ${detail}`);
      },
    });
  }
}
