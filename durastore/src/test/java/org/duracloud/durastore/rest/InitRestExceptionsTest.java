/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.durastore.rest;

import org.duracloud.common.rest.RestUtil;
import org.duracloud.storage.util.StorageProviderFactory;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

/**
 * @author: Bill Branan
 * Date: 9/19/11
 */
public class InitRestExceptionsTest {

    private InitRest initRest;
    private StorageProviderFactory storageProviderFactory;
    private RestUtil restUtil;

    private RestExceptionsTestSupport support = new RestExceptionsTestSupport();

    @Before
    public void setUp() throws Exception {
        storageProviderFactory = support.createStorageProviderFactory();
        restUtil = support.createRestUtil();
        initRest = new InitRest(storageProviderFactory, restUtil);
    }

    @Test
    public void testInitialize() throws Exception {
        Response response = initRest.initialize();
        support.verifyErrorResponse(response);
    }

    @Test
    public void testIsInitialized() throws Exception {
        Response response = initRest.isInitialized();
        int expectedStatus = Response.Status
            .SERVICE_UNAVAILABLE
            .getStatusCode();
        support.verifyErrorResponse(response, expectedStatus);
    }

}
