# Pull Request

## Summary

<!-- One sentence. What does this PR change and why is it needed? -->

## Type of change

- [ ] Bug fix (non-breaking)
- [ ] New feature (non-breaking, additive)
- [ ] Breaking change (requires major version bump)
- [ ] Docs / tests only
- [ ] Dependency update

## Checklist

- [ ] `./gradlew :sdk:build` is clean (includes `jacocoTestCoverageVerification` ≥ 85 % line gate).
- [ ] I updated `CHANGELOG.md` under `Unreleased` if this is a user-visible change.
- [ ] If the auth test vector was touched, the regression still matches the Python and .NET SDKs (ARCHITECTURE.md §3).
- [ ] Dependencies added via `gradle/libs.versions.toml` (no ad-hoc `implementation("group:name:version")`).
- [ ] No JCenter references (`mavenCentral()` only).
- [ ] I did NOT commit credentials, tokens or sandbox payloads (`*.webhook.json` is gitignored).

## Cross-SDK parity

- [ ] This change is Java-only.
- [ ] This change should also be mirrored in `checkout-python` and `checkout-dotnet`.
       <!-- Link sibling PRs or open issues: -->

## Testing

<!--
How did you verify this works? Include:
  - the command you ran
  - screenshots / log snippets if UI / behavior changed
  - sandbox output from the `SandboxSmokeTest` if you ran it against a real gateway
-->
