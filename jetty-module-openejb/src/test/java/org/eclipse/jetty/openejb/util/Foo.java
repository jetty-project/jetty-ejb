package org.eclipse.jetty.openejb.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Foo
{
    public String id = "foo";
    public Boolean init = null;
    public String val = "something";
    public Map<String, String> mapping;
    public List<String> members;
    public int[] ages;
    public Bar bar;

    public Foo()
    {
        mapping = new HashMap<>();
        mapping.put("a","b");
        mapping.put("c","d");
        mapping.put("z","woah");
        members = new ArrayList<>();
        members.add("joe");
        members.add("jesse");
        members.add("jan");
        ages = new int[] { 35, 40, 48 };

        Properties props;
        bar = new Bar();
        props = new Properties();
        props.setProperty("pmc","chair");
        bar.addData("greg",props);
        props = new Properties();
        props.setProperty("emeritus","true");
        bar.addData("gary",props);
        props = new Properties();
        props.setProperty("emeritus","true");
        props.setProperty("sponsor","true");
        bar.addData("gillian",props);
        bar.addData("goober",props);
    }
}
