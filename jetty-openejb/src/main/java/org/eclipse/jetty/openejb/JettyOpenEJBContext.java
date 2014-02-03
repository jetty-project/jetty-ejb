package org.eclipse.jetty.openejb;

import org.apache.openejb.assembler.classic.AppInfo;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Simple storage of OpenEJB appInfo and associated Jetty WebAppContext
 */
public class JettyOpenEJBContext
{
    public AppInfo appInfo;
    public WebAppContext webapp;

    @Override
    public String toString()
    {
        return String.format("JettyOpenEJBContext[appInfo=%s,webapp=%s]",appInfo,webapp);
    }
}
