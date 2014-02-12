package jetty.demo.ejb.jndi;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletResponse;

import jetty.demo.ejb.JndiDumper.Impl;

public class DefaultImpl implements Impl
{
    private static class CLoader
    {
        String name;
        String color;
        ClassLoader cl;
    }

    private static class CLoaderMap
    {
        private static CLoader EMPTY = new CLoader();
        int colorIdx = 0;
        Map loaderMap = new HashMap();

        public CLoader getCLoader(ClassLoader cl)
        {
            if (cl == null)
            {
                return EMPTY;
            }

            CLoader cloader = (CLoader)loaderMap.get(cl);
            if (cloader == null)
            {
                cloader = new CLoader();
                cloader.cl = cl;
                cloader.name = cl.getClass().getName() + "@" + Integer.toHexString(cl.hashCode());
                cloader.color = COLORS[colorIdx++];
                if (colorIdx > COLORS.length)
                {
                    throw new RuntimeException("Too Many ClassLoaders!?");
                }
                loaderMap.put(cl,cloader);
            }

            return cloader;
        }

        public CLoader getCLoader(Object obj)
        {
            return getCLoader(obj.getClass().getClassLoader());
        }
    }

    private static class JndiEntry
    {
        String name;
        String classname;
        Object value;
        CLoader cloader;

        public JndiEntry(String name, Object obj, CLoader cl)
        {
            this.name = name;
            this.value = obj;
            if (obj != null)
            {
                this.classname = obj.getClass().getName();
            }
            this.cloader = cl;
        }

        public JndiEntry(String name, String classname, Object obj, CLoader cl)
        {
            this.name = name;
            this.value = obj;
            this.classname = classname;
            this.cloader = cl;
        }
    }

