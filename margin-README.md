# Margin

An autonomous code-review **agent**: it reads a pull-request diff, decides when it
needs more context (pulling files on demand via a tool call), and writes findings
back as inline comments — plus a dashboard that shows the whole thing.

```
margin/
├── backend/    Spring Boot 3 · Spring AI (Gemini) · Apache Camel · JPA
└── frontend/   Angular 17 · standalone components
```

---

## Why it's an *agent*, not a prompt

A one-shot reviewer stuffs the whole repo into a prompt. Margin gives the model a
single tool — `getFileContent(path)` — and lets it **decide** when a hunk needs
surrounding code. That handles large diffs gracefully and keeps token cost down.
The tool is bound per-review to the correct PR head, so it always reads the right
version of a file.

---

## Backend architecture (layered, dependency-inverted)

```
api ──▶ review (application) ──▶ agent · diff · scm · persistence (ports)
                                      └── gemini · github · jpa (adapters)
        everything points inward at the domain model
```

The domain (`domain/model`) is plain immutable records with no framework imports.
Every outward dependency is an **interface (port)**; the concrete technology lives
in an **adapter**. The orchestrator (`DefaultReviewService`) knows the *sequence*
but none of the *implementations*.

### Design patterns, and where they live

| Pattern | Where | Why |
|---|---|---|
| **Strategy** | `ScmProvider` (GitHub / GitLab / …) | Swap the source-control host without touching review logic |
| **Factory** | `ScmProviderFactory` | Selects a provider by type; auto-discovers new beans (Open/Closed) |
| **Ports & Adapters** | `CodeReviewAgent`, `ReviewStore`, `FileContextProvider` | Domain and application depend on interfaces; Gemini/JPA/HTTP are replaceable |
| **Builder** | `Finding.builder()` | One validated construction path for findings everywhere |
| **Anti-corruption layer** | `AgentReviewResponse` + `AgentResponseMapper` | The LLM's loose JSON never leaks into the strict domain |
| **Template/Pipeline** | `ReviewRoute` (Camel) | Integration concerns (retry, dead-letter, concurrency) wrap a single business call |

### SOLID, concretely

- **S** — `ReviewPromptFactory` builds prompts; `AgentResponseMapper` maps; the agent only orchestrates the call.
- **O** — adding GitLab support = one new `ScmProvider` bean. No existing class changes.
- **L** — every `ScmProvider` is substitutable behind the factory.
- **I** — `FileContextProvider` is a single-method port; the agent depends on nothing wider.
- **D** — `DefaultReviewService` depends only on `DiffParser`, `CodeReviewAgent`, `FindingValidator`, `ReviewStore`, `ScmProviderFactory` — all interfaces.

### Two safeguards worth calling out

- **Hallucinated line numbers** → `FindingValidator` drops any line that isn't an
  *addressable* line in the diff (keeps the note, removes the bad anchor).
- **Idempotency** → reviews are keyed by commit `headSha`; the same commit is never
  reviewed twice, so no duplicate comments and no wasted model calls.

### Request flows

- **PR / webhook (async):** GitHub webhook → `WebhookController` → Camel `seda:reviews`
  → `ReviewService.reviewPullRequest` → fetch diff → parse → agent → validate →
  persist → publish review.
- **Ad-hoc (dashboard):** `POST /api/reviews/diff` → `ReviewService.reviewRawDiff`
  → parse → agent → validate. No SCM round-trip, no publish, not persisted
  (a scratchpad review). History reflects PR-flow reviews.

---

## Frontend architecture (Angular 17, standalone)

```
core/      models (typed DTOs + enums), services (ReviewService, TraceAnimator)
features/  review/  review-page (smart) + presentational child components
shared/    severity-badge, panel.css
```

- `ReviewPageComponent` is the only **smart** component — a small signal-driven
  state machine (`idle → reviewing → done/error`). Everything else is presentational
  and driven by `@Input`/`@Output`.
- `ReviewService` is the single API gateway; components never touch `HttpClient`.
- `TraceAnimator` is isolated so the scripted agent trace can later be swapped for a
  real server-sent stream of the agent's actual tool calls.

---

## Running it

### Backend
```bash
cd backend
export GITHUB_TOKEN=...           # for the PR/webhook flow
export GEMINI_API_KEY=...         # Google AI Studio API key
export GITHUB_WEBHOOK_SECRET=...   # shared secret for webhook verification
mvn spring-boot:run               # JDK 21 + Maven · starts on :8080 (H2 by default)
```
Swap H2 for Postgres by overriding `spring.datasource.*`.

### Frontend
```bash
cd frontend
npm install
npm start                         # :4200, proxies /api → :8080
```

### Try the ad-hoc flow
Open the dashboard, hit **Load sample**, then **Run review**.

### Endpoints
| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/reviews/diff` | Review a pasted diff (dashboard) |
| `POST` | `/api/reviews` | Review a PR by reference (fetch + publish) |
| `GET`  | `/api/reviews/{owner}/{repo}` | Past reviews for a repo, newest first |
| `POST` | `/api/webhooks/github` | GitHub `pull_request` events (signature-verified) |

```bash
# Review history for a repository
curl localhost:8080/api/reviews/acme/payments
```

```bash
curl -X POST localhost:8080/api/reviews/diff \
  -H 'Content-Type: application/json' \
  -d '{"rawDiff":"--- a/A.java\n+++ b/A.java\n@@ -1,1 +1,2 @@\n a();\n+b();"}'
```

---

## Tests
Covered with plain JUnit/Mockito, no Spring context required:
- `UnifiedDiffParserTest` — line-number tracking
- `FindingValidatorTest` — hallucinated-line guard
- `AgentResponseMapperTest` — enum fallbacks, malformed-finding skip
- `GeminiCodeReviewAgentTest` — empty-diff short-circuits without a model call
- `GitHubSignatureVerifierTest` — valid / tampered / missing signature

```bash
cd backend && mvn test
```

---

## Security
Every GitHub webhook is HMAC-SHA256 verified against `margin.github.webhook-secret`
(`X-Hub-Signature-256`) with a constant-time compare before any work is done;
invalid signatures get a 401. See `GitHubSignatureVerifier`.

## Notes / extension points
- Add a `GitLabScmProvider` to support GitLab — factory picks it up automatically.
- Stream real agent steps to the UI over SSE/WebSocket via the `TraceAnimator` seam.
