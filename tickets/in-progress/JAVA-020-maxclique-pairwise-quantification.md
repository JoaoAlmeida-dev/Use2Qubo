# JAVA-020 — MaxCliquePairwise example: quantify automation cost vs. hand-crafted QUBO (answers R2)

**Status:** Open
**Priority:** High (blocks resolving a reviewer comment on the submitted paper)
**Depends on:** none
**Context:** Reviewer 2 (`articles/qmod_2026/Reviews.md`, and inline `\Rtwo{}` comments in `sections/06-discussion.tex:27` and `sections/09-experiment.tex:6`) argues the paper's real novelty is the domain-model-to-optimisation-model step, since Ocean/PyQUBO already automate penalty weighting, HUBO→QUBO quadratization, and integer-to-binary encoding. They want a *quantified* comparison of automated vs. expert-hand-crafted QUBO formulation quality (penalty λ tightness, ancilla count, degree needed), not just qualitative description.

Existing verified numbers (from `CLAUDE.md` §Scope Limits, cross-checked against `examples/MaxClique/qubo.json`): the current max clique example's `cliqueProperty` invariant, contexted on `Vertex` (10 tasks, one per vertex, each task body still reads up to 9 other decision variables since it loops over all other selected vertices to check adjacency), produces `nVars=293` (10 decision + 283 ancillas), `polyDegree=9`, `exact=true` (exhaustive), Verma-Lewis `B=2.0`.

Lucas (2014) (already cited, `sections/02-related-work.tex:30`) catalogues a hand-crafted canonical Max Clique/Independent Set Ising formulation that is native degree-2 with zero ancillas, because it's phrased as one penalty term per non-adjacent vertex *pair*, not one predicate per vertex reading the whole candidate set.

The hypothesis: the ancilla/degree cost in the current example is an artefact of *how the OCL invariant is phrased* (whole-candidate-set predicate vs. pairwise), not an inherent limitation of the plugin's automated sampling/quadratization pipeline. If a pairwise-phrased invariant is added to a new model variant, `QuboEngine`'s own automatic pipeline should derive a QUBO matching Lucas's hand-crafted structure (degree 2, no ancillas needed for that term), which would let the paper show automation does not sacrifice formulation quality when the domain expert phrases invariants correctly, directly answering R2.

## Mechanics established during investigation (see Scope for how to apply)

- `PenaltyEvaluator` produces one penalty task per `(invariant, instance-of-context-class)` pair; `context Vertex inv ...` yields 10 tasks, each reading as many decision variables as its body's navigation touches.
- OCL invariants bind `self` to one object of the context class; a plain `association` (like `Edge`) has no object identity, so there's no way to get a native per-pair penalty task without giving pairs their own class.
- Adding an `associationclass NonAdjacent between Vertex[1] role va; Vertex[1] role vb end`, populated with the 27 non-edge pairs (complement of the 18-edge benchmark graph), lets a `context NonAdjacent inv nonAdjacentPenalty: not (self.va.solution->notEmpty() and self.vb.solution->notEmpty())` read exactly 2 decision variables per task (native degree 2, no escalation needed for this term).
- Full non-edge pair list (27 pairs, i<j, complement of the 18 edges in `MaxClique.cmd`):
  `(1,2)(1,4)(1,5)(1,6)(1,7)(1,8)(1,9)(1,10)(2,3)(2,6)(2,7)(2,8)(2,9)(2,10)(3,5)(3,8)(3,10)(4,5)(4,10)(5,6)(5,9)(5,10)(6,8)(6,10)(8,9)(8,10)(9,10)`
- CLI: `java -cp "target/use2qubo-1.0.0.jar;lib/*" org.tzi.use.plugin.use2qubo.cli.QuboCli --model <path>.use --cmd <path>.cmd --config <path>/qubo_config.json --out <path>/qubo.json` (build jar via `mvn clean package` in `tools/use2qubo/` first if `target/` is stale).
- Annealer: `python anneal_qubo.py <path-to-qubo.json> --reads 1000 --seed 42 --out-name <distinct-name>.json` from `experiments/` — must pass `--out-name` or it silently overwrites the GarageTrucks default result file. `REQUIRED_FOR_FEASIBILITY` is hardcoded to GarageTrucks stop labels, so for this clique variant, compare the annealer's reported active variables to the known optimum `{v3,v4,v6,v7,v9}` by eye; do not modify the script's feasibility check (out of scope).
- Two pre-existing doc bugs found, unrelated to this ticket but cheap to fix while in this area: (a) `README.md`'s examples table still points at nonexistent `examples/autoquboMaxClique/`, should read `examples/MaxClique/`; (b) `examples/GarageTrucks/export_config_schema.md` documents a `penalty_method` config field (`"verma-lewis"`/`"posiform-negaform"`/`"manual"`) that `QuboConfig.java` does not actually parse — Verma-Lewis is unconditional/hardcoded, no manual alternative is wired up. `articles/qmod_2026/CLAUDE.md`'s Key Claims section currently states the two examples differ by "`penalty_method`", which is inaccurate; fix to say they differ by `decision_var_associations`/`decision_vars`/`objective.expression`/`max_degree`.

