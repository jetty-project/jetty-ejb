package org.eclipse.jetty.openejb.jndi;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import javax.naming.Context;
import javax.naming.NamingException;

import org.eclipse.jetty.openejb.logging.LoggingUtil;
import org.junit.Test;

public class JettyJndiFactoryTest
{
    static
    {
        LoggingUtil.config();
    }

    @Test
    public void testConstructor() throws NamingException
    {
        JettyJndiFactory fact = new JettyJndiFactory();

        Context jndiContext = fact.createRootContext();
        
        assertThat(jndiContext.lookup("openejb/local"), instanceOf(Context.class));
        assertThat(jndiContext.lookup("openejb/remote"), instanceOf(Context.class));
        assertThat(jndiContext.lookup("openejb/client"), instanceOf(Context.class));
        assertThat(jndiContext.lookup("openejb/Deployment"), instanceOf(Context.class));
        assertThat(jndiContext.lookup("openejb/global"), instanceOf(Context.class));
    }
}
