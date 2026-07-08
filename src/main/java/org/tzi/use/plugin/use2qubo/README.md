# `use2qubo` (root package)

Plugin entry point. Holds `Use2QuboPlugin`, the `IPlugin` USE calls on startup so the
plugin registers cleanly (no diagram-manipulator NPE) even though it contributes no
diagram extensions — just two menu actions (see [`action/`](action/README.md)).

All real work lives in the sibling packages:

```mermaid
flowchart LR
    root["use2qubo (root)\nUse2QuboPlugin"] --> action["action/\nmenu actions"]
    action --> qubo["qubo/\nderivation engine"]
    action --> ui["ui/\nSwing views"]
    cli["cli/\nQuboCli (headless)"] --> qubo
    ui --> uitabs["ui/tabs/\nresult tab panels"]
    qubo --> util["util/\nlogging, constants, JSON"]
    ui --> util
```

See [`qubo/README.md`](qubo/README.md) for the actual derive-QUBO pipeline.
