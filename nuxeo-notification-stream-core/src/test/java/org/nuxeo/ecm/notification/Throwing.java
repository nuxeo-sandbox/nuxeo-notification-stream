/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification;

import java.util.function.Consumer;

/**
 * @since XXX
 */
public final class Throwing {

    private Throwing() {
    }

    public static <T> Consumer<T> rethrow(final ThrowingConsumer<T> consumer) {
        return consumer;
    }

    /**
     * The compiler sees the signature with the throws T inferred to a RuntimeException type, so it allows the unchecked
     * exception to propagate. http://www.baeldung.com/java-sneaky-throws
     */
    @SuppressWarnings("unchecked")
    public static <E extends Throwable> void sneakyThrow(Throwable ex) throws E {
        throw (E) ex;
    }

}
