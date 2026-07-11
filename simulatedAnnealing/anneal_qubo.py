"""Solve a USE2QUBO-exported qubo.json with classical simulated annealing.

Usage:
    python anneal_qubo.py [path/to/qubo.json] [--reads N] [--seed S]

Loads the plugin's qubo.json (nVars/linear/quadratic/constant/varLabels),
builds a dimod BQM, samples it with neal's SimulatedAnnealingSampler, decodes
the best sample back into variable labels, and writes a result JSON next to
this script under results/.
"""
import argparse
import json
import statistics
from pathlib import Path

import dimod
import neal

DEFAULT_QUBO = (
    Path(__file__).parent.parent
    / "tools" / "use2qubo" / "examples" / "GarageTrucks" / "qubo.json"
)

REQUIRED_FOR_FEASIBILITY = {
    "RouteRoad(route1,Road1)",
    "RouteRoad(route1,Road2)",
    "RouteRoad(route1,Road3)",
    "RouteRoad(route1,Road4)",
    "RouteRoad(route1,Road5)",
}


def load_bqm(qubo_path: Path):
    data = json.loads(qubo_path.read_text())
    q = {}
    for i_str, coeff in data["linear"].items():
        i = int(i_str)
        q[(i, i)] = coeff
    for key, coeff in data["quadratic"].items():
        i_str, j_str = key.split(",")
        q[(int(i_str), int(j_str))] = coeff
    bqm = dimod.BQM.from_qubo(q, offset=data["constant"])
    return bqm, data


def decode_sample(sample, var_labels):
    return {var_labels[i]: int(sample[i]) for i in range(len(var_labels))}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("qubo_path", nargs="?", default=str(DEFAULT_QUBO))
    parser.add_argument("--reads", type=int, default=1000)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument(
        "--out-name",
        default="garagetrucks_annealing_result.json",
        help="Output filename, written under results/ (default: %(default)s)",
    )
    args = parser.parse_args()

    qubo_path = Path(args.qubo_path)
    bqm, data = load_bqm(qubo_path)

    sampler = neal.SimulatedAnnealingSampler()
    sampleset = sampler.sample(bqm, num_reads=args.reads, seed=args.seed)

    best = sampleset.first
    best_labels = decode_sample(best.sample, data["varLabels"])
    active = {label for label, bit in best_labels.items() if bit == 1}

    energies = [rec.energy for rec in sampleset.data(["energy"])]
    energy_stats = {
        "min": min(energies),
        "max": max(energies),
        "mean": statistics.mean(energies),
    }

    is_feasible = REQUIRED_FOR_FEASIBILITY.issubset(active)

    result = {
        "qubo_path": str(qubo_path),
        "num_reads": args.reads,
        "seed": args.seed,
        "best_energy": best.energy,
        "best_sample": best_labels,
        "active_labels": sorted(active),
        "energy_stats": energy_stats,
        "qubo_exact": data.get("exact"),
        "is_feasible": is_feasible,
    }

    out_dir = Path(__file__).parent / "results"
    out_dir.mkdir(exist_ok=True)
    out_path = out_dir / args.out_name
    out_path.write_text(json.dumps(result, indent=2))

    print(f"Best energy: {best.energy}")
    print(f"Active variables: {sorted(active)}")
    print(f"qubo.json exact flag: {data.get('exact')}")
    print(f"Feasible (depot+disposal+all bin stops selected): {is_feasible}")
    print(f"Energy stats over {args.reads} reads: {energy_stats}")
    print(f"Result written to: {out_path}")


if __name__ == "__main__":
    main()
