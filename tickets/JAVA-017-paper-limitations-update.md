# JAVA-017 — Paper: state pair-encoding and escalation-cost limitations explicitly

**Status:** Open
**Priority:** Low (paper text only, no code)
**Depends on:** none (references JAVA-015's escalation-confirm feature as the mitigation to cite; can be written once JAVA-015 lands, or drafted earlier and adjusted)
**Context:** The architectural review behind JAVA-015/JAVA-016 surfaced two facts about `use2qubo` that the paper (`articles/qmod_2026`) doesn't currently state as explicit limitations:

1. Decision variables are hardcoded as `(association, classA, classB)` pairs (`DecisionVar`/`QuboConfig`, `qubo/context/QuboContextBuilder.java:106-118`) — an edge/pair-selection encoding. There's no path for a single-object boolean flag (e.g. `isOpen(Bin)`) or an n-ary decision variable. Both current examples (waste routing, max clique) happen to be pair/edge problems, so this ceiling is invisible in the case studies but is a real modelling constraint relevant to RQ1.
2. Degree-escalation sampling cost scales as `C(n, k)` (see JAVA-015) — currently only implicitly visible via the existing "Scale is also limited" sentence in `06-discussion.tex`.

The paper should name both explicitly rather than leaving them implied by "two graph problems" and a generic scale disclaimer.

## Scope

- `sections/04-approach.tex`, end of §Configuring `qubo_config.json` (after line 69, before the `QuboConfigView` sentence): add one sentence stating decision variables are currently restricted to binary associations between two object classes (pair/edge encoding); no single-object boolean decision variable or n-ary decision variable is expressible in the current `decision_vars` contract.
- `sections/06-discussion.tex`, threat-to-validity paragraph (lines 17-19, alongside "the two examples are not domain-general"): add a sentence naming the pair-encoding restriction as a distinct, structural limitation of the plugin's contract itself (not just a domain-coverage gap) — problems whose natural decision variable is a single-object flag or an n-ary relation need remodelling as pairwise associations, or an extension of the `decision_vars` schema.
- Same section, near the escalation discussion (lines 22-23): add a sentence noting sampling cost at degree `k` scales as `C(n, k)`, so `max_degree` beyond 2 is only practical for small-to-mid `n`; mention the interactive escalation confirmation (JAVA-015) as the plugin's current mitigation (keeps a human in the loop rather than hanging), and flag proper scaling (sparse/structured sampling instead of full combinatorial enumeration at higher degrees) as future work.
- `sections/07-conclusion.tex`, line 13 ("generalise the domain model to higher order problems"): extend to explicitly mention lifting the pairwise-decision-variable restriction, since it's now a named limitation rather than implicit.
- Follow existing `CLAUDE.md` writing rules throughout: no em dashes, one sentence per line, British spelling, `\ac{...}` macros for declared acronyms.

## Files Changed

| File | Change |
|---|---|
| `sections/04-approach.tex` | +1 sentence, §Configuring qubo_config.json |
| `sections/06-discussion.tex` | +2 sentences, threat-to-validity + escalation paragraphs |
| `sections/07-conclusion.tex` | Extend existing future-work sentence (line 13) |

## Current text, quoted, so this ticket is self-contained (re-check line numbers before editing — other work may shift them)

**`sections/04-approach.tex`, lines 66-70** (insert the new sentence after line 69, before the `QuboConfigView` sentence on line 70):
> `decision_vars` is an ordered list of entries, each pairing an association with its domain classes; this order fixes the index order of the flat binary vector `x` that AutoQUBO samples over, and consequently the row/column order of the resulting QUBO matrix.
> `objective.expression` is the OCL expression to sample [...] Section 5 instantiates this same four-field contract for two unrelated domains.
> USE2QUBO also ships a form-based `QuboConfigView` [...] ← new sentence goes immediately before this one.

**`sections/06-discussion.tex`, lines 17-19** (threat-to-validity paragraph — append the pair-encoding sentence here):
> A threat to validity is that the two examples are not domain-general.
> Waste collection and maximum clique show that the plugin handles two structurally different combinatorial problems via configuration alone, evidence of some structural generalisability, but other problem domains remain future work.
> There is also no quantum hardware execution: [...]

**`sections/06-discussion.tex`, lines 21-25** (escalation/quadratization paragraph — append the sampling-cost-scaling sentence here, near the end):
> On the path to automation, the plugin already automates Q matrix coefficient derivation end-to-end via live OCL sampling.
> Sampling itself escalates past degree 2 when needed, up to a configured `max_degree` [...]
> [...] Quadratization is also not free: each ancilla enlarges the search space (247 variables against 9 decision variables in the waste collection case), which Section 6 (experiment) shows can make the QUBO harder for a fixed-budget classical annealer to solve to optimality even once it is exact. ← new sentence goes after this one.

**`sections/07-conclusion.tex`, line 13**:
> We also plan to generalise the domain model to higher order problems and test larger problem instances, and to run a user study comparing time-to-correct-QUBO for domain experts using the USE OCL modelling path against direct QUBO coding to evaluate how the plugin helps real users reach their optimization goals.

**Writing rules, from `articles/qmod_2026/CLAUDE.md`** (quoted so this ticket doesn't require cross-referencing another file):
> - British English throughout.
> - No em dashes. Use semicolons, commas, or restructure.
> - Present tense for contributions.
> - One sentence per line in `.tex` source; new line only after `.`, no manual intra-sentence line breaks.
> - Use `acro` package macros for all declared acronyms (`\ac{qubo}`, `\ac{ocl}`, `\ac{uml}`, `\ac{vrp}`, `\ac{poc}`, `\ac{mde}`, `\ac{m2t}`, `\ac{qse}`, `\ac{qc}`, `\ac{nisq}`, `\ac{tsp}`).
> - Tool name USE is not an acronym; write `USE~\ac{ocl}` (non-breaking space before `\ac{ocl}`).

Note: quoted excerpts above are de-macro'd for readability in this ticket; the actual `.tex` source uses `\ac{qubo}`, `\ac{ocl}`, etc. — match that in the real edit, don't write plain "QUBO"/"OCL" in the `.tex` file itself.

## Acceptance criteria / Verification

- Rebuild `main.pdf`; no new overfull hboxes.
- Proofread new sentences against `CLAUDE.md` style rules (no em dashes, one sentence per line, British spelling, `\ac{}` macros for declared acronyms).
- Cross-check wording against JAVA-015's actual implemented behaviour before finalizing (don't describe the escalation-confirm mitigation in the past tense until it has landed).

## Commit

Once done, `git commit` the changes with a commit message of 15 words max.
