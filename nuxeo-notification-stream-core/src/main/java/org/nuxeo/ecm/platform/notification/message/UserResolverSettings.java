/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Nuxeo
 */

package org.nuxeo.ecm.platform.notification.message;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * @since XXX
 */
public class UserResolverSettings implements Serializable {

    private static final long serialVersionUID = 0L;

    protected Map<String, Boolean> settings;

    public UserResolverSettings() {
        // Empty constructor for Avro
    }

    public Map<String, Boolean> getSettings() {
        if (settings == null) {
            settings = new HashMap<>();
        }
        return settings;
    }

    public void setSettings(Map<String, Boolean> settings) {
        this.settings = settings;
    }

    public List<String> getEnabledDispatchers() {
        return settings.entrySet().stream().filter(Entry::getValue).map(Entry::getKey).collect(Collectors.toList());
    }
}
