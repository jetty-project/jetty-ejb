package jetty.demo.ejb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import jetty.demo.ejb.jndi.DefaultImpl;
import jetty.demo.ejb.jndi.JettyImpl;
import jetty.demo.ejb.jndi.OpenEJBImpl;

public class JndiDumper
{
    public static interface Impl
    {
        public boolean isKnown(Object obj);

        public void dump(HttpServletResponse resp, String searchTerm, Object obj) throws IOException;
    }

    private static final List impls = new ArrayList();
    static
    {
        impls.add(new JettyImpl());
        impls.add(new OpenEJBImpl());
    }

    public static Impl getImpl(Object obj)
    {
        Iterator iter = impls.iterator();
        while (iter.hasNext())
        {
            Impl impl = (Impl)iter.next();
            if (impl.isKnown(obj))
            {
                return impl;
            }
        }
        return DefaultImpl.INSTANCE;
    }
}
