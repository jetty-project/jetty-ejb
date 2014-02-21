package jetty.demo.ejb.commands;

import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletResponse;

import jetty.demo.ejb.Command;

public class JndiLookupCommand implements Command
{
    private static final Logger LOG = Logger.getLogger(JndiLookupCommand.class.getName());

    private void dumpContext(PrintWriter out, String prefix, Context context)
    {
        out.println(prefix + "/\t" + context.getClass().getName() + "\t" + context);

        try
        {
            NamingEnumeration children = context.list("");
            String parentName = prefix;
            if (!parentName.endsWith(":"))
            {
                parentName = parentName + "/";
            }

            while (children.hasMore())
            {
                NameClassPair ncPair = (NameClassPair)children.next();
                Object child = null;
                try
                {
                    child = context.lookup(ncPair.getName());
                }
                catch (NamingException e)
                {
                    child = "<Lookup Error>";
                }
                dumpJndiValue(out,parentName + ncPair.getName(),ncPair.getClassName(),child);
            }
        }
        catch (NamingException e)
        {
            out.println("<Error in context.list()>");
            e.printStackTrace(out);
        }
    }

    private void dumpJndiValue(PrintWriter out, String prefix, String type, Object value) throws NamingException
    {
        if (value != null)
        {
            if (value instanceof Context)
            {
                dumpContext(out,prefix,(Context)value);
            }
            else
            {
                out.println(prefix + "\t" + type + "\t" + value);
            }
        }
        else
        {
            out.println(prefix + "\t" + type + "\t<null>");
        }
    }

    public void exec(HttpServletResponse resp, String name) throws Throwable
    {
        LOG.log(Level.FINE,"doJndiLookup(" + name + ")");

        resp.setContentType("text/plain");
        PrintWriter out = resp.getWriter();

        InitialContext context = new InitialContext();
        out.println("JNDI[" + name + "]");

        Object result = context.lookup(name);
        String prefix = name;
        if (!prefix.endsWith(":"))
        {
            prefix = prefix + "/";
        }
        dumpJndiValue(out,prefix,result.getClass().getName(),result);
    }

    public String getName()
    {
        return "jndi-lookup";
    }
}
