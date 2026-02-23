# CLAUDE.md — MyCycleCoach Development Guide

**This file is the authoritative source of truth for all development decisions on this project.**
When in doubt, this document takes precedence over general convention, Stack Overflow answers, or framework defaults. Every AI agent and every developer working on this codebase must read and follow this file before writing any code.

---

## 1. Project Overview

**MyCycleCoach** is a cycling coaching platform that provides ride tracking, performance analytics, and personalised training recommendations for cyclists.

This is a **greenfield project**. All new features must follow the patterns defined in this document exactly. There is no legacy code to maintain backward compatibility with.

### Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.x |
| Build | Gradle (Kotlin DSL) |
| Persistence | Spring Data JPA + PostgreSQL |
| Messaging | Apache Kafka |
| Boilerplate reduction | Lombok |
| Security | Spring Security 6+ (OAuth2 / JWT) |
| API documentation | Springdoc / OpenAPI 3 |
| Testing | JUnit 5, Mockito, AssertJ, Testcontainers |
| Schema migrations | Flyway |
| Code formatting | Spotless + Palantir Java Format |

---

## 2. Build System and Tooling

### 2.1 Gradle

The build tool is **Gradle only**. Maven is never used; there is no `pom.xml`.

Always use the Gradle wrapper — never a system-installed `gradle` binary:

```bash
./gradlew build              # compile + run all tests + produce jar
./gradlew test               # run all tests
./gradlew bootRun            # start the application locally
./gradlew clean build        # full clean build
./gradlew test --tests "com.mycyclecoach.feature.ride.service.RideServiceImplTest"
./gradlew spotlessApply      # auto-format all source files
./gradlew flywayMigrate      # apply pending database migrations
```

The build file is `build.gradle.kts` (Kotlin DSL). All dependency versions are managed in `gradle/libs.versions.toml` (Gradle version catalog). **Never hardcode dependency versions directly in `build.gradle.kts`.**

### 2.2 Java Version

Java 25 is the target. Pin it using Gradle toolchains:

```kotlin
// build.gradle.kts
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
```

- Enable preview features only when explicitly required by a ticket. Document the reason inline.
- Prefer records, sealed interfaces, and pattern matching where idiomatic.

### 2.3 Application Entry Point

```
src/main/java/com/mycyclecoach/Main.java
```

- This is the **only** `@SpringBootApplication` class in the project.
- It lives at the root package `com.mycyclecoach` and component-scans everything below it.
- **Never add business logic to `Main.java`.**

---

## 3. Package and Feature Structure

### 3.1 Canonical Layout

```
src/main/java/com/mycyclecoach/
├── Main.java
├── feature/
│   └── <featurename>/           # lowercase singular noun, e.g. "ride", "athlete", "coaching"
│       ├── controller/          # REST controllers (@RestController)
│       ├── service/             # Business logic: interface + @Service implementation
│       ├── domain/              # JPA entities, DTOs (records), Kafka event records, enums, exceptions
│       ├── consumer/            # Kafka @KafkaListener classes
│       ├── publisher/           # Kafka KafkaTemplate wrapper classes
│       ├── repository/          # Spring Data JPA repositories
│       └── strategy/            # Strategy-pattern interfaces and implementations
├── infrastructure/              # Cross-cutting technical wiring only (e.g. KafkaTopicConfig, GlobalExceptionHandler)
└── config/                      # Spring @Configuration classes (SecurityConfig, OpenApiConfig, KafkaConfig, etc.)
```

### 3.2 Rules

- Feature names are **singular lowercase nouns**: `ride`, `athlete`, `segment`. Not `rides`, `Athletes`, `ride-data`.
- Only create sub-packages that are actually needed. A feature with no Kafka involvement gets no `consumer/` or `publisher/` package.
- **Anti-corruption rule:** No class may import from another feature's `domain/` or `repository/` package. Cross-feature communication goes through a service interface or a Kafka event. This rule is never relaxed.
- `infrastructure/` contains only technical wiring. Never business logic.
- `config/` contains `@Configuration` classes only: `SecurityConfig`, `OpenApiConfig`, `KafkaConfig`, etc.

