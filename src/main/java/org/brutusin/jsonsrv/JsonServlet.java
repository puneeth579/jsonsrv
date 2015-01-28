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
 */package org.brutusin.jsonsrv;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.brutusin.commons.json.JsonHelper;
import org.brutusin.commons.json.ValidationException;
import org.brutusin.commons.utils.Miscellaneous;
import org.brutusin.jsonsrv.impl.DefaultRenderer;
import org.brutusin.jsonsrv.plugin.Renderer;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class JsonServlet extends HttpServlet {

    public static final String INIT_PARAM_RENDERER = "renderer";
    public static final String INIT_PARAM_RENDERER_PARAM = "render-param";
    public static final String INIT_PARAM_DISABLE_SCHEMA = "schema-parameter-disabled";

    private static final List<String> SUPPORTED_PARAMS = Miscellaneous.createList(INIT_PARAM_RENDERER, INIT_PARAM_DISABLE_SCHEMA, INIT_PARAM_RENDERER_PARAM);

    public static final String PARAM_ID = "id";
    public static final String PARAM_INPUT = "input";
    public static final String PARAM_SCHEMA = "schema";

    public enum SchemaMode {

        I, O;
    }

    private String stringArraySchema;
    private JsonHelper jsonHelper;
    private Map<String, JsonService> services = new HashMap();
    private Renderer renderer;
    private boolean schemaParameterDisabled;

    @Override
    public final void init() throws ServletException {
        try {
            Enumeration<String> initParameterNames = getServletConfig().getInitParameterNames();
            while (initParameterNames.hasMoreElements()) {
                String initParam = initParameterNames.nextElement();
                if (getSupportedInitParams() == null || !getSupportedInitParams().contains(initParam)) {
                    throw new ServletException("Unsupported servlet init-param: " + initParam);
                }
            }
            String schemaDisabledString = getServletConfig().getInitParameter(INIT_PARAM_DISABLE_SCHEMA);
            if (schemaDisabledString != null) {
                schemaParameterDisabled = Boolean.valueOf(schemaDisabledString);
            }
            String rendererClassName = getServletConfig().getInitParameter(INIT_PARAM_RENDERER);
            if (rendererClassName != null) {
                Class rendererClass = getClassLoader().loadClass(rendererClassName);
                renderer = (Renderer) rendererClass.newInstance();
            } else {
                renderer = new DefaultRenderer();
            }
            renderer.init(getServletConfig().getInitParameter(INIT_PARAM_RENDERER_PARAM));
            this.jsonHelper = new JsonHelper(getObjectMapper());
            stringArraySchema = this.jsonHelper.getSchemaHelper().getSchemaString(String[].class);

            Map<String, JsonAction> actions = loadActions();
            for (Map.Entry<String, JsonAction> entry : actions.entrySet()) {
                String id = entry.getKey();
                JsonAction action = entry.getValue();
                JsonService service = new JsonService(id, action, jsonHelper);
                services.put(id, service);
            }
        } catch (Exception ex) {
            Logger.getLogger(JsonServlet.class.getName()).log(Level.SEVERE, null, ex);
            throw new ServletException(ex);
        }
    }

    @Override
    protected final void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doDelete(req, resp);
    }

    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
    }

    @Override
    protected final void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doHead(req, resp);
    }

    @Override
    protected final void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doOptions(req, resp);
    }

    @Override
    protected final void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
    }

    @Override
    protected final void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPut(req, resp);
    }

    @Override
    protected final void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doTrace(req, resp);
    }

    protected List<String> getSupportedInitParams() {
        return SUPPORTED_PARAMS;
    }

    /**
     * Does the work
     *
     * @param req
     * @param resp
     * @throws IOException
     */
    @Override
    protected final void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String reqETag;
        if (req.getMethod().equals("POST")) {
            // 304 (Not Modified) can no be returned to a POST request. So If-None-Match is ignored, despite of not being present in a HTTP 1.1 compliant POST request
            reqETag = null;
        } else {
            reqETag = req.getHeader("If-None-Match");
        }
        String id = req.getParameter(PARAM_ID);
        String schemaParam = req.getParameter(PARAM_SCHEMA);
        if (schemaParameterDisabled) {
            schemaParam = null;
        }
        resp.setContentType("application/json");
        JsonResponse jsonResponse = null;
        String json;
        boolean cache = false;
        SchemaMode schemaMode = null;
        try {
            if (schemaParam == null) {
                schemaMode = null;
            } else {
                try {
                    schemaMode = SchemaMode.valueOf(schemaParam.trim().toUpperCase());
                } catch (Exception e) {
                    throw new IllegalAccessException("Invalid schema parameter value. Supported values are: " + Miscellaneous.arrayToString(SchemaMode.values()));
                }
            }
            if (id == null) {
                if (schemaMode == SchemaMode.I) {
                    json = "";
                } else if (schemaMode == SchemaMode.O) {
                    // service listing output schema
                    json = this.stringArraySchema;
                } else {
                    jsonResponse = listServices();
                    json = this.jsonHelper.getDataHelper().transform(jsonResponse);
                }
                cache = true;
            } else {
                JsonService service = services.get(id);
                if (service == null) {
                    jsonResponse = new JsonResponse();
                    jsonResponse.setError(jsonResponse.new ErrorDescription(JsonResponse.Error.serviceNotFound));
                    json = this.jsonHelper.getDataHelper().transform(jsonResponse);
                } else {
                    if (schemaMode == SchemaMode.I) {
                        json = service.getInputSchema();
                        cache = true;
                    } else if (schemaMode == SchemaMode.O) {
                        json = service.getOutputSchema();
                        cache = true;
                    } else {
                        String inputStr = req.getParameter(PARAM_INPUT);
                        jsonResponse = execute(service, inputStr);
                        if (jsonResponse.getError() == null) {
                            cache = service.getAction().isCacheable();
                        }
                        json = this.jsonHelper.getDataHelper().transform(jsonResponse);
                    }
                }
            }

        } catch (Exception ex) {
            jsonResponse = new JsonResponse();
            jsonResponse.setError(jsonResponse.new ErrorDescription(JsonResponse.Error.internalError, Miscellaneous.getRootCauseMessage(ex)));
            try {
                json = this.jsonHelper.getDataHelper().transform(jsonResponse);
            } catch (JsonProcessingException pe) {
                throw new AssertionError();
            }
            Logger.getLogger(JsonServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (jsonResponse != null && jsonResponse.getError() != null) {
            if (jsonResponse.getError().getCode() == JsonResponse.Error.internalError.getCode()) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } else if (jsonResponse.getError().getCode() == JsonResponse.Error.serviceNotFound.getCode()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            } else if (jsonResponse.getError().getCode() == JsonResponse.Error.applicationError.getCode()) {
                // Application error is considered another successful outcome     
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        }
        String etag;
        if (cache) {
            resp.addHeader("Cache-Control", "private");
            etag = "\"" + String.valueOf((renderer.getClass() + json).hashCode()) + "\"";
            if (req.getMethod().equals("POST")) {
                addContentLocation(req, resp);
            }
        } else {
            etag = null;
            if (jsonResponse.getError() == null) {
                resp.addDateHeader("Expires", 0);
                resp.addHeader("Cache-Control", "max-age=0, no-cache, no-store");
                resp.addHeader("Pragma", "no-cache");
            }
        }
        if (reqETag == null || etag == null || !reqETag.equals(etag)) {
            if (etag != null) {
                resp.setHeader("ETag", etag);
            }
            renderer.service(getServletConfig(), req, resp, json, schemaMode);
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        }
    }

    private static void addContentLocation(HttpServletRequest req, HttpServletResponse resp) {
        StringBuffer requestURL = req.getRequestURL();
        Map<String, String[]> parameterMap = req.getParameterMap();
        boolean first = true;
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String name = entry.getKey();
            String[] value = entry.getValue();
            for (int i = 0; i < value.length; i++) {
                if (first) {
                    first = false;
                    requestURL.append("?");
                } else {
                    requestURL.append("&");
                }
                try {
                    requestURL.append(name).append("=").append(URLEncoder.encode(value[i], resp.getCharacterEncoding()));
                } catch (UnsupportedEncodingException ex) {
                    throw new AssertionError();
                }
            }
        }
        resp.addHeader("Content-Location", resp.encodeRedirectURL(requestURL.toString()));
    }

    protected ClassLoader getClassLoader() {
        return JsonServlet.class.getClassLoader();
    }

    protected ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    private JsonResponse listServices() {
        JsonResponse json = new JsonResponse();
        json.setValue(services.keySet());
        return json;
    }

    private JsonResponse execute(JsonService service, String inputStr) {
        JsonResponse json = new JsonResponse();
        if (service == null) {
            json.setError(json.new ErrorDescription(JsonResponse.Error.serviceNotFound));
            return json;
        }
        JsonAction action = service.getAction();
        Object input;

        if (inputStr == null) {
            input = null;
        } else {
            try {
                this.jsonHelper.getSchemaHelper().validate(service.getValidationInputSchema(), this.jsonHelper.parse(inputStr));
                input = this.jsonHelper.getDataHelper().transform(inputStr, service.getInputClass());
            } catch (ProcessingException ex) {
                json.setError(json.new ErrorDescription(JsonResponse.Error.parseError, Miscellaneous.getRootCauseMessage(ex)));
                return json;
            } catch (ValidationException ex) {
                ProcessingReport report = ex.getReport();
                Iterator<ProcessingMessage> iterator = report.iterator();
                List<String> messages = new ArrayList();
                while (iterator.hasNext()) {
                    ProcessingMessage processingMessage = iterator.next();
                    messages.add(processingMessage.getMessage());
                }
                json.setError(json.new ErrorDescription(JsonResponse.Error.invalidInput, messages));
                return json;
            } catch (JsonParseException ex) {
                json.setError(json.new ErrorDescription(JsonResponse.Error.parseError, Miscellaneous.getRootCauseMessage(ex)));
                return json;
            } catch (JsonMappingException ex) {
                json.setError(json.new ErrorDescription(JsonResponse.Error.invalidInput, Miscellaneous.getRootCauseMessage(ex)));
                return json;
            }
        }
        try {
            Object output = action.execute(input);
            json.setValue(output);

        } catch (IllegalArgumentException ex) {
            Logger.getLogger(JsonServlet.class.getName()).log(Level.WARNING, null, ex);
            json.setError(json.new ErrorDescription(JsonResponse.Error.invalidInput, Miscellaneous.getRootCauseMessage(ex)));
            return json;
        } catch (RuntimeException ex) {
            Logger.getLogger(JsonServlet.class.getName()).log(Level.SEVERE, null, ex);
            json.setError(json.new ErrorDescription(JsonResponse.Error.internalError, Miscellaneous.getRootCauseMessage(ex)));
            return json;
        } catch (Exception ex) {
            Logger.getLogger(JsonServlet.class.getName()).log(Level.INFO, null, ex);
            json.setError(json.new ErrorDescription(JsonResponse.Error.applicationError, Miscellaneous.getRootCauseMessage(ex)));
            return json;
        }
        return json;
    }

    protected Map<String, JsonAction> loadActions() throws Exception {
        Map<String, JsonAction> ret = new HashMap();
        Enumeration<URL> urls = getClassLoader().getResources("jsonsrv.json");
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            String fileContents = IOUtils.toString(url.openStream(), "UTF-8");
            ActionMapping[] ams = this.jsonHelper.getDataHelper().transform(fileContents, ActionMapping[].class);
            if (ams
                    != null) {
                for (int i = 0; i < ams.length; i++) {
                    ActionMapping am = ams[i];
                    if (ret.containsKey(am.getId())) {
                        throw new Error("Duplicated mapping found with id " + am.getId());
                    }
                    Class clazz = getClassLoader().loadClass(am.getClassName());
                    if (!JsonAction.class.isAssignableFrom(clazz)) {
                        throw new Error("Invalid action class found: " + am.getClassName());
                    }
                    JsonAction instance = (JsonAction) clazz.newInstance();
                    ret.put(am.getId(), instance);
                }
            }
        }
        return ret;
    }
}
