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

import com.github.fge.jsonschema.main.JsonSchema;
import net.jodah.typetools.TypeResolver;
import org.brutusin.commons.json.JsonHelper;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public final class JsonService<I, O> {

    private final String id;
    private final JsonAction<I, O> action;

    private final String inputSchema;
    private final String outputSchema;
    private final JsonSchema validationInputSchema;
    private final Class<I> inputClass;
    private final Class<O> outputClass;

    public JsonService(String id, JsonAction action, JsonHelper jsonHelper) {
        this.id = id;
        this.action = action;
         Class<?>[] types = TypeResolver.resolveRawArguments(JsonAction.class, action.getClass());
        this.inputClass = (Class<I>) types[0];
        this.outputClass = (Class<O>) types[1];
        this.inputSchema = jsonHelper.getSchemaHelper().getSchemaString(this.inputClass);
        this.outputSchema = jsonHelper.getSchemaHelper().getSchemaString(JsonResponse.class).replace("\"value\":{\"type\":\"any\"}", "\"value\":" + jsonHelper.getSchemaHelper().getSchemaString(this.outputClass));
        try {
            this.validationInputSchema = jsonHelper.getSchemaHelper().getSchema(jsonHelper.getSchemaHelper().getSchemaString(this.inputClass));
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
}
