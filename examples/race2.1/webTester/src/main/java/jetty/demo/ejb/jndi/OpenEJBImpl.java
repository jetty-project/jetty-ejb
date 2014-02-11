package jetty.demo.ejb.jndi;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;

import jetty.demo.ejb.JndiDumper.Impl;
import jetty.demo.ejb.reflect.ReflectUtils;

import org.apache.commons.io.output.WriterOutputStream;

public class OpenEJBImpl implements Impl
{
    private Class oejbIvmContextClass;
    private Class oejbObjectRefClass;

    public OpenEJBImpl()
    {
        oejbIvmContextClass = ReflectUtils.findOptionalClass("org.apache.openejb.core.ivm.naming.IvmContext");
        oejbObjectRefClass = ReflectUtils.findOptionalClass("org.apache.openejb.core.ivm.naming.ObjectReference");
    }

    public boolean isKnown(Object obj)
    {
        return ReflectUtils.isInstanceOf(obj,oejbIvmContextClass) || ReflectUtils.isInstanceOf(obj,oejbObjectRefClass);
    }

    public void dump(PrintWriter out, Object obj)
    {
        try
        {
            if (ReflectUtils.isInstanceOf(obj,oejbIvmContextClass))
            {
                Object node = ReflectUtils.getField(obj,"mynode");
                Class params[] = new Class[] { String.class, PrintStream.class };

                WriterOutputStream wstream = null;
                PrintStream pstream = null;
                try
                {
                    Charset utf8 = Charset.forName("UTF-8");
                    wstream = new WriterOutputStream(out,utf8);
                    pstream = new PrintStream(wstream,true);
                    Object args[] = new Object[] { "  ", pstream };
                    ReflectUtils.invokeMethod(node,"tree",params,args);
                    pstream.flush();
                    wstream.flush();
                }
                finally
                {
                    close(pstream);
                    close(wstream);
                }
            }
            else if (ReflectUtils.isInstanceOf(obj,oejbObjectRefClass))
            {
                out.println("(" + obj.getClass().getName() + "): " + obj);
                Object oref = ReflectUtils.invokeMethod(obj,"getObject",new Object[0]);
                out.print(" .getObject() = ");
                if (oref == null)
                {
                    out.println("<null>");
                }
                else
                {
                    out.println("(" + oref.getClass().getName() + "): " + oref);
                }
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace(out);
        }
    }

    private void close(Closeable c)
    {
        if (c == null)
        {
            return;
        }
        try
        {
            c.close();
        }
        catch (IOException ignore)
        {
            // ignore
        }
    }
}