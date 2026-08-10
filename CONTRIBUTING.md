# Contributing to the XYO Financial Java SDK

Thank you for your interest in contributing to the **XYO Financial SDK for Java** (`com.xyo.financial:xyo-sdk`).

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
│    • ⚠️ READ-ONLY: DO NOT EDIT OR FORMAT MANUALLY                                        │
└────────────────────────────────────────────┬─────────────────────────────────────────────┘
                                             │  Maven Dependency (com.xyo:xyo-sdk:2.0.0)
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

* **Location:** [`openapi/`](openapi)
* **Packages:** `com.xyo.client`, `com.xyo.api`, `com.xyo.model`
* **Artifact:** `com.xyo:xyo-sdk:2.0.0`
* **Lifecycle:** **Read-Only / Auto-Generated**.
* **Policy:** **DO NOT EDIT MANUALLY**. All source files inside this directory are auto-generated from the canonical OpenAPI specification maintained in the [`xyo-financial/specs`](https://github.com/xyo-financial/specs) repository. Any manual modifications committed directly to `openapi/` will be overwritten without warning during automated regeneration pipelines.

### 2. High-Level Wrapper Layer (`xyo-sdk/`)

* **Location:** [`xyo-sdk/`](xyo-sdk)
* **Primary Entrypoint:** [`com.xyo.financial.XyoClient`](xyo-sdk/src/main/java/com/xyo/financial/XyoClient.java)
* **Artifact:** `com.xyo.financial:xyo-sdk:2.0.0`
* **Lifecycle:** **Actively Maintained & Extensible**.
* **Purpose:** Serves as the idiomatic, developer-friendly facade for financial applications. Key responsibilities include:
  * **Java 17+ Modern Ergonomics:** Fluent builder patterns (`ClientConfig.Builder`, `EnrichmentRequest.Builder`), immutable data structures, and defensive copying.
  * **Thread Safety:** Ensuring all client instances and request/response models are 100% thread-safe for high-concurrency environments.
  * **Transport & Resilience:** Native `java.net.http.HttpClient` configuration, granular connection/read timeouts, maximum response payload guards, and strict HTTPS enforcement (preventing downgrade attacks).
  * **Error Handling & Normalization:** Translating raw HTTP/API exceptions into structured `XyoException` instances categorized by `ErrorCategory` (`VALIDATION`, `HTTP`, `TRANSPORT`, `PARSING`) with RFC 7807 Problem Details compliance for automated DLQ routing.
  * **Collection Download Helpers:** Streaming `tar.gz` archive decoding for bulk transaction enrichment results (`downloadEnrichmentCollection`).

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
               ┌─────────────────────────────┴─────────────────────────────┐
               ▼                                                           ▼
    API / Schema / Data Model                                  SDK Ergonomics / Helpers /
    Endpoints / Protocol Types                                 Bug Fixes / Unit Tests
               │                                                           │
               ▼                                                           ▼
    Propose & Merge PR in                                      Fork & Submit PR in
   xyo-financial/specs repo                                   xyo-financial/sdk-java
 (https://github.com/xyo-financial/specs)                      (this repository)
               │                                                           │
               ▼                                                           ▼
  Tag Push / repository_dispatch                              Pass Quality Gates (mvn test)
  Auto-Regenerates openapi/
```

### Track A: API & Data Model Changes
If you need to add an endpoint, modify request/response schemas, update field constraints, or adjust HTTP headers:
1. **Do not modify this repository directly.**
2. Propose your changes in the canonical specification repository: [https://github.com/xyo-financial/specs](https://github.com/xyo-financial/specs).
3. Once specification changes are merged and tagged in `specs`, the automated cross-repository generation workflow triggers via `repository_dispatch` to regenerate `openapi/`.

### Track B: SDK Ergonomics, Helpers, Tests & Bug Fixes
If you are improving client ergonomics, adding utility helpers, optimizing performance, improving documentation, or adding unit tests:
1. Fork and create a branch from `main` (or `release-v2`).
2. Implement your changes within the [`xyo-sdk/`](xyo-sdk) module.
3. Add or update unit tests in [`xyo-sdk/src/test/java`](xyo-sdk/src/test/java).
4. Run the full validation test suite across all modules.
5. Submit a Pull Request to this repository for engineering review.

---

## ⚙️ Code Generation

### Automated Cross-Repository Synchronization
When a new release tag is pushed to [`xyo-financial/specs`](https://github.com/xyo-financial/specs), a GitHub Actions workflow dispatches a `repository_dispatch` event (`spec_tagged`, `spec_updated`) to this repository. The [`.github/workflows/generate.yml`](.github/workflows/generate.yml) workflow:
1. Checks out `xyo-financial/specs` at the tagged release (`${{ github.event.client_payload.tag || inputs.spec_tag || 'main' }}`).
2. Runs `@openapitools/openapi-generator-cli` to regenerate `openapi/`.
3. Cleans up generator noise and scaffolding files (`git_push.sh`, `.travis.yml`, Gradle files, `AndroidManifest.xml`, `README.md`, `docs/`, `api/`).
4. Installs the generated client to the local Maven cache and runs tests on `xyo-sdk`.
5. Commits the updated generated client automatically.

### Manual / Local Code Generation
If you need to regenerate the low-level `openapi/` layer locally:

#### Prerequisites
* **JDK 17 LTS or newer** (Eclipse Temurin 17 or 21 recommended)
* **Apache Maven 3.8+**
* **Node.js 18+ & npx** (for `@openapitools/openapi-generator-cli`)
* **xyo-financial/specs clone** (sibling directory `../specs/openapi.yml` or direct path)

#### Command
Run from the root of the Java SDK repository:

```bash
npx -y @openapitools/openapi-generator-cli generate \
  -i ../specs/openapi.yml \
  -g java \
  -o ./openapi \
  --additional-properties=groupId=com.xyo,artifactId=xyo-sdk,artifactVersion=2.0.0,library=native,invokerPackage=com.xyo.client,apiPackage=com.xyo.api,modelPackage=com.xyo.model \
  --global-property apiTests=false,modelTests=false,apiDocs=false,modelDocs=false
```

#### Configuration Breakdown:
* `-i ../specs/openapi.yml`: Path to the canonical OpenAPI 3.0+ specification.
* `-g java`: Target Java client generator.
* `-o ./openapi`: Destination directory for generated sources and POM.
* `library=native`: Enforces modern Java 11+ / 17+ `java.net.http.HttpClient` transport without third-party HTTP client bloat.
* `groupId=com.xyo`, `artifactId=xyo-sdk`, `artifactVersion=2.0.0`: Coordinates for the internal low-level artifact.
* `invokerPackage=com.xyo.client`: Package for HTTP transport, serialization, and authentication interceptors.
* `apiPackage=com.xyo.api`: Package for API operation classes (e.g., `EnrichmentApi`).
* `modelPackage=com.xyo.model`: Package for generated request/response DTOs.
* `--global-property apiTests=false,modelTests=false,apiDocs=false,modelDocs=false`: Suppresses generator skeleton documentation and test stubs in favor of repository-level tests.

#### Post-Generation Clean-Up
After code generation completes, remove unnecessary generator scaffolding files:

```bash
rm -f openapi/git_push.sh \
      openapi/.travis.yml \
      openapi/build.gradle \
      openapi/build.sbt \
      openapi/gradle.properties \
      openapi/gradlew \
      openapi/gradlew.bat \
      openapi/settings.gradle \
      openapi/README.md \
      openapi/src/main/AndroidManifest.xml
rm -rf openapi/.github \
       openapi/gradle \
       openapi/docs \
       openapi/api
```

> **⚠️ IMMUTABLE RULE:** Under no circumstances should files inside `openapi/` be modified or reformatted manually. Any manual edits will be overwritten in subsequent regeneration runs.

---

## 🛡 Quality Gates & Verification Standards

All Pull Requests must pass institutional quality gates before approval and merge:

### 1. Install Generated Client to Local Maven Cache
```bash
mvn clean install -q -DskipTests --file openapi/pom.xml
```

### 2. Multi-Module Compilation & Test Execution
Compile the wrapper SDK and execute the entire test suite:
```bash
# Compile wrapper layer
mvn clean compile -q --file xyo-sdk/pom.xml

# Run all unit and boundary tests
mvn test -B --file xyo-sdk/pom.xml
```

### 3. Example Application Verification
The standalone example application in [`example/`](example) must build and run cleanly against the compiled SDK:
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
- [ ] **Commit Hygiene:** Commits follow conventional commit formatting (e.g., `feat:`, `fix:`, `docs:`, `ci:`, `chore:`).

---

## 🔒 Security Vulnerabilities

If you discover a security vulnerability, please do not open a public GitHub issue. Follow the disclosure policy outlined in [`SECURITY.md`](SECURITY.md) or contact security directly at [security@syniol.com](mailto:security@syniol.com).
