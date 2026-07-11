# Experiments

Classical simulated-annealing sanity checks against `qubo.json` files exported by the USE2QUBO plugin (`../tools/use2qubo/`).

## `anneal_qubo.py`

Loads a plugin-exported `qubo.json` (`nVars`/`linear`/`quadratic`/`constant`/`varLabels`), builds a `dimod` BQM, samples it with `neal`'s `SimulatedAnnealingSampler`, decodes the best sample back into variable labels, and writes a result JSON to `results/garagetrucks_annealing_result.json`.

### Setup

```bash
pip install -r requirements.txt
```

### Run

Default run (uses the GarageTrucks example's `qubo.json`, 1000 reads, seed 42):

```bash
python anneal_qubo.py
```

Point at a different `qubo.json` (e.g. the MaxClique example):

```bash
python anneal_qubo.py ../tools/use2qubo/examples/MaxClique/qubo.json
```

Custom read count and seed:

```bash
python anneal_qubo.py --reads 5000 --seed 7
```

Combine an explicit path with custom options:

```bash
python anneal_qubo.py ../tools/use2qubo/examples/GarageTrucks/qubo.json --reads 2000 --seed 42
```

Name the output file (avoids overwriting a previous run's result):

```bash
python anneal_qubo.py ../tools/use2qubo/examples/MaxClique/qubo.json --out-name maxclique_annealing_result.json
```

### Output

Console: best energy, active (selected) decision-variable labels, the `qubo.json` `exact` flag, a feasibility check (whether `RouteStop(route1,depot)`, `RouteStop(route1,disposal)`, and all bin-node stops for `route1` are selected), and energy stats (min/max/mean) over all reads.

File: `results/<--out-name>` (default `garagetrucks_annealing_result.json`). Without `--out-name`, reruns against a different `qubo.json` overwrite the default file.

### Known limitation

`REQUIRED_FOR_FEASIBILITY` in the script is hardcoded to the GarageTrucks example's `route1` stop labels (`n2`, `n3`, `n4`, depot, disposal). Running against `MaxClique/qubo.json` or the new `GarageTrucksV2` scenario will report `is_feasible: false` regardless of the actual sample, since those labels won't match — feasibility checking has not been generalised.
