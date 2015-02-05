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
package org.brutusin.jsonsrv.caching;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public final class ExpiringCachingInfo implements CachingInfo {

    public static final ExpiringCachingInfo ONE_DAY = new ExpiringCachingInfo(60 * 60 * 24);
    public static final ExpiringCachingInfo ONE_WEEK = new ExpiringCachingInfo(ONE_DAY.getMaxAge() * 7);
    public static final ExpiringCachingInfo ONE_MONTH = new ExpiringCachingInfo(ONE_DAY.getMaxAge() * 30);
    public static final ExpiringCachingInfo FOREVER = new ExpiringCachingInfo(Integer.MAX_VALUE);

    private final int maxAge;

    /**
     * @param maxAge in seconds
     */
    public ExpiringCachingInfo(int maxAge) {
        this.maxAge = maxAge;
    }

    /**
     *
     * @return age in seconds for a client to unconditionally cache the
     * response.
     */
    public int getMaxAge() {
        return maxAge;
    }
}
