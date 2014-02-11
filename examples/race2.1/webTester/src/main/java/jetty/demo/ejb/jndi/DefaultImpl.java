package jetty.demo.ejb.jndi;

import java.io.PrintWriter;

import jetty.demo.ejb.JndiDumper.Impl;

public class DefaultImpl implements Impl
{
    public static final Impl INSTANCE = new DefaultImpl();

    public boolean isKnown(Object obj)
    {
        return true;
    }

    public void dump(PrintWriter out, Object obj)
    {
        if (obj == null)
        {
            out.println("<null>");
            return;
        }

        out.println("(" + obj.getClass().getName() + "): " + obj);
    }
}