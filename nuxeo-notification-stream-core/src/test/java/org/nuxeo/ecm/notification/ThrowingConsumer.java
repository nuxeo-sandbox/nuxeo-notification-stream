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
 * @since 0.1
 * @param <T>
 */
@FunctionalInterface
public interface ThrowingConsumer<T> extends Consumer<T> {

    @SuppressWarnings("FunctionalInterfaceMethodChanged")
    @Override
    default void accept(final T e) {
        try {
            acceptThrows(e);
        } catch (Throwable ex) {
            Throwing.sneakyThrow(ex);
        }
    }

    void acceptThrows(T elem) throws Throwable;
}

