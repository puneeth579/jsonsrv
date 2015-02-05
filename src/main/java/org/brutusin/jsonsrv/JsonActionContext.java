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

import java.security.Principal;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public abstract class JsonActionContext {

    private static final ThreadLocal<JsonActionContext> CONTEXTS = new ThreadLocal();

    public static JsonActionContext getInstance() {
        return CONTEXTS.get();
    }

    static void setInstance(JsonActionContext context) {
        CONTEXTS.set(context);
    }

    static void clear() {
        CONTEXTS.remove();
    }

    public abstract Object getRequest();

    public abstract Object getResponse();

    public final Principal getUserPrincipal() {
        HttpServletRequest req = getHttpServletRequest();
        if (req == null) {
            return null;
        }
        return req.getUserPrincipal();
    }

    public final boolean isUserInRole(String role) {
        HttpServletRequest req = getHttpServletRequest();
        if (req == null) {
            return false;
        }
        return req.isUserInRole(role);
    }

    private HttpServletRequest getHttpServletRequest() {
        Object request = getRequest();
        if (!(request instanceof HttpServletRequest)) {
            return null;
        }
        return (HttpServletRequest) request;
    }

}
