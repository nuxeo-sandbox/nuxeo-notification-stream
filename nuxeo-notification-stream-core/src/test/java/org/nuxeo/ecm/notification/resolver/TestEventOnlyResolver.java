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

package org.nuxeo.ecm.notification.resolver;

import static org.assertj.core.api.Java6Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.nuxeo.ecm.notification.message.EventRecord;

public class TestEventOnlyResolver extends Resolver {

    public static final int TARGET_USERS = 10;

    @Override
    public boolean accept(EventRecord eventRecord) {
        return "test".equals(eventRecord.getEventName());
    }

    @Override
    public Stream<String> resolveTargetUsers(EventRecord eventRecord) {
        return IntStream.range(0, TARGET_USERS) //
                        .boxed()
                        .map(s -> RandomStringUtils.randomAlphabetic(10));
    }

    @Test
    public void testResult() {
        List<String> list = this.resolveTargetUsers(null).collect(Collectors.toList());
        assertThat(list).hasSize(TARGET_USERS);
    }

    @Override
    public Map<String, String> buildNotifierContext(String targetUsername, EventRecord eventRecord) {
        return Collections.emptyMap();
    }
}
