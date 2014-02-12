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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.naming.NamingException;

import org.apache.openejb.AppContext;
import org.apache.openejb.OpenEJBException;
import org.apache.openejb.SystemException;
import org.apache.openejb.assembler.classic.AppInfo;
import org.apache.openejb.assembler.classic.EjbJarInfo;
import org.apache.openejb.assembler.classic.EnterpriseBeanInfo;
import org.apache.openejb.core.JndiFactory;
import org.apache.openejb.loader.SystemInstance;
import org.eclipse.jetty.jndi.NamingUtil;
import org.eclipse.jetty.openejb.jndi.JettyJndiFactory;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class JettyAssembler extends org.apache.openejb.assembler.classic.Assembler
{
    private static final Logger LOG = Log.getLogger(JettyAssembler.class);

    public JettyAssembler()
    {
        // Force use of Jetty's JNDI
        super(new JettyJndiFactory());
        LOG.debug("<ctor>");
    }

    @Override
    public void init(Properties props) throws OpenEJBException
    {
        LOG.debug(".init({})",props);
        super.init(props);
    }

    @Override
    public AppContext createApplication(AppInfo appInfo, ClassLoader classLoader, boolean start) throws OpenEJBException, IOException, NamingException
    {
        establishParentJndiReferences(appInfo);
        return super.createApplication(appInfo,classLoader,start);
    }

    private void establishParentJndiReferences(AppInfo appInfo) throws NamingException, SystemException
    {
        JndiFactory jndiFactory = SystemInstance.get().getComponent(JndiFactory.class);
        String appName = appInfo.appId;

        Map<String, Object> appBindings = new HashMap<>();
        Map<String, Object> rootBindings = new HashMap<>();

        appBindings.put("comp/info/.","");
        appBindings.put("info/.","");
        appBindings.put(String.format("global/%s/.",appName),"");
        rootBindings.put(String.format("openejb/global/global/%s/.",appName),"");

        for (EjbJarInfo ejbJarInfo : appInfo.ejbJars)
        {
            String moduleName = ejbJarInfo.moduleName;

            appBindings.put(String.format("global/%s/%s/.",appName,moduleName),"");
            appBindings.put(String.format("app/%s/.",ejbJarInfo.moduleId),"");
            rootBindings.put(String.format("openejb/global/global/%s/%s/.",appName,moduleName),"");

            for (EnterpriseBeanInfo beanInfo : ejbJarInfo.enterpriseBeans)
            {
                rootBindings.put(String.format("openejb/Deployment/%s/.",beanInfo.ejbDeploymentId),"");
            }
        }

        jndiFactory.createComponentContext(appBindings);
        for (Map.Entry<String, Object> entry : rootBindings.entrySet())
        {
            NamingUtil.bind(jndiFactory.createRootContext(),entry.getKey(),entry.getValue());
        }
    }

    @Override
    public void build() throws OpenEJBException
    {
        LOG.debug(".build()");
        super.build();
    }

    @Override
    public void destroy()
    {
        LOG.debug(".destroy()");
        super.destroy();
    }

    @Override
    public void bindGlobals(Map<String, Object> bindings) throws NamingException
    {
        LOG.debug(".bindGlobals({})",bindings);
        super.bindGlobals(bindings);
    }

}
