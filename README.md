# CacheAwareSpringBootTest - Global Test Execution Analyzer

[![Maven Central](https://img.shields.io/maven-central/v/dev.silentcraft/tools-test-context-cache.svg)](https://search.maven.org/artifact/dev.silentcraft/tools-test-context-cache)

## Overview

`CacheAwareSpringBootTest` is an annotation-based testing extension designed to improve the developer experience
when working with Spring Boot integration tests by providing observability and metrics on Spring `ApplicationContext`
cache behavior.

This project integrates seamlessly with the Spring Test lifecycle to detect and analyze context cache misses during test
suite execution,
allowing you to optimize test startup times and prevent unnecessary context reloading.

The core component for reporting is the **Global Test Execution Analyzer** (`GlobalTestExecutionAnalyzer`), which
automatically listens to your test suite execution and outputs insightful metrics about cache usage without any manual
setup.

---

## Features

- **Automatic context cache miss detection** during Spring Boot test lifecycle
- **Centralized metrics registry** aggregating cache miss data per test class
- **Global analysis and reporting** at the end of the test suite
- **Detailed insights** into which test classes cause cache misses and active Spring profiles
- **Extensible cache observation mechanism** with pluggable listeners (for advanced users)
- **Seamless integration** through a single annotation: `@CacheAwareSpringBootTest`

---

## Usage

Add the dependency to your `pom.xml` or `build.gradle`:

```xml

<dependency>
    <groupId>dev.silentcraft.tools</groupId>
    <artifactId>spring-test-context-cache-metrics</artifactId>
    <version>0.0.1</version>
    <scope>test</scope>
</dependency>
```

Annotate your Spring Boot test classes with @CacheAwareSpringBootTest instead of @SpringBootTest:

@CacheAwareSpringBootTest(classes = MyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MyServiceIntegrationTest {
// Your test cases
}

No further configuration is needed. The GlobalTestExecutionAnalyzer will automatically run at the end of your test suite
and log cache miss summaries.

## What it does

* Tracks every Spring ApplicationContext cache miss triggered during test execution

* Aggregates detailed metadata including test class, active Spring profiles, and configuration classes

* Reports the number and distribution of cache misses across test classes

* Highlights the most frequent Spring profiles used during cache misses

## Sample output

```shell 
  TestPlan Execution finished!
 Cache Miss Analysis:
 UserServiceIntegrationTest - 4 cache misses
 OrderServiceIntegrationTest - 3 cache misses
 Most common profiles: {test=6, integration=3}
```

## Design considerations

* Early Bootstrap Integration: The context cache monitoring operates at the earliest test bootstrap phase, before the
  Spring application context is fully initialized.

* Static Listener Registration: Cache miss listeners are registered statically to ensure reliability during this early
  phase.

* Extensibility: While custom listener registration is possible, it’s currently limited by Spring Test lifecycle
  constraints.

* Future improvements: Possibility to export metrics to CI dashboards, introduce thresholds, and offer optimization
  suggestions.

## Contribution & License

Feel free to contribute via pull requests or open issues on GitHub.

This project is licensed under the Apache License Version 2.0 — see the [LICENSE](LICENSE) file for details.