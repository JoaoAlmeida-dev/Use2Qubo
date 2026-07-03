# Contributing

## Commit convention

PRs targeting `main` must use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

Enforced by `commitlint` on every PR (`.github/workflows/commitlint.yml`). Merges to `main` are analyzed by `semantic-release` (`.github/workflows/release.yml`) to auto-tag a semver release and attach the built jar as a GitHub Release asset.

### Commit message template

One-time per clone, point git at the repo's template so `git commit` (no `-m`) opens pre-filled:

```bash
git config commit.template .gitmessage.txt
```

(`git config` is per-clone, not versioned by git itself — this must be run once after cloning.)

| Type | Effect on version |
|---|---|
| `fix:` | patch bump |
| `feat:` | minor bump |
| `BREAKING CHANGE:` footer (any type) | major bump |
| `docs:`, `chore:`, `refactor:`, `perf:`, `test:`, `build:`, `ci:` | no release |

Examples:
```
feat: added new visualisation window to ui
fix: fixed bug in QuboEnginejava
docs: improved readme.md

BREAKING CHANGE: `exact` field moved from top level to `metadata.exact`.
```

## Releases

Do not hand-edit the version in `pom.xml` — it is set at build time by the release workflow from the semantic-release-computed tag. Local builds (`mvn clean package`) keep using the static `pom.xml` version, which is fine for development.
