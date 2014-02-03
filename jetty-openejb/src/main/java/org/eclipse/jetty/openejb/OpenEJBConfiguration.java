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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.openejb.assembler.classic.AppInfo;
import org.apache.openejb.assembler.classic.WebAppBuilder;
import org.apache.openejb.assembler.classic.WebAppInfo;
import org.apache.openejb.loader.SystemInstance;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
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