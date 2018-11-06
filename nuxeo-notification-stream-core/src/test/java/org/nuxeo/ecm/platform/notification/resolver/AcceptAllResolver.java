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

package org.nuxeo.ecm.platform.notification.resolver;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.nuxeo.ecm.platform.notification.EventToNotificationComputation.DEFAULT_USERS_BATCH_SIZE;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.event.Event;

public class AcceptAllResolver extends Resolver {

    public static final int MULTIPLIER = 3;

    @Override
    public boolean accept(Event event) {
        return true;
    }

    @Override
    public List<String> resolveTargetUsers(Event event) {
        return IntStream.range(0, Integer.parseInt(DEFAULT_USERS_BATCH_SIZE) * MULTIPLIER) //
                        .boxed()
                        .map(s -> RandomStringUtils.randomAlphabetic(10))
                        .collect(Collectors.toList());
    }

    @Override
    public void subscribe(String username, Map<String, String> ctx) {
        // Not required for the tests
    }

    @Test
    public void testResult() {
        assertThat(this.resolveTargetUsers(null)).hasSize(Integer.parseInt(DEFAULT_USERS_BATCH_SIZE) * MULTIPLIER);
    }
}
