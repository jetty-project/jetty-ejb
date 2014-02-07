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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.apache.openejb.loader.SystemInstance;
import org.eclipse.jetty.openejb.util.Dumper;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class JettyOpenEJBAppContexts extends HandlerCollection implements JettyEJBContainer
{
    private static final Logger LOG = Log.getLogger(JettyOpenEJBAppContexts.class);

    static
    {
        Assembler.installNaming("org.eclipse.jetty.jndi",true);
    }

    private List<File> appRefs = new ArrayList<>();
    private Map<String, String> modules = new HashMap<>();
    private Map<String, AppInfo> appInfos = new HashMap<>();
    private Map<String, AppContext> appContexts = new HashMap<>();
    // private DeploymentLoader deployLoader;
    private WebAppDeployer webappDeployer;
    private ConfigurationFactory configFactory;
    private Assembler assembler;

    public JettyOpenEJBAppContexts()
    {
        SystemInstance.get().setComponent(HandlerCollection.class,this);
        SystemInstance.get().setComponent(JettyEJBContainer.class,this);
    }

    public void addApp(File appFile)
    {
        if (appRefs.contains(appFile))
        {
            throw new RuntimeException("App already added: " + appFile);
        }
        appRefs.add(appFile);
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

    @Override
    protected void doStart() throws Exception
    {
        List<Throwable> errors = new ArrayList<>();

        webappDeployer = getRequiredSystemComponent(WebAppDeployer.class);
        configFactory = getRequiredSystemComponent(ConfigurationFactory.class);
        assembler = getRequiredSystemComponent(Assembler.class);

        // Loop through each app and load -> deploy it
        for (File appRef : appRefs)
        {
            try
            {
                deploy(appRef);
            }
            catch (Throwable t)
            {
                errors.add(t);
                LOG.warn("Unable to load/deploy app: " + appRef,t);
            }
        }

        if (!errors.isEmpty())
        {
            MultiException e = new MultiException();
            for (Throwable t : errors)
            {
                e.add(t);
            }
            throw e;
        }

        super.doStart();
    }

    private void deploy(File appRef) throws OpenEJBException, IOException, NamingException
    {
        deploy(appRef.getName(),appRef);
    }

    @Override
    public AppContext deploy(String name, File file) throws OpenEJBException, IOException, NamingException
    {
        LOG.debug("Deploying: {}: {}",name,file);

        AppInfo appInfo;
        AppContext appContext;

        if (WebAppDeployer.Helper.isWebApp(file))
        {
            // WebModule deployment
            String contextPath = file.getName();

            appInfo = webappDeployer.deploy(contextPath,file);

            if (appInfo != null)
            {
                appContext = assembler.getContainerSystem().getAppContext(appInfo.appId);
            }
            else
            {
                appContext = null;
            }
        }
        else
        {
            // Other Deployment (like EAR)
            appInfo = configFactory.configureApplication(file);
            appContext = assembler.createApplication(appInfo);
        }

//        if (LOG.isDebugEnabled())
//        {
//            LOG.debug("AppInfo: {}",Dumper.describe(appInfo));
//            LOG.debug("AppContext: {}",Dumper.describe(appContext));
//        }

        modules.put(name,null != appInfo?appInfo.path:null);
        appInfos.put(name,appInfo);
        appContexts.put(name,appContext);

        return appContext;
    }

    @Override
    public void undeploy(String name) throws UndeployException, NoSuchApplicationException
    {
        String moduleId = modules.remove(name);
        appInfos.remove(name);
        appContexts.remove(name);
        if (moduleId != null)
        {
            assembler.destroyApplication(moduleId);
        }
    }

    @Override
    public AppInfo getAppInfo(String name)
    {
        return appInfos.get(name);
    }

    @Override
    public AppContext getAppContext(String moduleId)
    {
        return appContexts.get(moduleId);
    }

    @Override
    public Set<String> getModuleIds()
    {
        return modules.keySet();
    }

    @Override
    public Context getJndiContext()
    {
        return assembler.getContainerSystem().getJNDIContext();
    }

    @Override
    public ConfigurationFactory getConfigurationFactory()
    {
        return configFactory;
    }
}
