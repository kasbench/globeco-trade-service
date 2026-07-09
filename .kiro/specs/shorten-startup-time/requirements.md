# Requirements Document

## Introduction

This feature shortens the startup time of the GlobeCo Trade Service, a Spring Boot microservice deployed as a container in Kubernetes on AWS. The approach involves upgrading from Java 21 to Java 25, implementing Spring AOT processing, and leveraging the AOT cache via Paketo buildpacks to pre-cache class data and JIT compilation information during a training run. The existing multi-stage Dockerfile and `kbuild.sh` docker buildx workflow will be replaced by Spring Boot's `bootBuildImage` task using Paketo buildpacks with AOT cache support.

## Glossary

- **Trade_Service**: The GlobeCo Trade Service Spring Boot microservice (globeco-trade-service)
- **Build_System**: The Gradle-based build system including `build.gradle` configuration and related build scripts
- **Container_Image**: The OCI-compliant container image produced by the build process and deployed to Kubernetes
- **AOT_Cache**: Spring Boot 3.3+ feature that pre-caches class data and JIT compilation profiles during a training run, stored in the container image for faster subsequent startups
- **Spring_AOT_Processing**: Compile-time code generation that pre-computes bean definitions, configuration metadata, and proxy classes to avoid runtime reflection and classpath scanning
- **Paketo_Buildpack**: Cloud Native Buildpacks provided by the Paketo project that build OCI container images from application source without a Dockerfile
- **bootBuildImage**: The Spring Boot Gradle task that uses Cloud Native Buildpacks to produce a container image
- **Multi_Architecture_Build**: A build that produces container images for multiple CPU architectures (linux/amd64 and linux/arm64)
- **Training_Run**: An automated execution of the application during image build that generates AOT cache data (class lists, JIT profiles) used to accelerate subsequent cold starts

## Requirements

### Requirement 1: Upgrade to Java 25

**User Story:** As a DevOps engineer, I want the Trade Service to run on Java 25, so that the application benefits from the latest JVM performance improvements and language features.

#### Acceptance Criteria

1. THE Build_System SHALL specify Java 25 as the toolchain language version in `build.gradle` (changing `JavaLanguageVersion.of(21)` to `JavaLanguageVersion.of(25)`)
2. THE Build_System SHALL configure the Paketo buildpack environment variable `BP_JVM_VERSION` to `25` in the `bootBuildImage` configuration block
3. WHEN the application is built, THE Build_System SHALL compile all source code targeting Java 25 bytecode (class file version 69)
4. WHEN the container image is built via Dockerfile, THE Container_Image SHALL use Java 25 base images for both the build stage and the runtime stage
5. WHEN the CI workflow executes, THE Build_System SHALL use Java 25 for the `setup-java` action in `.github/workflows/docker-publish.yml`
6. WHEN the application is built with Java 25, THE Build_System SHALL pass all existing unit tests without modification to test source code

### Requirement 2: Implement Spring AOT Processing

**User Story:** As a DevOps engineer, I want Spring AOT processing enabled at build time, so that the application avoids expensive runtime reflection and classpath scanning during startup.

#### Acceptance Criteria

1. THE Build_System SHALL configure Spring AOT processing through the Spring Boot Gradle plugin (`org.springframework.boot`) so that the `processAot` task is available and enabled
2. WHEN the application is built, THE Build_System SHALL execute the `processAot` task, which SHALL complete without errors and generate AOT-optimized source files and metadata in the build output directory
3. WHEN the application starts with AOT processing active, THE Trade_Service SHALL log an indication that AOT-optimized bean definitions are in use, and SHALL not perform runtime classpath scanning for component detection
4. IF a library is incompatible with AOT processing, THEN THE Build_System SHALL fail the build and output an error message identifying the incompatible library by name and the nature of the incompatibility
5. WHEN the application is built with AOT processing enabled, THE Build_System SHALL produce a runnable application artifact that starts successfully and serves HTTP requests on port 8082

### Requirement 3: Enable AOT Cache via Paketo Buildpack

**User Story:** As a DevOps engineer, I want the AOT cache enabled in the container image, so that JVM class loading and JIT compilation are pre-warmed for faster cold starts.

#### Acceptance Criteria

1. THE Build_System SHALL configure `bootBuildImage` to use the `paketobuildpacks/builder-noble-java-tiny` builder
2. THE Build_System SHALL set the `BP_JVM_AOTCACHE_ENABLED` environment variable to `true` and the `BP_JVM_VERSION` environment variable to `25` in the buildpack configuration
3. WHEN the container image is built, THE Paketo_Buildpack SHALL execute a Training_Run that starts the application to collect class loading data and JIT compilation profiles, producing a CDS archive and JIT profile data stored in the image
4. IF the Training_Run fails due to missing database connectivity or application startup failure, THEN THE Build_System SHALL provide a Spring profile or environment configuration that allows the application to start without requiring an active PostgreSQL connection during the Training_Run
5. THE Container_Image SHALL contain the AOT cache artifacts (CDS archive and JIT profile data) produced during the Training_Run
6. WHEN the Trade_Service starts from the built Container_Image, THE Trade_Service SHALL log indicators confirming that CDS archive and JIT profile data from the AOT_Cache are being loaded by the JVM

### Requirement 4: Replace Dockerfile with bootBuildImage

