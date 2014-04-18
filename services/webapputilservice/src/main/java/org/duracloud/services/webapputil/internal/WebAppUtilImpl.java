/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.services.webapputil.internal;

import org.duracloud.common.web.RestHttpHelper;
import org.duracloud.services.BaseService;
import org.duracloud.services.common.error.ServiceException;
import org.duracloud.services.common.error.ServiceRuntimeException;
import org.duracloud.services.common.model.NamedFilterList;
import org.duracloud.services.webapputil.WebAppUtil;
import org.duracloud.services.webapputil.error.WebAppDeployerException;
import org.duracloud.services.webapputil.tomcat.TomcatInstance;
import org.duracloud.services.webapputil.tomcat.TomcatUtil;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class abstracts the details of managing appservers used to host
 * DuraCloud services that run outside of an osgi-container.
 *
 * @author Andrew Woods
 *         Date: Nov 30, 2009
 */
public class WebAppUtilImpl extends BaseService implements WebAppUtil, ManagedService {

    private final Logger log = LoggerFactory.getLogger(WebAppUtilImpl.class);

    private int initialPort;
    private Set<Integer> portIndices = new HashSet<Integer>();
    private TomcatUtil tomcatUtil;

    private static final String AMAZON_HOST_QUERY = "http://169.254.169.254/2009-04-04/meta-data/public-ipv4";

    /**
     * This method deploys the arg war to a newly created appserver under the
     * arg serviceId context.
     *
     * @param serviceId is the name of the context of deployed webapp
     * @param war       to be deployed
     * @return URL of deployed webapp
     */
    public URL deploy(String serviceId, String portIndex, InputStream war) {
        return this.deploy(serviceId, portIndex, war, new HashMap<String, String>());
    }

    /**
     * This method deploys the arg war to a newly created appserver under the
     * arg serviceId context.
     *
     * @param serviceId is the name of the context of deployed webapp
     * @param war       to be deployed
     * @param env       of tomcat that will be installed/started
     * @return URL of deployed webapp
     */
    public URL deploy(String serviceId,
                      String portIndex,
                      InputStream war,
                      Map<String, String> env) {
        int port = getPort(portIndex);
        return doDeploy(serviceId, war, env, port);
    }

    /**
     * This method deploys the arg war to a newly created appserver under the
     * arg serviceId context.
     *
     * @param serviceId   is the name of the context of deployed webapp
     * @param portIndex   offset from configured base port
     * @param war         to be deployed
     * @param env         of tomcat that will be installed/started
     * @param filters     are names of files in the arg war to be filtered with
     *                    provided filters. Any filters that contain the strings
     *                    $DURA_HOST$ or $DURA_PORT$ will be swapped with the
     *                    host and port of the compute instance.
     * @return URL of deployed webapp
     */
    public URL filteredDeploy(String serviceId,
                              String portIndex,
                              InputStream war,
                              Map<String, String> env,
                              NamedFilterList filters) {
        Integer port = getPort(portIndex);
        NamedFilterList filterList = createFilters(filters, port);
        InputStream filteredWar = new FilteredWar(war, filterList);

        return doDeploy(serviceId, filteredWar, env, port);
    }

    private NamedFilterList createFilters(NamedFilterList argFilters,
                                          Integer port) {
        List<NamedFilterList.NamedFilter> namedFilters = new ArrayList<NamedFilterList.NamedFilter>();

        String webappHost = "$DURA_HOST$";
        String webappPort = "$DURA_PORT$";
        for (String filterName : argFilters.getNames()) {
            Map<String, String> filters = getFilters(filterName, argFilters);
            if (filters.containsKey(webappHost)) {
                filters.put(webappHost, getHost());
            }
            if (filters.containsKey(webappPort)) {
                filters.put(webappPort, port.toString());
            }

            namedFilters.add(new NamedFilterList.NamedFilter(filterName,
                                                             filters));
        }

        return new NamedFilterList(namedFilters);
    }

    private Map<String, String> getFilters(String filterName,
                                           NamedFilterList argFilters) {
        Map<String, String> filters = new HashMap<String, String>();
        try {
            NamedFilterList.NamedFilter namedFilter = argFilters.getFilter(
                filterName);
            for (String target : namedFilter.getFilterTargets()) {
                filters.put(target, namedFilter.getFilterValue(target));
            }

        } catch (ServiceException e) {
            log.warn("Error getting filter for: " + filterName);
        }
        return filters;
    }

