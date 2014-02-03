package org.eclipse.jetty.openejb;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.openejb.OpenEJBException;
import org.apache.openejb.assembler.classic.AppInfo;
import org.apache.openejb.assembler.classic.WebAppBuilder;
import org.apache.openejb.assembler.classic.WebAppInfo;
import org.apache.openejb.config.AppModule;
import org.apache.openejb.config.ConfigurationFactory;
import org.apache.openejb.config.DeploymentLoader;
import org.apache.openejb.config.WebModule;
import org.apache.openejb.loader.SystemInstance;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.webapp.WebAppContext;

public class JettyWebAppBuilder implements WebAppBuilder
{
    private static final Logger LOG = Log.getLogger(JettyWebAppBuilder.class);
    private final Server server;
    private final ConfigurationFactory configFactory;
    private final DeploymentLoader deployLoader;
    private Map<String, JettyOpenEJBContext> contexts = new HashMap<>();
    private Map<ClassLoader, Map<String, Set<String>>> jsfClasses = new HashMap<ClassLoader, Map<String, Set<String>>>();

    public JettyWebAppBuilder(Server server)
    {
        this.server = server;
        this.configFactory = new ConfigurationFactory();
        this.deployLoader = new DeploymentLoader();
    }

    private HandlerCollection getHandlers()
    {
        return SystemInstance.get().getComponent(HandlerCollection.class);
    }

    @Override
    public void deployWebApps(AppInfo appInfo, ClassLoader classLoader) throws Exception
    {
        LOG.debug("deployWebApps({},{})",appInfo,classLoader);
        HandlerCollection handlers = getHandlers();
        for (WebAppInfo webappInfo : appInfo.webApps)
        {
            LOG.debug("deploy webappInfo: {}",webappInfo);

            File appFile = new File(webappInfo.path);

            JettyOpenEJBContext context = getContext(appFile);
            if (context == null)
            {
                WebAppContext webapp = new WebAppContext();
                webapp.setWar(appFile.getAbsolutePath());
                String contextRoot = webappInfo.contextRoot;
                if (contextRoot.charAt(0) != '/')
                {
                    contextRoot = '/' + contextRoot;
                }
                webapp.setContextPath(contextRoot);
                handlers.addHandler(webapp);

                context = addContext(webapp);
                context.appInfo = appInfo;
            }
        }
    }

    public AppInfo configure(JettyWebAppFinder finder, WebAppContext context) throws OpenEJBException
    {
        File warFile = new File(context.getWar());

        ClassLoader isolatedClassloader = context.getClassLoader();

        ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader(isolatedClassloader);

            AppModule appModule = deployLoader.load(warFile);
            for (WebModule webMod : appModule.getWebModules())
            {
                webMod.setClassLoader(isolatedClassloader);
                webMod.setFinder(finder);
            }

            AppInfo appInfo = configFactory.configureApplication(appModule);
            LOG.debug("appInfo = {}",appInfo);

            return appInfo;
        }
        catch (OpenEJBException e)
        {
            throw e;
        }
        catch (Throwable t)
        {
            throw new OpenEJBException(t);
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(origLoader);
        }
    }

    @Override
    public void undeployWebApps(AppInfo appInfo) throws Exception
    {
        LOG.debug("undeployWebApps({})",appInfo);

        HandlerCollection handlers = getHandlers();

        List<Handler> remainingHandlers = new ArrayList<>();
        remainingHandlers.addAll(Arrays.asList(handlers.getHandlers()));

        for (WebAppInfo webappInfo : appInfo.webApps)
        {
            LOG.debug("undeploy webappInfo: {}",webappInfo);

            File appFile = new File(webappInfo.path);
            JettyOpenEJBContext context = getContext(appFile);
            if (context != null)
            {
                if (context.webapp != null)
                {
                    context.webapp.stop();
                    remainingHandlers.remove(context.webapp);
                }
                removeContext(context.webapp);
            }
        }

        int len = remainingHandlers.size();
        handlers.setHandlers(remainingHandlers.toArray(new Handler[len]));
    }

    @Override
    public Map<ClassLoader, Map<String, Set<String>>> getJsfClasses()
    {
        LOG.debug("getJsfClasses()");
        return jsfClasses;
    }

    public JettyOpenEJBContext addContext(WebAppContext webapp)
    {
        String contextRoot = webapp.getContextPath();
        JettyOpenEJBContext context;
        synchronized (contexts)
        {
            context = contexts.get(contextRoot);
            if (context == null)
            {
                context = new JettyOpenEJBContext();
                context.webapp = webapp;
                contexts.put(contextRoot,context);
            }
        }
        return context;
    }

    public void removeContext(WebAppContext webapp)
    {
        String contextRoot = webapp.getContextPath();
        synchronized (contexts)
        {
            contexts.remove(contextRoot);
        }
    }

    public JettyOpenEJBContext getContext(String contextRoot)
    {
        JettyOpenEJBContext context;
        synchronized (contexts)
        {
            context = contexts.get(contextRoot);
        }
        return context;
    }

    public JettyOpenEJBContext getContext(File file)
    {
        synchronized (contexts)
        {
            for (JettyOpenEJBContext context : contexts.values())
            {
                if (context.appInfo != null)
                {
                    // use OpenEJB information
                    if (context.appInfo.webAppAlone)
                    {
                        File path = new File(context.appInfo.path);
                        File war = new File(context.appInfo.path + ".war");
                        if (file.equals(path) || file.equals(war))
                        {
                            return context;
                        }
                    }
                }
                else if (context.webapp != null)
                {
                    // Use Jetty information
                    File war = new File(context.webapp.getWar());
                    File base = new File(context.webapp.getResourceBase());
                    if (file.equals(war) || file.equals(base))
                    {
                        return context;
                    }
                }
            }
        }
        return null;
    }
}
