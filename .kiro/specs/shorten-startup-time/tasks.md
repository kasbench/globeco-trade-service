# Implementation Plan: Shorten Startup Time

## Overview

This plan upgrades the GlobeCo Trade Service from Java 21 to Java 25, configures Spring AOT processing with Paketo buildpack AOT cache, replaces the Dockerfile-based build path with `bootBuildImage`, rewrites `kbuild.sh` for multi-arch manifest builds, and updates the CI workflow accordingly. Tasks are ordered so foundational changes land first, with build script and CI updates following.

## Tasks

- [x] 1. Upgrade Java toolchain and configure bootBuildImage in build.gradle
  - [x] 1.1 Upgrade Java toolchain from 21 to 25 and add bootBuildImage configuration
    - Change `JavaLanguageVersion.of(21)` to `JavaLanguageVersion.of(25)` in the `java.toolchain` block
    - Add a `tasks.named('bootBuildImage')` block configuring the Paketo builder, image name, tags, and environment variables (`BP_JVM_VERSION=25`, `BP_JVM_AOTCACHE_ENABLED=true`, `BP_SPRING_AOT_ENABLED=true`, `TRAINING_RUN_JAVA_TOOL_OPTIONS` with Flyway/Hibernate overrides)
    - _Requirements: 1.1, 1.2, 2.1, 3.1, 3.2, 3.4, 4.1, 4.2_

  - [ ]* 1.2 Run unit tests to verify Java 25 compatibility
    - Execute `./gradlew test` and confirm zero failures
    - _Requirements: 1.6, 7.7_

- [x] 2. Update Dockerfile base images to Java 25
  - [x] 2.1 Update Dockerfile FROM statements to Java 25
    - Change `eclipse-temurin:21-jdk` to `eclipse-temurin:25-jdk` in the build stage
    - Change `eclipse-temurin:21-jre` to `eclipse-temurin:25-jre` in the run stage
    - Retain the Dockerfile in the repository as a fallback (do not delete)
    - _Requirements: 1.4, 4.4_

- [x] 3. Rewrite kbuild.sh for multi-arch bootBuildImage builds
  - [x] 3.1 Rewrite kbuild.sh to use sequential bootBuildImage invocations and docker manifest
    - Replace the existing `docker buildx build` command with two `./gradlew bootBuildImage` invocations (one for `--imagePlatform=linux/amd64`, one for `--imagePlatform=linux/arm64`) using architecture-suffixed image names
    - Add `docker push` commands for each architecture-specific tag
    - Add `docker manifest create` and `docker manifest push` commands for both versioned and `latest` tags
    - Retain the `kubectl delete` / `kubectl apply` deployment steps at the end
    - Add `set -e` at script start for fail-fast behavior
    - Update the VERSION variable to the next release version
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

- [x] 4. Checkpoint - Verify local build
  - Ensure `./gradlew bootBuildImage` completes successfully on the local machine. Ensure all tests pass, ask the user if questions arise.

- [x] 5. Update CI workflow for Java 25 and bootBuildImage
  - [x] 5.1 Update .github/workflows/docker-publish.yml to Java 25 and bootBuildImage-based builds
    - Change `java-version: '21'` to `java-version: '25'` in the `setup-java` step
    - Replace the `Build JAR` step and `docker/build-push-action` with sequential `./gradlew bootBuildImage --imagePlatform=linux/amd64` and `./gradlew bootBuildImage --imagePlatform=linux/arm64` steps
    - Add steps for `docker push` of architecture-specific images
    - Add steps for `docker manifest create` and `docker manifest push` for both `latest` and SHA-tagged manifest lists
    - Remove the `docker/setup-buildx-action` step (no longer needed)
    - Retain the DockerHub login/logout steps
    - _Requirements: 1.5, 5.1, 5.2, 5.3_

- [x] 6. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- The user will handle Kubernetes deployment testing manually
- The Dockerfile is retained as a fallback but is no longer in the primary build path
- No data model or application logic changes are required — all modifications are build/infrastructure configuration
- Property-based tests are not applicable for this feature (build infrastructure, not application logic)

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "2.1", "3.1"] },
    { "id": 2, "tasks": ["5.1"] }
  ]
}
```
