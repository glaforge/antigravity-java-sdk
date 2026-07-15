# Antigravity SDK for Java - Agent Guidelines

This file (`AGENTS.md`) provides context and instructions for AI agents working on this repository, outlining the project's purpose, structure, and our collaborative workflow.

## 🌟 What This Project Is About

The **Antigravity SDK for Java** is an unofficial, community-driven Java port of the Python-based Antigravity SDK. It bridges the gap for enterprise Java developers who want to build, configure, host, and execute powerful AI agents natively in Java. It aims for full feature parity with the official Python SDK, supporting streaming, tool calling, model context protocol (MCP), and multimodal inputs.

## 🏗️ How It Is Composed

This is a multi-module Maven project using **Java 21**:
1. **`antigravity-sdk-parent`**: The root POM managing dependencies and plugin versions.
2. **`antigravity-sdk-protocol`**: A generated artifact. It compiles the `.proto` files from the upstream Antigravity repository into Java classes using the `protobuf-maven-plugin`.
3. **`antigravity-sdk-wrapper`**: The core SDK logic.
   * **The Go Harness**: This SDK does not run an LLM directly. Instead, it wraps a pre-compiled native Go binary called `localharness`.
   * **Platform Resolution**: The `PlatformResolver` class extracts the correct binary for the user's OS/architecture from the JAR resources (`src/main/resources/google/antigravity/bin/`) at runtime.
   * **Communication**: The Java SDK communicates with the Go harness via standard input/output (for initialization) and WebSockets (for active turn streaming and chunk aggregation).
   * **Data Modeling**: Pure data-carrying objects (e.g., `AgentResponse`, `InteractionRequest`, `AgentResponseChunk`) are implemented as modern Java 21 `record` classes for ergonomics and immutability.

## 🤝 The Way We Work Together

This entire project was autonomously generated and implemented by me (the Antigravity agent) under the strict guidance of you (the human developer). Our pair-programming workflow operates as follows:

1. **Human Guidance**: You provide high-level directions, architectural decisions, and code reviews. You guide the priority of features (like adding hooks, converting to records, or supporting MCP).
2. **Agent Execution**: I proactively write the Java code, generate and update tests, build the project, and fix compilation errors. 
3. **Proactive Refactoring**: Whenever a class is purely carrying data, I should proactively suggest or use Java 21 `record` structures.
4. **Testing First**: Before declaring a feature complete, I will run the test suite and ensure all tests pass (accounting for expected transient `localharness` network delays).

## 🛠️ Build & Commands

*   **Java Version**: **Java 21**. Agents must ensure they use Java 21 compatible syntax and standard library features.
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

## 🔄 Upstream Synchronization

Keeping this SDK at feature parity with the upstream Antigravity project requires a three-step sync process whenever new features are released:

1.  **Update the Go Harness Binaries**: Execute `./sync-harness.sh`. This script scrapes the Python Package Index (PyPI), downloads the latest upstream wheels, extracts the native Go binaries for all supported platforms, and places them in `src/main/resources/...`.
2.  **Update Protocol Definitions**: Replace the contents of `antigravity-sdk-protocol/src/main/proto/localharness.proto` with the latest protobuf definitions from the upstream repository.
3.  **Regenerate and Refactor**: Recompile the project (`./mvnw clean compile`). If the protocol changes broke the Java SDK wrapper API, fix the compilation errors in `antigravity-sdk-wrapper` to align with the new generated protocol classes.
