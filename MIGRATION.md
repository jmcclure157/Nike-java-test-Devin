# Java 8 → Java 21 migration

Result: `./gradlew build` (compile, unit tests, PMD, SpotBugs, JaCoCo, jar) passes on **JDK 21**, compiling with `--release 21`. CI (`.github/workflows/build.yml`) now runs on Corretto 21.

```bash
export JAVA_HOME=/path/to/jdk-21
./gradlew clean build
```

## Build system: Gradle, not Maven

The repo has never had a `pom.xml`; it builds with Gradle. The migration keeps Gradle and treats `./gradlew build` as the equivalent of `mvn verify`. Converting to Maven would add a build-tool rewrite on top of the Java upgrade, which is out of scope.

## Before / after

### Toolchain and build plugins

| Item | Before | After | Why |
|---|---|---|---|
| Java (compile target) | 1.8 (`sourceCompatibility`) | 21 (`options.release = 21`) | Migration goal. `--release` also checks API usage against the Java 21 platform. |
| CI JDK | Corretto 8 | Corretto 21 | Same. |
| Gradle wrapper | 4.8.1 | 8.14.3 | Gradle < 8.5 cannot run on Java 21. |
| Plugin repository | `jcenter()` | `mavenCentral()` + `gradlePluginPortal()` | JCenter is shut down; the old build could not resolve its plugins. |
| Shadow plugin | `com.github.jengelman.gradle.plugins:shadow` 4.0.3 | `com.gradleup.shadow` 8.3.9 | Old coordinates are dead; the project moved to GradleUp. Same fat-jar behavior (`archiveClassifier = ''`). |
| `maven` plugin | applied | removed | Removed in Gradle 7. `maven-publish` was already applied and is kept. |
| FindBugs | Gradle `findbugs` plugin (3.0.1) | SpotBugs plugin 6.5.13 (SpotBugs 4.9) | FindBugs plugin was removed in Gradle 6. SpotBugs is its maintained successor; same `codequality/findbugs.xml` filter, same scope (main only). |
| PMD | 6.21.0, `rulesets/java/basic.xml` | 7.28.0, `codequality/pmd-ruleset.xml` | PMD 6 cannot parse Java 21 class files. PMD 7 removed `basic.xml`; the new ruleset lists the same rules under their PMD 7 category names. |
| Coverage | Cobertura plugin 2.6.1 + Coveralls plugin 2.6.3 | JaCoCo (built into Gradle) | Cobertura cannot instrument Java 21 bytecode. CI already uploads to Codecov; the report path now points at the JaCoCo XML. |
| OWASP dependency-check plugin | 5.3.2 (wired into `check`) | 10.0.3 (run on demand) | See "Decisions" below. |

### Runtime dependencies

| Dependency | Before | After |
|---|---|---|
| OkHttp | 4.9.3 | 4.12.0 |
| Jackson (forced) | 2.13.3 (databind only) | 2.19.4 (core, databind, annotations) |
| AWS SDK for Java v1 (core, sts) | `1.11.+` (dynamic) | 1.12.797 (pinned) |
| Gson | 2.5 | 2.13.2 |
| commons-lang3 | 3.4 | 3.20.0 |
| slf4j-api | 1.7.25 | 1.7.36 |
| joda-time (shadow) | 2.8.1 | 2.14.4 |
| resilience4j-all | 1.7.1 | 1.7.1 (+ explicit `resilience4j-retry` 1.7.1) |

### Test dependencies

| Dependency | Before | After |
|---|---|---|
| JUnit | 4.12 | 4.13.2 |
| Mockito | 1.10.19 | 5.20.0 |
| PowerMock (api-mockito, module-junit4) | 1.6.4 | removed |
| System Stubs (junit4) | – | 2.1.8 (new) |
| AssertJ | 2.3.0 | 3.27.6 |
| commons-io | 2.4 | 2.20.0 |
| junit-dataprovider | 1.10.0 | 1.13.1 |
| slf4j-simple | 1.7.25 | 1.7.36 |
| com.fieldju:commons (integration tests) | 1.1.0 | removed (local helper) |

## javax → jakarta audit

Every `javax.*` import in `src/` was checked:

| Import | Files | Action |
|---|---|---|
| `javax.net.ssl.SSLException` | `CerberusClient` | none |
| `javax.net.ssl.SSLSocketFactory`, `javax.net.ssl.X509TrustManager` | `DefaultCerberusClientFactory`, `DefaultCerberusClientFactoryTest` | none |
| `javax.net.ssl.TrustManagerFactory` | `DefaultCerberusClientFactoryTest` | none |

`javax.net.ssl` is part of the JDK (`java.base`), not Java EE, so it never moved to `jakarta.*`. The library uses no Java EE / Jakarta EE APIs (no servlet, JAX-RS, JAXB, `javax.annotation`, `javax.inject`, validation, etc.), so **no `jakarta` renames apply**. None of the upgraded dependencies pulls in `javax.*`/`jakarta.*` EE APIs either: OkHttp, Jackson core/databind, Gson and AWS SDK v1 are namespace-neutral.

