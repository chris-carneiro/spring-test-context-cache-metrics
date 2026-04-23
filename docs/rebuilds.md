# Understanding Rebuilds

Spring reuses a cached `ApplicationContext` only when the test class requesting it produces an **identical**
[`MergedContextConfiguration`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/context/MergedContextConfiguration.html)
to one that is already cached. Any difference — however small — forces a full rebuild.

This page explains every property that contributes to that identity, why mismatches happen, and how to prevent them.

---

!!! warning "Current reporting scope"
    The rebuild report currently surfaces **active profile differences** between the cached context and the
    class that triggered a rebuild. Differences in other properties — such as `@MockBean` declarations,
    configuration classes, or property overrides — are detected and counted, but are **not yet broken out
    individually** in the output. Per-property diagnosis is planned for a future release.

---

## Causes at a glance

| Cause | Typical trigger | Intentional? |
|-------|----------------|--------------|
| [Active profiles](#active-profiles) | `@ActiveProfiles`, `spring.profiles.active` | Sometimes |
| [Mock and spy beans](#mock-and-spy-beans) | `@MockBean`, `@SpyBean` | Usually yes |
| [`@Import` on the test class](#import-on-the-test-class) | Test-specific config added via `@Import` | Usually yes |
| [Configuration classes](#configuration-classes) | `@SpringBootTest(classes = ...)` mismatch | Rarely intentional |
| [Property overrides](#property-overrides) | `@TestPropertySource(properties = ...)` | Sometimes |
| [Property source locations](#property-source-locations) | `@TestPropertySource(locations = ...)` | Rarely |
| [Web environment](#web-environment) | `@SpringBootTest(webEnvironment = ...)` mismatch | Rarely intentional |
| [Context initializers](#context-initializers) | `@ContextConfiguration(initializers = ...)` | Usually yes |
| [Explicit eviction](#explicit-eviction-dirtiescontext) | `@DirtiesContext` | Always intentional |
| [Context loader](#context-loader) | Custom `@ContextConfiguration(loader = ...)` | Rarely |
| [Parent context](#parent-context) | Nested application context hierarchies | Rarely |

---

## Active profiles

Spring includes the full set of active profiles in the cache key. A context loaded under `[test, integration]`
and a context loaded under `[test]` are two distinct entries — even if every other property is identical.

> The rebuild report logs `This class profiles` and `Cached context profiles` side by side so you can
> immediately see which profile is missing or extra.

---

## Mock and spy beans

`@MockBean` and `@SpyBean` are implemented as context customizers — they are part of the cache key.
Two test classes that declare a different set of mocked beans will always load separate contexts, even
if their profiles and configuration classes match.

---

## `@Import` on the test class

`@Import` on a test class is also processed as a context customizer. Two test classes importing a
different set of types produce different cache keys, even when only one extra class is added.

---

## Configuration classes

When `classes` is specified explicitly in `@CacheAwareSpringBootTest(classes = ...)`, the exact set
of classes is part of the cache key. Mixing explicit declarations with auto-detection, or declaring
different class sets across test classes, prevents reuse.

When all tests need the same explicit classes, centralise the `classes` declaration on the base class.
When auto-detection works for your module, omit `classes` entirely — Spring Boot will resolve the
`@SpringBootApplication` class consistently across all test classes in the same source tree.

---

## Property overrides

Inline properties declared via `@TestPropertySource(properties = ...)` or
`@CacheAwareSpringBootTest(properties = ...)` are part of the cache key. Even a single extra or
differing property forces a new context.

---

## Property source locations

`@TestPropertySource(locations = ...)` points to external property files. Different file paths — or
different sets of files — produce different cache keys.

---

## Web environment

`@CacheAwareSpringBootTest(webEnvironment = ...)` defaults to `MOCK`. A class using `RANDOM_PORT`
cannot share a context with a class using `MOCK` or `NONE`, even if everything else is identical.

---

## Context initializers

`@ContextConfiguration(initializers = ...)` registers `ApplicationContextInitializer` implementations
that run before the context refreshes. Each distinct set of initializer classes is a separate cache key.
This is commonly seen with Testcontainers, where a shared initializer wires up container connection
properties before the context starts.

---

## Explicit eviction — `@DirtiesContext`

`@DirtiesContext` instructs Spring to close and evict the context after the annotated test class (or
method) completes. Any test class that runs after it will receive a `REBUILD` — this is expected
and intentional.

!!! info "When a REBUILD after `@DirtiesContext` appears in the report"
    This is not a misconfiguration. If `@DirtiesContext` appears frequently, consider whether the
    underlying cause — mutated shared state, a side-effecting operation — can be eliminated so the
    annotation is no longer needed.

---

## Context loader

Relevant only when `@ContextConfiguration(loader = ...)` is used to provide a custom context loader.
Two test classes using different loader implementations will always produce separate cache keys.

---

## Parent context

Relevant only when using Spring's application context hierarchy feature. Different parent context
configurations produce different cache keys for child contexts.

---

## The fix in most cases

Every cause above has the same root: configuration that should be shared is declared independently on
each test class, and small inconsistencies compound into unnecessary rebuilds. The solution is to
**centralise everything that must be consistent on a shared abstract base class** (or a meta-annotation).
Profiles, mocks, web environment, imports — declared once, inherited everywhere.

!!! tip "DO — declare everything that must be consistent on a shared base"
    ```java linenums="1" hl_lines="4"
    @CacheAwareSpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles({"test", "integration"})
    @Import(TestSecurityConfig.class)
    abstract class AbstractIntegrationTest {

        @MockBean
        PaymentGateway paymentGateway;

        @MockBean
        NotificationService notificationService;
    }

    class OrderServiceTest extends AbstractIntegrationTest { }
    class UserServiceTest extends AbstractIntegrationTest { }
    ```
    Both classes inherit an identical configuration — Spring returns the same cached context for both.

!!! failure "DON'T — let each test class declare its own configuration independently"
    ```java linenums="1"
    @CacheAwareSpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles({"test", "integration"})
    class OrderServiceTest {
        @MockBean PaymentGateway paymentGateway;
    }

    @CacheAwareSpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK) // different env
    @ActiveProfiles("test")                                                         // missing profile
    class UserServiceTest {
        @MockBean NotificationService notificationService;                          // different mock
    }
    ```
    Every difference above is an independent reason for Spring to create a new context.

!!! note "Intentional rebuilds"
    The goal is not zero rebuilds — it is **intentional** rebuilds. A test class that genuinely needs
    a unique profile, a distinct mock, or `@DirtiesContext` will always get its own context. The tool
    helps you tell the difference between a deliberate choice and an accidental misconfiguration.
