# Contributing to the XYO Financial Java SDK

Thank you for your interest in contributing to the **XYO Financial SDK for Java**.

This SDK is engineered to provide institutional-grade reliability, thread-safety, and developer ergonomics for integrating XYO's transaction enrichment services into enterprise Java ecosystems. To maintain the highest standards of stability, performance, and security across Tier-1 financial institutions, all contributions must adhere to the architectural guidelines, contribution workflows, and quality gates detailed below.

---

## 🏗 Two-Layer Architecture

The Java SDK codebase is partitioned into two distinct, decoupled layers:

```
                  ┌────────────────────────────────────────────────────────┐
                  │                 xyo-financial / specs                  │
                  │             (Canonical OpenAPI Spec YAML)              │
                  └──────────────────────────┬─────────────────────────────┘
                                             │
                                             │ npx @openapitools/openapi-generator-cli
                                             ▼
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│ 1. GENERATED LAYER (openapi/)                                                            │
│    • Low-level HTTP transport and serialization (com.xyo.client, com.xyo.api, com.xyo.model)│
│    • READ-ONLY contract — strictly auto-generated from specs                             │
│    • DO NOT EDIT DIRECTLY                                                                │
└────────────────────────────────────────────┬─────────────────────────────────────────────┘
                                             │  Maven Dependency (com.xyo:xyo-sdk:1.0.0)
                                             ▼
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│ 2. WRAPPER LAYER (xyo-sdk/)                                                              │
│    • Public Entrypoint: com.xyo.financial.XyoClient                                      │
│    • Java 17+ fluent builders, defensive immutability, and thread safety                 │
│    • Native java.net.http.HttpClient transport, timeout bounds, and RFC 7807 errors     │
│    • Editable for ergonomics, helpers, validation, and unit tests                        │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

### 1. Low-Level Generated Layer (`openapi/`)

* **Location:** [`openapi/`](file:///Users/hadi/dev/start-ups/xyo/sdks/java/openapi)
* **Packages:** `com.xyo.client`, `com.xyo.api`, `com.xyo.model`
* **Lifecycle:** **Read-Only / Auto-Generated**.
* **Policy:** **DO NOT EDIT MANUALLY**. All source files inside this directory are auto-generated from the canonical OpenAPI specification maintained in the [`xyo-financial/specs`](https://github.com/xyo-financial/specs) repository. Any manual modifications committed directly to `openapi/` will be overwritten without warning during automated regeneration pipelines.

### 2. High-Level Wrapper Layer (`xyo-sdk/`)

* **Location:** [`xyo-sdk/`](file:///Users/hadi/dev/start-ups/xyo/sdks/java/xyo-sdk)
* **Primary Entrypoint:** [`com.xyo.financial.XyoClient`](file:///Users/hadi/dev/start-ups/xyo/sdks/java/xyo-sdk/src/main/java/com/xyo/financial/XyoClient.java)
* **Lifecycle:** **Actively Maintained & Extensible**.
* **Purpose:** Serves as the idiomatic, developer-friendly facade for financial applications. Key responsibilities include:
  * **Java 17+ Modern Ergonomics:** Fluent builder patterns ([`ClientConfig.Builder`](file:///Users/hadi/dev/start-ups/xyo/sdks/java/xyo-sdk/src/main/java/com/xyo/financial/ClientConfig.java), [`EnrichmentRequest.Builder`](file:///Users/hadi/dev/start-ups/xyo/sdks/java/xyo-sdk/src/main/java/com/xyo/financial/EnrichmentRequest.java)), immutable data structures, and defensive copying.
  * **Thread Safety:** Ensuring all client instances and request/response models are 100% thread-safe for high-concurrency environments.
  * **Transport & Resilience:** Native `java.net.http.HttpClient` configuration, granular connection/read timeouts, maximum response payload guards, and strict HTTPS enforcement (preventing downgrade attacks).
  * **Error Handling & Normalization:** Translating raw HTTP/API exceptions into structured [`XyoException`](file:///Users/hadi/dev/start-ups/xyo/sdks/java/xyo-sdk/src/main/java/com/xyo/financial/XyoException.java) instances categorized by [`ErrorCategory`](file:///Users/hadi/dev/start-ups/xyo/sdks/java/xyo-sdk/src/main/java/com/xyo/financial/ErrorCategory.java) (`VALIDATION`, `HTTP`, `TRANSPORT`, `PARSING`) with RFC 7807 Problem Details compliance for automated DLQ routing.

| Dimension | Generated Layer (`openapi/`) | Wrapper Layer (`xyo-sdk/`) |
| :--- | :--- | :--- |
| **Package** | `com.xyo.client`, `com.xyo.api`, `com.xyo.model` | `com.xyo.financial` |
| **Source of Truth** | `xyo-financial/specs` (`openapi.yml`) | Java SDK Repository |
| **Editability** | ❌ Read-Only (Machine generated) | ✅ Editable (Human authored) |
| **Target Audience** | Internal SDK plumbing | Public SDK consumers & enterprise services |
| **Design Paradigm** | Raw DTOs & direct HTTP endpoints | Immutable builders, thread safety & resilience |

---

## 🔀 Contribution Workflow

Contributions follow one of two tracks depending on the layer affected:

```
                                  What are you changing?
                                            │
               ┌────────────────────────────┴────────────────────────────┐
               ▼                                                         ▼
    API / Schema / Data Model                                SDK Ergonomics / Helpers /
    Endpoints / Protocol Types                               Bug Fixes / Unit Tests
               │                                                         │
               ▼                                                         ▼
   Propose & Merge PR in                                     Fork & Submit PR in
  xyo-financial/specs repo                                  xyo-financial/sdk-java
