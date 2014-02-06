package org.eclipse.jetty.openejb.util;

import org.apache.openejb.assembler.classic.JndiEncInfo;
import org.junit.Test;

public class DumperTest
{
    @Test
    public void testDumpJndiEnc()
    {
        System.out.println(Dumper.describe(new JndiEncInfo()));
    }
}
