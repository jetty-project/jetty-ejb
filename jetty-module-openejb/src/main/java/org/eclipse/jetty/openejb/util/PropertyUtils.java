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

import java.util.Properties;

/***
 * Property Utils
 */
public class PropertyUtils
{
    public static void defaultIfUnset(Properties properties, String key, String defValue)
    {
        String val = properties.getProperty(key);
        if (val == null)
        {
            properties.setProperty(key,defValue);
        }
    }
}
