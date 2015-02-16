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
package org.brutusin.jsonsrv.utils;

import java.io.IOException;
import org.brutusin.commons.utils.Miscellaneous;
import org.brutusin.jsonsrv.impl.VersionAction;

/**
 * 
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class JsonSrvUtils {

    private JsonSrvUtils() {
    }

    public static String getVersion() {
        try {
            return Miscellaneous.toString(VersionAction.class.getClassLoader().getResourceAsStream("jsonsrv.version"), "UTF-8");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
