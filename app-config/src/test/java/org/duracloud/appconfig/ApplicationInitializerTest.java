/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.appconfig;

import org.duracloud.appconfig.domain.Application;
import org.duracloud.appconfig.domain.DuradminConfig;
import org.duracloud.appconfig.domain.DurabossConfig;
import org.duracloud.appconfig.domain.DurastoreConfig;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author Andrew Woods
 *         Date: Apr 22, 2010
 */
public class ApplicationInitializerTest {

    private String duradminPort = "duradminPort";
    private String duradminContext = "duradminContext";
    private String durastorePort = "durastorePort";
    private String durastoreContext = "durastoreContext";
    private String durabossPort = "durabossPort";
    private String durabossContext = "durabossContext";
    private String allHost = "allHost";

    @Test
    public void testLoad() throws IOException {
        Properties props = createProps();
        File propsFile = File.createTempFile("app-init-", ".props");
        FileOutputStream output = new FileOutputStream(propsFile);
        props.store(output, "no comments");
        output.close();

        ApplicationInitializer config = new ApplicationInitializer(propsFile);
        verifyApplicationInitializer(config);
    }

    private Properties createProps() {
        Properties props = new Properties();

        String dot = ".";
        String p = ApplicationInitializer.QUALIFIER + dot;
        String pAdm = p + DuradminConfig.QUALIFIER + dot;
        String pStr = p + DurastoreConfig.QUALIFIER + dot;
        String pRpt = p + DurabossConfig.QUALIFIER + dot;
        String pWild = p + ApplicationInitializer.wildcardKey + dot;

        String host = ApplicationInitializer.hostKey;
        String port = ApplicationInitializer.portKey;
        String context = ApplicationInitializer.contextKey;

        props.put(pAdm + port, duradminPort);
        props.put(pStr + port, durastorePort);
        props.put(pRpt + port, durabossPort);
        props.put(pAdm + context, duradminContext);
        props.put(pStr + context, durastoreContext);
        props.put(pRpt + context, durabossContext);
        props.put(pWild + host, allHost);

        return props;
    }

    private void verifyApplicationInitializer(ApplicationInitializer config) {
        Application duradmin = config.getDuradmin();
        Application durastore = config.getDurastore();
        Application duraboss = config.getDuraboss();

        Assert.assertNotNull(duradmin);
        Assert.assertNotNull(durastore);
        Assert.assertNotNull(duraboss);

        String adminHost = duradmin.getHost();
        String adminPort = duradmin.getPort();
        String adminContext = duradmin.getContext();
        String storeHost = durastore.getHost();
        String storePort = durastore.getPort();
        String storeContext = durastore.getContext();
        String reportHost = duraboss.getHost();
        String reportPort = duraboss.getPort();
        String reportContext = duraboss.getContext();

        Assert.assertNotNull(adminHost);
        Assert.assertNotNull(adminPort);
        Assert.assertNotNull(adminContext);
        Assert.assertNotNull(storeHost);
        Assert.assertNotNull(storePort);
        Assert.assertNotNull(storeContext);
        Assert.assertNotNull(reportHost);
        Assert.assertNotNull(reportPort);
        Assert.assertNotNull(reportContext);

        Assert.assertEquals(allHost, adminHost);
        Assert.assertEquals(duradminPort, adminPort);
        Assert.assertEquals(duradminContext, adminContext);
        Assert.assertEquals(allHost, storeHost);
        Assert.assertEquals(durastorePort, storePort);
        Assert.assertEquals(durastoreContext, storeContext);
        Assert.assertEquals(allHost, reportHost);
        Assert.assertEquals(durabossPort, reportPort);
        Assert.assertEquals(durabossContext, reportContext);
    }

}