### 3.3 Naming Conventions Per Layer

| Layer | Suffix / Convention | Example |
|---|---|---|
| Controller | `Controller` | `RideController` |
| Service interface | `Service` | `RideService` |
| Service implementation | `ServiceImpl` | `RideServiceImpl` |
| Repository | `Repository` | `RideRepository` |
| JPA Entity | bare noun | `Ride` |
| DTO (request) | `Request` | `CreateRideRequest` |
| DTO (response) | `Response` | `RideResponse` |
| Kafka event | `Event` | `RideCreatedEvent` |
| Kafka Consumer | `Consumer` | `RideEventConsumer` |
| Kafka Publisher | `Publisher` | `RideEventPublisher` |
| Strategy interface | `Strategy` | `PaceCalculationStrategy` |
| Strategy implementation | descriptive noun | `AveragePaceCalculationStrategy` |
| Custom exception | `Exception` | `RideNotFoundException` |
| Global error handler | `GlobalExceptionHandler` | `GlobalExceptionHandler` |
| Mapper | `Mapper` | `RideMapper` |

---

## 4. Configuration Rules

### 4.1 YAML Only

- Primary file: `src/main/resources/application.yaml`
- Profile overrides: `application-dev.yaml`, `application-test.yaml`, `application-prod.yaml`
- **Never use `application.properties`.** YAML only, everywhere.
- **Never commit secrets to git.** Use environment variable substitution:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

### 4.2 Kafka Configuration in YAML

All Kafka topics, group IDs, bootstrap servers, and serializer config live in YAML:

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: mycyclecoach-consumer-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.mycyclecoach.*"
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

mycyclecoach:
  kafka:
    topics:
      ride-created: ride.created
      ride-updated: ride.updated
      athlete-registered: athlete.registered
```

### 4.3 Reading Config in Java

Bind config to a `@ConfigurationProperties` record. **Never use `@Value` for anything with more than one related property.**

```java
// config/KafkaTopicsProperties.java
@ConfigurationProperties(prefix = "mycyclecoach.kafka.topics")
@Validated
public record KafkaTopicsProperties(
    @NotBlank String rideCreated,
    @NotBlank String rideUpdated,
    @NotBlank String athleteRegistered
) {}
```

Annotate a `@Configuration` class with `@EnableConfigurationProperties(KafkaTopicsProperties.class)`.

`@Value` is only acceptable for a single, truly isolated property. Prefer `@ConfigurationProperties` in all other cases.

### 4.4 Custom Properties Namespace

All custom application properties live under the `mycyclecoach:` top-level key. Never place custom properties at the root level.

---

## 5. Domain Layer Conventions

### 5.1 JPA Entities

Entities live in `feature/<name>/domain/`.

```java
@Entity
@Table(name = "rides")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Double distanceKm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

Rules:
- Use `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder` explicitly. **`@Data` is forbidden on entities** — it generates `equals`/`hashCode` based on all fields, which breaks JPA proxies and bidirectional relationships.
- All entities must have an `@Id` field. Use `Long` with `@GeneratedValue(strategy = GenerationType.IDENTITY)` unless there is an explicit domain reason for a different strategy.
- Auditing fields use `@CreatedDate` and `@LastModifiedDate` from Spring Data. Enable JPA auditing in a `@Configuration` class.
- **Never expose JPA entities directly from controllers.** Always map to a response record.

### 5.2 DTOs

Use Java **records** for all DTOs. They are immutable by design and require no Lombok.

```java
// Request
public record CreateRideRequest(
    @NotBlank String title,
    @NotNull @Positive Double distanceKm,
    @NotNull Long athleteId
) {}

// Response
public record RideResponse(
    Long id,
    String title,
    Double distanceKm,
    Long athleteId,
    LocalDateTime createdAt
) {}
```

