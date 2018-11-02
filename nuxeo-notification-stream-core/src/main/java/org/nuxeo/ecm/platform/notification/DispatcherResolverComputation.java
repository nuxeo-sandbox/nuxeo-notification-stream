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

package org.nuxeo.ecm.platform.notification;

import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;

/**
 * @since XXX
 */
class DispatcherResolverComputation extends AbstractComputation {

    public static final String ID = "dispatcherResolverComputation";

    public DispatcherResolverComputation(int nbOutputStreams) {
        super(DispatcherResolverComputation.ID, 1, nbOutputStreams);
    }

    @Override
    public void processRecord(ComputationContext ctx, String s, Record record) {
        metadata.outputStreams().forEach(os -> ctx.produceRecord(os, record));
        ctx.askForCheckpoint();
    }
}
