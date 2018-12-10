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

package org.nuxeo.ecm.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;
import static org.nuxeo.ecm.notification.resolver.DescendantChangesResolver.FOLDERISH_ID_FIELD;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.notification.notifier.CounterNotifier;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.stream.StreamHelper;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features({ NotificationFeature.class, PlatformFeature.class })
@Deploy("org.nuxeo.ecm.platform.notification.stream.default")
@Deploy("org.nuxeo.ecm.platform.collections.core")
@Deploy("org.nuxeo.ecm.platform.notification.stream.default:OSGI-INF/default-contrib.xml")
public class TestDescendantChangesResolver {

    protected static final String USER_ID = "tmp-user";

    @Inject
    protected NotificationStreamCallback ncs;

    @Inject
    protected NotificationService ns;

    @Inject
    protected CoreSession session;

    @Inject
    protected UserManager userManager;

    @Inject
    protected TransactionalFeature txFeature;

    @Before
    public void before() {
        DocumentModel user = userManager.getBareUserModel();
        user.setPropertyValue(userManager.getUserIdField(), USER_ID);
        userManager.createUser(user);

        PathRef docRef = new PathRef("/");
        ACP acp = session.getACP(docRef);
        acp.getOrCreateACL().add(ACE.builder(USER_ID, READ).build());
        session.setACP(docRef, acp, true);

        waitAllAsync();
    }

    @Test
    public void testDescendantWorksOnlyOnFolderish() {
        DocumentModel file = session.createDocumentModel("/", "foobar", "File");
        file = session.createDocument(file);

        try {
            ncs.doSubscribe("Administrator", "descendantChanges",
                    Collections.singletonMap(FOLDERISH_ID_FIELD, file.getId()));
            failBecauseExceptionWasNotThrown(NuxeoException.class);
        } catch (NuxeoException e) {
            assertThat(e).hasMessageStartingWith("Unable to subscribe to this resolver with a non-folderish");
        }
    }

    @Test
    public void testDescendantAllowSubscribeOnFolderish() {
        DocumentModel folder = session.createDocumentModel("/", "foobar", "Folder");
        folder = session.createDocument(folder);

        Map<String, String> ctx = Collections.singletonMap(FOLDERISH_ID_FIELD, folder.getId());
        ncs.doSubscribe("Administrator", "descendantChanges", ctx);

        assertThat(ns.getSubscriptions("descendantChanges", ctx).getUsernames()).containsExactly("Administrator");
    }

    @Test
    public void testDescendantModifications() {
        DocumentModel folder = session.createDocumentModel("/", "foobar", "Folder");
        folder = session.createDocument(folder);

        Map<String, String> ctx = Collections.singletonMap(FOLDERISH_ID_FIELD, folder.getId());
        ncs.doSubscribe(USER_ID, "descendantChanges", ctx);

        assertThat(CounterNotifier.processed).isEqualTo(0);

        DocumentModel doc = session.createDocumentModel(folder.getPathAsString(), "myFirstFile", "File");
        session.createDocument(doc);

        waitAllAsync();

        assertThat(CounterNotifier.processed).isEqualTo(1);
    }

    @Test
    public void testDeeperDescendantUpdate() {
        DocumentModel folderRoot = session.createDocumentModel("/", "foobar", "Folder");
        folderRoot = session.createDocument(folderRoot);

        AtomicReference<DocumentModel> atomicFolder = new AtomicReference<>(folderRoot);
        IntStream.range(0, 5).forEach(i -> {
            String path = atomicFolder.get().getPathAsString();
            DocumentModel nTh = session.createDocumentModel(path, "folder-" + i, "Folder");
            atomicFolder.set(session.createDocument(nTh));
        });

        DocumentModel lastFolder = atomicFolder.get();
        assertThat(session.getParentDocumentRefs(lastFolder.getRef())).hasSize(6);

        DocumentModel file = session.createDocumentModel(lastFolder.getPathAsString(), "myfile", "File");
        file = session.createDocument(file);

        waitAllAsync();

        Map<String, String> ctx = Collections.singletonMap(FOLDERISH_ID_FIELD, folderRoot.getId());
        ncs.doSubscribe(USER_ID, "descendantChanges", ctx);

        assertThat(CounterNotifier.processed).isEqualTo(0);

        file.setPropertyValue("dc:title", "Make my new title great again.");
        session.saveDocument(file);

        waitAllAsync();
        assertThat(CounterNotifier.processed).isEqualTo(1);
    }

    protected void waitAllAsync() {
        session.save();

        txFeature.nextTransaction();
        assertThat(StreamHelper.drainAndStop()).isTrue();
    }

}
