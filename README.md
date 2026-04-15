# CacheAwareSpringBootTest - Global Test Execution Analyzer

[![Maven Central](https://img.shields.io/maven-central/v/dev.silentcraft.tools/spring-test-context-cache-metrics.svg)](https://search.maven.org/artifact/dev.silentcraft.tools/spring-test-context-cache-metrics)

## Overview

`CacheAwareSpringBootTest` is an annotation-based testing extension that replaces `@SpringBootTest` to add
observability on Spring `ApplicationContext` cache behavior during test execution.

It hooks into the Spring Test lifecycle and tracks every context load event across your test suite, classifying each as:

- **BUILD** — the first `ApplicationContext` created in the entire build execution
- **REBUILD** — a context that was discarded and rebuilt because a different test configuration prevented reuse
- **REUSE** — a cache hit; an existing context was returned without rebuilding

The core reporting component is the **Global Test Execution Analyzer** (`GlobalTestExecutionAnalyzer`), which runs
automatically at the end of your test suite and logs a cache usage summary — no manual setup required.

---

## Important Usage Notes and Dependencies

- **Logging:**
  This library uses the [SLF4J API](https://www.slf4j.org/) for logging. **You must provide an SLF4J-compatible
  logging implementation in your project** (e.g. `logback-classic`) to see the output. The library does not
  bundle one to avoid conflicts.

- **Dependency Scope:**
  This library is for testing only. Declare it with `scope test` in Maven or Gradle to avoid polluting your
  production classpath.

- **Spring Dependencies:**
  The library depends on the following Spring modules (all marked `optional` to avoid transitive version conflicts):
  - `spring-boot-autoconfigure`
  - `spring-test`
  - `spring-context`
  - `spring-boot-test`

- **Spring Boot Version Compatibility:**
  Built and tested against **Spring Boot 3.5.10**. Compatibility with earlier versions is not guaranteed.

- **JUnit Version Compatibility:**
  Requires **JUnit 5** (`junit-jupiter`). JUnit 4 is not supported.
  JUnit dependencies used internally:
  - `junit-jupiter-api`
  - `junit-jupiter-params`
  - `junit-platform-commons`
  - `junit-platform-engine`
  - `junit-platform-launcher`

---

## Usage

Add the dependency to your `pom.xml` with `test` scope:

```xml
<dependency>
    <groupId>dev.silentcraft.tools</groupId>
    <artifactId>spring-test-context-cache-metrics</artifactId>
    <version>0.1.0-alpha</version>
    <scope>test</scope>
</dependency>
```

Replace `@SpringBootTest` with `@CacheAwareSpringBootTest` on your integration test classes:

```java
@CacheAwareSpringBootTest(classes = MyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MyServiceIntegrationTest {
    // your tests
}
```

All attributes supported by `@SpringBootTest` are available, including `properties`, `args`, and `useMainMethod`:

```java
@CacheAwareSpringBootTest(
        classes = MyApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "feature.flag=true",
        args = "--spring.profiles.active=ci",
        useMainMethod = SpringBootTest.UseMainMethod.ALWAYS
)
public class MyServiceIntegrationTest {
    // your tests
}
```

The annotation is `@Inherited`, so annotating an abstract base class is supported:

```java
@CacheAwareSpringBootTest
@ActiveProfiles("integration")
abstract class AbstractIntegrationTest {
}

class MyServiceTest extends AbstractIntegrationTest {
    // inherits @CacheAwareSpringBootTest
}
```

No further configuration is needed. `GlobalTestExecutionAnalyzer` will automatically run at the end of your test suite.

---

## What it reports

At the end of the test suite, the analyzer logs one of two outcomes:

**Perfect** — every test class reused the same `ApplicationContext`:
```
[OCC] Perfect! No cache misses detected, all your tests share the same configuration.
```

**Rebuild report** — at least one context rebuild was detected:
```
[OCC] Cache Miss Analysis:
[OCC] Total cache misses detected: 2 - following the 5 most impactful test classes
[OCC] /!\ UserServiceIntegrationTest - Could not reuse cached application context
[OCC] UserServiceIntegrationTest - Active profiles [test, integration]
[OCC] /!\ OrderServiceIntegrationTest - Could not reuse cached application context
[OCC] OrderServiceIntegrationTest - Active profiles [test]
[OCC] - Cached application context was based on this (class) & [configuration]
       (AppConfigTest) - {profiles: [test]} - use it to configure test classes
       that could not use cached context.
```

The last line identifies the test class that established the original `ApplicationContext` and its active profiles.
Use it as a reference to align other test classes and eliminate unnecessary rebuilds.

---

## On test isolation and context reuse

Context reuse is a **consequence** of good test isolation — not a goal in itself.

When each test class is fully self-contained (no static mutable state, no cross-test dependencies, consistent
profile and configuration declarations), Spring can safely return a cached `ApplicationContext` to every class
that shares the same configuration. The tool surfaces when that isn't happening so you can investigate whether
it is intentional or a misconfiguration.

**Sharing JVM state between tests masks real cache fragmentation.** If test classes mutate shared static state or
depend on side effects from previous tests, contexts may appear to be reused when the underlying isolation is broken.
Well-isolated tests that happen to share the same configuration will naturally reuse the same `ApplicationContext`.

> **Note:** To benefit from context caching, all tests must run in the same JVM process. When using Maven Surefire,
> ensure `forkCount` is not set to `always` or `pertest`, as forking prevents context reuse across test classes
> and will significantly slow down your build.

---

## Multi-module projects

When all modules run in the same JVM (e.g. Maven Surefire with `forkCount=0`), `GlobalTestExecutionAnalyzer`
produces a **combined report across all modules** at the end of each module's test plan. The registry is never
auto-cleared between modules, so the final report reflects the full picture of cache usage across the entire build.

If you need per-module isolation, call `ContextCacheMetricsRegistry.clear()` at the start of each module via
a custom `TestExecutionListener` registered before this one.

---

## Registry API and IDE re-runs

`ContextCacheMetricsRegistry` exposes a public `clear()` method for explicit control over when the registry resets:

```java
ContextCacheMetricsRegistry.clear();
```

**Known trade-off:** When running tests repeatedly in an IDE that reuses the JVM between runs (IntelliJ, VS Code),
the registry accumulates data across runs. A second run will include events from the first. Call `clear()` before
the test suite starts if single-run isolation is required.

---

## Design considerations

- **Early bootstrap integration:** Context cache monitoring operates at the earliest test bootstrap phase, before
  the Spring `ApplicationContext` is fully initialized.

- **Static listener registration:** Cache event listeners are registered statically to ensure they are in place
  before any context is loaded.

- **Extensibility:** Custom listener registration is possible via `ObservableContextCache#registerListener`, but
  is currently constrained by Spring Test lifecycle timing (see `ContextCacheMissesListener` Javadoc).

- **Future improvements:**
  - Export metrics to CI dashboards and introduce configurable rebuild thresholds
  - Warn when Maven Surefire `forkMode` settings conflict with context caching

---

## Contribution & License

Contributions are welcome via pull requests or issues
on [GitHub](https://github.com/chris-carneiro/spring-test-context-cache-metrics/issues).

This project is licensed under the Apache License Version 2.0 — see the [LICENSE](LICENSE) file for details.