## Scope

1. New example directory `examples/MaxCliquePairwise/` (leaves `examples/MaxClique/` untouched, preserving the already-reported degree-9/283-ancilla case-study result):
   - `MaxCliquePairwise.use`: copy of `MaxClique.use`'s `Vertex`/`Solution`/`Edge`/`Contains`/`positiveId`, replacing `cliqueProperty` with the new `associationclass NonAdjacent` and `nonAdjacentPenalty` invariant above.
   - `MaxCliquePairwise.cmd`: same vertex/edge/optimal-clique population as `MaxClique.cmd`, plus 27 `!insert` links for `NonAdjacent` over the complement pairs listed above.
   - `qubo_config.json`: same `decision_var_associations`/`decision_vars`/`objective` as `MaxClique/qubo_config.json`; `max_degree` can be set low (e.g. 4) since no term should need escalation.
2. Build the plugin jar (if `target/use2qubo-1.0.0.jar` is stale) and run `QuboCli` against the new example to produce `qubo.json`. Record `nVars`, `polyDegree`, `nAncillaVars`, `exact`, and the logged Verma-Lewis `B`.
3. Hand-verify the true optimal clique `{v3,v4,v6,v7,v9}` still evaluates to the expected minimal energy in the new export before citing the numbers anywhere.
4. Run `anneal_qubo.py` on the new export (1000 reads, seed 42, `--out-name maxclique_pairwise_annealing_result.json`), matching the existing protocol in `sections/09-experiment.tex`; compare the best sample to the known optimum.
5. Paper edits:
   - `sections/06-discussion.tex`: replace the `\Rtwo{}` block at line 27 with the three-way comparison: Lucas (2014) hand-crafted (degree 2, 0 ancillas, analytically-bounded λ) vs. current `MaxClique` `context Vertex` phrasing (degree 9, 283 ancillas, auto λ=2.0) vs. new `MaxCliquePairwise` phrasing (numbers from step 2, expected ≈ degree 2, 0/near-0 ancillas). State plainly that the gap traces to invariant phrasing, not the automated pipeline itself.
   - `sections/09-experiment.tex`: answer the `\Rtwo{}` block at line 6 by adding a short new subsection (same style/protocol as the existing waste collection and max clique simulation subsections) reporting the new annealing run. Explicitly scope out, as future work and say why, the parts of R2's ask not covered: a full "MiniZinc + Ocean/PyQUBO" combined-pipeline baseline, and explicit usability/user-effort metrics (lines of config, time-to-first-QUBO) at operationally realistic sizes.
   - `sections/05-casestudy.tex` §5.2 stays as-is (still documents the actual PoC design used); the pairwise variant is an ablation/quantification, not a replacement of the case study.
6. `articles/qmod_2026/CLAUDE.md`: add the new `MaxCliquePairwise` numbers to Scope Limits once verified; fix the two doc bugs noted above (README example path, `penalty_method` Key Claims line).
7. Out of scope: any other `\Rtwo{}`/R1/R3 comments; generalising `anneal_qubo.py`'s feasibility check; touching `examples/MaxClique/` itself.

## Files Changed

| File | Change |
|---|---|
| `examples/MaxCliquePairwise/MaxCliquePairwise.use` | new |
| `examples/MaxCliquePairwise/MaxCliquePairwise.cmd` | new |
| `examples/MaxCliquePairwise/qubo_config.json` | new |
| `examples/MaxCliquePairwise/qubo.json` | new, generated |
| `experiments/results/maxclique_pairwise_annealing_result.json` | new, generated |
| `sections/06-discussion.tex` | replace `\Rtwo{}` at line 27 with quantified comparison |
| `sections/09-experiment.tex` | replace/answer `\Rtwo{}` at line 6; add new subsection |
| `articles/qmod_2026/CLAUDE.md` | add verified numbers; fix `penalty_method`/README doc bugs |
| `tools/use2qubo/README.md` | fix stale `examples/autoquboMaxClique/` path |

## Acceptance criteria / Verification

- New `qubo.json` inspected directly for `exact`, `nVars`, `polyDegree`, `nAncillaVars`, penalty weight.
- True-optimum energy hand-verified against the new export.
- Annealer run completes and its best sample is compared to the known optimum.
- `examples/MaxClique/qubo.json` unchanged (no regression to the existing reported case-study numbers).
- Paper rebuilds (`main.pdf`) with no new LaTeX errors/overfull hboxes.
- New sentences follow `CLAUDE.md` style rules (no em dashes, one sentence per line, British spelling, `\ac{}` macros).

## Commit

Once done, `git commit` the changes with a commit message of 15 words max.
