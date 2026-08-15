# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Dynamic API key rotation support via `Supplier<String>` (`apiKeySupplier`) in `ClientConfig.Builder` enabling runtime credential rotation without client re-instantiation.
- Implemented `AutoCloseable` on `XyoClient` for try-with-resources support and container lifecycle management.
- Environment variable configuration fallback for API base URL via `XYO_API_BASE_URL` (`ClientConfig.resolveDefaultBaseUrl()`).
- Fail-fast builder validation in `ClientConfig.Builder` ensuring either `apiKey` or `apiKeySupplier` is provided.
- Multi-MIME `Accept` header support (`application/gzip, application/x-tar, application/octet-stream;q=0.9, */*;q=0.8`) during collection archive downloads.
- Intermediate WAF and proxy challenge diagnostic detection for non-binary content types returned during archive retrieval.

### Changed
- Replaced in-memory TAR processing with memory-bounded streaming extraction using Apache Commons Compress `TarArchiveInputStream` and non-closing filter streams.
- Updated GitHub Actions release workflow (`.github/workflows/release.yml`) to execute full test suite prior to build and distribution packaging.
- Standardized `LICENSE` file text for automated license compliance scanners.

### Security
- Zero-trust domain validation restricting archive download URLs exclusively to the configured API base host or authorized AWS S3 endpoints (`*.amazonaws.com`).
- Eliminated SSRF error preview in exception messages to prevent leaking sensitive internal network responses.
- Enforced strict URI scheme validation allowing only HTTPS (and HTTP if explicitly configured) and rejecting unsupported schemes (`file://`, `ftp://`, etc.).
- Prevented CWE-113 HTTP response splitting / header injection by rejecting CRLF characters (`\r`, `\n`) in `apiUser`.
- Prevented CWE-117 log injection by sanitizing TAR entry names against carriage returns, newlines, and control characters (`[\r\n\p{C}]`).
- Enforced credential leak protection by stripping `Authorization` Bearer headers when downloading collection archives from third-party hosts.
- Bounded input validation enforcing 128-character limit on transaction query text and 2-letter ISO 3166-1 alpha-2 country codes in `EnrichmentRequest`.
- Defended against zip bombs and zip slip path traversal via bounded decompressed stream verification (`maxResponseBytes`), entry limit caps (`DEFAULT_MAX_TAR_ENTRIES = 10000`), and path sanitization.
- Enforced TLS 1.2+ minimum protocol version across native HTTP client connections.

## [2.0.0] - 2026-08-09

### Added
- Generated OpenAPI client module (`com.xyo:xyo-sdk:2.0.0`) based on canonical OpenAPI 3.0 specification.
- Deterministic OpenAPI Generator CLI setup with workflow automation (`.github/workflows/generate.yml`).
- Java 17+ `XyoClient` wrapper around generated `EnrichmentApi` with fluent builder pattern and `Duration` timeouts.
- Comprehensive JUnit 5 test suite with unit and boundary tests.
- Download and stream decompression for enrichment collection TAR archives (`downloadEnrichmentCollection`).

### Changed
- Modernized SDK baseline to Java 17+.
- Replaced custom HTTP transport layer with OpenAPI generated client backing native `java.net.http.HttpClient`.
- Updated documentation, example project, and contribution guidelines.
- Relicensed SDK under Apache License 2.0.

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

[Unreleased]: https://github.com/xyo-financial/sdk-java/compare/v2.0.0...HEAD
[2.0.0]: https://github.com/xyo-financial/sdk-java/compare/v1.0.2...v2.0.0
[1.0.2]: https://github.com/xyo-financial/sdk-java/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/xyo-financial/sdk-java/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/xyo-financial/sdk-java/releases/tag/v1.0.0
