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

package org.nuxeo.ecm.notification.notifier;

import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import java.util.Map;

import org.nuxeo.ecm.notification.NotificationSettingsService;
import org.nuxeo.ecm.notification.message.Notification;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;

/**
 * @since 0.1
 */
public abstract class Notifier extends AbstractComputation {

    protected Map<String, String> properties;

    protected Codec<Notification> codec;

    public Notifier(NotifierDescriptor desc) {
        super(desc.id, 1, 0);
        properties = desc.getProperties();
    }

    /**
     * Compute Notifier messageKey property key following the pattern: label.notification.notifier.RESOLVER_NAME
     *
     * @return notifier's label messageKey property entry key
     */
    public String getLabelKey() {
        return String.format("label.notification.notifier.%s", getName());
    }

    /**
     * Compute Notifier messageKey property key following the pattern: description.notification.notifier.RESOLVER_NAME
     *
     * @return notifier's description messageKey property entry key
     */
    public String getDescriptionKey() {
        return String.format("description.notification.notifier.%s", getName());
    }

    @Override
    public void init(ComputationContext context) {
        super.init(context);
        // Init the codec
        codec = Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC, Notification.class);
    }

    @Override
    public void processRecord(ComputationContext ctx, String inputStreamName, Record record) {

        Notification notification = codec.decode(record.getData());
        if (isEnabled(notification)) {
            process(notification);
        }

        ctx.askForCheckpoint();
    }

    protected boolean isEnabled(Notification notification) {
        return Framework.getService(NotificationSettingsService.class)
                        .getResolverSettings(notification.getUsername())
                        .getSettings(notification.getResolverId())
                        .isEnabled(getName());
    }

    public String getName() {
        return metadata.name();
    }

    public abstract void process(Notification notification);

    public String getProperty(String key) {
        return getProperty(key, null);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }
}
