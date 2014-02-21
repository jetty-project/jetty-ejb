package org.eclipse.jetty.openejb.util;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.net.URISyntaxException;

import org.junit.Test;

public class DumperTest
{
    @Test
    public void testDumpFoo() throws URISyntaxException
    {
        CharSequence result = Dumper.describe(new Foo());
        System.out.println(result);
        assertThat("result",result.toString(),containsString("Foo"));
    }
}
