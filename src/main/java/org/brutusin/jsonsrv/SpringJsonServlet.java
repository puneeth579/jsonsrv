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

import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class SpringJsonServlet extends JsonServlet {

    public static final String INIT_PARAM_SPRING_CFG_FILE = "spring-cfg";
    private static final String DEFAULT_CFG_FILE = "classpath*:/jsonsrv.xml";

    @Override
    protected List<String> getSupportedInitParams() {
        List<String> supportedInitParams = super.getSupportedInitParams();
        supportedInitParams.add(INIT_PARAM_SPRING_CFG_FILE);
        return supportedInitParams;
    }

    @Override
    protected Map<String, JsonAction> loadActions() throws Exception {
        String springConfigFile = getServletConfig().getInitParameter(INIT_PARAM_SPRING_CFG_FILE);
        ClassPathXmlApplicationContext applicationContext;
        if (springConfigFile == null) {
            applicationContext = new ClassPathXmlApplicationContext(DEFAULT_CFG_FILE);
        } else {
            applicationContext = new ClassPathXmlApplicationContext(springConfigFile, DEFAULT_CFG_FILE);
        }
        applicationContext.setClassLoader(getClassLoader());
        return applicationContext.getBeansOfType(JsonAction.class);
    }
}
