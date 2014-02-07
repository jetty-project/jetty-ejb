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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class Dumper
{
    private static void appendToString(Appendable out, Object obj) throws IOException
    {
        if (obj instanceof CharSequence)
        {
            out.append('"');
            out.append(obj.toString());
            out.append('"');
        }
        else
        {
            out.append(obj.toString());
        }
    }

    public static CharSequence describe(Object obj)
    {
        StringWriter writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        Set<Object> seen = new HashSet<>();
        try
        {
            dump(out,"",obj,seen);
        }
        catch (IOException e)
        {
            e.printStackTrace(out);
        }
        return writer.getBuffer();
    }

    private static void dump(Appendable out, String indent, Object obj, Set<Object> seen) throws IOException
    {
        if (obj == null)
        {
            out.append("<null>\n");
            return;
        }
        else
        {
            out.append(String.format("(%s@%x)",obj.getClass().getName(),obj.hashCode()));
        }
        ObjectMap beans = new ObjectMap(obj);
        int size = beans.size();
        if (size == 0)
        {
            return;
        }

        List<String> keys = new ArrayList<>();
        keys.addAll(beans.keySet());
        Collections.sort(keys);

        size = keys.size();
        int i = 0;
        for (String key : keys)
        {
            Object val = beans.get(key);
            out.append("\n");
            out.append(indent).append(" + ").append(key).append(" = ");
            dumpValue(out,smartIndent(indent,i,size),val,seen);
            i++;
        }
        if (i < size)
        {
            out.append(indent).append(" |\n");
        }
    }

    private static void dumpValue(Appendable out, String indent, Object val, Set<Object> seen) throws IOException
    {
        if (val == null)
        {
            out.append("<null>");
            return;
        }

        if (val.getClass().isArray())
        {
            int size = Array.getLength(val);
            out.append("(array[]::size=").append(Integer.toString(size));
            out.append(")");
            for (int i = 0; i < size; i++)
            {
                out.append("\n");
                out.append(indent).append(" +[");
                out.append(Integer.toString(i)).append("] ");
                Object entry = Array.get(val,i);
                dumpValue(out,smartIndent(indent,i,size),entry,seen);
            }
            return;
        }

        if (isSimple(val))
        {
            appendToString(out,val);
            return;
        }

        if (val instanceof Properties)
        {
            Properties props = (Properties)val;
            int size = props.size();
            out.append("(Properties::size=").append(Integer.toString(size));
            out.append(")");

            @SuppressWarnings("unchecked")
            Enumeration<String> names = (Enumeration<String>)props.propertyNames();
            while (names.hasMoreElements())
            {
                String name = names.nextElement();
                String pval = props.getProperty(name);

                out.append("\n");
                out.append(indent).append(" +[\"");
                out.append(name).append("\"] = \"");
                out.append(pval).append("\"");
            }
            return;
        }

        if (val instanceof Map<?, ?>)
        {
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>)val;
            int size = map.size();
            out.append("(").append(map.getClass().getSimpleName());
            out.append("::size=").append(Integer.toString(size));
            out.append(")");
            int i = 0;
            for (Map.Entry<Object, Object> entry : map.entrySet())
            {
                out.append("\n");
                out.append(indent).append(" +[");
                appendToString(out,entry.getKey());
                out.append("] = ");
                dumpValue(out,smartIndent(indent,i,size),entry.getValue(),seen);
                i++;
            }
            return;
        }

        if (val instanceof Collection<?>)
        {
            @SuppressWarnings("rawtypes")
            Collection coll = (Collection)val;
            int size = coll.size();
            out.append("(").append(coll.getClass().getSimpleName());
            out.append("::size=").append(Integer.toString(size));
            out.append(")");
            int i = 0;
            for (Object entry : coll)
            {
                out.append("\n");
                out.append(indent).append(" + ");
                dumpValue(out,smartIndent(indent,i,size),entry,seen);
                i++;
            }
            return;
        }

        if (seen.contains(val) && !isSimple(val))
        {
            out.append(String.format("(%s@%x) <seen already>",val.getClass().getName(),val.hashCode()));
            return;
        }

        seen.add(val);

        dump(out,indent,val,seen);
    }

    private static boolean isSimple(Object obj)
    {
        Class<?> val = obj.getClass();
        return (val.isPrimitive() || //
                (val == java.lang.CharSequence.class) || //
                (val == java.lang.String.class) || //
                (val == java.lang.Integer.class) || //
                (val == java.lang.Long.class) || //
                (val == java.lang.Short.class) || //
                (val == java.lang.Byte.class) || //
                (val == java.lang.Character.class) || //
                (val == java.lang.Double.class) || //
                (val == java.lang.Float.class) || //
                (val == java.lang.Boolean.class) || //
        (val == java.net.URI.class //
        ));
    }

    private static String smartIndent(String indent, int i, int size)
    {
        return indent + (((i + 1) == size)?"    ":" |  ");
    }
}
