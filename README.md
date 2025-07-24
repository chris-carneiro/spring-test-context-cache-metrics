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

## Important Usage Notes and Dependencies

- **Logging:**  
  This library uses the [SLF4J API](https://www.slf4j.org/) for logging and internally bundles `logback-classic` as the
  default logging implementation.  
  However, **you must explicitly specify the SLF4J logging implementation you want to use in your project** to avoid
  conflicts and ensure compatibility.  
  This design leaves the choice of logging backend fully to the consumer without imposing `logback` at runtime.


- **Dependency Scope:**  
  All dependencies of this library are **meant for testing purposes only**.  
  Therefore, you **must declare this library and its dependencies with scope `test`** in your build tool (Maven/Gradle)
  to avoid polluting your production classpath and to prevent conflicts with your application code.


- **Spring Dependencies:**  
  Internally, this library depends on the following Spring modules:
    - `spring-boot-autoconfigure`
    - `spring-test`
    - `spring-context`
    - `spring-boot-test`

  These dependencies are necessary to hook into Spring's test lifecycle and cache infrastructure.


- **Spring Boot Version Compatibility:**  
  This library is built and tested against **Spring Boot 3.5.3** and **does not guarantee compatibility with earlier
  Spring Boot versions**.  
  Importantly, this dependency is **not transitively exposed** to avoid build conflicts in your projects.


- **JUnit Version Compatibility:**  
  The library depends on **JUnit 5** (`junit-jupiter`) and has only been tested with JUnit 5.  
  Support for earlier JUnit versions (JUnit 4 or older) will be considered for future releases but is not currently
  provided.


- **Junit Dependencies**
- `junit-jupiter-api`
- `junit-platform-commons`
- `junit-platform-engine`
- `junit-platform-launcher`

---

## Usage

Add the dependency to your `pom.xml` or `build.gradle` with `test` scope:

```xml

<dependency>
    <groupId>dev.silentcraft</groupId>
    <artifactId>tools-test-context-cache</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

Annotate your Spring Boot test classes with `@CacheAwareSpringBootTest` instead of `@SpringBootTest`:

```java 

@CacheAwareSpringBootTest(classes = MyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MyServiceIntegrationTest {
// Your test cases
}
```

No further configuration is needed. The `GlobalTestExecutionAnalyzer` will automatically run at the end of your test
suite
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

* Future improvements:
    * Possibility to export metrics to CI dashboards, introduce thresholds, and offer optimization
      suggestions.
    * Warn when forkMode maven/surefire settings clashes with caching

```shell 
> To benefit from the caching mechanism, all tests must run within the same process or test suite. 
> This can be achieved by executing all tests as a group within an IDE. Similarly, when executing tests with a build
 > framework such as Ant, Maven, or Gradle, it is important to make sure that the build framework does not fork 
 > between tests. For example, if the forkMode for the Maven Surefire plug-in is set to always or pertest, the 
 > TestContext framework cannot cache application contexts between test classes, and the build process runs 
 > significantly more slowly as a result.
 ```

## Contribution & License

Feel free to contribute via pull requests or open issues
on [GitHub](https://github.com/chris-carneiro/spring-test-context-cache-metrics/issues).

This project is licensed under the Apache License Version 2.0 — see the [LICENSE](LICENSE) file for details.