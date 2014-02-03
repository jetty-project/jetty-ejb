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

import java.util.Properties;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;

import org.apache.openejb.resource.XAResourceWrapper;
import org.apache.openejb.resource.jdbc.pool.DataSourceCreator;
import org.apache.xbean.recipe.ObjectRecipe;

public class JettyDataSourceCreator implements DataSourceCreator
{
    @Override
    public DataSource managed(String name, CommonDataSource ds)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataSource poolManaged(String name, DataSource ds, Properties properties)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataSource pool(String name, DataSource ds, Properties properties)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataSource poolManagedWithRecovery(String name, XAResourceWrapper xaResourceWrapper, String driver, Properties properties)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataSource poolManaged(String name, String driver, Properties properties)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CommonDataSource pool(String name, String driver, Properties properties)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void destroy(Object object) throws Throwable
    {
        // TODO Auto-generated method stub

    }

    @Override
    public ObjectRecipe clearRecipe(Object object)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
