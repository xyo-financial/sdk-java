# Contributing to XYO.Financial Java SDK

First of all, thank you for taking the time to contribute!

## Development Setup

1. Make sure you have Java 17 and Maven installed.
2. Clone the repository: `git clone https://github.com/syniol/xyo-sdk-java.git`
3. Run `mvn clean install` to build the SDK and run all tests.

## Coding Standards

- The codebase is written in standard Java.
- We use Jackson for JSON mapping and `java.net.http.HttpClient` for networking. Do not introduce legacy networking dependencies like Apache HttpClient.
- Please write unit tests for any new features using JUnit 5.
- Keep backwards compatibility in mind when changing the public API.

## Submitting a Pull Request

1. Fork the repository and create your branch from `main`.
2. If you've added code that should be tested, add tests.
3. If you've changed APIs, update the documentation.
4. Ensure the test suite passes: `mvn clean test`.
5. Open your pull request.

Thank you!
