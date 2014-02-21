package org.eclipse.jetty.openejb;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.openejb.AppContext;
import org.apache.openejb.NoSuchApplicationException;
import org.apache.openejb.OpenEJBException;
import org.apache.openejb.UndeployException;
import org.apache.openejb.assembler.WebAppDeployer;
import org.apache.openejb.assembler.classic.AppInfo;
import org.apache.openejb.assembler.classic.Assembler;
import org.apache.openejb.config.ConfigurationFactory;
import org.apache.openejb.loader.Embedder;
import org.apache.openejb.loader.ProvisioningUtil;
import org.apache.openejb.loader.SystemInstance;
import org.eclipse.jetty.openejb.app.JettyAssembler;
import org.eclipse.jetty.openejb.util.EnvUtil;
import org.eclipse.jetty.openejb.util.PathWatcher;
import org.eclipse.jetty.openejb.util.PathWatcher.Config;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * The JettyOpenEJB Module.
 * <p>
 * Note: OpenEJB is per JVM, not per Server.
 * <p>
 * It is not possible to have multiple Jetty servers per JVM and OpenEJB at the same time.
 */
public class JettyOpenEJBModule extends ContainerLifeCycle implements JettyEJBContainer, PathWatcher.Listener
{
    private static class DeployedApp
    {
        String name;
        public File file;
        public String path;
        AppInfo appInfo;
        AppContext appContext;
    }

    private static final Logger LOG = Log.getLogger(JettyOpenEJBModule.class);

    static
    {
        Assembler.installNaming("org.eclipse.jetty.jndi",true);
    }

    private static boolean enabled = false;

    /** The server associated with this module */
    private Server server;

    /** The handler collection to add {@link org.apache.openejb.config.WebModule}'s to */
    private ContextHandlerCollection handlers;
    /** The directory to scan for possible openejb deployable artifacts */
    private File appScanDirectory;

    /** Map of Deployed app names to {@link DeployedApp} references */
    private Map<String, DeployedApp> appMap = new HashMap<>();

    private WebAppDeployer webappDeployer;
    private ConfigurationFactory configFactory;
    private Assembler assembler;

    public JettyOpenEJBModule(Server server, ContextHandlerCollection handlers)
    {
        this.server = server;
        this.handlers = handlers;
        String basePath = EnvUtil.lookupPropertyValue("jetty.base","jetty.home","user.dir");
        File baseDir = EnvUtil.lookupPath(basePath,".");
        File appsDir = new File(baseDir,"apps");
        LOG.info("OpenEJB App Scan Directory: {}",appsDir);
        this.appScanDirectory = appsDir;
    }

    private void configWebAppClassLoader(String key, Set<String> patterns)
    {
        String strs[] = patterns.toArray(new String[patterns.size()]);
        server.setAttribute(key,strs);
    }

