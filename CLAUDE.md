# use2qubo — Plan & Ticket Protocol

Repo-specific rules for how Claude plans and tracks work here. Applies whenever plan mode produces an implementation plan for this repo.

## Plan structure: waves

Every plan's task list is organized into **waves**, not a flat list.

- A wave groups tasks with no cross-dependency within the wave — same wave, disjoint files, no shared state.
- Waves run in order; tasks inside a wave run in parallel, one subagent per task, when the plan's scope justifies it (skip parallel subagents for trivial/single-file plans — use judgement on scope).
- State the dependency graph driving the wave split, not just the waves themselves — say why a task waits for an earlier wave.
- Prefer cheap/small-model subagents for mechanical, well-specified tasks (code moves, boilerplate extraction); reserve full-context/sequential agents for steps needing whole-file understanding (final rewiring, cross-cutting changes).
- Compile/test after each wave, not only at the end, so a broken wave is caught before the next wave builds on it.
- Final wave is always verification (build, tests, spot-check), sequential.

Reference example: `tickets/JAVA-*` (once sorted into `done/`, see below) and any plan file under `~/.claude/plans/` written for this repo follow this shape — see the `QuboEngine.java` split plan for a worked example of wave grouping, dependency notes, and subagent assignment.

## Ticket protocol

Plans for this repo get written to disk as tickets in `tickets/`, split into three folders:

- `tickets/todo/` — plan written, not started.
- `tickets/in-progress/` — implementation underway.
- `tickets/done/` — implementation complete, verified.

Workflow:

1. When a plan is approved, write it as a ticket markdown file in `tickets/todo/`, using the template below. Include the wave-organized task list from the plan.
2. **First task of implementation is always**: move the ticket file from `tickets/todo/` to `tickets/in-progress/`.
3. Execute the wave-organized tasks.
4. **Last task of implementation is always**: move the ticket file from `tickets/in-progress/` to `tickets/done/`, updating its `Status:` field.

Ticket ID convention: `JAVA-NNN-short-slug.md`, sequential numbering across all three folders (check the highest existing number across `todo/`, `in-progress/`, `done/` before assigning a new one).

### Ticket template

```markdown
# JAVA-NNN — Short title

**Status:** Open | In Progress | Done
**Priority:** Low | Medium | High
**Depends on:** none, or JAVA-NNN (what it provides)
**Context:** Why this ticket exists — the problem, finding, or request that triggered it. Enough detail that the ticket is self-contained without re-reading the conversation that produced it.

## Scope

- Bullet or numbered list of concrete changes, file by file or concern by concern.
- Quote current text/code being changed where it makes the ticket self-contained (line numbers drift — say "re-check before editing" if precision matters).

## Execution waves

(Omit this section for single-file/trivial tickets — use judgement on scope, per the plan-structure rules above.)

**Wave 0 — ...:**
- task, task

**Wave 1 — depends on Wave 0 (N parallel subagents):**
- task (file/class it touches, what it depends on)

...

**Wave N — sequential verification:**
- build, test, spot-check

## Files Changed

| File | Change |
|---|---|
| `path/to/File.java` | one-line description |

## Acceptance criteria / Verification

- Build/test commands to run.
- Specific behaviour or output to check (exact numbers/values where known).

## Commit

Once done, `git commit` the changes with a commit message of 15 words max.
```

Reference tickets already following this shape (once sorted into `done/`): `JAVA-015-derive-isolation-and-escalation-confirm.md` (multi-section scope, implementation notes for a handoff), `JAVA-017-paper-limitations-update.md` (quoted current text for self-containment).

## Worktrees for this repo

`use2qubo` is a git submodule of the outer `Ai_driven_research_papers` repo (`.git` is a file redirecting to `.../Ai_driven_research_papers/.git/modules/articles/qmod_2026/tools/use2qubo`). `EnterWorktree` with a bare `name` refuses outright on this redirect ("a core.worktree redirect... commands run there would write outside the worktree") — a false positive: `git worktree add` resolves the submodule redirect correctly (verified: the created worktree's own `.git` file points cleanly to `.../modules/.../worktrees/<name>`), but the tool's safety heuristic can't tell that from a real misconfiguration and bails before creating anything.

**Workflow for implementation tickets in a worktree:**

1. `git worktree add .claude/worktrees/<name> -b <branch-name> <ref>` manually via Bash — `<ref>` should be local `develop` HEAD (or the branch you're building on), not `origin/develop`. (`worktree.baseRef` is set to `"head"` at the outer repo's `.claude/settings.json`, so `EnterWorktree`-driven worktrees now default to local HEAD too — but this manual step is still required to dodge the submodule refusal in the first place.)
2. `EnterWorktree` with `path: .claude/worktrees/<name>` (not `name:`) to attach the session — entering an *existing* worktree by path skips the redirect safety check that blocks creation by `name`.

Skipping step 1 and calling `EnterWorktree` with `name` will refuse; don't retry the same call expecting it to succeed differently.
