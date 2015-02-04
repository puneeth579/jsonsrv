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

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.module.jsonSchema.types.AnySchema;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema;
import com.fasterxml.jackson.module.jsonSchema.types.BooleanSchema;
import com.fasterxml.jackson.module.jsonSchema.types.IntegerSchema;
import com.fasterxml.jackson.module.jsonSchema.types.NullSchema;
import com.fasterxml.jackson.module.jsonSchema.types.NumberSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.fasterxml.jackson.module.jsonSchema.types.SimpleTypeSchema;
import com.fasterxml.jackson.module.jsonSchema.types.StringSchema;

/**
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class JsonsrvSchemaFactory extends com.fasterxml.jackson.module.jsonSchema.factories.JsonSchemaFactory {
    
    void enrich(SimpleTypeSchema schema, BeanProperty beanProperty) {
        schema.setTitle(beanProperty.getName());
    }
    
    @Override
    public AnySchema anySchema() {
        return new AnySchema() {
            @Override
            public void enrichWithBeanProperty(BeanProperty beanProperty) {
                enrich(this, beanProperty);
            }
        };
    }
    
    @Override
    public ArraySchema arraySchema() {
        return new ArraySchema() {
            @Override
            public void enrichWithBeanProperty(BeanProperty beanProperty) {
                enrich(this, beanProperty);
            }
        };
    }
    
    @Override
    public BooleanSchema booleanSchema() {
        return new BooleanSchema() {
            @Override
            public void enrichWithBeanProperty(BeanProperty beanProperty) {
                enrich(this, beanProperty);
            }
        };
    }
    
    @Override
    public IntegerSchema integerSchema() {
        return new IntegerSchema() {
            @Override
            public void enrichWithBeanProperty(BeanProperty beanProperty) {
                enrich(this, beanProperty);
            }
        };
    }
    
    @Override
    public NullSchema nullSchema() {
        return new NullSchema() {
            @Override
            public void enrichWithBeanProperty(BeanProperty beanProperty) {
                enrich(this, beanProperty);
            }
        };
    }
    
    @Override
    public NumberSchema numberSchema() {
        return new NumberSchema() {
            @Override
            public void enrichWithBeanProperty(BeanProperty beanProperty) {
                enrich(this, beanProperty);
            }
        };
    }
    
    @Override
    public ObjectSchema objectSchema() {
        return new ObjectSchema() {
            @Override
            public void enrichWithBeanProperty(BeanProperty beanProperty) {
                enrich(this, beanProperty);
            }
        };
    }
    
    @Override
    public StringSchema stringSchema() {
        return new StringSchema() {
            @Override
            public void enrichWithBeanProperty(BeanProperty beanProperty) {
                enrich(this, beanProperty);
            }
        };
    }
}
