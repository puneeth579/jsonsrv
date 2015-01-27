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
public abstract class JsonAction<I, O> {

    private final Class<I> inputClass;
    private final Class<O> outputClass;
    private String inputSchema;
    private String outputSchema;
    private JsonSchema validationInputSchema;

    public JsonAction() {
        Class<?>[] types = TypeResolver.resolveRawArguments(JsonAction.class, getClass());
        this.inputClass = (Class<I>) types[0];
        this.outputClass = (Class<O>) types[1];
    }

    final void init(JsonHelper jsonHelper) throws Exception {
        this.inputSchema = jsonHelper.getSchemaHelper().getSchemaString(getInputClass());
        this.outputSchema = jsonHelper.getSchemaHelper().getSchemaString(JsonResponse.class).replace("\"value\":{\"type\":\"any\"}", "\"value\":" + jsonHelper.getSchemaHelper().getSchemaString(getOutputClass()));
        try {
            this.validationInputSchema = jsonHelper.getSchemaHelper().getSchema(jsonHelper.getSchemaHelper().getSchemaString(this.inputClass));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    final JsonSchema getValidationInputSchema() {
        return validationInputSchema;
    }

    final String getInputSchema() {
        return inputSchema;
    }

    final String getOutputSchema() {
        return outputSchema;
    }

    final Class<I> getInputClass() {
        return inputClass;
    }

    final Class<O> getOutputClass() {
        return outputClass;
    }

    protected boolean isCacheable() {
        return false;
    }

    protected abstract O execute(I input) throws Exception;
}