(https://github.com/xyo-financial/specs)                    (this repository)
               │                                                         │
               ▼                                                         ▼
  CI Auto-Regenerates openapi/                               Pass Quality Gates (mvn test)
```

### Track A: API & Data Model Changes
If you need to add an endpoint, modify request/response schemas, update field constraints, or adjust HTTP headers:
1. **Do not modify this repository directly.**
2. Propose your changes in the canonical specification repository: [https://github.com/xyo-financial/specs](https://github.com/xyo-financial/specs).
3. Once the specification changes are approved and merged to `main`, automated CI workflows ([`.github/workflows/generate.yml`](file:///Users/hadi/dev/start-ups/xyo/sdks/java/.github/workflows/generate.yml)) will regenerate the `openapi/` module and open an automated pull request against this repository.

### Track B: SDK Ergonomics, Helpers, Tests & Bug Fixes
If you are improving client ergonomics, adding utility helpers, optimizing performance, improving documentation, or adding unit tests:
1. Fork and create a branch from `main`.
2. Implement your changes within the [`xyo-sdk/`](file:///Users/hadi/dev/start-ups/xyo/sdks/java/xyo-sdk) module.
3. Add or update unit tests in [`xyo-sdk/src/test/java`](file:///Users/hadi/dev/start-ups/xyo/sdks/java/xyo-sdk/src/test/java).
4. Run the full validation test suite across all modules.
5. Submit a Pull Request to this repository for engineering review.

---

## ⚙️ Local Code Generation & Build Setup

### Prerequisites

* **JDK 17 LTS or newer** (Eclipse Temurin 17 or 21 recommended)
* **Apache Maven 3.8+**
* **Node.js 18+ & npx** (required for `openapi-generator-cli`)
* **Git**

### 1. Generating the OpenAPI Client

To regenerate the `openapi/` layer from a local copy of `specs/openapi.yml` (or relative path `../specs/openapi.yml`), run:

```bash
npx -y @openapitools/openapi-generator-cli generate \
  -i ../specs/openapi.yml \
  -g java \
  -o ./openapi \
  --additional-properties=groupId=com.xyo,artifactId=xyo-sdk,artifactVersion=1.0.0,library=native,invokerPackage=com.xyo.client,apiPackage=com.xyo.api,modelPackage=com.xyo.model
```

#### Configuration Breakdown:
* `-i ../specs/openapi.yml`: Path to the canonical OpenAPI 3.0+ specification.
* `-g java`: Target language generator.
* `-o ./openapi`: Destination directory for generated sources and POM.
* `library=native`: Enforces modern Java 11+ / 17+ `java.net.http.HttpClient` transport without legacy third-party HTTP clients.
* `invokerPackage=com.xyo.client`: Base package for HTTP transport, serialization, and authentication interceptors.
* `apiPackage=com.xyo.api`: Base package for API operation classes (e.g., `EnrichmentApi`).
* `modelPackage=com.xyo.model`: Base package for generated request/response DTOs.

### 2. Installing the Generated Artifact Locally

Because `xyo-sdk` depends on the generated module, install the generated client into your local Maven cache (`~/.m2/repository`):

```bash
cd openapi
mvn clean install -q -DskipTests
```

### 3. Compiling and Testing the Wrapper SDK

Navigate to the wrapper module to verify compilation and execute all unit and boundary tests:

```bash
cd ../xyo-sdk
mvn clean compile
mvn test
```

---

## 🛡 Quality Gates & Verification Standards

All Pull Requests must pass institutional quality gates before approval and merge:

### 1. Multi-Module Compilation
Both the `openapi/` and `xyo-sdk/` modules must compile with **zero warnings and zero errors**:
```bash
# Verify openapi module
mvn clean compile --file openapi/pom.xml

# Verify wrapper module
mvn clean compile --file xyo-sdk/pom.xml
```

### 2. Test Suite Execution
All unit and boundary test suites must pass 100%:
```bash
# Run openapi model & serialization tests
mvn test --file openapi/pom.xml

# Run wrapper ergonomic, validation, and network mock tests
mvn test --file xyo-sdk/pom.xml
```

### 3. Example Application Verification
The standalone example application in [`example/`](file:///Users/hadi/dev/start-ups/xyo/sdks/java/example) must build and run cleanly against the compiled SDK:
```bash
# Package example
mvn clean package --file example/pom.xml

# Run example application
mvn exec:java -Dexec.mainClass="com.xyo.example.Main" --file example/pom.xml
```

### 4. Java 17+ Idiomatic Standards
* **Immutability by Default:** All configuration classes and request/response models must remain immutable once constructed. Use defensive copying for collections and builders.
* **Modern Networking:** Use standard Java `java.net.http.HttpClient`. Do not introduce legacy external HTTP clients (e.g., Apache HttpClient, Retrofit, OkHttp).
* **Nullability & Validation:** Implement fail-fast validation in constructors and builders. Annotate nullable parameters explicitly with JSpecify annotations where applicable.
* **Structured Exceptions:** Throw `XyoException` with a descriptive `ErrorCategory` and wrap root cause exceptions rather than rethrowing generic `RuntimeException` or `ApiException`.
* **Zero PII Leakage:** Ensure `toString()` implementations, logs, and exception messages never output raw PAN, CVV, authentication tokens, or sensitive user credentials.

---

## 🚀 Pull Request Checklist

Before submitting a Pull Request, confirm that:

- [ ] **Two-Layer Compliance:** No manual edits were introduced inside the `openapi/` directory.
- [ ] **Tests Added:** Unit tests (JUnit 5) have been added or updated to cover all new behaviors or bug fixes.
- [ ] **Quality Gates Passed:** `mvn compile` and `mvn test` execute cleanly across `openapi/`, `xyo-sdk/`, and `example/`.
- [ ] **Backwards Compatibility:** Public API signatures in `com.xyo.financial` maintain backward compatibility.
- [ ] **Documentation:** Relevant Javadoc comments, `README.md`, and code examples have been updated.
- [ ] **Commit Hygiene:** Commits follow conventional commit formatting (e.g., `feat:`, `fix:`, `docs:`, `test:`, `refactor:`).

---

## 🔒 Security Vulnerabilities

If you discover a security vulnerability, please do not open a public GitHub issue. Follow the disclosure policy outlined in [`SECURITY.md`](file:///Users/hadi/dev/start-ups/xyo/sdks/java/SECURITY.md) or contact security directly at [security@xyo.financial](mailto:security@xyo.financial).