    private void configWebAppContextDefaults()
    {
        LOG.debug("Configuring WebAppContext Defaults");

        // Setup Jetty default classloader filters
        setupWebAppClassloader();

        // Setup Configurations for WebAppContexts
        Configuration.ClassList conflist = Configuration.ClassList.serverDefault(server);

        // TODO: test if already setup via start.jar and --module=annotation
        // otherwise set it up ourselves, these are required for proper
        // functioning of the openejb module anyway.
        conflist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration",//
                "org.eclipse.jetty.annotations.AnnotationConfiguration");
        conflist.addAfter("org.eclipse.jetty.webapp.FragmentConfiguration",//
                "org.eclipse.jetty.plus.webapp.EnvConfiguration",//
                "org.eclipse.jetty.plus.webapp.PlusConfiguration");
        // Setup OpenEJB Configuration
        conflist.addAfter("org.eclipse.jetty.webapp.FragmentConfiguration",//
                OpenEJBConfiguration.class.getName());
        server.addBean(conflist);
    }

    @Override
    public AppContext deploy(String name, File file) throws OpenEJBException, IOException, NamingException
    {
        LOG.debug("Deploying: {}: {}",name,file);

        DeployedApp app = new DeployedApp();

        if (WebAppDeployer.Helper.isWebApp(file))
        {
            // WebModule deployment
            String contextPath = file.getName();

            app.appInfo = webappDeployer.deploy(contextPath,file);

            if (app.appInfo != null)
            {
                app.appContext = assembler.getContainerSystem().getAppContext(app.appInfo.appId);
            }
            else
            {
                app.appContext = null;
            }
        }
        else
        {
            // Other Deployment (like EAR)
            app.appInfo = configFactory.configureApplication(file);
            app.appContext = assembler.createApplication(app.appInfo);
        }

        // if (LOG.isDebugEnabled())
        // {
        // LOG.debug("AppInfo: {}",Dumper.describe(appInfo));
        // LOG.debug("AppContext: {}",Dumper.describe(appContext));
        // }

        app.name = name;
        app.file = file;
        app.path = (app.appInfo != null)?app.appInfo.path:null;

        appMap.put(app.name,app);

        return app.appContext;
    }

    @Override
    protected void doStart() throws Exception
    {
        enableOpenEJB();

        webappDeployer = getRequiredSystemComponent(WebAppDeployer.class);
        configFactory = getRequiredSystemComponent(ConfigurationFactory.class);
        assembler = getRequiredSystemComponent(Assembler.class);

        PathWatcher watcher = new PathWatcher();
        watcher.addListener(this);

        Config config = new Config(this.appScanDirectory);
        config.addInclude(Pattern.compile(".*\\.ear$",Pattern.CASE_INSENSITIVE));
        config.addInclude(Pattern.compile(".*\\.war$",Pattern.CASE_INSENSITIVE));
        config.addInclude(Pattern.compile(".*\\.jar$",Pattern.CASE_INSENSITIVE));
        config.addInclude(Pattern.compile(".*\\.sar$",Pattern.CASE_INSENSITIVE));
        watcher.addRoot(config);

        addBean(watcher);
    }

    private void enableOpenEJB()
    {
        if (enabled)
        {
            return;
        }

        enabled = true;
        LOG.info("Configuring OpenEJB for Jetty {}",Server.getVersion());

        // Jetty JNDI naming
        Assembler.installNaming("org.eclipse.jetty.jndi",true);

        // Configure WebAppContext defaults when running in OpenEJB
        configWebAppContextDefaults();

        Properties props = new Properties();
        props.setProperty("openejb.embedder.source",this.getClass().getName());
        props.setProperty("openejb.loader","jetty-system");

        String pwd = System.getProperty("user.dir");

        String homeDir = System.getProperty("jetty.home",pwd);
        props.setProperty("openejb.home",homeDir);
        System.setProperty("openejb.home",homeDir);

        String baseDir = System.getProperty("jetty.base",homeDir);
        props.setProperty("openejb.base",baseDir);
        System.setProperty("openejb.base",baseDir);

        File openEjbWebAppLibs = new File(baseDir,"lib/openejb");
        props.setProperty("openejb.libs",openEjbWebAppLibs.getAbsolutePath());

        try
        {
            // OpenEJB Provisioning
            ProvisioningUtil.addAdditionalLibraries();
        }
        catch (IOException ignore)
        {
            LOG.ignore(ignore);
        }

        // as reported by rmannibucau on #openejb (freenode irc) this
        // property is supposed to instruct openejb to avoid various jndi
        // spec breaking behaviors such as deep binding or duplicate bindings
        // (note: this doesn't work)
        // SystemInstance.get().setProperty("openejb.geronimo","true");

        props.setProperty("openejb.assembler",JettyAssembler.class.getName());

        Embedder embedder = new Embedder(JettyLoader.class.getName());
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

        SystemInstance.get().setComponent(Server.class,this.server);
        SystemInstance.get().setComponent(HandlerCollection.class,this.handlers);
        SystemInstance.get().setComponent(JettyEJBContainer.class,this);
    }

    private DeployedApp findAppByFileSystemPath(Path path)
    {
        String fspath = path.toString();
        for (DeployedApp app : appMap.values())
        {
            // TODO: this is not a robust enough test
            if (app.equals(fspath))
            {
                return app;
            }
        }
        return null;
    }

    @Override
    public AppContext getAppContext(String moduleId)
    {
        DeployedApp app = appMap.get(moduleId);
        if (app == null)
        {
            return null;
        }
        return app.appContext;
    }

    @Override
    public AppInfo getAppInfo(String moduleId)
    {
        DeployedApp app = appMap.get(moduleId);
        if (app == null)
        {
            return null;
        }
        return app.appInfo;
    }

    @Override
    public ConfigurationFactory getConfigurationFactory()
    {
        return configFactory;
    }

    @Override
    public HandlerCollection getHandlerCollection()
    {
        return this.handlers;
    }

    @Override
    public Context getJndiContext()
    {
        return assembler.getContainerSystem().getJNDIContext();
    }

    @Override
    public Set<String> getModuleIds()
    {
        return appMap.keySet();
    }

    private <T> T getRequiredSystemComponent(Class<T> type) throws OpenEJBException
    {
        T component = SystemInstance.get().getComponent(type);
        LOG.debug("Get System Component {} => {}",type.getName(),component);
        if (component == null)
        {
            throw new OpenEJBException("Unable to find required component: " + type.getName());
        }
        return component;
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

    @Override
    public void onPathAdded(Path path)
    {
        if (!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))
        {
            LOG.debug("Not deploying non-file: {}",path);
            return;
        }

        if (!Files.isReadable(path))
        {
            LOG.warn("Unable to read file: {}",path);
            return;
        }

        LOG.debug("Path Added: {}",path);

        String name = path.getFileName().toString();
        try
        {
            deploy(name,path.toFile());
        }
        catch (OpenEJBException | IOException | NamingException e)
        {
            LOG.warn("Unable to deploy: " + path,e);
        }
    }

    @Override
    public void onPathRemoved(Path path)
    {
        if (!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))
        {
            LOG.debug("Skipping non-file: {}",path);
            return;
        }

        DeployedApp app = findAppByFileSystemPath(path);
        if (app == null)
        {
            LOG.debug("Skipping, no app currently deployed from: {}",path);
            return;
        }

        LOG.debug("Path Removed: {}",path);

        // TODO: if path is in a deployed directory, then this app should be redeployed

        try
        {
            undeploy(app.name);
        }
        catch (UndeployException | NoSuchApplicationException e)
        {
            LOG.warn("Unable to undeploy: " + app.name,e);
        }
    }

    @Override
    public void onPathUpdated(Path path)
    {
        if (!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))
        {
            LOG.debug("Skipping non-file: {}",path);
            return;
        }

        DeployedApp app = findAppByFileSystemPath(path);
        if (app == null)
        {
            LOG.debug("Skipping, no app currently deployed from: {}",path);
            return;
        }

        LOG.debug("Path Updated: {}",path);

        // perform 'redeploy' steps
        String name = app.name;
        try
        {
            undeploy(name);
            deploy(name,path.toFile());
        }
        catch (OpenEJBException | IOException | NamingException e)
        {
            LOG.warn("Unable to redeploy: " + name,e);
        }
    }

    /**
     * Directory to scan for possible application deployments.
     * <p>
     * Looks for <code>*.war</code>, <code>*.ear</code>, and <code>*.jar</code> files to deploy
     * 
     * @param dir
     *            the directory to scan for
     */
    public void setAppScanDirectory(File dir)
    {
        this.appScanDirectory = dir;
    }

    private void setupWebAppClassloader()
    {
        Set<String> serverSysClasses = getStringSetAttribute(WebAppContext.SERVER_SYS_CLASSES,WebAppContext.__dftSystemClasses);

        // System Classes
        serverSysClasses.add("javax.ws.");
        serverSysClasses.add("org.apache.openejb.");
        serverSysClasses.add("org.apache.xbean.");
        serverSysClasses.add("org.apache.tomee.");
        serverSysClasses.add("org.apache.tomcat.");

        configWebAppClassLoader(WebAppContext.SERVER_SYS_CLASSES,serverSysClasses);
    }

    @Override
    public void undeploy(String name) throws UndeployException, NoSuchApplicationException
    {
        DeployedApp app = appMap.remove(name);
        if (app != null)
        {
            assembler.destroyApplication(app.appContext);
        }
    }
}
