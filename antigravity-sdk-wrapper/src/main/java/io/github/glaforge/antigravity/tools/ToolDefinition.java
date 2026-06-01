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
package io.github.glaforge.antigravity.tools;

import io.github.glaforge.antigravity.localharness.Tool;

/**
 * A definition of a tool, containing its name, description, and JSON schema for its parameters.
 * This class wraps the internal protobuf representation.
 */
public class ToolDefinition {
    private final String name;
    private final String description;
    private final String parametersJsonSchema;
    
    private ToolDefinition(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.parametersJsonSchema = builder.parametersJsonSchema;
    }
    
    /**
     * Creates a new builder for ToolDefinition.
     * @return a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * @return the name of the tool
     */
    public String getName() {
        return name;
    }

    /**
     * @return the description of the tool
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the JSON schema for the tool parameters
     */
    public String getParametersJsonSchema() {
        return parametersJsonSchema;
    }
    
    /**
     * Converts this definition to the internal protobuf representation.
     * @return the protobuf Tool
     */
    public Tool toProtobuf() {
        Tool.Builder builder = Tool.newBuilder();
        if (this.name != null) {
            builder.setName(this.name);
        }
        if (this.description != null) {
            builder.setDescription(this.description);
        }
        if (this.parametersJsonSchema != null) {
            builder.setParametersJsonSchema(this.parametersJsonSchema);
        }
        return builder.build();
    }
    
    /**
     * Builder for ToolDefinition.
     */
    public static class Builder {
        private String name;
        private String description;
        private String parametersJsonSchema;
        
        /**
         * Sets the name of the tool.
         * @param name the name
         * @return this builder
         */
        public Builder name(String name) { 
            this.name = name; 
            return this; 
        }
        
        /**
         * Sets the description of the tool.
         * @param description the description
         * @return this builder
         */
        public Builder description(String description) { 
            this.description = description; 
            return this; 
        }
        
        /**
         * Sets the JSON schema of the tool parameters.
         * @param schema the schema
         * @return this builder
         */
        public Builder parametersJsonSchema(String schema) { 
            this.parametersJsonSchema = schema; 
            return this; 
        }
        
        /**
         * Builds the ToolDefinition.
         * @return the ToolDefinition
         */
        public ToolDefinition build() { 
            return new ToolDefinition(this); 
        }
    }
}
