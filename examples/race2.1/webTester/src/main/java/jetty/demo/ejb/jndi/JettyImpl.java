package jetty.demo.ejb.jndi;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import jetty.demo.ejb.JndiDumper.Impl;
import jetty.demo.ejb.reflect.ReflectUtils;

public class JettyImpl implements Impl
{
    private Class jettyNamingContextClass;
    private Class jettyJavaRootContextClass;
    private Class jettyLocalContextClass;

    public JettyImpl()
    {
        jettyNamingContextClass = ReflectUtils.findOptionalClass("org.eclipse.jetty.jndi.NamingContext");
        jettyJavaRootContextClass = ReflectUtils.findOptionalClass("org.eclipse.jetty.jndi.java.javaRootURLContext");
        jettyLocalContextClass = ReflectUtils.findOptionalClass("org.eclipse.jetty.jndi.local.localContextRoot");
    }

    public void dump(HttpServletResponse resp, String searchTerm, Object obj) throws IOException
    {
        resp.setContentType("text/plain");
        PrintWriter out = resp.getWriter();
        out.println("Dump JNDI(searchTerm='" + searchTerm + "')");
        out.println("Lookup Result: " + obj);
        dump(out,obj);
    }

    private void dump(PrintWriter out, Object obj)
    {
        try
        {
            if (ReflectUtils.isInstanceOf(obj,jettyLocalContextClass))
            {
                out.println("Jetty Local Context: (" + obj.getClass().getName() + ")");
                Object root = ReflectUtils.invokeMethod(obj.getClass(),"getRoot",new Object[0]);
                out.println(" .getRoot() = " + root);
                dump(out,root);
            }
            else if (ReflectUtils.isInstanceOf(obj,jettyJavaRootContextClass))
            {
                out.println("Jetty Java Root Context: (" + obj.getClass().getName() + ")");
                Object root = ReflectUtils.invokeMethod(obj.getClass(),"getRoot",new Object[0]);
                out.println(" .getRoot() = " + root);
                dump(out,root);
            }
            else if (ReflectUtils.isInstanceOf(obj,jettyNamingContextClass))
            {
                out.println("Jetty NamingContext: (" + obj.getClass().getName() + ")");
                dumpNamingContext(out,obj);
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace(out);
        }
    }

    private void dumpNamingContext(PrintWriter out, Object obj) throws ReflectiveOperationException
    {
        // public void dump(Appendable out,String indent)
        Class params[] = new Class[] { Appendable.class, String.class };
        Object args[] = new Object[] { out, "  " };
        ReflectUtils.invokeMethod(obj,"dump",params,args);
    }

    public boolean isKnown(Object obj)
    {
        return ReflectUtils.isInstanceOf(obj,jettyNamingContextClass) || ReflectUtils.isInstanceOf(obj,jettyJavaRootContextClass)
                || ReflectUtils.isInstanceOf(obj,jettyLocalContextClass);
    }
}