package org.eclipse.jetty.openejb;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.openejb.Core;
import org.apache.openejb.assembler.classic.Assembler;
import org.apache.openejb.loader.Embedder;
import org.apache.openejb.loader.ProvisioningUtil;
import org.apache.openejb.loader.SystemInstance;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.webapp.WebAppContext;

public class JettyOpenEJB extends ContainerLifeCycle
{
    private static final Logger LOG = Log.getLogger(JettyOpenEJB.class);

    private boolean enabled = false;
    private Server server;
    private HandlerCollection handlers;
    
    public JettyOpenEJB(Server server, HandlerCollection handlers)
    {
        this.server = server;
        this.handlers = handlers;
    }

    @Override
    protected void doStart() throws Exception
    {
        enableOpenEJB();
    }

    private void enableOpenEJB()
    {
        if (enabled)
        {
            return;
        }

        enabled = true;
        LOG.info("Configuring OpenEJB for {}",server);

        // Default JNDI naming (for OpenEJB)
        Assembler.installNaming();
        // Jetty JNDI naming
        Assembler.installNaming("org.eclipse.jetty.jndi",true);

        // Setup Jetty default classloader filters
        setupWebAppClassloader();

        Properties props = new Properties();
        props.setProperty("openejb.embedder.source",this.getClass().getName());
        props.setProperty("openejb.loader","jetty-system");

        String pwd = System.getProperty("user.dir");

        String homeDir = System.getProperty("jetty.home",pwd);
        props.setProperty("openejb.home",homeDir);
        System.setProperty("openejb.home",homeDir);

        String baseDir = System.getProperty("jetty.base",homeDir);
        props.setProperty("openejb.base",baseDir);
        System.setProperty("openejb.basE",baseDir);

        File openEjbWebAppLibs = new File(baseDir,"lib/openejb");
        props.setProperty("openejb.libs",openEjbWebAppLibs.getAbsolutePath());

        // TODO: set "tomcat.version" and "tomcat.built" system properties?

        try
        {
            // OpenEJB Provisioning
            ProvisioningUtil.addAdditionalLibraries();
        }
        catch (IOException ignore)
        {
            LOG.ignore(ignore);
        }

        SystemInstance.get().setComponent(Server.class,server);
        SystemInstance.get().setComponent(HandlerCollection.class,handlers);

        Embedder embedder = new Embedder(JettyOpenEJBLoader.class.getName());
        SystemInstance.get().setComponent(Embedder.class,embedder);
        try
        {
            // Setup the parent classloader
            SystemInstance.init(props);

            // integrate openejb
            embedder.init(props);
        }
        catch (Exception e)
        {
            LOG.warn(e);
        }
    }

    private void setupWebAppClassloader()
    {
        // System Classes
        Set<String> serverSysClasses = new TreeSet<>();

        String[] serverSysRefs = (String[])server.getAttribute(WebAppContext.SERVER_SYS_CLASSES);

        if (serverSysRefs == null)
        {
            serverSysRefs = WebAppContext.__dftSystemClasses;
        }

        for (String classRef : serverSysRefs)
        {
            serverSysClasses.add(classRef);
        }

        serverSysClasses.add("javax.ws.");
        serverSysClasses.add("org.apache.openejb.");
        serverSysClasses.add("org.apache.xbean.");
        serverSysClasses.add("org.apache.tomee.");
        serverSysClasses.add("org.apache.tomcat.");

        serverSysRefs = serverSysClasses.toArray(new String[serverSysClasses.size()]);
        server.setAttribute(WebAppContext.SERVER_SYS_CLASSES,serverSysRefs);
    }

    public static void enableOn(Server server, HandlerCollection handlers)
    {
        Core.warmup();
        JettyOpenEJB joe = new JettyOpenEJB(server,handlers);
        joe.enableOpenEJB();
        server.addBean(joe);
    }
}
