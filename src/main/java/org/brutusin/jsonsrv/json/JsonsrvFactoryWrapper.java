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
package org.brutusin.jsonsrv.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.module.jsonSchema.factories.FormatVisitorFactory;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import com.fasterxml.jackson.module.jsonSchema.factories.VisitorContext;
import com.fasterxml.jackson.module.jsonSchema.factories.WrapperFactory;

/**
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class JsonsrvFactoryWrapper extends SchemaFactoryWrapper {

    private final WrapperFactory wrapperFactory = new WrapperFactory() {
        @Override
        public SchemaFactoryWrapper getWrapper(SerializerProvider p) {
            return new JsonsrvFactoryWrapper(p);
        }

        @Override
        public SchemaFactoryWrapper getWrapper(SerializerProvider provider, VisitorContext rvc) {
            JsonsrvFactoryWrapper wrapper = new JsonsrvFactoryWrapper(provider);
            wrapper.setVisitorContext(rvc);
            return wrapper;
        }

    };

    public JsonsrvFactoryWrapper() {
        this(null);
    }

    public JsonsrvFactoryWrapper(SerializerProvider p) {
        super(p);
        visitorFactory = new FormatVisitorFactory(wrapperFactory);
        schemaProvider = new JsonsrvSchemaFactory();
    }

    public static void main(String[] args) throws Exception {
        JsonsrvFactoryWrapper visitor = new JsonsrvFactoryWrapper();
        ObjectMapper mapper = new ObjectMapper();
        mapper.acceptJsonFormatVisitor(mapper.constructType(Test.class), visitor);
        com.fasterxml.jackson.module.jsonSchema.JsonSchema finalSchema = visitor.finalSchema();
        System.out.println(mapper.writeValueAsString(finalSchema));
    }

}

class Test {

    private enum Tipo {

        tipo1,
        tipo2;
    }
    @JsonProperty
    private String name;
    private String address;
    private Tipo tipo;
    private int i;

    public Tipo getTipo() {
        return tipo;
    }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }
}
