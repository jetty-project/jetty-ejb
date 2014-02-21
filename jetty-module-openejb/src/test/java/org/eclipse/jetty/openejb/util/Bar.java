package org.eclipse.jetty.openejb.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Bar
{
    @SuppressWarnings("unused")
    private static class Data
    {
        private String name;
        private Properties props;

        public Data(String name, Properties props)
        {
            super();
            this.name = name;
            this.props = props;
        }

        public String getName()
        {
            return name;
        }

        public Properties getProps()
        {
            return props;
        }
    }

    private List<Data> datas = new ArrayList<>();

    public List<Data> getDatas()
    {
        return datas;
    }

    public void addData(String name, Properties props)
    {
        datas.add(new Data(name,props));
    }
}
