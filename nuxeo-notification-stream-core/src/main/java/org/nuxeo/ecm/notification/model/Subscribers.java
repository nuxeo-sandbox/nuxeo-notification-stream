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

package org.nuxeo.ecm.notification.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @since XXX
 */
public class Subscribers implements Serializable {
    protected HashSet<String> usernames = new HashSet<>();

    protected Subscribers() {

    }

    public Stream<String> getUsernames() {
        return usernames.stream();
    }

    public Subscribers remove(String username) {
        usernames.remove(username);
        return this;
    }

    public Subscribers addUsername(String username) {
        usernames.add(username);
        return this;
    }

    public static Subscribers empty() {
        return new Subscribers();
    }

    public static Subscribers withUser(String username) {
        Subscribers subs = new Subscribers();
        subs.addUsername(username);
        return subs;
    }

    public static Subscribers withUsers(Collection<String> usernames) {
        Subscribers subs = new Subscribers();
        usernames.forEach(subs::addUsername);
        return subs;
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
}
