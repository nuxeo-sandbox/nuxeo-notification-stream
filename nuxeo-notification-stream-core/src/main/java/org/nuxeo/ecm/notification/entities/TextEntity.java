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

package org.nuxeo.ecm.notification.entities;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.MatchResult;

import org.apache.avro.reflect.Nullable;

/**
 * Simple POJO to store MatchResult information (start, end, id...) and entity resolved information (for instance;
 * username, lastname, ...)
 *
 * @since XXX
 */
public class TextEntity {
    public static final String DOCUMENT = "doc";

    public static final String USERNAME = "user";

    public TextEntity() {
        // Empty constructor for Avro
    }

    protected String id;

    protected int start;

    protected int end;

    @Nullable
    protected String type;

    @Nullable
    protected String opts;

    protected Map<String, String> values = new HashMap<>();

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return getValue("title");
    }

    public String getValue(String key) {
        return values.get(key);
    }

    public Map<String, String> getValues() {
        return values;
    }

    public Boolean isComputed() {
        return !values.isEmpty();
    }

    public static TextEntity from(MatchResult match) {
        TextEntity res = new TextEntity();
        res.start = match.start();
        res.end = match.end();

        res.type = match.group(1);
        res.opts = match.group(2);
        res.id = match.group(3);

        return res;
    }
}
