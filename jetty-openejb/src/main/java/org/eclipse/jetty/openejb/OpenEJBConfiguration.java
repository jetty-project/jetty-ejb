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

import java.net.URI;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.openejb.assembler.classic.AppInfo;
import org.apache.openejb.assembler.classic.EjbLocalReferenceInfo;
import org.apache.openejb.assembler.classic.EjbReferenceInfo;
import org.apache.openejb.assembler.classic.EnvEntryInfo;
import org.apache.openejb.assembler.classic.PersistenceContextReferenceInfo;
import org.apache.openejb.assembler.classic.PersistenceUnitReferenceInfo;
import org.apache.openejb.assembler.classic.ResourceEnvReferenceInfo;
import org.apache.openejb.assembler.classic.ResourceReferenceInfo;
import org.apache.openejb.assembler.classic.ServiceReferenceInfo;
import org.apache.openejb.assembler.classic.WebAppBuilder;
import org.apache.openejb.assembler.classic.WebAppInfo;
import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.util.URLs;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.openejb.util.Dumper;
import org.eclipse.jetty.openejb.webapp.JettyWebAppBuilder;
import org.eclipse.jetty.openejb.webapp.JettyWebAppFinder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;

public class OpenEJBConfiguration extends AbstractConfiguration
{
    private static final Logger LOG = Log.getLogger(OpenEJBConfiguration.class);
    private JettyWebAppFinder annoFinder;

    public void preConfigure(WebAppContext context) throws Exception
    {
        boolean annoConfigured = false;

        annoFinder = new JettyWebAppFinder(context);

        for (Configuration config : context.getConfigurations())
        {
            if (config instanceof AnnotationConfiguration)
            {
                AnnotationConfiguration annoConfig = (AnnotationConfiguration)config;
                annoConfig.addDiscoverableAnnotationHandler(annoFinder);
                annoConfigured = true;
            }
        }

        if (!annoConfigured)
        {
            throw new JettyOpenEJBException("Annotation scanning is mandatory, enable the `annotation` module, or add the "
                    + AnnotationConfiguration.class.getName() + " to the WebAppContext configuration");
        }
    }

    @Override
    public void postConfigure(WebAppContext context) throws Exception
    {
        LOG.debug("postConfigure({})",context);

        LOG.debug("dump():{}",context.dump());

        annoFinder.onScanComplete();

        ClassLoader origCL = Thread.currentThread().getContextClassLoader();
        try
        {
            ClassLoader webappCL = context.getClassLoader();
            Thread.currentThread().setContextClassLoader(webappCL);

            JettyWebAppBuilder builder = (JettyWebAppBuilder)SystemInstance.get().getComponent(WebAppBuilder.class);

            AppInfo appInfo = builder.configure(annoFinder,context);

            if (LOG.isDebugEnabled())
            {
                dumpJndiTree(context);
                dumpJndiTree(appInfo);
            }

            LOG.debug("appInfo: {}",appInfo);

            for (WebAppInfo webappInfo : appInfo.webApps)
            {
                for (String svc : webappInfo.ejbRestServices)
                {
                    LOG.debug("ejbRestService: {}",svc);
                }
                for (String svc : webappInfo.ejbWebServices)
                {
                    LOG.debug("ejbWebService: {}",svc);
                }
            }
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(origCL);
        }
    }

    private void dumpJndiTree(AppInfo appInfo)
    {
        LOG.debug(".dumpJndiTree(AppInfo:{})",appInfo.appId);
        if ((appInfo.webApps == null) || (appInfo.webApps.isEmpty()))
        {
            LOG.debug("No webapps");
            return;
        }

        for (WebAppInfo webAppInfo : appInfo.webApps)
        {
            URI moduleUri = URLs.uri(webAppInfo.moduleId);
            for (EnvEntryInfo ref : webAppInfo.jndiEnc.envEntries)
            {
                dump(ref);
            }
            for (EjbReferenceInfo ref : webAppInfo.jndiEnc.ejbReferences)
            {
                dump(ref);
            }
            for (EjbLocalReferenceInfo ref : webAppInfo.jndiEnc.ejbLocalReferences)
            {
                dump(ref);
            }
            for (PersistenceContextReferenceInfo ref : webAppInfo.jndiEnc.persistenceContextRefs)
            {
                dump(ref,moduleUri);
            }
            for (PersistenceUnitReferenceInfo ref : webAppInfo.jndiEnc.persistenceUnitRefs)
            {
                dump(ref,moduleUri);
            }
            for (ResourceReferenceInfo ref : webAppInfo.jndiEnc.resourceRefs)
            {
                dump(ref);
            }
            for (ResourceEnvReferenceInfo ref : webAppInfo.jndiEnc.resourceEnvRefs)
            {
                dump(ref);
            }
            for (ServiceReferenceInfo ref : webAppInfo.jndiEnc.serviceRefs)
            {
                dump(ref);
            }
        }
    }

    private void dump(ServiceReferenceInfo ref)
    {
        LOG.debug("ServiceReferenceInfo: {}",Dumper.describe(ref));
    }

    private void dump(ResourceEnvReferenceInfo ref)
    {
        LOG.debug("ResourceEnvReferenceInfo: {}",Dumper.describe(ref));
    }

    private void dump(ResourceReferenceInfo ref)
    {
        LOG.debug("ResourceReferenceInfo: {}",Dumper.describe(ref));
    }

    private void dump(PersistenceUnitReferenceInfo ref, URI moduleUri)
    {
        LOG.debug("PersistenceUnitReferenceInfo[{}]: {}",moduleUri,Dumper.describe(ref));
    }

    private void dump(PersistenceContextReferenceInfo ref, URI moduleUri)
    {
        LOG.debug("PersistenceContextReferenceInfo[{}]: {}",moduleUri,Dumper.describe(ref));
    }

    private void dump(EjbReferenceInfo ref)
    {
        LOG.debug("EjbReferenceInfo: {}",Dumper.describe(ref));
    }

    private void dump(EnvEntryInfo ref)
    {
        LOG.debug("EnvEntryInfo: {}",Dumper.describe(ref));
    }

    private void dumpJndiTree(WebAppContext webappContext)
    {
        LOG.debug("Dumping JNDI Tree");
        try
        {
            Context context = new InitialContext();
            NamingEnumeration<?> enumeration = context.list("/");
            while (enumeration.hasMore())
            {
                LOG.debug("JNDI: {}",enumeration.next());
            }
        }
        catch (NamingException e)
        {
            LOG.debug(e);
        }
    }
}