Validation annotations (`@NotNull`, `@NotBlank`, `@Min`, `@Positive`, etc.) go on request records only.

### 5.3 Kafka Event Objects

Kafka events are also records with the `Event` suffix. They live in `feature/<name>/domain/`:

```java
public record RideCreatedEvent(
    Long rideId,
    Long athleteId,
    String title,
    Double distanceKm,
    LocalDateTime occurredAt
) {}
```

---

## 6. Service Layer Conventions

### 6.1 Interface + Implementation

Every service is a Java interface with a single `@Service`-annotated implementation:

```java
// feature/ride/service/RideService.java
public interface RideService {
    RideResponse createRide(CreateRideRequest request);
    RideResponse getRideById(Long id);
    List<RideResponse> getRidesByAthlete(Long athleteId);
    RideResponse updateRide(Long id, UpdateRideRequest request);
    void deleteRide(Long id);
}

// feature/ride/service/RideServiceImpl.java
@Service
@RequiredArgsConstructor
@Slf4j
public class RideServiceImpl implements RideService {

    private final RideRepository rideRepository;
    private final RideEventPublisher rideEventPublisher;
    private final RideMapper rideMapper;

    @Override
    @Transactional
    public RideResponse createRide(CreateRideRequest request) {
        // implementation
    }

    @Override
    @Transactional(readOnly = true)
    public RideResponse getRideById(Long id) {
        return rideRepository.findById(id)
            .map(rideMapper::toResponse)
            .orElseThrow(() -> new RideNotFoundException(id));
    }
}
```

- `@RequiredArgsConstructor` from Lombok is mandatory for constructor injection. **Never use `@Autowired` field injection.**
- `@Slf4j` from Lombok is the logger annotation. **Never use `System.out.println`.**

### 6.2 Mapper Pattern

Mapping between entity and DTO is done in a dedicated `<Feature>Mapper` class:

```java
// feature/ride/domain/RideMapper.java
@Component
public class RideMapper {

    public RideResponse toResponse(Ride ride) {
        return new RideResponse(
            ride.getId(),
            ride.getTitle(),
            ride.getDistanceKm(),
            ride.getAthlete().getId(),
            ride.getCreatedAt()
        );
    }
}
```

### 6.3 Transactions

- `@Transactional` goes on the **service implementation method**, never on the interface.
- Read-only queries use `@Transactional(readOnly = true)`.
- Write operations use `@Transactional` without qualifiers.

---

## 7. Controller Layer Conventions

### 7.1 Controller Structure

```java
@RestController
@RequestMapping("/api/v1/rides")
@RequiredArgsConstructor
@Tag(name = "Rides", description = "Ride management endpoints")
public class RideController {

    private final RideService rideService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new ride")
    public RideResponse createRide(@Valid @RequestBody CreateRideRequest request) {
        return rideService.createRide(request);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get a ride by ID")
    @ApiResponse(responseCode = "404", description = "Ride not found")
    public RideResponse getRideById(@PathVariable Long id) {
        return rideService.getRideById(id);
    }
}
```

Rules:
- All controllers are under `/api/v1/` prefix.
- Use `@Valid` on all `@RequestBody` parameters.
- `@ResponseStatus` is declared explicitly on **every** method.
- `@Tag` and `@Operation` OpenAPI annotations are **required** on every controller and method. This is not optional.
- Non-obvious response codes must have `@ApiResponse` annotations.
- **Controllers contain zero business logic.** They are thin delegation layers to the service.

### 7.2 API Versioning

URL path versioning only: `/api/v1/`, `/api/v2/`. Never header or query-parameter versioning.

When a breaking change is needed, create a new controller class in `controller/` with a `V2` suffix and a new request path.

---

## 8. Repository Layer Conventions

