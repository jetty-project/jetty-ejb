package org.eclipse.jetty.openejb;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import org.apache.openejb.core.ParentClassLoaderFinder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class JettyParentClassLoaderFinder implements ParentClassLoaderFinder
{
    private static class LoggingClassLoader extends ClassLoader
    {
        private static final Logger LOG = Log.getLogger(JettyParentClassLoaderFinder.LoggingClassLoader.class);

        private final ClassLoader delegate;

        public LoggingClassLoader(ClassLoader delegate)
        {
            LOG.debug("delegate {}",delegate);
            this.delegate = delegate;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException
        {
            Class<?> ret = delegate.loadClass(name);
            LOG.debug(".loadClass({}): {}",name,ret);
            return ret;
        }

        @Override
        public URL getResource(String name)
        {
            URL url = delegate.getResource(name);
            LOG.debug(".getResource({}): {}",name,url);
            return url;
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException
        {
            Enumeration<URL> ret = delegate.getResources(name);
            LOG.debug(".getResources({}): {}",name,ret);
            return ret;
        }

        @Override
        public InputStream getResourceAsStream(String name)
        {
            InputStream ret = delegate.getResourceAsStream(name);
            LOG.debug(".getResourceAsStream({}): {}",name,ret);
            return ret;
        }
    }

    private ClassLoader parent;

    public JettyParentClassLoaderFinder(Server server)
    {
        if (Thread.currentThread().getContextClassLoader() != null)
        {
            parent = new LoggingClassLoader(Thread.currentThread().getContextClassLoader());
            return;
        }

        parent = new LoggingClassLoader(ClassLoader.getSystemClassLoader());
    }

    @Override
    public ClassLoader getParentClassLoader(ClassLoader fallback)
    {
        return (parent != null)?parent:fallback;
    }
}
