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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.brutusin.commons.Pair;
import org.brutusin.commons.json.codec.JsonCodec;
import org.brutusin.commons.json.JsonHelper;
import org.brutusin.commons.json.ParseException;
import org.brutusin.commons.json.ValidationException;
import org.brutusin.commons.json.codec.impl.DefaultJsonCodec;
import org.brutusin.commons.utils.Miscellaneous;
import org.brutusin.jsonsrv.caching.CachingInfo;
import org.brutusin.jsonsrv.caching.ConditionalCachingInfo;
import org.brutusin.jsonsrv.caching.ExpiringCachingInfo;
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
    public static final String INIT_PARAM_ACCESS_CONTROL = "access-control-allow-origin";

    public static final String PARAM_ID = "id";
    public static final String PARAM_INPUT = "input";
    public static final String PARAM_SCHEMA = "schema";

    private static final List<String> SUPPORTED_PARAMS = Miscellaneous.createList(INIT_PARAM_RENDERER, INIT_PARAM_DISABLE_SCHEMA, INIT_PARAM_RENDERER_PARAM, INIT_PARAM_ACCESS_CONTROL);

    public enum SchemaMode {

        I, O;
    }

    private final Map<String, JsonService> services = new HashMap();

    private String stringArraySchema;
    private JsonHelper jsonHelper;
    private Renderer renderer;
    private boolean schemaParameterDisabled;
    private String accessControlOrigin;

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
                try {
                    Class rendererClass = getClassLoader().loadClass(rendererClassName);
                    renderer = (Renderer) rendererClass.newInstance();
                } catch (Exception ex) {
                    throw new Error("Error loading renderer " + rendererClassName, ex);
                }
            } else {
                renderer = new DefaultRenderer();
            }
            renderer.init(getServletConfig().getInitParameter(INIT_PARAM_RENDERER_PARAM));
            this.jsonHelper = new JsonHelper(getJsonCodec());
            stringArraySchema = this.jsonHelper.getSchemaHelper().getSchemaString(String[].class);

            Map<String, JsonAction> actions = loadActions();
            for (Map.Entry<String, JsonAction> entry : actions.entrySet()) {
                String id = entry.getKey();
                JsonAction action = entry.getValue();
                JsonService service = new JsonService(id, action, jsonHelper);
                services.put(id, service);
            }

            accessControlOrigin = getServletConfig().getInitParameter(INIT_PARAM_ACCESS_CONTROL);
        } catch (Exception ex) {
            Logger.getLogger(JsonServlet.class.getName()).log(Level.SEVERE, null, ex);
            throw new ServletException(ex);
        }
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
            // 304 (Not Modified) cannot be returned to a POST request. So If-None-Match is ignored, despite of not being present in a HTTP 1.1 compliant POST request
            reqETag = null;
        } else {
            reqETag = req.getHeader("If-None-Match");
            if (reqETag != null) {
                reqETag = reqETag.replaceFirst("W/", "");
            }
        }
        resp.addHeader("X-Powered-By", "jsonsrv");
        if (accessControlOrigin != null) {
            resp.addHeader("Access-Control-Allow-Origin", accessControlOrigin);
        }
        String id = req.getParameter(PARAM_ID);
        String schemaParam = req.getParameter(PARAM_SCHEMA);
        if (schemaParameterDisabled) {
            schemaParam = null;
        }
        resp.setContentType("application/json");
        JsonResponse jsonResponse = null;
        String json;
        CachingInfo cachingInfo = null;
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
            } else {
                JsonService service = services.get(id);
                if (service == null) {
                    jsonResponse = new JsonResponse();
                    jsonResponse.setError(jsonResponse.new ErrorDescription(JsonResponse.Error.serviceNotFound));
                    json = this.jsonHelper.getDataHelper().transform(jsonResponse);
                } else {
                    if (schemaMode == SchemaMode.I) {
                        json = service.getInputSchema();
                    } else if (schemaMode == SchemaMode.O) {
                        json = service.getOutputSchema();
                    } else {
                        String inputStr = req.getParameter(PARAM_INPUT);
                        prepareActionContext(req, resp);
                        try {
                            Pair<JsonResponse, CachingInfo> result = execute(service, inputStr, reqETag);
                            jsonResponse = result.getElement1();
                            cachingInfo = result.getElement2();
                            if (jsonResponse != null) {
                                json = this.jsonHelper.getDataHelper().transform(jsonResponse);
                            } else {
                                json = null;
                            }
                        } finally {
                            clearActionContext();
                        }

                    }
                }
            }
        } catch (Exception ex) {
            jsonResponse = new JsonResponse();
            jsonResponse.setError(jsonResponse.new ErrorDescription(JsonResponse.Error.internalError, Miscellaneous.getRootCauseMessage(ex)));
            json = this.jsonHelper.getDataHelper().transform(jsonResponse);
            Logger.getLogger(JsonServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (jsonResponse
                != null && jsonResponse.getError()
                != null) {
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
        if (cachingInfo == null) {
            resp.addDateHeader("Expires", 0);
            resp.addHeader("Cache-Control", "max-age=0, no-cache, no-store, must-revalidate");
            resp.addHeader("Pragma", "no-cache");
            renderer.service(getServletConfig(), req, resp, json, schemaMode, id);
        } else if (cachingInfo instanceof ConditionalCachingInfo) {
            ConditionalCachingInfo cc = (ConditionalCachingInfo) cachingInfo;
            resp.addHeader("Cache-Control", "private, must-revalidate");
            resp.setHeader("ETag", "W/" + cc.getEtag());
            if (req.getMethod().equals("POST")) {
                addContentLocation(req, resp);
            }
            if (json != null) {
                renderer.service(getServletConfig(), req, resp, json, schemaMode, id);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            }
        } else if (cachingInfo instanceof ExpiringCachingInfo) {
            ExpiringCachingInfo ec = (ExpiringCachingInfo) cachingInfo;
            // max-age overrides expires. For legacy proxies (intermedy) cache control is ignored and no cache is performed, the desired behaviour for a private cache. See http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9.3
            resp.addDateHeader("Expires", 0);
            resp.addHeader("Cache-Control", "max-age=" + ec.getMaxAge() + ", private, must-revalidate");
            renderer.service(getServletConfig(), req, resp, json, schemaMode, id);
        } else {
            throw new AssertionError();
        }
    }

    private static void prepareActionContext(final HttpServletRequest req, final HttpServletResponse resp) {
        JsonActionContext.setInstance(new JsonActionContext() {
            @Override
            public Object getRequest() {
                return req;
            }

            @Override
            public Object getResponse() {
                return resp;
            }
        });
    }

    private static void clearActionContext() {
        JsonActionContext.clear();
    }

    protected List<String> getSupportedInitParams() {
        return SUPPORTED_PARAMS;

    }

    protected ClassLoader getClassLoader() {
        return JsonServlet.class
                .getClassLoader();
    }

    protected JsonCodec getJsonCodec() {
        return new DefaultJsonCodec();
    }

    protected Map<String, JsonAction> loadActions() throws Exception {
        Map<String, JsonAction> ret = new HashMap();
        Enumeration<URL> urls = getClassLoader().getResources("jsonsrv.json");
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            String fileContents = IOUtils.toString(url.openStream(), "UTF-8");
            ActionMapping[] ams = this.jsonHelper.getDataHelper().transform(fileContents, ActionMapping[].class
            );
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

    private JsonResponse listServices() {
        JsonResponse json = new JsonResponse();
        json.setValue(services.keySet());
        return json;
    }

    /**
     * jsonresponse = null means not-modified
     */
    private Pair<JsonResponse, CachingInfo> execute(JsonService service, String inputStr, String etag) {
        Pair<JsonResponse, CachingInfo> ret = new Pair();
        JsonResponse jsonResponse = new JsonResponse();
        ret.setElement1(jsonResponse);
        if (service == null) {
            jsonResponse.setError(jsonResponse.new ErrorDescription(JsonResponse.Error.serviceNotFound));
            return ret;
        }
        JsonAction action = service.getAction();
        Object input;

        if (inputStr == null) {
            input = null;
        } else {
            try {
                this.jsonHelper.getSchemaHelper().validate(service.getValidationInputSchema(), inputStr);
                input = this.jsonHelper.getDataHelper().transform(inputStr, service.getInputClass());
            } catch (ParseException ex) {
                jsonResponse.setError(jsonResponse.new ErrorDescription(JsonResponse.Error.parseError, Miscellaneous.getRootCauseMessage(ex)));
                return ret;
            } catch (ValidationException vex) {
                jsonResponse.setError(jsonResponse.new ErrorDescription(JsonResponse.Error.invalidInput, vex.getMessages()));
                return ret;
            }
        }
        try {
            CachingInfo cachingInfo = action.getCachingInfo(input);
            ret.setElement2(cachingInfo);
            boolean execute = true;
            if (etag != null && cachingInfo != null) {
                if (cachingInfo instanceof ConditionalCachingInfo) {
                    ConditionalCachingInfo conditionalCaching = (ConditionalCachingInfo) cachingInfo;
                    if (conditionalCaching.getEtag().equals(etag)) {
                        execute = false;
                        ret.setElement1(null);
                    }
                }
            }
            if (execute) {
                Object output = action.execute(input);
                jsonResponse.setValue(output);

            }
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(JsonServlet.class
                    .getName()).log(Level.WARNING, null, ex);
            jsonResponse.setError(jsonResponse.new ErrorDescription(JsonResponse.Error.invalidInput, Miscellaneous.getRootCauseMessage(ex)));
            return ret;
        } catch (RuntimeException ex) {
            Logger.getLogger(JsonServlet.class
                    .getName()).log(Level.SEVERE, null, ex);
            jsonResponse.setError(jsonResponse.new ErrorDescription(JsonResponse.Error.internalError, Miscellaneous.getRootCauseMessage(ex)));
            return ret;
        } catch (Exception ex) {
            Logger.getLogger(JsonServlet.class
                    .getName()).log(Level.INFO, null, ex);
            jsonResponse.setError(jsonResponse.new ErrorDescription(JsonResponse.Error.applicationError, Miscellaneous.getRootCauseMessage(ex)));
            return ret;
        }
        return ret;
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
}