```java
// feature/ride/repository/RideRepository.java
@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {

    List<Ride> findByAthleteId(Long athleteId);

    @Query("SELECT r FROM Ride r WHERE r.athlete.id = :athleteId AND r.distanceKm >= :minDistance")
    List<Ride> findByAthleteIdAndMinDistance(
        @Param("athleteId") Long athleteId,
        @Param("minDistance") Double minDistance
    );
}
```

Rules:
- Extend `JpaRepository<Entity, ID>`.
- Use Spring Data derived query methods (naming convention) for simple queries.
- Use `@Query` with JPQL for anything non-trivial.
- Native SQL queries are permitted only with a documented performance justification in a comment on the method.
- **Repositories are interfaces only.** No implementation class is ever written manually.

---

## 9. Kafka Conventions

### 9.1 Publishers

```java
// feature/ride/publisher/RideEventPublisher.java
@Component
@RequiredArgsConstructor
@Slf4j
public class RideEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicsProperties kafkaTopicsProperties;

    public void publishRideCreated(RideCreatedEvent event) {
        log.info("Publishing RideCreatedEvent for rideId={}", event.rideId());
        kafkaTemplate.send(kafkaTopicsProperties.rideCreated(), event.rideId().toString(), event);
    }
}
```

Rules:
- Always inject `KafkaTopicsProperties` to get topic names. **Never hardcode topic name strings.**
- Log at `INFO` level before publishing.
- Use the entity/event ID as the Kafka message key for partition consistency.

### 9.2 Consumers

```java
// feature/coaching/consumer/RideEventConsumer.java
@Component
@RequiredArgsConstructor
@Slf4j
public class RideEventConsumer {

    private final CoachingService coachingService;

    @KafkaListener(
        topics = "${mycyclecoach.kafka.topics.ride-created}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onRideCreated(RideCreatedEvent event) {
        log.info("Received RideCreatedEvent for rideId={}", event.rideId());
        try {
            coachingService.processNewRide(event);
        } catch (Exception e) {
            log.error("Failed to process RideCreatedEvent for rideId={}", event.rideId(), e);
            // Dead-letter handling or re-throw based on retry strategy — document the choice inline
        }
    }
}
```

