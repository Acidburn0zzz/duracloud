/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.services.duplication;

import org.duracloud.client.ContentStore;
import org.duracloud.error.ContentStoreException;
import org.duracloud.error.NotFoundException;
import org.easymock.classextension.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Andrew Woods
 *         Date: Jan 21, 2011
 */
public class SpaceDuplicatorUpdateTest {

    private SpaceDuplicator replicator;

    private ContentStore fromStore;
    private ContentStore toStore;

    private String spaceId = "space-id";


    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
        EasyMock.verify(fromStore);
        EasyMock.verify(toStore);
    }

    private void init(Mode cmd) throws ContentStoreException {
        fromStore = createMockFromStore(cmd);
        toStore = createMockToStore(cmd);

        replicator = new SpaceDuplicator(fromStore, toStore);
    }

    @Test
    public void testUpdateSpace() throws Exception {
        init(Mode.OK);
        replicator.updateSpace(spaceId);
    }

    @Test
    public void testNullInputUpdateSpace() throws Exception {
        init(Mode.NULL_INPUT);
        replicator.updateSpace(null);
    }

    @Test
    public void testSetMetadataExceptionUpdateSpace() throws Exception {
        init(Mode.SET_METADATA_EXCEPTION);
        replicator.updateSpace(spaceId);
    }

    @Test
    public void testNotFoundUpdateSpace() throws Exception {
        init(Mode.NOT_FOUND);
        replicator.updateSpace(spaceId);
    }

    @Test
    public void testNotFoundCreateExceptionUpdateSpace() throws Exception {
        init(Mode.NOT_FOUND_CREATE_EXCEPTION);
        replicator.updateSpace(spaceId);
    }

    @Test
    public void testCreateExceptionUpdateSpace() throws Exception {
        init(Mode.CREATE_EXCEPTION);
        replicator.updateSpace(spaceId);
    }

    @Test
    public void testGetAccessExceptionUpdateSpace() throws Exception {
        init(Mode.GET_ACCESS_EXCEPTION);
        replicator.updateSpace(spaceId);
    }

    @Test
    public void testSetAccessExceptionUpdateSpace() throws Exception {
        init(Mode.SET_ACCESS_EXCEPTION);
        replicator.updateSpace(spaceId);
    }

    private ContentStore createMockFromStore(Mode cmd)
        throws ContentStoreException {
        ContentStore store = EasyMock.createMock("FromStore",
                                                 ContentStore.class);
        EasyMock.expect(store.getStorageProviderType()).andReturn("f-type");

        mockGetSpaceMetadataExpectation(cmd, store);
        mockGetSpaceAccessExpectation(cmd, store);

        EasyMock.replay(store);
        return store;
    }

    private void mockGetSpaceMetadataExpectation(Mode cmd, ContentStore store)
        throws ContentStoreException {
        EasyMock.expect(store.getSpaceMetadata(spaceId)).andReturn(
                    createSpaceMetadata(cmd));
    }

    private void mockGetSpaceAccessExpectation(Mode cmd, ContentStore store)
        throws ContentStoreException {
        switch (cmd) {
            case CREATE_EXCEPTION:
            case SET_METADATA_EXCEPTION:
            case NOT_FOUND:
            case NOT_FOUND_CREATE_EXCEPTION:
            case SET_ACCESS_EXCEPTION:
            case OK:
                EasyMock.expect(store.getSpaceAccess(spaceId)).andReturn(
                    createSpaceAccess(cmd));
                break;
            case GET_ACCESS_EXCEPTION:
                store.getSpaceAccess(spaceId);
                EasyMock.expectLastCall().andThrow(new ContentStoreException(
                    "test-exception"));
                break;
        }
    }

    private Map<String, String> createSpaceMetadata(Mode cmd) {
        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put(ContentStore.SPACE_COUNT, "10");

        return metadata;
    }

    private ContentStore.AccessType createSpaceAccess(Mode cmd) {
        ContentStore.AccessType spaceAccess = ContentStore.AccessType.OPEN;

        return spaceAccess;
    }

    private ContentStore createMockToStore(Mode cmd)
        throws ContentStoreException {
        ContentStore store = EasyMock.createMock("ToStore", ContentStore.class);
        EasyMock.expect(store.getStorageProviderType()).andReturn("t-type");

        mockSetSpaceMetadataExpectation(cmd, store);
        mockSetSpaceAccessExpectation(cmd, store);
        mockCreateSpaceExpectation(cmd, store);

        EasyMock.replay(store);
        return store;
    }

    private void mockSetSpaceMetadataExpectation(Mode cmd, ContentStore store)
        throws ContentStoreException {
        switch (cmd) {
            case SET_ACCESS_EXCEPTION:
            case GET_ACCESS_EXCEPTION:
            case OK:
                store.setSpaceMetadata(spaceId, createSpaceMetadata(cmd));
                EasyMock.expectLastCall();              
                break;
            case SET_METADATA_EXCEPTION:
                store.setSpaceMetadata(spaceId, createSpaceMetadata(cmd));
                EasyMock.expectLastCall().andThrow(new ContentStoreException(
                    "test-exception"));
                store.setSpaceMetadata(spaceId, createSpaceMetadata(cmd));
                EasyMock.expectLastCall();
                break;
            case CREATE_EXCEPTION:
            case NOT_FOUND_CREATE_EXCEPTION:
            case NOT_FOUND:
                store.setSpaceMetadata(spaceId, createSpaceMetadata(cmd));
                EasyMock.expectLastCall().andThrow(new NotFoundException(
                    "test-exception"));
                break;
        }
    }

    private void mockSetSpaceAccessExpectation(Mode cmd, ContentStore store)
        throws ContentStoreException {
        switch (cmd) {
            case SET_METADATA_EXCEPTION:
            case NOT_FOUND:
            case NOT_FOUND_CREATE_EXCEPTION:
            case CREATE_EXCEPTION:
            case OK:
                store.setSpaceAccess(spaceId, createSpaceAccess(cmd));
                EasyMock.expectLastCall();
                break;
            case SET_ACCESS_EXCEPTION:
                store.setSpaceAccess(spaceId, createSpaceAccess(cmd));
                EasyMock.expectLastCall().andThrow(new NotFoundException(
                    "test-exception"));
                break;
        }
    }

    private void mockCreateSpaceExpectation(Mode cmd, ContentStore store)
        throws ContentStoreException {
        switch (cmd) {
            case NOT_FOUND:
                store.createSpace(spaceId, createSpaceMetadata(cmd));
                EasyMock.expectLastCall();
                break;
            case NOT_FOUND_CREATE_EXCEPTION:
                store.createSpace(spaceId, createSpaceMetadata(cmd));
                EasyMock.expectLastCall().andThrow(new ContentStoreException(
                    "test-exception")).times(2);
                store.createSpace(spaceId, createSpaceMetadata(cmd));
                EasyMock.expectLastCall();
                break;
            case CREATE_EXCEPTION:
                store.createSpace(spaceId, createSpaceMetadata(cmd));
                EasyMock.expectLastCall().andThrow(new ContentStoreException(
                    "test-exception")).times(3);
                break;
        }
    }

    private enum Mode {
        OK, NULL_INPUT, SET_METADATA_EXCEPTION,
        NOT_FOUND, NOT_FOUND_CREATE_EXCEPTION, CREATE_EXCEPTION,
        GET_ACCESS_EXCEPTION, SET_ACCESS_EXCEPTION;
    }
}