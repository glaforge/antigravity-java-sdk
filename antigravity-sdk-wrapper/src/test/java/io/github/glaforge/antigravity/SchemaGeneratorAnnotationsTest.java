/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.glaforge.antigravity;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.glaforge.antigravity.tools.Param;
import io.github.glaforge.antigravity.tools.SchemaGenerator;
import io.github.glaforge.antigravity.tools.Tool;
import io.github.glaforge.antigravity.tools.ToolDefinition;
import io.github.glaforge.antigravity.tools.ToolRegistry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class SchemaGeneratorAnnotationsTest {

	// --- Simulated Tier 3 ecosystem annotations (matching LangChain4j, Spring AI,
	// MCP, Swagger, Nullable) ---

	@Target({ElementType.TYPE, ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Description {
		String[] value();
	}

	@Target(ElementType.PARAMETER)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface P {
		String value();
		boolean required() default true;
	}

	@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.RECORD_COMPONENT})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface ToolParam {
		String description() default "";
		boolean required() default true;
	}

	@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.RECORD_COMPONENT})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface McpToolParam {
		String description() default "";
		boolean required() default true;
	}

	@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.RECORD_COMPONENT})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface McpArg {
		String name() default "";
		String description() default "";
		boolean required() default true;
	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface McpTool {
		String name() default "";
		String description() default "";
	}

	@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.RECORD_COMPONENT})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface ToolArg {
		String name() default "";
		String description() default "";
		boolean required() default true;
	}

	public enum RequiredMode {
		AUTO, REQUIRED, NOT_REQUIRED
	}

	@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER,
			ElementType.RECORD_COMPONENT})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Schema {
		String name() default "";
		String description() default "";
		String defaultValue() default "";
		RequiredMode requiredMode() default RequiredMode.AUTO;
		boolean required() default false;
		boolean hidden() default false;
	}

	@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Nullable {
	}

	// --- Tier 1: Jackson Annotations ---

	@JsonClassDescription("Priority level of a ticket")
	public enum Priority {
		@JsonProperty("low")
		LOW, @JsonProperty("high")
		HIGH
	}

	@JsonClassDescription("Represents a customer support ticket")
	public record SupportTicket(
			@JsonProperty("ticket_id") @JsonPropertyDescription("Unique ticket identifier") String ticketId,
			@JsonProperty(value = "severity", defaultValue = "low") @JsonPropertyDescription("Ticket severity") Priority severity,
			@JsonProperty(required = false) @JsonPropertyDescription("Optional assignee email") String assignee,
			@JsonIgnore String internalSecret) {
	}

	@Test
	public void testTier1JacksonAnnotationsSchemaAndDeserialization() throws Exception {
		ObjectNode schema = SchemaGenerator.generateSchema(SupportTicket.class);

		assertEquals("object", schema.get("type").asText());
		assertEquals("Represents a customer support ticket", schema.get("description").asText());

		JsonNode props = schema.get("properties");
		assertNotNull(props);
		assertTrue(props.has("ticket_id"));
		assertFalse(props.has("ticketId"));
		assertEquals("Unique ticket identifier", props.get("ticket_id").get("description").asText());

		assertTrue(props.has("severity"));
		assertEquals("Ticket severity", props.get("severity").get("description").asText());
		assertEquals("low", props.get("severity").get("default").asText());
		assertEquals(List.of("low", "high"),
				SchemaGenerator.getMapper().convertValue(props.get("severity").get("enum"), List.class));

		assertTrue(props.has("assignee"));
		assertEquals("Optional assignee email", props.get("assignee").get("description").asText());

		// @JsonIgnore field must be excluded
		assertFalse(props.has("internalSecret"));

		// Only ticket_id is required (severity has defaultValue, assignee has
		// required=false)
		List<?> required = SchemaGenerator.getMapper().convertValue(schema.get("required"), List.class);
		assertEquals(List.of("ticket_id"), required);

		// Verify AgentResponse.getStructuredOutput deserializes JSON matching the
		// schema
		AgentResponse response = new AgentResponse("{\"ticket_id\":\"T-100\",\"severity\":\"high\"}", "", null);
		SupportTicket ticket = response.getStructuredOutput(SupportTicket.class);
		assertEquals("T-100", ticket.ticketId());
		assertEquals(Priority.HIGH, ticket.severity());
		assertNull(ticket.assignee());
	}

	// --- Tier 2: Expanded @Param Annotation, Optional, Map, Set, and Recursion ---

	@Param(description = "A search query payload")
	public record SearchFilter(@Param(name = "search_term", description = "Keywords to search for") String searchTerm,
			@Param(description = "Max items to return", required = false) Integer limit, Optional<String> category,
			Set<String> tags, Map<String, Integer> boosts) {
	}

	public static class CategoryNode {
		@Param(description = "Category name")
		public String name;
		public List<CategoryNode> children;
	}

	@Test
	public void testTier2ParamAnnotationAndJavaTypes() throws Exception {
		ObjectNode schema = SchemaGenerator.generateSchema(SearchFilter.class);

		assertEquals("object", schema.get("type").asText());
		assertEquals("A search query payload", schema.get("description").asText());

		JsonNode props = schema.get("properties");
		assertTrue(props.has("search_term"));
		assertEquals("Keywords to search for", props.get("search_term").get("description").asText());

		assertTrue(props.has("limit"));
		assertEquals("integer", props.get("limit").get("type").asText());
		assertEquals("Max items to return", props.get("limit").get("description").asText());

		// Optional<String> unwraps to string and is not required
		assertTrue(props.has("category"));
		assertEquals("string", props.get("category").get("type").asText());

		// Set<String> maps to array of string
		assertTrue(props.has("tags"));
		assertEquals("array", props.get("tags").get("type").asText());
		assertEquals("string", props.get("tags").get("items").get("type").asText());

		// Map<String, Integer> maps to object with integer additionalProperties
		assertTrue(props.has("boosts"));
		assertEquals("object", props.get("boosts").get("type").asText());
		assertEquals("integer", props.get("boosts").get("additionalProperties").get("type").asText());

		List<?> required = SchemaGenerator.getMapper().convertValue(schema.get("required"), List.class);
		assertEquals(List.of("search_term", "tags", "boosts"), required);

		// 1. Verify @Param(name = "search_term") and omitted Optional<String>
		// deserialize to Optional.empty() (not null!)
		AgentResponse responseAbsent = new AgentResponse(
				"{\"search_term\":\"antigravity\",\"tags\":[\"java\"],\"boosts\":{\"title\":2}}", "", null);
		SearchFilter parsedAbsent = responseAbsent.getStructuredOutput(SearchFilter.class);
		assertEquals("antigravity", parsedAbsent.searchTerm());
		assertEquals(Optional.empty(), parsedAbsent.category());
		assertEquals(Set.of("java"), parsedAbsent.tags());
		assertEquals(Map.of("title", 2), parsedAbsent.boosts());

		// 2. Verify populated Optional<String> deserializes from plain JSON string
		AgentResponse responsePresent = new AgentResponse(
				"{\"search_term\":\"antigravity\",\"category\":\"sdk\",\"tags\":[\"java\"],\"boosts\":{\"title\":2}}",
				"", null);
		SearchFilter parsedPresent = responsePresent.getStructuredOutput(SearchFilter.class);
		assertEquals(Optional.of("sdk"), parsedPresent.category());

		// 3. Verify OptionalSerializer writes plain string / null on serialization
		String serialized = SchemaGenerator.getMapper().writeValueAsString(parsedPresent);
		JsonNode serializedNode = SchemaGenerator.getMapper().readTree(serialized);
		assertEquals("sdk", serializedNode.get("category").asText());
	}

	@Test
	public void testRecursiveTypeDoesNotStackOverflow() {
		ObjectNode schema = SchemaGenerator.generateSchema(CategoryNode.class);
		assertEquals("object", schema.get("type").asText());
		assertEquals("Category name", schema.get("properties").get("name").get("description").asText());
		assertEquals("array", schema.get("properties").get("children").get("type").asText());
		assertEquals("object", schema.get("properties").get("children").get("items").get("type").asText());
	}

	// --- Tier 3: LangChain4j, Spring AI, MCP, Swagger, and @Nullable Annotations
	// ---

	@Description({"A book review", "extracted from text."})
	public record PolyglotReview(@P("Title of the book") String title,
			@ToolParam(description = "Rating from 1 to 5", required = false) Integer rating,
			@McpToolParam(description = "Reviewer summary", required = true) String summary,
			@ToolArg(name = "author_name", description = "Book author", required = false) String author,
			@McpArg(name = "isbn_code", description = "Book ISBN", required = false) String isbn,
			@Schema(name = "pub_year", description = "Publication year", defaultValue = "2026", requiredMode = RequiredMode.NOT_REQUIRED) int year,
			@Schema(hidden = true) String hiddenNotes, @Nullable String optionalTag) {
	}

	@Test
	public void testTier3PolyglotAnnotationsOnRecord() throws Exception {
		ObjectNode schema = SchemaGenerator.generateSchema(PolyglotReview.class);

		assertEquals("A book review extracted from text.", schema.get("description").asText());

		JsonNode props = schema.get("properties");
		assertEquals("Title of the book", props.get("title").get("description").asText());
		assertEquals("Rating from 1 to 5", props.get("rating").get("description").asText());
		assertEquals("Reviewer summary", props.get("summary").get("description").asText());

		// @ToolArg(name = "author_name")
		assertTrue(props.has("author_name"));
		assertEquals("Book author", props.get("author_name").get("description").asText());

		// @McpArg(name = "isbn_code")
		assertTrue(props.has("isbn_code"));
		assertEquals("Book ISBN", props.get("isbn_code").get("description").asText());

		// @Schema(name = "pub_year", defaultValue = "2026")
		assertTrue(props.has("pub_year"));
		assertEquals("Publication year", props.get("pub_year").get("description").asText());
		assertEquals("2026", props.get("pub_year").get("default").asText());

		// @Schema(hidden = true)
		assertFalse(props.has("hiddenNotes"));

		// Required check: only title and summary are required
		List<?> required = SchemaGenerator.getMapper().convertValue(schema.get("required"), List.class);
		assertEquals(List.of("title", "summary"), required);

		// Deserialization honors @ToolArg(name = "author_name"), @McpArg(name =
		// "isbn_code"), and @Schema(name = "pub_year")
		AgentResponse response = new AgentResponse(
				"{\"title\":\"Dune\",\"summary\":\"Great sci-fi\",\"author_name\":\"Frank Herbert\",\"isbn_code\":\"978-0441172719\",\"pub_year\":1965}",
				"", null);
		PolyglotReview review = response.getStructuredOutput(PolyglotReview.class);
		assertEquals("Dune", review.title());
		assertEquals("Frank Herbert", review.author());
		assertEquals("978-0441172719", review.isbn());
		assertEquals(1965, review.year());
	}

	// --- Non-Record POJO Getter Annotations & Enum Custom Name Introspection ---

	@Param(description = "Release channel")
	public enum Channel {
		@Param(name = "stable_channel")
		STABLE, @Schema(name = "canary_channel")
		CANARY
	}

	public static class CustomerProfilePojo {
		private String customerName;
		private boolean vip;
		private String notes;
		private String secretToken;
		private Channel channel;

		@JsonProperty("customer_name")
		@JsonPropertyDescription("Full name of the customer")
		public String getCustomerName() {
			return customerName;
		}

		public void setCustomerName(String customerName) {
			this.customerName = customerName;
		}

		@Param(name = "is_vip", description = "Whether customer has VIP status", required = false)
		public boolean isVip() {
			return vip;
		}

		public void setVip(boolean vip) {
			this.vip = vip;
		}

		@Nullable
		@Schema(description = "Optional account notes")
		public String getNotes() {
			return notes;
		}

		public void setNotes(String notes) {
			this.notes = notes;
		}

		@JsonIgnore
		public String getSecretToken() {
			return secretToken;
		}

		public void setSecretToken(String secretToken) {
			this.secretToken = secretToken;
		}

		@Param(description = "Preferred release channel")
		public Channel getChannel() {
			return channel;
		}

		public void setChannel(Channel channel) {
			this.channel = channel;
		}
	}

	@Test
	public void testPojoGetterAnnotationsAndCustomEnumNames() throws Exception {
		ObjectNode schema = SchemaGenerator.generateSchema(CustomerProfilePojo.class);
		JsonNode props = schema.get("properties");

		assertTrue(props.has("customer_name"));
		assertEquals("Full name of the customer", props.get("customer_name").get("description").asText());

		assertTrue(props.has("is_vip"));
		assertEquals("boolean", props.get("is_vip").get("type").asText());
		assertEquals("Whether customer has VIP status", props.get("is_vip").get("description").asText());

		assertTrue(props.has("notes"));
		assertEquals("Optional account notes", props.get("notes").get("description").asText());

		assertFalse(props.has("secretToken"));

		assertTrue(props.has("channel"));
		assertEquals("Preferred release channel", props.get("channel").get("description").asText());
		assertEquals(List.of("stable_channel", "canary_channel"),
				SchemaGenerator.getMapper().convertValue(props.get("channel").get("enum"), List.class));

		List<?> required = SchemaGenerator.getMapper().convertValue(schema.get("required"), List.class);
		assertEquals(List.of("customer_name", "channel"), required);

		// Verify Jackson deserializes POJO getters and @Param/@Schema enum names
		AgentResponse response = new AgentResponse(
				"{\"customer_name\":\"Ada Lovelace\",\"is_vip\":true,\"channel\":\"canary_channel\"}", "", null);
		CustomerProfilePojo profile = response.getStructuredOutput(CustomerProfilePojo.class);
		assertEquals("Ada Lovelace", profile.getCustomerName());
		assertTrue(profile.isVip());
		assertEquals(Channel.CANARY, profile.getChannel());
	}

	// --- ToolRegistry Integration with Tiers 1, 2, 3, Optional<T>, and Complex
	// Record Args ---

	public static class PolyglotToolService {
		@Tool(name = "search_items", description = "Searches items with optional and annotated parameters")
		public String search(@JsonProperty("query_text") @JsonPropertyDescription("The search query") String query,
				@Param(name = "max_results", description = "Max results", required = false) Optional<Integer> maxResults,
				@Schema(defaultValue = "relevance") @ToolParam(description = "Sort order", required = false) String sort) {
			return query + "|" + maxResults.orElse(-1) + "|" + sort;
		}

		@McpTool(name = "mcp_echo", description = "Echoes message using MCP annotations")
		public String mcpEcho(@McpToolParam(description = "Message to echo") String msg,
				@P(value = "Optional prefix", required = false) Optional<String> prefix,
				@McpArg(name = "repeat_count", description = "Repeat count", required = false) Optional<Integer> repeat) {
			return (prefix.orElse("") + msg).repeat(repeat.orElse(1));
		}

		@Tool(name = "filter_catalog", description = "Filters catalog with a complex annotated record")
		public String filterCatalog(@Param(name = "filter", description = "Search filter") SearchFilter filter) {
			return filter.searchTerm() + ":" + filter.category().orElse("all") + ":" + filter.tags().size();
		}
	}

	@Test
	public void testToolRegistryWithPolyglotAnnotationsAndOptionalParams() throws Exception {
		ToolRegistry registry = new ToolRegistry();
		registry.registerToolsFromObject(new PolyglotToolService());

		List<ToolDefinition> defs = registry.getToolDefinitions();
		assertEquals(3, defs.size());

		ToolDefinition searchDef = defs.stream().filter(d -> d.getName().equals("search_items")).findFirst()
				.orElseThrow();
		JsonNode searchSchema = SchemaGenerator.getMapper().readTree(searchDef.getParametersJsonSchema());

		assertEquals("The search query", searchSchema.get("properties").get("query_text").get("description").asText());
		assertEquals("integer", searchSchema.get("properties").get("max_results").get("type").asText());
		assertEquals("Max results", searchSchema.get("properties").get("max_results").get("description").asText());
		assertEquals("relevance", searchSchema.get("properties").get("sort").get("default").asText());

		List<?> searchReq = SchemaGenerator.getMapper().convertValue(searchSchema.get("required"), List.class);
		assertEquals(List.of("query_text"), searchReq);

		// Execute with only required parameter (optional Optional<Integer> becomes
		// Optional.empty(), sort uses defaultValue)
		JsonNode args1 = SchemaGenerator.getMapper().readTree("{\"query_text\": \"books\"}");
		assertEquals("{\"result\":\"books|-1|relevance\"}", registry.execute("search_items", args1, null));

		// Execute with all parameters provided
		JsonNode args2 = SchemaGenerator.getMapper()
				.readTree("{\"query_text\": \"books\", \"max_results\": 10, \"sort\": \"date\"}");
		assertEquals("{\"result\":\"books|10|date\"}", registry.execute("search_items", args2, null));

		// Verify @McpTool registration and execution with @McpArg
		ToolDefinition mcpDef = defs.stream().filter(d -> d.getName().equals("mcp_echo")).findFirst().orElseThrow();
		assertEquals("Echoes message using MCP annotations", mcpDef.getDescription());
		JsonNode mcpArgs = SchemaGenerator.getMapper()
				.readTree("{\"msg\": \"Hi!\", \"prefix\": \"MCP: \", \"repeat_count\": 2}");
		assertEquals("{\"result\":\"MCP: Hi!MCP: Hi!\"}", registry.execute("mcp_echo", mcpArgs, null));

		// Verify complex record tool execution with @Param(name = "search_term") and
		// nested Optional<String>
		JsonNode filterArgsPresent = SchemaGenerator.getMapper().readTree(
				"{\"filter\": {\"search_term\": \"laptop\", \"category\": \"electronics\", \"tags\": [\"pro\"], \"boosts\": {}}}");
		assertEquals("{\"result\":\"laptop:electronics:1\"}",
				registry.execute("filter_catalog", filterArgsPresent, null));

		JsonNode filterArgsAbsent = SchemaGenerator.getMapper()
				.readTree("{\"filter\": {\"search_term\": \"laptop\", \"tags\": [\"pro\", \"air\"], \"boosts\": {}}}");
		assertEquals("{\"result\":\"laptop:all:2\"}", registry.execute("filter_catalog", filterArgsAbsent, null));
	}

	@Test
	public void testResponseSchemaAndToolDefinitionBuilderIntegration() throws Exception {
		AgentConfig config = AgentConfig.builder().instructions("Extract support ticket")
				.responseSchema(SupportTicket.class).build();
		JsonNode responseSchema = SchemaGenerator.getMapper().readTree(config.getResponseSchema());
		assertEquals("Represents a customer support ticket", responseSchema.get("description").asText());
		assertTrue(responseSchema.get("properties").has("ticket_id"));
		assertFalse(responseSchema.get("properties").has("internalSecret"));

		ToolDefinition toolDef = ToolDefinition.builder().name("search").description("Search filter tool")
				.parametersSchema(SearchFilter.class).build();
		JsonNode paramSchema = SchemaGenerator.getMapper().readTree(toolDef.getParametersJsonSchema());
		assertEquals("A search query payload", paramSchema.get("description").asText());
		assertTrue(paramSchema.get("properties").has("search_term"));
		assertEquals(List.of("search_term", "tags", "boosts"),
				SchemaGenerator.getMapper().convertValue(paramSchema.get("required"), List.class));
	}
}