Rules:
- Topic names in `@KafkaListener` **always** come from property placeholders, never string literals.
- Wrap the consumer body in try/catch. Log failures with the full exception.
- Document the chosen dead-letter strategy (manual DLT vs Spring Kafka's `@RetryableTopic`) inline.

### 9.3 Topic Configuration Beans

Topic creation beans live in `infrastructure/`:

```java
// infrastructure/KafkaTopicConfig.java
@Configuration
@RequiredArgsConstructor
public class KafkaTopicConfig {

    private final KafkaTopicsProperties kafkaTopicsProperties;

    @Bean
    public NewTopic rideCreatedTopic() {
        return TopicBuilder.name(kafkaTopicsProperties.rideCreated())
            .partitions(3)
            .replicas(1)
            .build();
    }
}
```

---

## 10. Exception Handling Conventions

### 10.1 Error Response Structure

A single `ErrorResponse` record is used everywhere in the application:

```java
// infrastructure/ErrorResponse.java
public record ErrorResponse(
    int status,
    String error,
    String message,
    String path,
    LocalDateTime timestamp
) {}
```

### 10.2 Global Exception Handler

Lives at `infrastructure/GlobalExceptionHandler.java`:

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return new ErrorResponse(404, "Not Found", ex.getMessage(),
            request.getRequestURI(), LocalDateTime.now());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return new ErrorResponse(400, "Bad Request", message,
            request.getRequestURI(), LocalDateTime.now());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        return new ErrorResponse(500, "Internal Server Error",
            "An unexpected error occurred", request.getRequestURI(), LocalDateTime.now());
    }
}
```

### 10.3 Custom Exception Classes

- Per-feature exceptions live in `feature/<name>/domain/`.
- All custom exceptions extend `RuntimeException`.
- Naming: `<Resource>NotFoundException`, `<Resource>AlreadyExistsException`, `<Feature>ProcessingException`.

```java
// feature/ride/domain/RideNotFoundException.java
public class RideNotFoundException extends RuntimeException {
    public RideNotFoundException(Long id) {
        super("Ride not found with id: " + id);
    }
}
```

- **Every custom exception must be registered in `GlobalExceptionHandler`** with its appropriate HTTP status.

---

## 11. Security Conventions

```java
// config/SecurityConfig.java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .build();
    }
}
```

Rules:
- Use `SecurityFilterChain` bean. **Never extend `WebSecurityConfigurerAdapter`** (removed in Spring Security 6+).
- Stateless session (`STATELESS`).
- These endpoints are **always public**: `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`.
- All other endpoints require authentication by default.
- Role-based access is controlled with `@PreAuthorize` on the **service method**, not the controller.

---

## 12. OpenAPI / Springdoc Conventions

```java
// config/OpenApiConfig.java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("MyCycleCoach API")
                .description("Cycling coaching platform REST API")
                .version("1.0.0"));
    }
}
```

Rules:
- Every controller must have `@Tag(name = "...", description = "...")`.
- Every endpoint method must have `@Operation(summary = "...")`.
- Non-obvious response codes must have `@ApiResponse` annotations.
- **This is a hard requirement, not optional documentation.** A controller without these annotations is incomplete.

Springdoc exposes the UI at `/swagger-ui.html` and the spec at `/v3/api-docs` automatically when the dependency is on the classpath.

---

## 13. Testing Conventions

This is the strictest section. Every feature must be tested at multiple levels before it is considered complete.

### 13.1 Test Structure

Tests mirror the main source tree:

```
src/test/java/com/mycyclecoach/
└── feature/
    └── ride/
        ├── controller/
        │   └── RideControllerTest.java
        ├── service/
        │   └── RideServiceImplTest.java
        ├── consumer/
        │   └── RideEventConsumerTest.java
        ├── publisher/
        │   └── RideEventPublisherTest.java
        ├── repository/
        │   └── RideRepositoryIntegrationTest.java
        └── strategy/
            └── AveragePaceCalculationStrategyTest.java
```

### 13.2 Naming Conventions

| Test type | Class name pattern | Method name pattern |
|---|---|---|
| Unit | `<Subject>Test` | `should<Outcome>When<Condition>` |
| Integration | `<Subject>IntegrationTest` | `should<Outcome>When<Condition>` |
| Web slice | `<Subject>ControllerTest` | `should<Outcome>When<Condition>` |

Examples:
- `shouldReturnRideResponseWhenRideExists()`
- `shouldThrowRideNotFoundExceptionWhenIdIsInvalid()`
- `shouldPublishRideCreatedEventWhenRideIsCreated()`

### 13.3 Unit Tests (Services, Strategies, Publishers)

```java
@ExtendWith(MockitoExtension.class)
class RideServiceImplTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private RideEventPublisher rideEventPublisher;

    @Mock
    private RideMapper rideMapper;

    @InjectMocks
    private RideServiceImpl rideService;

    @Test
    void shouldReturnRideResponseWhenRideExists() {
        // given
        Ride ride = Ride.builder().id(1L).title("Morning Ride").distanceKm(25.0).build();
        RideResponse expected = new RideResponse(1L, "Morning Ride", 25.0, 10L, LocalDateTime.now());
        given(rideRepository.findById(1L)).willReturn(Optional.of(ride));
        given(rideMapper.toResponse(ride)).willReturn(expected);

        // when
        RideResponse actual = rideService.getRideById(1L);

        // then
        assertThat(actual).isEqualTo(expected);
        then(rideRepository).should().findById(1L);
    }

    @Test
    void shouldThrowRideNotFoundExceptionWhenIdIsInvalid() {
        // given
        given(rideRepository.findById(99L)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> rideService.getRideById(99L))
            .isInstanceOf(RideNotFoundException.class)
            .hasMessageContaining("99");
    }
}
```

Rules:
- Use **BDDMockito** (`given`/`when`/`then`) structure.
- Use **AssertJ** (`assertThat`) for all assertions. Never JUnit's `assertEquals`.
- `@ExtendWith(MockitoExtension.class)` on all unit tests.
- **Never use `@SpringBootTest` for unit tests.**

### 13.4 Controller Tests (Web Slice)

```java
@WebMvcTest(RideController.class)
@Import(SecurityConfig.class)
class RideControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RideService rideService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void shouldReturn201WhenRideIsCreated() throws Exception {
        // given
        CreateRideRequest request = new CreateRideRequest("Morning Ride", 25.0, 1L);
        RideResponse response = new RideResponse(1L, "Morning Ride", 25.0, 1L, LocalDateTime.now());
        given(rideService.createRide(request)).willReturn(response);

