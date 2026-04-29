# @CacheAwareSpringBootTest

A drop-in replacement for `@SpringBootTest` that adds observability on Spring `ApplicationContext` cache
behavior during your integration test suite.

It hooks into the Spring Test lifecycle and classifies every context load event as one of:

| Event | Meaning |
|-------|---------|
| **BUILD** | The first `ApplicationContext` created in the entire build |
| **REBUILD** | A context that was discarded and rebuilt — a different test configuration prevented reuse |
| **REUSE** | A cache hit; an existing context was returned without rebuilding |

At the end of your test suite, the **Global Test Execution Analyzer** (`GlobalTestExecutionAnalyzer`)
logs a summary automatically — no manual setup required.

---

## Install

[![Maven Central](https://img.shields.io/maven-central/v/dev.silentcraft.tools/spring-test-context-cache-metrics.svg)](https://search.maven.org/artifact/dev.silentcraft.tools/spring-test-context-cache-metrics)

Add the dependency with `test` scope:

=== "Maven"

    ```xml
    <dependency>
        <groupId>dev.silentcraft.tools</groupId>
        <artifactId>spring-test-context-cache-metrics</artifactId>
        <version>0.1.0-alpha</version>
        <scope>test</scope>
    </dependency>
    ```

=== "Gradle"

    ```groovy
    testImplementation 'dev.silentcraft.tools:spring-test-context-cache-metrics:0.1.0-alpha'
    ```

!!! note "SLF4J"
    This library uses the SLF4J API for logging. You must provide an SLF4J-compatible implementation
    (e.g. `logback-classic`) in your project to see the output.

---

## Usage

Replace `@SpringBootTest` with `@CacheAwareSpringBootTest`:

```java title="Use @CacheAwareSpringBootTest" linenums="1" hl_lines="1"
@CacheAwareSpringBootTest
class MyServiceIntegrationTest {
    // your tests
}
```

All `@SpringBootTest` attributes are supported — `classes`, `properties`, `args`, `webEnvironment`, `useMainMethod`:

```java linenums="1" hl_lines="1-5"
@CacheAwareSpringBootTest(
        classes = MyApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "feature.flag=true"
)
class MyServiceIntegrationTest {
    // your tests
}
```

The annotation is `@Inherited`, so you can annotate an abstract base class:

```java linenums="1" hl_lines="3"
@CacheAwareSpringBootTest
@ActiveProfiles("integration")
abstract class AbstractIntegrationTest { }

class MyServiceTest extends AbstractIntegrationTest { }
```

---

## Sample output

**All test classes share the same `ApplicationContext`:**

``` shell
[OCC] Perfect! No cache misses detected, all your tests share the same configuration.
```

**At least one context rebuild detected:**

``` shell
[OCC] Cache Miss Analysis:
[OCC] Total cache misses detected: 2 - following the 5 most impactful test classes
[OCC] /!\ UserServiceIntegrationTest - Could not reuse cached application context
[OCC]      This class profiles: [test, integration]
[OCC] /!\ OrderServiceIntegrationTest - Could not reuse cached application context
[OCC]      This class profiles: [test]
[OCC]      Cached context profiles : [test, integration]
```

The last line identifies the class that established the initial `ApplicationContext` and its active profiles.
Use it as a reference to align other test classes and eliminate unnecessary rebuilds.

---

## What causes a REBUILD?

Spring creates a new `ApplicationContext` whenever the `MergedContextConfiguration` differs from what is
already cached. Common causes include mismatched active profiles, different `@MockBean` / `@SpyBean`
declarations, `@DirtiesContext`, or different configuration classes.

See [**Understanding Rebuilds**](rebuilds.md) for a full cause list with DO/DON'T patterns.

---

## Requirements

- Spring Boot **3.5.x**
- JUnit **5** (`junit-jupiter`)
- Java **17+**

!!! note "JVM forking and report scope"
    `@CacheAwareSpringBootTest` tracks context cache events in a single static registry per JVM process.
    When all tests run in the same JVM — the default Surefire configuration — the report covers the
    entire test suite.

    If you intentionally configure Surefire to fork a new JVM per test class (`reuseForks=false`),
    each process produces its own independent report. This is a valid choice — just be aware that
    cross-class cache analysis is scoped to each fork, not the full suite.

    If a single global report is what you need, you still have the option to enforce fork reuse:
    ```xml
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
            <forkCount>1</forkCount>
            <reuseForks>true</reuseForks>
        </configuration>
    </plugin>
    ```
