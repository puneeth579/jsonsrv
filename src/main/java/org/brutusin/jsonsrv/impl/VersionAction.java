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
package org.brutusin.jsonsrv.impl;

import org.brutusin.jsonsrv.SafeAction;
import org.brutusin.jsonsrv.utils.JsonSrvUtils;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class VersionAction extends SafeAction<Void, String> {

    @Override
    public String execute(Void input) throws Exception {
        return JsonSrvUtils.getVersion();
    }

}
