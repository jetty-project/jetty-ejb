package jetty.demo.ejb.commands;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.servlet.http.HttpServletResponse;

import jetty.demo.ejb.Command;
import jetty.demo.ejb.JndiDumper;

public class JndiDumpNativeCommand implements Command
{
    private static final Logger LOG = Logger.getLogger(JndiDumpNativeCommand.class.getName());

    public void exec(HttpServletResponse resp, String name) throws Throwable
    {
        LOG.log(Level.FINE,"doJndiDumpNative(" + name + ")");

        InitialContext context = new InitialContext();

        Object result = context.lookup(name);
        String prefix = name;
        if (!prefix.endsWith(":"))
        {
            prefix = prefix + "/";
        }

        JndiDumper.Impl dumper = JndiDumper.getImpl(result);
        dumper.dump(resp,name,result);
    }

    public String getName()
    {
        return "jndi-dump-native";
    }
}
