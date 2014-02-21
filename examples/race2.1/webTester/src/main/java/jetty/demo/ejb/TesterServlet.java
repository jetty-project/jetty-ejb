package jetty.demo.ejb;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jetty.demo.ejb.commands.FindTeamInfoCommand;
import jetty.demo.ejb.commands.JndiDumpDefaultCommand;
import jetty.demo.ejb.commands.JndiDumpNativeCommand;
import jetty.demo.ejb.commands.JndiLookupCommand;
import jetty.demo.ejb.commands.TeamInfoCommand;

public class TesterServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    private Map commands = new HashMap();

    private void addCommand(Command command)
    {
        commands.put('/' + command.getName() + '/',command);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String pathInfo = req.getPathInfo();

        try
        {
            boolean found = false;
            Iterator keys = commands.keySet().iterator();
            while (keys.hasNext())
            {
                String prefix = (String)keys.next();
                if (pathInfo.startsWith(prefix))
                {
                    found = true;
                    String arg = pathInfo.substring(prefix.length());
                    Command command = (Command)commands.get(prefix);
                    command.exec(resp,arg);
                }
            }

            if (!found)
            {
                resp.setContentType("text/plain");
                PrintWriter out = resp.getWriter();

                out.println("ERROR: Unknown command: \"" + pathInfo + "\"");
                out.println("Available Commands:");

                Iterator iter = commands.keySet().iterator();
                while (iter.hasNext())
                {
                    out.println("  " + iter.next() + "{arg}");
                }
            }
        }
        catch (Throwable t)
        {
            resp.setContentType("text/plain");
            PrintWriter out = resp.getWriter();

            out.println();
            out.print("ERROR: ");
            t.printStackTrace(out);
            t.printStackTrace(System.err);
        }
    }

    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);

        addCommand(new TeamInfoCommand());
        addCommand(new FindTeamInfoCommand());
        addCommand(new JndiDumpDefaultCommand());
        addCommand(new JndiDumpNativeCommand());
        addCommand(new JndiLookupCommand());
    }
}
