//
//  ========================================================================
//  Copyright (c) 1995-2014 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package jetty.demo.ejb;

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;

public class Locator
{
    public static RaceInfo getRaceInfo(Context context) throws NamingException, RemoteException, CreateException
    {
        Object ref = context.lookup(Services.RACE_INFO);
        if (ref == null)
        {
            throw new javax.naming.NameNotFoundException("Unable to find RaceInfoHome reference");
        }
        RaceInfoHome home = (RaceInfoHome)PortableRemoteObject.narrow(ref,RaceInfoHome.class);
        return home.create();
    }

    public static TeamInfo getTeamInfo(Context context) throws NamingException, RemoteException, CreateException
    {
        Object ref = context.lookup(Services.TEAM_INFO);
        if (ref == null)
        {
            throw new javax.naming.NameNotFoundException("Unable to find TeamInfoHome reference");
        }
        TeamInfoHome home = (TeamInfoHome)PortableRemoteObject.narrow(ref,TeamInfoHome.class);
        return home.create();
    }

    private Locator()
    {
    }
}
