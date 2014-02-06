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

package org.eclipse.jetty.openejb.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public final class Dumper
{
    public static CharSequence describe(Object obj)
    {
        StringWriter writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        Set<Object> seen = new HashSet<>();
        describe(obj,out,"",seen);
        return writer.getBuffer();
    }

    private static void describe(Object obj, PrintWriter out, String indent, Set<Object> seen)
    {
        if (obj == null)
        {
            out.printf("%s<null>%n",indent);
            return;
        }
        out.printf("%s(%s) %s%n",indent,obj.getClass().getName(),obj.toString());

        ObjectMap objMap = new ObjectMap(obj);
        List<String> keys = new ArrayList<>();
        keys.addAll(objMap.keySet());
        Collections.sort(keys);
        for (String key : keys)
        {
            Object val = objMap.get(key);
            out.print(indent);
            out.print(" .");
            out.print(key);
            out.print(" = ");
            describeValue(val,out,indent,seen);
            out.println();
        }
    }

    private static void describeValue(Object val, PrintWriter out, String indent, Set<Object> seen)
    {
        if (val == null)
        {
            out.print("<null>");
            return;
        }

        if (val.getClass().isArray())
        {
            int length = Array.getLength(val);
            out.printf("[ size=%d%n",length);
            for (int i = 0; i < length; i++)
            {
                out.printf("%s  [%d]: ",indent,i);
                Object entry = Array.get(val,i);
                describeValue(entry,out,indent + "  ",seen);
                out.println();
            }
            out.print("]");
            return;
        }

        if (isSimple(val))
        {
            if (val instanceof CharSequence)
            {
                out.printf("\"%s\"",val.toString());
            }
            else
            {
                out.print(val.toString());
            }
            return;
        }

        if (val instanceof Properties)
        {
            Properties props = (Properties)val;
            out.printf("[Properties.size=%d",props.size());
            @SuppressWarnings("unchecked")
            Enumeration<String> names = (Enumeration<String>)props.propertyNames();
            while (names.hasMoreElements())
            {
                String name = names.nextElement();
                String pval = props.getProperty(name);
                out.printf("%s   \"%s\" = \"%s\"%n",name,pval);
            }
            out.print(" ]");
            return;
        }

        if (val instanceof Collection<?>)
        {
            @SuppressWarnings("rawtypes")
            Collection coll = (Collection)val;
            out.printf("< %s.size=%d",coll.getClass().getSimpleName(),coll.size());
            int i = 0;
            for (Object entry : coll)
            {
                out.printf("%n%s   <%d> ",indent,i++);
                describeValue(entry,out,indent + "    ",seen);
            }
            out.print(" >");
            return;
        }

        if (seen.contains(val) && !isSimple(val))
        {
            out.printf("(%s@%x) <seen already>",val.getClass().getName(),val.hashCode());
            return;
        }

        seen.add(val);

        describe(val,out,indent + "  ",seen);
    }

    private static boolean isSimple(Object obj)
    {
        Class<?> val = obj.getClass();
        return (val.isPrimitive() || //
                val == java.lang.CharSequence.class || //
                val == java.lang.String.class || //
                val == java.lang.Integer.class || //
                val == java.lang.Long.class || //
                val == java.lang.Short.class || //
                val == java.lang.Byte.class || //
                val == java.lang.Character.class || //
                val == java.lang.Double.class || //
                val == java.lang.Float.class || //
                val == java.lang.Boolean.class || //
        val == java.net.URI.class //
        );
    }
}
