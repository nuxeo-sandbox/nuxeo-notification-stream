/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.platform.notification.processors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.platform.notification.NotificationService;
import org.nuxeo.ecm.platform.notification.resolver.Resolver;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * StreamProcessor for saving the new subscriptions or unsubscribe users for a given notification.
 *
 * @since XXX
 */
public class SubscriptionsProcessor implements StreamProcessorTopology {

    public static final String INPUT_SUBSCRIPTIONS_STREAM = "toto";

    @Override
    public Topology getTopology(Map<String, String> map) {
        return Topology.builder().addComputation(SubscriptionsComputation::new, Collections.singletonList("i1:" + INPUT_SUBSCRIPTIONS_STREAM))
                .build();
    }

    protected class SubscriptionsComputation extends AbstractComputation {

        public static final String ID = "subscriptionsComputation";

        public SubscriptionsComputation() {
            super(ID, 1, 0);
        }

        @Override
        public void processRecord(ComputationContext computationContext, String s, Record record) {

            // Get the resolver for the notification
            String resolverId = "";

            if (!StringUtils.isEmpty(resolverId)) {
                String username = "";
                Map<String, String> ctx = new HashMap<>();

                Resolver resolver = Framework.getService(NotificationService.class).getResolver(resolverId);
                resolver.subscribe(username, ctx);
            }

            // End the computation
            computationContext.askForCheckpoint();
        }
    }
}
