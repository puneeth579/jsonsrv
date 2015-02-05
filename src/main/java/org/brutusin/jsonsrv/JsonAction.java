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

import org.brutusin.jsonsrv.caching.CachingInfo;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public abstract class JsonAction<I, O> {

    public CachingInfo getCachingInfo(I input) {
        return null;
    }

    public abstract O execute(I input) throws Exception;
}
