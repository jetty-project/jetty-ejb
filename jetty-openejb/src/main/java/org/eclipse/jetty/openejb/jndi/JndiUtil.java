package org.eclipse.jetty.openejb.jndi;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

public final class JndiUtil
{
    public static void dump(Context context)
    {
        try
        {
            System.out.printf("Context: [%s] %s%n",context.getNameInNamespace(),context);
            NamingEnumeration<NameClassPair> en = context.list("/*");
            while (en.hasMore())
            {
                NameClassPair entry = en.next();
                System.out.printf(" %s: (%s) %s%n",entry.getName(),entry.getClassName(),entry);
            }
        }
        catch (NamingException e)
        {
            e.printStackTrace();
        }
    }

    public static void dump()
    {
        try
        {
            dump(new InitialContext());
        }
        catch (NamingException e)
        {
            e.printStackTrace();
        }
    }
}
