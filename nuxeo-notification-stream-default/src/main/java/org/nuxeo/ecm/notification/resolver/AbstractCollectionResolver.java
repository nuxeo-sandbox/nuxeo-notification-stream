/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.resolver;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstract class for all the Collection resolvers. It allows the resolvers to share the same key in the KVS ti get the
 * list of subscribers.
 *
 * @since XXX
 */
public abstract class AbstractCollectionResolver extends SubscribableResolver {

    public static final String COLLECTION_DOC_ID = "collectionDocId";

    public static final String KEY_COLLECTION = "collection";

    @Override
    public List<String> getRequiredContextFields() {
        return Arrays.asList(COLLECTION_DOC_ID);
    }

    @Override
    public String computeSubscriptionsKey(Map<String, String> ctx) {
        return KEY_COLLECTION + DELIMITER
                + getRequiredContextFields().stream().map(ctx::get).collect(Collectors.joining(DELIMITER));
    }
}
