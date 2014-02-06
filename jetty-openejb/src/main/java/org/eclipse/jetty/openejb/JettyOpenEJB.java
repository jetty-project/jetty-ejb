package org.eclipse.jetty.openejb;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

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

        // OpenEJB JNDI naming
        // Assembler.installNaming(Assembler.OPENEJB_URL_PKG_PREFIX);
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

    private Set<String> getStringSetAttribute(String key, String[] defValues)
    {
        String[] strs = (String[])server.getAttribute(key);
        if (strs == null)
        {
            strs = defValues;
        }
        Set<String> strSet = new HashSet<>();
        strSet.addAll(Arrays.asList(strs));
        return strSet;
    }

    private void configWebAppClassLoader(String key, Set<String> patterns)
    {
        String strs[] = patterns.toArray(new String[patterns.size()]);
        server.setAttribute(key,strs);
    }

    private void setupWebAppClassloader()
    {
        Set<String> serverSysClasses = getStringSetAttribute(WebAppContext.SERVER_SYS_CLASSES,WebAppContext.__dftSystemClasses);
        // Set<String> serverSrvClasses = getStringSetAttribute(WebAppContext.SERVER_SRV_CLASSES,WebAppContext.__dftServerClasses);

        // System Classes
        serverSysClasses.add("javax.ws.");
        // serverSysClasses.remove("org.eclipse.jetty.jndi."); // temporary
        serverSysClasses.add("org.apache.openejb.");
        serverSysClasses.add("org.apache.xbean.");
        serverSysClasses.add("org.apache.tomee.");
        serverSysClasses.add("org.apache.tomcat.");

        // Server Classes
        // serverSrvClasses.remove("-org.eclipse.jetty.jndi."); // temporary

        configWebAppClassLoader(WebAppContext.SERVER_SYS_CLASSES,serverSysClasses);
        // configWebAppClassLoader(WebAppContext.SERVER_SRV_CLASSES,serverSrvClasses);
    }

    public static void enableOn(Server server, HandlerCollection handlers)
    {
        Core.warmup();
        JettyOpenEJB joe = new JettyOpenEJB(server,handlers);
        joe.enableOpenEJB();
        server.addBean(joe);
    }
}
