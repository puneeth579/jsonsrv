/*
 * Copyright (c) 2011, DREAMgenics and/or its affiliates. All rights reserved.
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
 *
 * @author Ignacio del Valle Alles idelvall@dreamgenics.com
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