    private static class NameComparator implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            JndiEntry e1 = (JndiEntry)o1;
            JndiEntry e2 = (JndiEntry)o2;
            return e1.name.compareTo(e2.name);
        }
    }

    private static final String COLORS[] = new String[] {
            // VGA color palette
            "#FF0000", // red
            "#FFFF00", // yellow
            "#00FF00", // lime
            "#00FFFF", // aqua / cyan
            "#0000FF", // blue
            "#FF00FF", // fuchsia / magenta
            "#800000", // maroon (dark red)
            "#808000", // olive (dark yellow / brown)
            "#008000", // green (dark lime)
            "#008080", // teal (dark aqua)
            "#000080", // navy (dark blue)
            "#800080" // purple (dark magenta)
    };

    public static final Impl INSTANCE = new DefaultImpl();

    private void collectEntries(String prefix, Object obj, CLoaderMap cloaders, List entries) throws NamingException
    {
        if (obj == null)
        {
            return;
        }

        CLoader cl = cloaders.getCLoader(obj);
        JndiEntry entry = new JndiEntry(prefix,obj,cl);
        entries.add(entry);

        if (obj instanceof Context)
        {
            Context ctx = (Context)obj;

            NamingEnumeration children = ctx.list("");
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
                    child = ctx.lookup(ncPair.getName());
                }
                catch (NamingException e)
                {
                    child = "<Lookup Error>";
                }
                collectEntries(parentName + ncPair.getName(),child,cloaders,entries);
            }
        }
    }

    public void dump(HttpServletResponse resp, String searchTerm, Object obj) throws IOException
    {
        resp.setContentType("text/html; charset=utf-8");
        PrintWriter out = resp.getWriter();

        out.println("<html>");
        out.println("<head>");
        out.println("<title>JNDI Dump of '" + searchTerm + "'</title>");
        out.println("</head>");

        out.println("<body style='font-family:san-serif;'>");
        out.println("<h3>JNDI Dump of '" + searchTerm + "'</h3>");

        if (obj == null)
        {
            out.println("(null - no value)");
            return;
        }
        else
        {
            // map of ClassLoader to CLoader objects
            CLoaderMap loaderMap = new CLoaderMap();
            // list of JndiEntry hits
            List entries = new ArrayList();
            // jndi prefix
            String prefix = searchTerm;
            if (!prefix.endsWith(":"))
            {
                prefix = prefix + "/";
            }
            // collect entries
            try
            {
                collectEntries(prefix,obj,loaderMap,entries);

                // print out tree sorted by entry names.
                dumpTreeSortedByEntryName(out,entries);

                // print out tree grouped by classloader
                dumpClassLoaderTree(out,loaderMap);
            }
            catch (NamingException e)
            {
                out.println("<h4>ERROR</h4>");
                out.println("<pre>");
                e.printStackTrace(out);
                out.println("</pre>");
            }
        }

        out.println("</body>");
        out.println("</html>");
    }

    private void dumpClassLoaderTree(PrintWriter out, ClassLoader cl)
    {
        out.println("<ul>");
        out.println("<li>");
        out.println("<p>");
        out.println(cl.getClass().getName() + "@" + Integer.toHexString(cl.hashCode()));
        out.println("<br/>toString() = " + safeStr(cl));

        if (cl instanceof URLClassLoader)
        {
            URLClassLoader ucl = (URLClassLoader)cl;
            URL urls[] = ucl.getURLs();
            for (int i = 0; i < urls.length; i++)
            {
                out.println("<br/>URL: " + safeStr(cl));
            }
        }
        out.println("</p>");

        if (cl.getParent() != null)
        {
            dumpClassLoaderTree(out,cl.getParent());
        }

        out.println("</li>");
        out.println("</ul>");
    }

    private void dumpClassLoaderTree(PrintWriter out, CLoaderMap loaderMap)
    {
        out.println("<hr/>");
        out.println("<h4>Classloader Tree</h4>");

        int i = 0;

        Iterator iter = loaderMap.loaderMap.values().iterator();
        while (iter.hasNext())
        {
            i++;
            CLoader cloader = (CLoader)iter.next();
            out.print("<p style='background-color:" + cloader.color + "'>");
            out.print("ClassLoader #" + i + ": " + cloader.cl.getClass().getName());
            out.println("@" + Integer.toHexString(cloader.cl.hashCode()) + "</p>");
            dumpClassLoaderTree(out,cloader.cl);
        }
    }

    private void dumpTreeSortedByEntryName(PrintWriter out, List entries)
    {
        out.println("<h4>JNDI Entries sorted by name</h4>");

        Collections.sort(entries,new NameComparator());

        out.println("<table cellspacing='0' cellpadding='1' border='1'>");

        out.print("<tr>");
        out.print("<th>#</th>");
        out.print("<th>Classloader</th>");
        out.print("<th>Type</th>");
        out.print("<th>Name</th>");
        out.print("<th>Value</th>");
        out.println("</tr>");

        for (int i = 0; i < entries.size(); i++)
        {
            out.print("<tr>");
            // Entry Number
            out.println("<td>" + i + "</td>");
            // ClassLoader
            JndiEntry entry = (JndiEntry)entries.get(i);
            if (entry.cloader != null)
            {
                String color = entry.cloader.color;
                if (color == null)
                {
                    color = "#808080";
                }
                out.print("<td style='background-color:" + color + "'>");
                if (entry.cloader.cl != null)
                {
                    ClassLoader cl = entry.cloader.cl;
                    out.print("<span title='" + cl.getClass().getName() + "\n" + cl.toString() + "'>");
                    out.print(cl.getClass().getSimpleName());
                    out.print("@" + Integer.toHexString(cl.hashCode()));
                    out.print("</span>");
                }
                out.println("</td>");
            }
            else
            {
                out.println("<td style='background-color:#c0c0c0'>(n/a)</td>");
            }
            // Type
            if (entry.value != null)
            {
                Class type = entry.value.getClass();
                out.println("<td><span title='" + type.getName() + "'>" + type.getSimpleName() + "</span></td>");
            }
            else
            {
                out.println("<td style='background-color:#c0c0c0'>(null)</td>");
            }
            // Name
            out.println("<td style='font-family:monospace'>" + entry.name + "</td>");
            // Value
            out.println("<td>" + entry.value + "</td>");
            out.println("</tr>");
        }

        out.println("</table>");
    }

    public boolean isKnown(Object obj)
    {
        return true;
    }

    private String safeStr(Object obj)
    {
        if (obj == null)
        {
            return safeStr("<null>");
        }
        return safeStr(obj.toString());
    }

    private String safeStr(String str)
    {
        StringBuilder ret = new StringBuilder();
        int len = str.length();
        for (int i = 0; i < len; i++)
        {
            char c = str.charAt(i);
            if (c == '<')
            {
                ret.append("&lt;");
            }
            else if (c == '>')
            {
                ret.append("&gt;");
            }
            else if (c == '&')
            {
                ret.append("&amp;");
            }
            else
            {
                ret.append(c);
            }
        }
        return ret.toString();
    }
}