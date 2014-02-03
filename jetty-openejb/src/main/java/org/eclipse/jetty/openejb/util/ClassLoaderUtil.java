package org.eclipse.jetty.openejb.util;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class ClassLoaderUtil
{
    private static final Logger LOG = Log.getLogger(ClassLoaderUtil.class);

    public static void whereIs(String classname)
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String classAsResource = classname.replace('.','/') + ".class";
        int foundCount = 0;
        int clCount = 0;
        while (cl != null)
        {
            try
            {
                Enumeration<URL> urls = cl.getResources(classAsResource);
                LOG.debug("Looking for <{}> in {}",classAsResource,cl);
                if (urls.hasMoreElements())
                {
                    clCount++;
                }
                ;
                while (urls.hasMoreElements())
                {
                    URL url = urls.nextElement();
                    LOG.debug("  Found: {}",url.toExternalForm());
                    foundCount++;
                }
            }
            catch (IOException e)
            {
                String msg = String.format("Class <%s> not found in %s",classname,cl);
                LOG.debug(msg,e);
            }
            finally
            {
                cl = cl.getParent();
            }
        }
        LOG.debug("Found <{}> {} times across {} classloaders",classname,foundCount,clCount);
    }
}
