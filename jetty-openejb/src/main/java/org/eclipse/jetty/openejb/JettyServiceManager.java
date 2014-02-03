package org.eclipse.jetty.openejb;

import java.util.Set;
import java.util.TreeSet;

import org.apache.openejb.server.SimpleServiceManager;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class JettyServiceManager extends SimpleServiceManager
{
    private static final Logger LOG = Log.getLogger(JettyServiceManager.class);
    private static final Set<String> SUPPORTED_SERVICES = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    
    static {
        SUPPORTED_SERVICES.add("httpejbd");
        SUPPORTED_SERVICES.add("ejbd");
        SUPPORTED_SERVICES.add("ejbds");
        SUPPORTED_SERVICES.add("admin");
    }

    public JettyServiceManager() {
        setServiceManager(this);
    }
    
    @Override
    protected boolean accept(String serviceName)
    {
        boolean ret = SUPPORTED_SERVICES.contains(serviceName);
        LOG.debug("accept({}): {}",serviceName,ret);
        return ret;
    }
}
