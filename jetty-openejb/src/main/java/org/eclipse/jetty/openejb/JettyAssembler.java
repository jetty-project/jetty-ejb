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

import java.util.Map;
import java.util.Properties;

import javax.naming.NamingException;

import org.apache.openejb.OpenEJBException;
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