        // when / then
        mockMvc.perform(post("/api/v1/rides")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.title").value("Morning Ride"));
    }
}
```

- `@WebMvcTest` slices only the web layer.
- Use `@MockBean` for service dependencies.
- Use `@WithMockUser` for authenticated request simulation.

### 13.5 Repository Integration Tests

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class RideRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private RideRepository rideRepository;

    @Test
    void shouldFindRidesByAthleteId() {
        // given ... persist test data ...
        // when
        List<Ride> rides = rideRepository.findByAthleteId(1L);
        // then
        assertThat(rides).hasSize(2);
    }
}
```

- **Never use H2 in-memory database for JPA tests.** The project targets PostgreSQL; tests must use the same engine via Testcontainers.

### 13.6 Kafka Tests

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"ride.created"})
class RideEventPublisherTest {
    // Verify messages are sent to the correct topic with the correct key and payload
}
```

### 13.7 Coverage Requirements

- **Minimum 80% line coverage** on all service and strategy classes.
- Every public service method must have at minimum: one happy-path test and one exception/invalid-input test.
- JaCoCo is configured in `build.gradle.kts` to enforce this threshold. The CI build fails if coverage drops below it.

---

## 14. Logging Conventions

- Always use `@Slf4j` (Lombok). The logger variable is named `log`.
- **Log levels:**
  - `ERROR` — unhandled exceptions, data integrity failures, consumer processing failures
  - `WARN` — business rule violations, retries, unexpected but handled conditions
  - `INFO` — lifecycle events (Kafka message published/received, significant state transitions)
  - `DEBUG` — detailed internal state, query parameters (never enabled in production config)
- **Never log passwords, tokens, PII, or full request bodies containing sensitive data.**
- Use structured log arguments — **never string concatenation** in log calls:

```java
// Correct
log.info("Processing ride rideId={} athleteId={}", rideId, athleteId);

