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

package org.nuxeo.ecm.platform.notification.dispatcher;

import java.util.Map;

import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;

/**
 * @since XXX
 */
public abstract class Dispatcher extends AbstractComputation {

    protected Map<String, String> properties;

    public Dispatcher(DispatcherDescriptor desc, int nbInputStreams, int nbOutputStreams) {
        super(desc.id, nbInputStreams, nbOutputStreams);
        properties = desc.getProperties();
    }

    @Override
    public void processRecord(ComputationContext ctx, String inputStreamName, Record record) {
        process(record);
        ctx.askForCheckpoint();
    }

    public String getName() {
        return metadata.name();
    }

    public abstract void process(Record record);

    public String getProperty(String key) {
        return getProperty(key, null);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }
}
