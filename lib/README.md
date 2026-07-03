# lib/ — USE JAR Dependencies

These JARs are committed to the repo (exception carved out of `.gitignore`'s
`*.jar` rule) so CI/release workflows can build without cloning and building
`useocl/use` from source. Place the following JARs here before running `mvn package`:

| File | Source |
|------|--------|
| `use-core-7.5.0.jar` | Built from the [USE repo](https://github.com/useocl/use) via `mvn package -pl use-core` |
| `use-gui-7.5.0.jar`  | Built from the USE repo via `mvn package -pl use-gui` |

Alternatively, if you have a USE release installed, find them at:
- `<USE_HOME>/lib/use-core-7.5.0.jar`
- `<USE_HOME>/lib/use-gui-7.5.0.jar`

These JARs are compile-time only (`system` scope) and must NOT be bundled into the
plugin JAR — USE provides them at runtime.
