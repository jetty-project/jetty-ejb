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

import java.io.File;

public final class EnvUtil
{
    /**
     * Search System Properties for a value.
     * 
     * @param lookupKeys
     *            the keys to search, returning the first value found.
     * @return the value found, or null if not found.
     */
    public static String lookupPropertyValue(String... lookupKeys)
    {
        for (String lookupKey : lookupKeys)
        {
            String val = System.getProperty(lookupKey);
            if (val != null)
            {
                return val;
            }
        }
        return null;
    }

    /**
     * Lookup a path, using the pathStr specified, if null, or not found use the defValue instead.
     * 
     * @param pathStr
     *            the path string to possibly use. (not used if path is null or does not exist)
     * @param defValue
     *            the default value if pathStr not valid. (no validation of default File path performed)
     * @return the File path reference.
     */
    public static File lookupPath(String pathStr, String defValue)
    {
        File path = new File(separators(pathStr));
        if (path.exists())
        {
            return path;
        }
        return new File(separators(defValue));
    }

    /**
     * Convert path separators to the System path separators.
     * <p>
     * This helps ensure that the paths provided equally as well on unix / osx / windows.
     * 
     * @param path
     *            the raw path to convert
     * @return the converted path
     */
    public static String separators(String path)
    {
        StringBuilder ret = new StringBuilder();
        for (char c : path.toCharArray())
        {
            if ((c == '/') || (c == '\\'))
            {
                ret.append(File.separatorChar);
            }
            else
            {
                ret.append(c);
            }
        }
        return ret.toString();
    }
}
