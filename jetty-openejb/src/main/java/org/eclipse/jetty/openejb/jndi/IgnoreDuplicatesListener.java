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

package org.eclipse.jetty.openejb.jndi;

import javax.naming.Binding;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;

import org.eclipse.jetty.jndi.NamingContext;
import org.eclipse.jetty.jndi.NamingContext.Listener;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Ignore attempts to bind to a name that already exists.
 */
public class IgnoreDuplicatesListener implements Listener
{
    private static final Logger LOG = Log.getLogger(IgnoreDuplicatesListener.class);
    public static final IgnoreDuplicatesListener INSTANCE = new IgnoreDuplicatesListener();

    @Override
    public Binding bind(NamingContext ctx, Binding binding)
    {
        try
        {
            Object val = ctx.lookup(binding.getName());
            if (val != null)
            {
                LOG.warn("Duplicate detected: "+binding,new NameAlreadyBoundException(binding.getName()));
                return null;
            }
        }
        catch (NamingException e)
        {
        }
        return binding;
    }

    @Override
    public void unbind(NamingContext ctx, Binding binding)
    {
    }
}
