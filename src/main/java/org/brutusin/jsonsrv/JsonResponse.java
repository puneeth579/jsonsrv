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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class JsonResponse<E> {

    public enum Error {

        parseError(-32700, "Parse error", "Invalid JSON was received by the server. An error occurred on the server while parsing the JSON input"),
        serviceNotFound(-32601, "Service not found", "The service does not exist / is not available"),
        invalidInput(-32602, "Invalid input", "Invalid service input. Received input does not meet schema restrictions"),
        internalError(-32603, "Internal error", "Internal service error"),
        applicationError(-32000,"Application error","Error contemplated by the application logic");

        private final int code;
        private final String name;
        private final String description;

        Error(int code, String name, String description) {
            this.code = code;
            this.name = name;
            this.description = description;
        }

        public int getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }
    
    private ErrorDescription error;
    private E value;

    public E getValue() {
        return value;
    }

    public void setValue(E value) {
        this.value = value;
    }

    public ErrorDescription getError() {
        return error;
    }

    public void setError(ErrorDescription error) {
        this.error = error;
    }

    public class ErrorDescription {
        @JsonProperty(required = true)
        private final int code;
        @JsonProperty(required = true)
        private final String message;
        @JsonProperty(required = true)
        private final String meaning;
        private final Object data;

        public ErrorDescription(Error error) {
            this(error, null);
        }

        public ErrorDescription(Error error, Object data) {
            this.code = error.getCode();
            this.message = error.getName();
            this.meaning = error.getDescription();
            this.data = data;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public String getMeaning() {
            return meaning;
        }

        public Object getData() {
            return data;
        }
    }
}