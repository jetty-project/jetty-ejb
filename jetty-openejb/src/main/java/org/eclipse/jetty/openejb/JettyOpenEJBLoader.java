package org.eclipse.jetty.openejb;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.openejb.OpenEJB;
import org.apache.openejb.assembler.WebAppDeployer;
import org.apache.openejb.assembler.classic.Assembler;
import org.apache.openejb.assembler.classic.WebAppBuilder;
import org.apache.openejb.classloader.WebAppEnricher;
import org.apache.openejb.core.ParentClassLoaderFinder;
import org.apache.openejb.core.ServerFederation;
import org.apache.openejb.loader.Loader;
import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.server.ServerService;
import org.apache.openejb.server.ServiceManager;
import org.apache.openejb.server.ejbd.EjbServer;
import org.apache.openejb.util.LogCategory;
import org.apache.openejb.util.OptionsLog;
import org.eclipse.jetty.openejb.jndi.JettyJndiFactory;
import org.eclipse.jetty.openejb.webapp.JettyWebAppBuilder;
import org.eclipse.jetty.openejb.webapp.JettyWebAppDeployer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class JettyOpenEJBLoader implements Loader
{
    private static final Logger LOG = Log.getLogger(JettyOpenEJBLoader.class);

    /** OpenEJB Server Daemon */
    private static EjbServer ejbServer;
    /** OpenEJB Service Manager that manage services */
    private static ServiceManager manager;
    /** List of other services */
    private static final List<ServerService> services = new ArrayList<ServerService>();

    /**
     * This is not context specific
     */
    @Override
    public void init(Properties properties) throws Exception
    {
        LOG.debug("init({})",properties);
        initSystemEJBS(properties);

        SystemInstance.init(properties);

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

        SystemInstance.get().setComponent(WebAppBuilder.class,new JettyWebAppBuilder());

        SystemInstance.get().setComponent(ParentClassLoaderFinder.class,new JettyParentClassLoaderFinder(server));

        SystemInstance.get().setComponent(WebAppDeployer.class,new JettyWebAppDeployer());

        SystemInstance.get().setComponent(WebAppEnricher.class,new JettyClassEnricher(server));

        // TODO: deal with JaxRS
        // TODO: deal with JaxWs

        ejbServer = new EjbServer();
        SystemInstance.get().setComponent(EjbServer.class,ejbServer);
        OpenEJB.init(properties,new ServerFederation());

        SystemInstance.get().setComponent(Assembler.class,new Assembler(new JettyJndiFactory()));

        // TODO: move JNDI resources defined in server to OpenEJB?

        Properties ejbProps = new Properties();
        ejbProps.putAll(properties);
        copySystemProp(ejbProps,"ejbd.serializer");
        copySystemProp(ejbProps,"ejbd.gzip");
        // TODO: how to calculate appropriate url?
        ejbProps.setProperty("openejb.ejbd.uri","http://127.0.0.1:8080/ejb");
        ejbServer.init(ejbProps);

        // Setup service manager
        ClassLoader cl = JettyOpenEJBLoader.class.getClassLoader();
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

    private void copySystemProp(Properties props, String key)
    {
        String value = SystemInstance.get().getProperty(key);
        if (value != null)
        {
            props.put(key,value);
        }
    }

    private void initSystemEJBS(Properties properties)
    {
        defaultIfUnset(properties,"openejb.system.apps","true");
        defaultIfUnset(properties,"openejb.deployments.classpath","false");
        defaultIfUnset(properties,"openejb.deployments.classpath.filter.systemapps","false");

        properties.setProperty("openejb.provider.default","org.eclipse.jetty.openejb");
    }

    private void defaultIfUnset(Properties properties, String key, String defValue)
    {
        String val = properties.getProperty(key);
        if (val == null)
        {
            properties.setProperty(key,defValue);
        }
    }
}
