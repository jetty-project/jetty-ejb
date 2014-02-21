//
//  ========================================================================
//  Copyright (c) 1995-2014 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.openejb;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.openejb.OpenEJB;
import org.apache.openejb.assembler.WebAppDeployer;
import org.apache.openejb.assembler.classic.WebAppBuilder;
import org.apache.openejb.config.ConfigurationFactory;
import org.apache.openejb.core.ParentClassLoaderFinder;
import org.apache.openejb.core.ServerFederation;
import org.apache.openejb.loader.Loader;
import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.server.ServerService;
import org.apache.openejb.server.ServiceManager;
import org.apache.openejb.server.ejbd.EjbServer;
import org.apache.openejb.util.LogCategory;
import org.apache.openejb.util.OptionsLog;
import org.eclipse.jetty.openejb.app.JettyParentClassLoaderFinder;
import org.eclipse.jetty.openejb.util.PropertyUtils;
import org.eclipse.jetty.openejb.webapp.JettyWebAppBuilder;
import org.eclipse.jetty.openejb.webapp.JettyWebAppDeployer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Embedded Server loader for OpenEJB to call when loading from {@link org.apache.openejb.loader.Embedder}
 */
public class JettyLoader implements Loader
{
    private static final Logger LOG = Log.getLogger(JettyLoader.class);

    /** OpenEJB Server Daemon */
    private static EjbServer ejbServer;
    /** OpenEJB Service Manager that manage services */
    private static ServiceManager manager;
    /** List of other services */
    private static final List<ServerService> services = new ArrayList<ServerService>();

    /**
     * Default Constructor.
     */
    public JettyLoader()
    {
        /* REQUIRED BY OPENEJB */
    }

    private void copySystemInstanceProperty(Properties props, String key)
    {
        String value = SystemInstance.get().getProperty(key);
        if (value != null)
        {
            props.put(key,value);
        }
    }

    /**
     * Called by {@link org.apache.openejb.loader.Embedder}
     */
    @Override
    public void init(Properties properties) throws Exception
    {
        LOG.debug("init({})",properties);

        // System Instance defaults
        PropertyUtils.defaultIfUnset(properties,"openejb.system.apps","true");
        PropertyUtils.defaultIfUnset(properties,"openejb.deployments.classpath","false");
        PropertyUtils.defaultIfUnset(properties,"openejb.deployments.classpath.filter.systemapps","false");

        properties.setProperty("openejb.provider.default","org.eclipse.jetty.openejb");

        SystemInstance.init(properties);

        // Setup logging
        OptionsLog.install();

        // See if we can just use what's there
        if (OpenEJB.isInitialized())
        {
            ejbServer = SystemInstance.get().getComponent(EjbServer.class);
            return;
        }

        // no dice, lets create it?
        SystemInstance.get().setProperty("openejb.default.deployment-module",
                System.getProperty("openejb.default.deployment-module","org.apache.openejb.config.WebModule"));

        Server server = SystemInstance.get().getComponent(Server.class);

        SystemInstance.get().setComponent(ConfigurationFactory.class,new ConfigurationFactory());

        SystemInstance.get().setComponent(WebAppBuilder.class,new JettyWebAppBuilder());

        SystemInstance.get().setComponent(ParentClassLoaderFinder.class,new JettyParentClassLoaderFinder(server));

        SystemInstance.get().setComponent(WebAppDeployer.class,new JettyWebAppDeployer());

        // TODO: deal with JaxRS?
        // TODO: deal with JaxWs?

        ejbServer = new EjbServer();
        SystemInstance.get().setComponent(EjbServer.class,ejbServer);
        OpenEJB.init(properties,new ServerFederation());

        Properties ejbProps = new Properties();
        ejbProps.putAll(properties);
        copySystemInstanceProperty(ejbProps,"ejbd.serializer");
        copySystemInstanceProperty(ejbProps,"ejbd.gzip");
        // TODO: how to calculate appropriate url?
        ejbProps.setProperty("openejb.ejbd.uri","http://127.0.0.1:8080/ejb");
        ejbServer.init(ejbProps);

        // Setup service manager
        ClassLoader cl = JettyOpenEJBModule.class.getClassLoader();
        if (SystemInstance.get().getOptions().get("openejb.servicemanager.enabled",true))
        {
            String clazz = SystemInstance.get().getOptions().get("openejb.service.manager.class",JettyServiceManager.class.getName());
            try
            {
                manager = (ServiceManager)cl.loadClass(clazz).newInstance();
            }
            catch (ClassNotFoundException cnfe)
            {
                LOG.warn("can't find the service manager {}, the Jetty one will be used",clazz);
                manager = new JettyServiceManager();
            }
            manager.init();
            manager.start(false);
        }
        else
        {
            // WS
            try
            {
                ServerService cxfService = (ServerService)cl.loadClass("org.apache.openejb.server.cxf.CxfService").newInstance();
                cxfService.init(properties);
                cxfService.start();
                services.add(cxfService);
            }
            catch (ClassNotFoundException ignored)
            {
            }
            catch (Exception e)
            {
                Logger logger = Log.getLogger(LogCategory.OPENEJB_STARTUP.getName());
                logger.warn("Webservices failed to start",e);
            }

            // REST
            try
            {
                ServerService restService = (ServerService)cl.loadClass("org.apache.openejb.server.cxf.rs.CxfRSService").newInstance();
                restService.init(properties);
                restService.start();
                services.add(restService);
            }
            catch (ClassNotFoundException ignored)
            {
            }
            catch (Exception e)
            {
                LOG.warn("REST failed to start",e);
            }
        }
    }
}
