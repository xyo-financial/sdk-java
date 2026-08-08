# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2026-08-09
### Added
- Generated OpenAPI client module (`com.xyo:xyo-sdk:2.0.0`) based on canonical OpenAPI 3.0 specification.
- Deterministic OpenAPI Generator CLI setup with workflow automation (`.github/workflows/generate.yml`).
- Java 17+ `XyoClient` wrapper around generated `EnrichmentApi` with fluent builder pattern and `Duration` timeouts.
- Comprehensive JUnit 5 test suite with 15 passing unit and boundary tests.

### Changed
- Modernized SDK baseline to Java 17+.
- Replaced custom HTTP transport layer with OpenAPI generated client backing native `java.net.http.HttpClient`.
- Updated documentation, example project, and contribution guidelines.

## [1.0.2] - 2026-08-07
### Changed
- Updated repository URLs to `https://github.com/xyo-financial/sdk-java.git`.
- Bumped SDK version to 1.0.2.

## [1.0.1] - 2026-07-20
### Changed
- Relicensed SDK to BSD 3-Clause License across repository and documentation.
- Bumped SDK version to 1.0.1.

## [1.0.0] - 2026-07-11
### Added
- Initial release of the Java SDK for XYO.Financial API.
- Maven `pom.xml` configuration for dependency management.
- `XyoClient` with methods to enrich individual transactions and collections.
- `ClientConfig` class to adjust timeout and connection parameters.
- Built-in `HttpTransport` implementation using native `java.net.http.HttpClient`.
- Data models: `EnrichmentRequest`, `EnrichmentResponse`, `EnrichTransactionCollectionResponse`.
- JUnit 5 test suite for robust validation of API handling.
- Integrated GitHub Actions CI and Release pipelines.