// Wrong — never do this
log.info("Processing ride " + rideId + " for athlete " + athleteId);
```

---

## 15. Lombok Usage Rules

### Mandatory

| Annotation | Where |
|---|---|
| `@Slf4j` | All Spring beans: `@Service`, `@Component`, `@RestControllerAdvice`, consumers, publishers |
| `@RequiredArgsConstructor` | All `@Service`, `@Component`, `@RestController`, `@RestControllerAdvice` classes |
| `@Builder` | JPA entities (always paired with `@NoArgsConstructor` + `@AllArgsConstructor`) |
| `@Getter` + `@Setter` | JPA entities |

### Forbidden

| Annotation | Reason |
|---|---|
| `@Data` on JPA entities | Generates `equals`/`hashCode` based on all fields; breaks Hibernate proxy and bidirectional relationships |
| `@SneakyThrows` | Hides checked exceptions; handle them explicitly |
| `@UtilityClass` on Spring beans | Use `@Configuration` instead |

Records use **no Lombok**. They are self-contained.

---

## 16. Adding a New Feature — Step-by-Step Checklist

Follow this checklist in order every time a new feature is added. Do not skip steps.

```
New Feature Checklist: "<featurename>"

 1. Create the package tree under feature/<featurename>/ for only the layers
    actually needed (controller, service, domain, repository, consumer,
    publisher, strategy).

 2. Domain layer:
    - JPA entity (if DB persistence is needed)
    - Request and Response records
    - Kafka event records (if events are needed)
    - Custom exception classes (XxxNotFoundException, etc.)

 3. Repository layer:
    - JpaRepository interface

 4. Service layer:
    - Service interface
    - @Service implementation (@RequiredArgsConstructor, @Slf4j)
    - Mapper class (@Component)

 5. Kafka layer (if applicable):
    - Publisher class in publisher/
    - Consumer class in consumer/
    - Add topic name(s) to application.yaml under mycyclecoach.kafka.topics
    - Add NewTopic bean(s) to infrastructure/KafkaTopicConfig.java

 6. Controller layer (if an HTTP endpoint is needed):
    - @RestController under /api/v1/<featurename>
    - @Tag and @Operation OpenAPI annotations on every method
    - @Valid on all @RequestBody parameters
    - @ResponseStatus on every method

 7. Exception handling:
    - Register all custom exceptions in GlobalExceptionHandler
    - Ensure every service method that can fail throws a typed exception

 8. Configuration:
    - No hardcoded strings anywhere
    - Any new configurable value goes in application.yaml
    - If 2+ related properties, bind to a @ConfigurationProperties record

 9. Tests (all of the following are required):
    - Unit tests for every service method (shouldXxxWhenYyy naming)
    - Unit tests for every publisher method
    - Unit tests for every consumer method
    - Unit tests for every strategy
    - @WebMvcTest for the controller
    - @DataJpaTest + Testcontainers for the repository

10. Update OpenApiConfig if new tags need top-level documentation.

11. Run ./gradlew build — this must pass with zero failures before
    the feature is considered complete.
```

---

## 17. What is Forbidden

The following are **hard prohibitions**. Code that violates this list will not be merged.

- **No `@Autowired` field injection.** Constructor injection via `@RequiredArgsConstructor` only.
- **No `System.out.println`.** Use `@Slf4j` logging.
- **No hardcoded strings** for: Kafka topics, database URLs, ports, passwords, API keys, or any environment-specific value.
- **No `application.properties`.** YAML only.
- **No direct cross-feature repository or domain access.** Features communicate via service interfaces or Kafka events only.
- **No business logic in controllers.** Controllers are delegation layers only.
- **No business logic in `Main.java`.**
- **No `@Data` on JPA entities.**
- **No Maven (`pom.xml`).** Gradle only.
- **No H2 in-memory database for tests.** Use Testcontainers with PostgreSQL.
- **No literal topic strings in `@KafkaListener`.** Topic names must be property placeholder references.
- **No skipping tests.** Every feature must have a full test suite before it is considered complete.

---

## 18. Database Migration Conventions

- Use **Flyway** for all schema changes. Add `org.flywaydb:flyway-core` to dependencies.
- Migration scripts live in `src/main/resources/db/migration/`.
- Naming convention: `V<version>__<description>.sql`
  - Example: `V1__create_rides_table.sql`, `V2__add_athlete_table.sql`, `V3__add_distance_index.sql`
- **Never modify an already-applied migration file.** Always create a new version.
- Spring Boot auto-runs Flyway on startup when the dependency is on the classpath.
- To apply pending migrations locally: `./gradlew flywayMigrate`

---

## 19. Code Style and Formatting

- **Spotless** Gradle plugin with **Palantir Java Format** is the formatter.
- Spotless check runs automatically as part of `./gradlew build`. The build fails on formatting violations.
- To auto-fix formatting: `./gradlew spotlessApply`
- **Line length:** 120 characters.
- **No wildcard imports.**
- **Import ordering:** managed by the formatter (static imports first, then `java.*`, then `javax.*`, then third-party, then project imports).
- `@Override` annotation is required on **all** implementing methods.
