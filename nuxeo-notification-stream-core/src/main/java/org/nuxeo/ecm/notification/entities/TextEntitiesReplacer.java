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

import static java.util.stream.Collectors.joining;
import static org.nuxeo.ecm.core.schema.types.constraints.Constraint.MESSAGES_BUNDLE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.utils.i18n.I18NUtils;

/**
 * Manipulate text entities (format: @{type:value}) from a String.
 * <p>
 * Two manipulations available: <br>
 * - #replaceCtxKeys: transpose entity's value to the corresponding context's value - #buildTextEntities: build a list
 * of TextEntity obj that corresponds to matching entities.
 *
 * @since 0.1
 */
public class TextEntitiesReplacer {
    public static final String DELIMITER = ":";

    public static final String ENTITY_VALUE_REGEX = "([\\w-@.+]+)";

    public static final String ENTITY_REGEX = "@\\{(?:(\\w+)" + DELIMITER + ")?" + "(?:" + ENTITY_VALUE_REGEX
            + DELIMITER + ")?" + ENTITY_VALUE_REGEX + "}";

    protected static final Pattern PATTERN = Pattern.compile(ENTITY_REGEX, Pattern.MULTILINE);

    protected final String message;

    protected final Map<String, String> ctx;

    protected TextEntitiesReplacer(String message, Map<String, String> ctx) {
        this.message = StringUtils.isBlank(message) ? "" : message;
        this.ctx = ctx;
    }

    public static TextEntitiesReplacer from(String message, Map<String, String> ctx) {
        return new TextEntitiesReplacer(message, ctx);
    }

    public static TextEntitiesReplacer from(String message) {
        return from(message, Collections.emptyMap());
    }

    public List<TextEntity> buildTextEntities() {
        List<TextEntity> entities = new ArrayList<>();

        Matcher matcher = PATTERN.matcher(message);
        while (matcher.find()) {
            entities.add(TextEntity.from(matcher.toMatchResult()));
        }

        return entities;
    }

    public String replaceCtxKeys() {
        Matcher matcher = PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String type = matcher.group(1);
            String opts = ctx.get(matcher.group(2));
            String value = ctx.get(matcher.group(3));

            String entity = Stream.of(type, opts, value).filter(StringUtils::isNotBlank).collect(joining(DELIMITER));
            matcher.appendReplacement(sb, entity.contains(DELIMITER) ? "@{" + entity + "}" : entity);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
