package org.eclipse.jetty.openejb.webapp;

import java.io.File;

import org.apache.openejb.OpenEJBRuntimeException;
import org.apache.openejb.assembler.WebAppDeployer;
import org.apache.openejb.assembler.classic.AppInfo;
import org.apache.openejb.assembler.classic.WebAppBuilder;
import org.apache.openejb.assembler.classic.WebAppInfo;
import org.apache.openejb.loader.SystemInstance;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class JettyWebAppDeployer implements WebAppDeployer
{
    private static final Logger LOG = Log.getLogger(JettyWebAppDeployer.class);

    @Override
    public AppInfo deploy(String contextRoot, File file)
    {
        LOG.debug("deploy({},{})",contextRoot,file);

        JettyWebAppBuilder webappBuilder = (JettyWebAppBuilder)SystemInstance.get().getComponent(WebAppBuilder.class);

        try
        {
            AppInfo appInfo = toBasicAppInfo(file,contextRoot);
            webappBuilder.deployWebApps(appInfo,null);
        }
        catch (Exception e)
        {
            LOG.warn("Unable to deploy webapps",e);
            throw new OpenEJBRuntimeException(e);
        }

        JettyOpenEJBContext context = webappBuilder.getContext(file);
        if (context == null || context.appInfo == null)
        {
            // no context exists yet with this file?
            LOG.warn("Cannot find AppInfo for {}",file);
            return null;
        }

        return context.appInfo;
    }

    private AppInfo toBasicAppInfo(File file, String contextRoot)
    {
        AppInfo info = new AppInfo();
        info.path = file.getAbsolutePath();
        info.webAppAlone = true;

        WebAppInfo webInfo = new WebAppInfo();
        webInfo.path = info.path;
        if (contextRoot == null)
        {
            webInfo.contextRoot = file.getName();
        }
        else
        {
            webInfo.contextRoot = contextRoot;
        }

        webInfo.moduleId = webInfo.contextRoot;
        info.webApps.add(webInfo);

        return info;
    }

    @Override
    public void reload(String path)
    {
        LOG.debug("reload({})",path);
        JettyWebAppBuilder webappBuilder = (JettyWebAppBuilder)SystemInstance.get().getComponent(WebAppBuilder.class);
        File file = new File(path);
        JettyOpenEJBContext context = webappBuilder.getContext(file);
        if (context == null || context.webapp == null)
        {
            LOG.warn("Cannot find WebAppContext for {}",file);
            return;
        }

        try
        {
            context.webapp.stop();
        }
        catch (Exception e)
        {
            throw new OpenEJBRuntimeException("Unable to reload->stop webapp: " + context.webapp,e);
        }
        try
        {
            context.webapp.start();
        }
        catch (Exception e)
        {
            throw new OpenEJBRuntimeException("Unable to reload->start webapp: " + context.webapp,e);
        }
    }
}