**User Story:** As a DevOps engineer, I want the build process to use Spring Boot's `bootBuildImage` instead of the multi-stage Dockerfile, so that the buildpack handles JVM optimization, layering, and AOT cache integration automatically.

#### Acceptance Criteria

1. THE Build_System SHALL define a `bootBuildImage` task in `build.gradle` that specifies the Paketo builder, image name, and all required buildpack environment variables (as defined in Requirements 1 and 3)
2. THE Build_System SHALL configure the `bootBuildImage` task to tag the image as `kasbench/globeco-trade-service` with both a version tag matching the project version and a `latest` tag
3. WHEN `./gradlew bootBuildImage` is executed, THE Build_System SHALL produce a Container_Image that starts successfully and exposes the health endpoint on port 8082 without requiring the Dockerfile
4. THE Build_System SHALL retain the existing Dockerfile in the repository but SHALL NOT use it in the primary build path
5. IF `./gradlew bootBuildImage` fails due to a buildpack error, THEN THE Build_System SHALL output a non-zero exit code and an error message indicating the failing buildpack stage

### Requirement 5: Support Multi-Architecture Container Builds

**User Story:** As a DevOps engineer, I want the container image to support both linux/amd64 and linux/arm64 architectures, so that the service can deploy on either x86 or ARM-based Kubernetes nodes in AWS.

#### Acceptance Criteria

1. THE Build_System SHALL produce Container_Image artifacts for both linux/amd64 and linux/arm64 platforms in a single invocation of `kbuild.sh`
2. THE Build_System SHALL push multi-architecture images to the `kasbench/globeco-trade-service` container registry with both a `latest` tag and a versioned tag
3. WHEN deployed to a Kubernetes node, THE Container_Image SHALL match the node's CPU architecture automatically via manifest list
4. THE Build_System SHALL update `kbuild.sh` to invoke build commands that produce a multi-architecture manifest list containing both linux/amd64 and linux/arm64 variants
5. IF a build for either architecture fails, THEN THE Build_System SHALL exit with a non-zero status code and SHALL NOT push a partial manifest list to the registry
6. WHEN `kbuild.sh` completes a successful build and push, THE Build_System SHALL perform Kubernetes redeployment by deleting and re-applying the deployment manifest

### Requirement 6: Preserve Existing Deployment Configuration

**User Story:** As a DevOps engineer, I want the Kubernetes deployment manifests to remain unchanged, so that resource allocations and probe configurations are not affected by the build tooling change.

#### Acceptance Criteria

1. THE Trade_Service SHALL expose the health endpoint at `/actuator/health/readiness` on port 8082 that returns HTTP 200 when the application is ready to serve traffic
2. THE Trade_Service SHALL expose the health endpoint at `/actuator/health/liveness` on port 8082 that returns HTTP 200 when the application is running
3. THE Trade_Service SHALL reach ready state (HTTP 200 on `/actuator/health/readiness`) within 200 seconds of container start, as determined by the startup probe configuration (initialDelaySeconds=5, periodSeconds=5, failureThreshold=40)
4. THE Container_Image SHALL expose port 8082 for the application and operate within the existing resource limits of 1000m CPU and 700Mi memory
5. THE Build_System SHALL NOT modify any files under `k8s_aws/` including deployment manifests, resource requests, resource limits, probe configurations, or environment variable definitions

### Requirement 7: Maintain Application Functionality

**User Story:** As a DevOps engineer, I want the application to function identically after the Java upgrade and AOT changes, so that no regressions are introduced.

#### Acceptance Criteria

1. WHEN the Trade_Service starts with AOT processing enabled, THE Trade_Service SHALL initialize all Spring Data JPA repositories such that CRUD operations on all entities (blotter, destination, execution, execution_status, trade_order, trade_type) return correct results through the REST API
2. WHEN the Trade_Service starts with AOT processing enabled, THE Trade_Service SHALL execute Flyway database migrations such that the schema reaches the same target version as without AOT processing and no migration errors are reported in application logs
3. WHEN the Trade_Service starts with AOT processing enabled, THE Trade_Service SHALL initialize Caffeine caches such that the cache-related Actuator endpoint reports all configured caches as active
4. WHEN the Trade_Service starts with AOT processing enabled, THE Trade_Service SHALL configure Resilience4j circuit breakers and retries such that the Actuator health endpoint reports circuit breaker state as CLOSED and retry configurations are present
5. WHEN the Trade_Service starts with AOT processing enabled, THE Trade_Service SHALL serve SpringDoc OpenAPI documentation at the configured endpoint such that an HTTP GET request returns a valid OpenAPI 3.x JSON or YAML document with a 200 status code
6. WHEN the Trade_Service starts with AOT processing enabled, THE Trade_Service SHALL export OpenTelemetry metrics and traces such that the configured OTLP collector receives metric and trace data
7. WHEN the Trade_Service starts with AOT processing enabled, THE Trade_Service SHALL pass the existing JUnit 5 test suite with zero test failures and zero test errors
8. WHEN the Trade_Service starts with AOT processing enabled, THE Trade_Service SHALL respond to Kubernetes readiness and liveness probe requests on the Actuator health endpoint with a 200 status code within 5 seconds of the application reporting ready state
9. IF the Trade_Service fails to initialize any subsystem during AOT-enabled startup, THEN THE Trade_Service SHALL terminate with a non-zero exit code and log an error message indicating which subsystem failed to initialize
