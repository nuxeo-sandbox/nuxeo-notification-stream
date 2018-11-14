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

package org.nuxeo.ecm.notification.message;

import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;

/**
 * @since XXX
 */
public class SubscriptionAction {
    public enum Action {
        SUBSCRIBE, UNSUBSCRIBE
    }

    protected String id;

    protected Action action;

    protected String username;

    protected String resolverId;

    protected Map<String, String> ctx;

    protected SubscriptionAction() {
        // Empty constructor for Avro
    }

    public String getId() {
        return id;
    }

    public Action getAction() {
        return action;
    }

    public String getUsername() {
        return username;
    }

    public String getResolverId() {
        return resolverId;
    }

    public Map<String, String> getCtx() {
        return ctx;
    }

    public byte[] encode() {
        return Framework.getService(CodecService.class)
                        .getCodec(DEFAULT_CODEC, SubscriptionAction.class)
                        .encode(this);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static SubscriptionAction subscribe(String username, String resolverId, Map<String, String> ctx) {
        return with(username, resolverId, Action.SUBSCRIBE, ctx);
    }

    public static SubscriptionAction unsubscribe(String username, String resolverId, Map<String, String> ctx) {
        return with(username, resolverId, Action.UNSUBSCRIBE, ctx);
    }

    protected static SubscriptionAction with(String username, String resolverId, Action action,
                                             Map<String, String> ctx) {
        SubscriptionAction record = new SubscriptionAction();
        record.id = UUID.randomUUID().toString();
        record.username = username;
        record.resolverId = resolverId;
        record.action = action;
        record.ctx = ctx;
        return record;
    }
}
