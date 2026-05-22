package io.github.glaforge.antigravity;

import io.github.glaforge.antigravity.hooks.*;
import io.github.glaforge.antigravity.tools.AntigravityTool;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.awaitility.Awaitility.await;

public class HooksTest {

    public static class EchoTool {
        @AntigravityTool(description = "Echoes the input string")
        public String echo(String message) {
            return "{\"result\": \"ECHO: " + message + "\"}";
        }
    }

    @Test
    public void testLifecycleHooksOrder() throws Exception {
        List<String> events = new ArrayList<>();

        OnSessionStartHook startHook = () -> {
            events.add("start");
            return CompletableFuture.completedFuture(null);
        };
        OnSessionEndHook endHook = () -> {
            events.add("end");
            return CompletableFuture.completedFuture(null);
        };
        PreTurnHook preTurnHook = (prompt) -> {
            events.add("pre_turn:" + prompt);
            return CompletableFuture.completedFuture(HookResult.allowed());
        };
        PostTurnHook postTurnHook = (resp) -> {
            events.add("post_turn");
            return CompletableFuture.completedFuture(null);
        };
        PreToolCallDecideHook preToolHook = (call) -> {
            events.add("pre_tool:" + call.name());
            return CompletableFuture.completedFuture(HookResult.allowed());
        };
        PostToolCallHook postToolHook = (call, result) -> {
            events.add("post_tool:" + result);
            return CompletableFuture.completedFuture(null);
        };

        AgentConfig config = AgentConfig.builder()
                .modelName("models/gemini-2.5-flash")
                .persona("You are a helpful assistant. If the user asks you to echo, you must call the echo tool EXACTLY ONCE. After the tool returns, immediately reply to the user with the result and finish the turn.")
                .addTool(new EchoTool())
                .addHook(startHook)
                .addHook(endHook)
                .addHook(preTurnHook)
                .addHook(postTurnHook)
                .addHook(preToolHook)
                .addHook(postToolHook)
                .build();

        try (AntigravityAgent agent = new AntigravityAgent(config)) {
            AgentResponse response = agent.chat("Say hello").join();
            assertNotNull(response.getText());
        }

        System.out.println("Events: " + events);

        assertEquals("start", events.get(0));
        assertEquals("pre_turn:Say hello", events.get(1));
        assertEquals("pre_tool:echo", events.get(2));
        assertTrue(events.get(3).startsWith("post_tool:{\"result\": \"ECHO: hello\"}"));
        assertEquals("post_turn", events.get(4));
        assertEquals("end", events.get(5));
    }
}
