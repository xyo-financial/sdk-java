# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
