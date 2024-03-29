/*
 * Copyright 2015 brutusin.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.brutusin.jsonsrv;

import net.jodah.typetools.TypeResolver;
import org.brutusin.commons.json.spi.JsonCodec;
import org.brutusin.commons.json.spi.JsonSchema;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public final class JsonService<I, O> {

    private final String id;
    private final JsonAction<I, O> action;
    private final String description;

    private final String inputSchema;
    private final String outputSchema;
    private final JsonSchema validationInputSchema;
    private final Class<I> inputClass;
    private final Class<O> outputClass;

    public JsonService(String id, JsonAction action, String description) {
        this.id = id;
        this.action = action;
        this.description = description;
        Class<?>[] types = TypeResolver.resolveRawArguments(JsonAction.class, action.getClass());
        this.inputClass = (Class<I>) types[0];
        this.outputClass = (Class<O>) types[1];
        this.inputSchema = JsonCodec.getInstance().getSchemaString(this.inputClass);
        this.outputSchema = JsonCodec.getInstance().getSchemaString(this.outputClass);
        try {
            this.validationInputSchema = JsonCodec.getInstance().parseSchema(JsonCodec.getInstance().getSchemaString(this.inputClass));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public String getId() {
        return id;
    }

    public JsonAction getAction() {
        return action;
    }

    public String getInputSchema() {
        return inputSchema;
    }

    public String getOutputSchema() {
        return outputSchema;
    }

    public JsonSchema getValidationInputSchema() {
        return validationInputSchema;
    }

    public Class<I> getInputClass() {
        return inputClass;
    }

    public Class<O> getOutputClass() {
        return outputClass;
    }

    public String getDescription() {
        return description;
    }
}
