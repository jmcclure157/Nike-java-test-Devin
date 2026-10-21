# Devin prompt: Cerberus Java client, Java 8 → Java 21 migration

Copy everything between the two `---8<---` lines into a fresh Devin session.

---8<---

Migrate this repository (Nike's Cerberus Java client, `com.nike:cerberus-client`) from Java 8 to Java 21.

## Objective

- Build and run on **Java 21** (compile with `--release 21`, CI on JDK 21).
- Move `javax.*` → `jakarta.*` **where applicable**. Only Java EE / Jakarta EE APIs moved to `jakarta.*`. JDK packages such as `javax.net.ssl`, `javax.crypto` and `javax.security` are part of Java itself and must stay `javax.*`. Audit every `javax` import and record the result in MIGRATION.md, even if nothing needs renaming.
- Upgrade dependencies (OkHttp, Jackson, AWS SDK, Gson, test libraries, build plugins, etc.) to current versions that work on Java 21 and are jakarta-compatible.
- Keep going until the full build passes on JDK 21. This repo uses **Gradle** (wrapper 4.8.1), not Maven, so the bar is `./gradlew build`, Gradle's equivalent of `mvn verify`: compile, unit tests, static analysis and packaging. Upgrade the Gradle wrapper as needed. Do not switch build tools.

## HOW TO WORK (IMPORTANT)

- Work completely on your own from start to finish. Do not ask me questions and do not wait for my approval at any point, including plan approval.
- When something is unclear, make the reasonable, industry-standard choice, write it down under "Assumptions" in the PR description, and keep going.
- If a step is blocked, document why, choose the safest workaround, and continue. Never stop and wait for me.

## Constraints

- Small, reviewable commits, one logical change each, in this order: build/dependency updates (the Gradle files here play the role of a `pom.xml`), then `javax` → `jakarta` renames (if any apply), then API and test fixes. Each commit message says what changed and why.
- No refactoring beyond what the migration requires. No renames, reformatting or "while I'm here" cleanups. Do not change existing test assertions. If a test has to be rewritten (for example, because its mocking library does not run on Java 21), keep the same scenarios and the same assertions.
- No committed secrets, tokens or credentials.
- Tests that cannot run without a live Cerberus backend or AWS credentials (for example `src/integration/java`) are documented in MIGRATION.md, with what they need and how to run them. Do not silently delete them, skip them or stub them out.

## Definition of done

1. `./gradlew build` is green on JDK 21. Paste the final summary line in the PR.
2. MIGRATION.md at the repo root with:
   - a before/after version table (Java, Gradle, every dependency and plugin you changed);
   - the `javax` → `jakarta` audit result;
   - every decision and why, including anything left undone;
   - how to run the tests that need live infrastructure.
3. A PR whose description has an **"Assumptions"** section listing every judgment call.
4. A clean commit history that an architect can review one diff at a time, in the order the work was done (the Cognizant/Odyssey architect-review model).

## Demo considerations

- Aim to finish within about **1 hour**.
- If the full migration will not fit, do these first: (1) Java 21 compilation, (2) the `javax` → `jakarta` audit and renames. Then document what is left (remaining dependency bumps, test fixes, static-analysis findings) in MIGRATION.md under "Remaining work", open the PR anyway, and say in the PR which parts are done.

---8<---

## Usage notes (for the presenter, not part of the prompt)

- **Launch:** open a fresh Devin session pointed at this repo, paste the prompt once, then narrate while it works (plan, Gradle upgrade, dependency bumps, test fixes, MIGRATION.md, PR). Don't type anything else into the session unless you mean to steer it.
- **Fallback:** keep the pre-completed dry-run branch (and its PR) open before the demo starts. If the live run stalls or runs long, switch to it and walk through it commit by commit. Don't merge that PR before the demo: the live session needs `master` still on Java 8.
- **Autonomy check:** make sure your session settings don't force a plan-approval step (anything that makes Devin wait for you to approve its plan before it starts). Do a dry run with this exact prompt a day ahead and confirm Devin starts working on its own without waiting for you.
