package org.eclipse.jetty.openejb;

import java.net.URL;

import org.apache.openejb.classloader.WebAppEnricher;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class JettyClassEnricher implements WebAppEnricher
{
    private static final Logger LOG = Log.getLogger(JettyClassEnricher.class);
    private final Server server;

    public JettyClassEnricher(Server server)
    {
        this.server = server;
    }

    @Override
    public URL[] enrichment(ClassLoader webappClassLoader)
    {
        LOG.debug("enrichment({})",webappClassLoader);
        return new URL[0];
    }
}
