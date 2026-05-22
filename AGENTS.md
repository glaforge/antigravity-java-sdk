# Antigravity SDK for Java - Agent Guidelines

This file (`AGENTS.md`) provides context and instructions for AI agents working on this repository.

## 🏛️ Architecture Context

*   **The Go Harness**: This SDK does not run an LLM directly. Instead, it wraps a pre-compiled native Go binary called `localharness`.
*   **Platform Resolution**: The `PlatformResolver` class extracts the correct binary for the user's OS/architecture from the JAR resources (`src/main/resources/google/antigravity/bin/`) at runtime.
*   **Communication**: The Java SDK communicates with the Go harness via standard input/output (for initialization) and WebSockets (for active turn streaming and chunk aggregation).

## 🛠️ Build & Commands

*   **Java Version**: This project uses **Java 21**. Agents must ensure they use Java 21 compatible syntax and standard library features.
*   **Maven Wrapper**: Always use the provided Maven wrapper (`./mvnw`). Do not rely on globally installed `mvn` or `gradle`.
*   **Compilation**: `./mvnw clean compile`
*   **Testing**: `./mvnw test`
*   **Code Formatting**: `./mvnw spotless:apply`

## 🎨 Coding Standards

*   **No FQNs**: Never use Fully Qualified Names (FQNs) directly in the code (e.g., `java.util.List<String> list = ...`). Always import the classes at the top of the file.
*   **Code Formatting**: The project uses the Maven Spotless plugin with the Eclipse formatter. Always run `./mvnw spotless:apply` and ensure the code passes before committing.
*   **License Headers**: Spotless will automatically inject the Apache 2.0 license header. Do not manually add copyright headers.

## 🧪 Testing Guidelines

*   **Framework**: We use JUnit 6.
*   **Asynchronous Assertions**: Because agent interactions happen asynchronously over WebSockets and `CompletableFuture`s, **never use `Thread.sleep()`** for waiting on agent responses in tests. Instead, always use `Awaitility` (e.g., `await().atMost(...)`).

## 🔄 Maintenance Scripts

*   **`sync-harness.sh`**: If the underlying `localharness` binary needs to be updated to match upstream changes, execute `./sync-harness.sh`. This script will scrape the Python Package Index (PyPI), download the upstream wheels, extract the native Go binaries for all supported platforms, and place them in the correct `src/main/resources/...` directories.
