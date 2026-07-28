# Streaming & Reactive Integration — Antigravity SDK for Java

This document explains token streaming mechanisms and reactive framework integration using Java 9 Reactive Streams (`Flow.Publisher`).

---

## 1. Functional Callback Streaming (`chatStream`)

`chatStream` emits `AgentResponseChunk` instances incrementally as tokens are produced by the LLM.

```java
import io.github.glaforge.antigravity.Agent;
import io.github.glaforge.antigravity.AgentConfig;
import io.github.glaforge.antigravity.AgentResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

AgentConfig config = AgentConfig.builder()
    .instructions("Generate a comprehensive technical summary.")
    .build();

try (Agent agent = new Agent(config)) {
    CompletableFuture<AgentResponse> future = agent.chatStream(
        "Summarize Kafka consumer group rebalancing.",
        chunk -> System.out.print(chunk.textDelta())
    );
    
    // Wait for stream completion
    AgentResponse fullResponse = future.get(120, TimeUnit.SECONDS);
    System.out.println("\nTotal tokens used: " + fullResponse.usageMetadata().totalTokenCount());
}
```

---

## 2. Reactive Streams (`Flow.Publisher`)

`chatPublisher` returns a standard `java.util.concurrent.Flow.Publisher<AgentResponseChunk>`, making the SDK natively compatible with modern reactive streams adapters.

### Spring WebFlux / Project Reactor

```java
import io.github.glaforge.antigravity.Agent;
import io.github.glaforge.antigravity.AgentResponseChunk;
import java.util.concurrent.Flow;
import reactor.core.publisher.Flux;

try (Agent agent = new Agent(config)) {
    Flow.Publisher<AgentResponseChunk> publisher = agent.chatPublisher("Write a Spring Boot controller.");
    
    // Convert Java Flow.Publisher to Project Reactor Flux
    Flux<AgentResponseChunk> chunkFlux = Flux.from(publisher);
    
    chunkFlux
        .map(AgentResponseChunk::textDelta)
        .doOnNext(System::print)
        .blockLast();
}
```

### RxJava 3

```java
import io.github.glaforge.antigravity.AgentResponseChunk;
import java.util.concurrent.Flow;
import io.reactivex.rxjava3.core.Flowable;

try (Agent agent = new Agent(config)) {
    Flow.Publisher<AgentResponseChunk> publisher = agent.chatPublisher("Explain Java 21 virtual threads.");
    
    Flowable<AgentResponseChunk> flowable = Flowable.fromPublisher(publisher);
    
    flowable
        .subscribe(
            chunk -> System.out.print(chunk.textDelta()),
            Throwable::printStackTrace,
            () -> System.out.println("\n[Stream Completed]")
        );
}
```

### Standard Java 9 `Flow.Subscriber`

```java
import io.github.glaforge.antigravity.AgentResponseChunk;
import java.util.concurrent.Flow;

publisher.subscribe(new Flow.Subscriber<>() {
    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(AgentResponseChunk chunk) {
        if (chunk.textDelta() != null) {
            System.out.print(chunk.textDelta());
        }
    }

    @Override
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    @Override
    public void onComplete() {
        System.out.println("\nStream finished.");
    }
});
```

---

## 3. Sugared Reasoning & Tool Stream (`AgentStream`)

`streamChat` returns an `AgentStream` object that exposes separate reactive channels for model thoughts, tool dispatch events, raw chunks, and the final response future.

```java
import io.github.glaforge.antigravity.Agent;
import io.github.glaforge.antigravity.AgentResponse;
import io.github.glaforge.antigravity.AgentStream;
import reactor.core.publisher.Flux;
import java.util.concurrent.TimeUnit;

try (Agent agent = new Agent(config)) {
    AgentStream stream = agent.streamChat("Calculate optimal partition count for 500GB daily ingestion.");

    // Channel 1: Stream model internal reasoning/thoughts
    Flux.from(stream.thoughts())
        .subscribe(thought -> System.out.println("[Thought]: " + thought));

    // Channel 2: Stream tool invocation events
    Flux.from(stream.toolCalls())
        .subscribe(call -> System.out.println("[Executing Tool]: " + call.name()));

    // Channel 3: Wait for complete turn response
    AgentResponse finalResponse = stream.result().get(120, TimeUnit.SECONDS);
    System.out.println("Final Output: " + finalResponse.text());
}
```
