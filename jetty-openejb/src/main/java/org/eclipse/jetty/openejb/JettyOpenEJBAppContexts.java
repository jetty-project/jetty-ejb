package org.eclipse.jetty.openejb;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.openejb.OpenEJBException;
import org.apache.openejb.assembler.classic.AppInfo;
import org.apache.openejb.assembler.classic.WebAppBuilder;
import org.apache.openejb.config.AppModule;
import org.apache.openejb.config.ConfigurationFactory;
import org.apache.openejb.config.DeploymentLoader;
import org.apache.openejb.loader.SystemInstance;
import org.eclipse.jetty.openejb.jndi.JndiUtil;
import org.eclipse.jetty.openejb.util.Dumper;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class JettyOpenEJBAppContexts extends HandlerCollection
{
    private static final Logger LOG = Log.getLogger(JettyOpenEJBAppContexts.class);
    private List<File> appRefs = new ArrayList<>();
    private DeploymentLoader deployLoader;
    private ConfigurationFactory configFactory;

    public JettyOpenEJBAppContexts()
    {
        SystemInstance.get().setComponent(HandlerCollection.class,this);
    }

    public void addApp(File appFile)
    {
        if (appRefs.contains(appFile))
        {
            throw new RuntimeException("App already added: " + appFile);
        }
        appRefs.add(appFile);
    }

    @Override
    protected void doStart() throws Exception
    {
        List<Throwable> errors = new ArrayList<>();

        deployLoader = SystemInstance.get().getComponent(DeploymentLoader.class);
        if (deployLoader == null)
        {
            deployLoader = new DeploymentLoader();
        }
        configFactory = SystemInstance.get().getComponent(ConfigurationFactory.class);
        if (configFactory == null)
        {
            configFactory = new ConfigurationFactory();
        }

        // Loop through each app and load -> deploy it
        for (File appRef : appRefs)
        {
            try
            {
                loadAndDeploy(appRef);
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

    private void loadAndDeploy(File appRef) throws OpenEJBException
    {
        LOG.debug("Loading: {}",appRef);
        AppModule appModule = deployLoader.load(appRef);
        LOG.debug("Loaded module: {}",appModule);
        if (LOG.isDebugEnabled())
        {
            dump(appModule);
        }
        LOG.debug("Configuring: {}",appModule);
        AppInfo appInfo = configFactory.configureApplication(appModule);
        LOG.debug("Configured: {}",appInfo);
        
        if(LOG.isDebugEnabled())
        {
            dump(appInfo);
            JndiUtil.dump();
        }

        WebAppBuilder builder = SystemInstance.get().getComponent(WebAppBuilder.class);
        try
        {
            builder.deployWebApps(appInfo,appModule.getClassLoader());
        }
        catch (OpenEJBException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new OpenEJBException(e);
        }
    }

    private void dump(AppInfo info)
    {
        LOG.debug("AppInfo: {}",Dumper.describe(info));
        /*
        LOG.debug("  .appId: {}",info.appId);
        LOG.debug("  .appJndiEnc: {}",info.appJndiEnc);
        LOG.debug("  .autoDeploy: {}",info.autoDeploy);
        LOG.debug("  .clients: {}",info.clients);
        LOG.debug("  .cmpMappingsXml: {}",info.cmpMappingsXml);
        LOG.debug("  .connectors: {}",info.connectors);
        LOG.debug("  .delegateFirst: {}",info.delegateFirst);
        LOG.debug("  .ejbJars: {}",info.ejbJars);
        LOG.debug("  .eventClassesNeedingAppClassloader: {}",info.eventClassesNeedingAppClassloader);
        LOG.debug("  .globalJndiEnc: {}",info.globalJndiEnc);
        LOG.debug("  .jaxrsProviders: {}",info.jaxRsProviders);
        LOG.debug("  .jmx: {}",info.jmx);
        LOG.debug("  .jsfClasses: {}",info.jsfClasses);
        LOG.debug("  .libs: {}",info.libs);
        LOG.debug("  .mbeans: {}",info.mbeans);
        LOG.debug("  .path: {}",info.path);
        LOG.debug("  .paths: {}",info.paths);
        LOG.debug("  .persistenceUnits: {}",info.persistenceUnits);
        LOG.debug("  .pojoConfigurations: {}",info.pojoConfigurations);
        LOG.debug("  .properties: {}",info.properties);
        LOG.debug("  .resourceAliases: {}",info.resourceAliases);
        LOG.debug("  .resourceIds: {}",info.resourceIds);
        LOG.debug("  .services: {}",info.services);
        LOG.debug("  .standaloneModule: {}",info.standaloneModule);
        LOG.debug("  .watchedResources: {}",info.watchedResources);
        LOG.debug("  .webAppAlone: {}",info.webAppAlone);
        LOG.debug("  .webApps: {}",info.webApps); */
    }

    private void dump(AppModule module)
    {
        LOG.debug("AppModule: {}",module);
        LOG.debug("  .moduleId: {}",module.getModuleId());
        LOG.debug("  .moduleUri: {}",module.getModuleUri());
        LOG.debug("  .file: {}",module.getFile());
        LOG.debug("  .jarLocation: {}",module.getJarLocation());
        LOG.debug("  .additionalLibMbeans: {}",module.getAdditionalLibMbeans());
        LOG.debug("  .additionalLibraries: {}",module.getAdditionalLibraries());
        LOG.debug("  .altDDs: {}",module.getAltDDs());
        LOG.debug("  .application: {}",module.getApplication());
        LOG.debug("  .clientModules: {}",module.getClientModules());
        LOG.debug("  .cmpMappings: {}",module.getCmpMappings());
        LOG.debug("  .connectorModules: {}",module.getConnectorModules());
        LOG.debug("  .deploymentModules: {}",module.getDeploymentModule());
        LOG.debug("  .earLibFinder: {}",module.getEarLibFinder());
        LOG.debug("  .ejbModules: {}",module.getEjbModules());
        LOG.debug("  .jaxRsProviders: {}",module.getJaxRsProviders());
        LOG.debug("  .persistenceModules: {}",module.getPersistenceModules());
        LOG.debug("  .pojoConfigurations: {}",module.getPojoConfigurations());
        LOG.debug("  .properties: {}",module.getProperties());
        LOG.debug("  .resources: {}",module.getResources());
        LOG.debug("  .services: {}",module.getServices());
        LOG.debug("  .validation: {}",module.getValidation());
        LOG.debug("  .validationContexts: {}",module.getValidationContexts());
        LOG.debug("  .watchedResources: {}",module.getWatchedResources());
        LOG.debug("  .webModules: {}",module.getWebModules());
    }
}