## Decisions

1. **Keep Gradle; `./gradlew build` is the gate.** See above.
2. **Bump within the current major line where the next major is a rewrite.** OkHttp stays on 4.x (5.x changes artifacts/Kotlin multiplatform), AWS SDK stays on v1 1.12.x (v2 is a different API, `software.amazon.awssdk`), slf4j stays on the 1.7.x API (2.x changes the binding mechanism for consumers), resilience4j stays on 1.7.1 (works on Java 21; 2.x has API changes). Moving to those majors is an API migration, not a Java upgrade.
3. **Pin the AWS SDK version.** `1.11.+` resolves to whatever is newest at build time; a fixed version makes builds reproducible.
4. **`resilience4j-retry` declared explicitly.** `CerberusClient` imports `io.github.resilience4j.retry.*` directly. With Gradle 4's `compile`, it came in transitively through `resilience4j-all`; with Gradle 8's `implementation` it is no longer on the compile classpath, so it is declared.
5. **PowerMock replaced, not patched.** PowerMock 1.6.4 and its repackaged CGLIB fail on Java 17+ (`InaccessibleObjectException`), and PowerMock is unmaintained. Adding `--add-opens` flags would only hide the problem. Changes:
   - `System.getenv` / `System.getProperty` static mocks → System Stubs JUnit 4 rules (`EnvironmentVariablesRule`, `SystemPropertiesRule`), which set the real variable/property for one test and restore it afterwards.
   - `@RunWith(PowerMockRunner.class)` removed where it was only a runner; `PowerMockito.mock/when` → `Mockito.mock/when`; `org.mockito.Matchers` → `org.mockito.ArgumentMatchers`.
   - `mock(String.class)` in `StsCerberusCredentialsProviderTest` → `""`. Mockito 5 refuses to mock `String`; the old mock behaved as a blank string, which `get_token_throws_exception_when_url_is_blank` relies on.
   - Same scenarios and assertions. 75 unit tests before, 75 after, 0 skipped.
6. **SpotBugs 4.x findings excluded in the existing filter.** SpotBugs 4.9 reports 47 findings in code that FindBugs 3.0.1 accepted: `EI_EXPOSE_REP` / `EI_EXPOSE_REP2` (getters/setters returning mutable lists, maps and dates), `CT_CONSTRUCTOR_THROW` (constructors that validate arguments), and `SS_SHOULD_BE_STATIC` on `BaseAwsCredentialsProvider.paddingTimeInSeconds` (a protected field subclasses use). Fixing them changes the public API or behavior, so they are excluded in `codequality/findbugs.xml` and listed under remaining work. All other detectors still fail the build.
7. **OWASP dependency-check no longer runs inside `check`.** Plugin 10.x downloads the full NVD database through the NVD API, which needs an API key (a secret, not committed) and can take a long time without one. It is still configured (`failBuildOnCVSS = 11`, i.e. report-only as before) and runs with `./gradlew dependencyCheckAnalyze` (set `nvd.apiKey` from an environment variable in CI if needed).
8. **Coveralls plugin removed.** It is incompatible with Gradle 8 and CI does not use it; CI publishes coverage to Codecov.
9. **`com.fieldju:commons` replaced by a local helper.** It was only published to JCenter. The integration tests used one method, `EnvUtils.getRequiredEnv`; it is now a package-private class in `src/integration/java` with the same contract.

## Tests that need a live Cerberus backend or AWS credentials

`src/integration/java` is **not** part of `./gradlew build` (same as before the migration). It compiles as part of the build, so it stays in sync with the code, but running it needs real infrastructure:

| Test class | What it does | Needs |
|---|---|---|
| `com.nike.cerberus.client.auth.aws.CerberusClientTest` | Authenticates via STS, then writes, reads, lists and deletes secrets and files in a real safe deposit box | Running Cerberus, AWS credentials for an IAM principal with SDB access |
| `com.nike.cerberus.client.auth.aws.StsAuthCerberusClientTest` | Authenticates against Cerberus with signed STS headers and checks a token comes back | Running Cerberus, AWS credentials |

Run them with:

```bash
export TEST_REGION=us-west-2              # region of the Cerberus environment
export CERBERUS_ADDR=https://cerberus.example.com
# AWS credentials from the default provider chain (env vars, ~/.aws, instance role, ...)
./gradlew integration
```

They were **not run** during this migration (no Cerberus environment or AWS account available). Nothing in them was deleted, skipped or stubbed; the only change is the `EnvUtils` import (decision 9).

## Remaining work (out of scope for this migration)

- Run `./gradlew integration` against a real Cerberus environment on JDK 21.
- Run `./gradlew dependencyCheckAnalyze` with an NVD API key and triage any CVEs.
- Decide on the 47 SpotBugs 4.x findings (defensive copies in model classes, etc.) and remove the exclusions.
- Optional major upgrades: OkHttp 5, AWS SDK v2, slf4j 2, resilience4j 2, JUnit 5.
