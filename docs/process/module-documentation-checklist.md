# Module Documentation Drift Checklist

Use this checklist whenever a module is added, renamed, moved, removed, split,
or promoted to a different repository. The goal is to keep Gradle registration,
README catalogs, CI coverage, examples, and release metadata synchronized in the
same change.

## When To Use It

- New module or demo directory.
- Module rename or path move.
- Module removal or external repository split.
- Module family change, such as adding a backend-specific variant.
- Public README, BOM, or CI behavior changes caused by module registration.

## Required Updates

### Gradle Registration

- Update `settings.gradle.kts` or the relevant include helper when automatic
  registration does not cover the module.
- Confirm the Gradle path and published artifact name match the directory.
- Check BOM/catalog aggregation if the module should be published.

### README Locale Set

- Add or update the module `README.md`.
- Add or update the matching `README.ko.md` when the module has a Korean README
  pair, or create both files for new bluetape4k modules.
- Keep the language switch directly below the title in both files.
- Keep architecture, features, usage, configuration, dependency, and benchmark
  sections synchronized when both locales exist.
- Update root `README.md` and `README.ko.md` module catalogs and diagrams when
  the module affects the public catalog.

### Repo And Agent References

- Update repo-local `AGENTS.md` if the module group list or workflow rule
  changes.
- Update `.codex/references/module-groups.md` when the module group map changes.
- Add or update `docs/lessons/YYYY-MM-DD-{slug}.md` with context, decision,
  outcome, verification, and future guidance.

### CI, Nightly, And Examples

- Update `.github/workflows/ci.yml` path filters, changed-module mapping, or test
  jobs when the module must run in PR CI.
- Update Nightly or examples workflows when Testcontainers, external services,
  or long-running examples are involved.
- Update summary jobs and `needs` lists when new jobs are added.
- Keep container-backed verification in one sequential lane when required.

### Release And Dependency Metadata

- Check `bluetape4k-bom` or publication aggregation for publishable modules.
- Check dependency catalog aliases if the module introduces or moves centrally
  governed dependencies.
- Update release notes, changelog, or migration notes only when the module
  change is user-visible.

### Manual Publication Contract

- Keep `docs/manual` repository links relative so the authoring source remains
  reviewable in the same change as the implementation.
- Run the release contract validator to prove that every relative source and
  test target exists at the documented release commit.
- The validator derives snapshot-only manuals from manifest modules whose
  `sourceDir` is absent from that release. Do not add manual skip lists or
  weaken link checks for modules that exist in the release tree.
- Require the public-site snapshot publisher to rewrite repository-relative
  links to the immutable release tag or commit. Verify representative generated
  URLs before deployment; a link to `develop` is not release provenance.
- Use only the central `bluetape4k-dependencies` BOM in generated consumer
  examples. Repository-local BOM coordinates are implementation metadata, not
  the version users should select.

## Validation Commands

Run the smallest set that proves the checklist for the change:

```bash
./gradlew projects --no-configuration-cache
./gradlew exportManualModuleInventory --no-configuration-cache
ruby scripts/manual/validate_manuals.rb
ruby scripts/manual/export_manifest.rb --check
ruby scripts/manual/validate_release_manuals.rb 1.11.0 6187173b58e8b4c5c435c145e00e94708f31ef75
git diff --check
rg -n "<old-module-name>|<old-artifact-name>|<old-path>"
rg -n "<new-module-name>|<new-artifact-name>" README.md README.ko.md .codex/references/module-groups.md AGENTS.md
```

For compile/test evidence, prefer affected module tasks:

```bash
./gradlew :<module>:compileKotlin :<module>:compileTestKotlin --no-configuration-cache
./gradlew :<module>:test --no-configuration-cache
```

For README link/path checks, verify touched relative links directly:

```bash
test -f <path-from-link-target>
test -d <module-directory>
```

## Expected Evidence In PRs

- Gradle project path appears in `./gradlew projects`.
- Root README locale pair lists or intentionally omits the module.
- Module README locale pair exists and stays synchronized.
- CI/Nightly/example workflow impact is listed, even when no change is needed.
- BOM/catalog impact is listed, even when automatic aggregation covers it.
- Generated consumer examples use `bluetape4k-dependencies`, and published
  source links resolve to the documented release rather than `develop`.
- Stale old module names are absent or explicitly marked historical.

## Common Skip Reasons

- Documentation-only wording change with no module lifecycle impact.
- Historical plan/spec files under `docs/superpowers` that intentionally record
  old module paths.
- Archived security review evidence under `docs/security-review`.
