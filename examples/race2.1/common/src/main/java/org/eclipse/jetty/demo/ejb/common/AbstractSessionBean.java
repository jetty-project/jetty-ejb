package org.eclipse.jetty.demo.ejb.common;

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

public abstract class AbstractSessionBean implements SessionBean
{
    private static final long serialVersionUID = -8211504840245406630L;
    private SessionContext sessionContext;

    public void ejbActivate() throws EJBException, RemoteException
    {
        /* do nothing */
    }

    public void ejbPassivate() throws EJBException, RemoteException
    {
        /* do nothing */
    }

    public void ejbRemove() throws EJBException, RemoteException
    {
        /* do nothing */
    }

    public void ejbCreate() throws CreateException
    {
        /* do nothing */
    }

    public void setSessionContext(SessionContext context) throws EJBException, RemoteException
    {
        this.sessionContext = context;
    }

    public SessionContext getSessionContext()
    {
        return sessionContext;
    }
}