    private URL doDeploy(String serviceId,
                         InputStream war,
                         Map<String, String> env,
                         int port) {
        File installDir = getInstallDir(serviceId, port);

        TomcatInstance tomcat = getTomcatUtil().installTomcat(installDir, port);
        tomcat.start(env);
        tomcat.deploy(serviceId, war);

        return buildURL(port, serviceId);
    }

    private File getInstallDir(String serviceId, int port) {
        String dirName = serviceId + "-" + port;
        File installDir = new File(getTomcatInstallDir(), dirName);
        if (!installDir.exists() && !installDir.mkdirs()) {
            throw new WebAppDeployerException("Error creating: " + installDir);
        }

        return installDir;
    }

    private File getTomcatInstallDir() {
        return new File(getServiceWorkDir(), "tomcat");
    }

    private URL buildURL(int port, String contextPath) {
        String url = getBaseURL(port) + "/" + contextPath;
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            String msg = "Error building URL for: " + contextPath + ", " + port;
            log.error(msg);
            throw new WebAppDeployerException(msg, e);
        }
    }

    private String getBaseURL(int port) {
        String url = "http://" + getHost() + ":" + port;
        log.debug("webapputil url: '" + url + "'");
        return url;
    }

    private String getHost() {
        String host = getAmazonHost();
        if (null != host) {
            return host;
        }

        host = getLocalHost();
        if (null != host) {
            return host;
        }
        throw new WebAppDeployerException("Unable to find host ip.");
    }

    private String getAmazonHost() {
        String host = null;
        RestHttpHelper httpHelper = new RestHttpHelper();
        try {
            RestHttpHelper.HttpResponse response = httpHelper.get(
                AMAZON_HOST_QUERY);
            if (null != response && response.getStatusCode() == 200) {
                host = response.getResponseBody();
            }
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
        return host;
    }

    private String getLocalHost() {
        return "localhost";
    }

    /**
     * This method undeploys the webapp currently deployed at the arg URL.
     *
     * @param url of the currently deployed webapp
     */
    public void unDeploy(URL url) {
        String context = getContext(url);
        int port = getPort(url);

        File installDir = getInstallDir(context, port);
        File catalinaHome = getTomcatUtil().getCatalinaHome(installDir);

        TomcatInstance tomcatInstance = getTomcatUtil().getTomcatInstance(
            catalinaHome,
            port);
        tomcatInstance.unDeploy(context);
        tomcatInstance.stop();

        getTomcatUtil().unInstallTomcat(tomcatInstance);

        // unregister use of port.
        portIndices.remove(port);
    }

    private String getContext(URL url) {
        String context = url.getPath();
        if (null == context || context.length() == 0) {
            throw new WebAppDeployerException(
                "Context not found in url: " + url.toString());
        }
        return context;
    }

    private int getPort(URL url) {
        int port = url.getPort();
        if (port == -1) {
            throw new WebAppDeployerException(
                "Port not found in url: " + url.toString());
        }
        return port;
    }

    private int getPort(String portIndex) {
        int port = getInitialPort() + Integer.parseInt(portIndex);
        if (!portIndices.add(port)) {
            throw new ServiceRuntimeException(
                "port index unavailable: " + portIndex);
        }
        return port;
    }

    public int getInitialPort() {
        return initialPort;
    }

    public void setInitialPort(int initialPort) {
        this.initialPort = initialPort;
    }

    private TomcatUtil getTomcatUtil() {
        if (tomcatUtil != null) {
            File resourceDir = new File(getServiceWorkDir());
            tomcatUtil.setResourceDir(resourceDir);
        }
        return tomcatUtil;
    }

    public void setTomcatUtil(TomcatUtil tomcatUtil) {
        this.tomcatUtil = tomcatUtil;
    }

    public void updated(Dictionary config) throws ConfigurationException {
        log.debug("WebAppUtilImpl updating config: ");
        if (config != null) {
            Enumeration keys = config.keys();
            {
                while (keys.hasMoreElements()) {
                    String key = (String) keys.nextElement();
                    String val = (String) config.get(key);
                    log.info(" [" + key + "|" + val + "] ");
                }
            }
        } else {
            log.info("config is null.");
        }
    }
}