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
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.openejb.AppContext;
import org.apache.openejb.NoSuchApplicationException;
import org.apache.openejb.OpenEJBException;
import org.apache.openejb.UndeployException;
import org.apache.openejb.assembler.classic.AppInfo;
import org.apache.openejb.config.ConfigurationFactory;

public interface JettyEJBContainer
{
    public AppInfo getAppInfo(String name);

    public AppContext getAppContext(String moduleId);

    public Set<String> getModuleIds();

    public Context getJndiContext();

    public ConfigurationFactory getConfigurationFactory();

    public AppContext deploy(String name, File file) throws OpenEJBException, IOException, NamingException;

    public void undeploy(String name) throws UndeployException, NoSuchApplicationException;
}
