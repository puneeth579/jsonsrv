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
package org.brutusin.jsonsrv.plugin;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.brutusin.jsonsrv.JsonService;
import org.brutusin.jsonsrv.JsonServlet;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public abstract class Renderer {

    public void init(String initParam) {
        doInit(initParam); // This non-sense delegation is performed in order to keep an similar notation to JsonAction init/doInit
    }
    
    protected void doInit(String initParam) {
        // to be overridden
    }
    
    public abstract void service(ServletConfig servletConfig, HttpServletRequest req, HttpServletResponse resp, String json, JsonServlet.SchemaMode schemaMode, JsonService service) throws IOException;
}
