package jetty.demo.ejb;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.CreateException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TesterServlet extends HttpServlet
{
    private static final Logger LOG = Logger.getLogger(TesterServlet.class.getName());
    private static final long serialVersionUID = 1L;

    private static final Map COMMANDS;

    static
    {
        COMMANDS = new HashMap();
        COMMANDS.put("/team-info/",findMethod("doTeamInfo"));
        COMMANDS.put("/find-team-info/",findMethod("doFindTeamInfo"));
        COMMANDS.put("/jndi-lookup/",findMethod("doJndiLookup"));
    }

    private static Method findMethod(String commandMethodName)
    {
        Class[] parameterTypes = new Class[] { PrintWriter.class, String.class };
        try
        {
            return TesterServlet.class.getDeclaredMethod(commandMethodName,parameterTypes);
        }
        catch (NoSuchMethodException e)
        {
            throw new RuntimeException("Unable to find Method: " + commandMethodName,e);
        }
        catch (SecurityException e)
        {
            throw new RuntimeException("Unable to access Method: " + commandMethodName,e);
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String pathInfo = req.getPathInfo();

        resp.setContentType("text/plain");
        PrintWriter out = resp.getWriter();

        try
        {
            boolean found = false;
            Iterator keys = COMMANDS.keySet().iterator();
            while (keys.hasNext())
            {
                String command = (String)keys.next();
                if (pathInfo.startsWith(command))
                {
                    found = true;
                    String arg = pathInfo.substring(command.length());
                    execCommand(command,out,arg);
                }
            }
            if (!found)
            {
                out.println("ERROR: Unknown command: \"" + pathInfo + "\"");
                out.println("Available Commands:");

                Iterator iter = COMMANDS.keySet().iterator();
                while (iter.hasNext())
                {
                    out.println("  " + iter.next() + "{arg}");
                }
            }
        }
        catch (Throwable t)
        {
            out.println();
            out.print("ERROR: ");
            t.printStackTrace(out);
            t.printStackTrace(System.err);
        }
    }

    private void execCommand(String commandId, PrintWriter out, String arg) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        Method method = (Method)COMMANDS.get(commandId);
        if (method == null)
        {
            throw new RuntimeException("Unable to find method for command: " + commandId);
        }
        Object args[] = new Object[] { out, arg };
        method.invoke(this,args);
    }

    public void doJndiLookup(PrintWriter out, String name) throws NamingException
    {
        LOG.log(Level.FINE, "doJndiLookup(" + name + ")");
        InitialContext context = new InitialContext();
        out.println("JNDI[" + name + "]");

        Object result = context.lookup(name);
        String prefix = name;
        if (!prefix.endsWith(":"))
        {
            prefix = prefix + "/";
        }
        dumpJndiValue(out,prefix,result.getClass().getName(),result);
    }

    private void dumpJndiValue(PrintWriter out, String prefix, String type, Object value) throws NamingException
    {
        if (value != null)
        {
            if (value instanceof Context)
            {
                dumpContext(out,prefix,(Context)value);
            }
            else
            {
                out.println(prefix + "\t" + type + "\t" + value);
            }
        }
        else
        {
            out.println(prefix + "\t" + type + "\t<null>");
        }
    }

    private void dumpContext(PrintWriter out, String prefix, Context context)
    {
        out.println(prefix + "/\t" + context.getClass().getName() + "\t" + context);

        try
        {
            NamingEnumeration children = context.list("");
            String parentName = prefix;
            if (!parentName.endsWith(":"))
            {
                parentName = parentName + "/";
            }

            while (children.hasMore())
            {
                NameClassPair ncPair = (NameClassPair)children.next();
                Object child = null;
                try
                {
                    child = context.lookup(ncPair.getName());
                }
                catch (NamingException e)
                {
                    child = "<Lookup Error>";
                }
                dumpJndiValue(out,parentName + ncPair.getName(),ncPair.getClassName(),child);
            }
        }
        catch (NamingException e)
        {
            out.println("<Error in context.list()>");
            e.printStackTrace(out);
        }
    }

    public void doTeamInfo(PrintWriter out, String teamId) throws NamingException, RemoteException, CreateException
    {
        TeamInfo teamInfo = findTeamInfo(out);
        TeamDetails team = teamInfo.getTeamDetails(teamId);
        out.println("team.id = " + team.getId());
        out.println("team.teamName = " + team.getTeamName());
        out.println("team.boatName = " + team.getBoatName());
        List members = team.getMembers();
        out.println("team.members.size = " + members.size());
        for (int i = 0; i < members.size(); i++)
        {
            out.println("member[" + i + "] = " + members.get(i));
        }
    }

    private TeamInfo findTeamInfo(PrintWriter out) throws NamingException, RemoteException, CreateException
    {
        InitialContext context = new InitialContext();
        TeamInfoHome home = (TeamInfoHome)context.lookup("java:global/raceApp/ejbTeam/TeamInfoEJB");
        return home.create();
    }

    public void doFindTeamInfo(PrintWriter out, String arg) throws NamingException
    {
        List jndiRefs = new ArrayList();
//        jndiRefs.add(TeamInfo.class.getName());
//        jndiRefs.add(TeamInfoHome.class.getName());
//        jndiRefs.add(TeamInfoBean.class.getName());
        jndiRefs.add("openejb:global/global/raceApp/ejbTeam/TeamInfoEJB!jetty.demo.ejb.TeamInfoHome");
        jndiRefs.add("openejb:global/global/raceApp/ejbTeam/TeamInfoEJB");
        jndiRefs.add("java:global/raceApp/ejbTeam/TeamInfoEJB");
        jndiRefs.add("java:global/raceApp/ejbTeam/TeamInfoEJB");
        jndiRefs.add("java:global/raceApp/ejbTeam/TeamInfoEJB/");
        jndiRefs.add("java:global/raceApp/ejbTeam/TeamInfoEJB!jetty.demo.ejb.TeamInfoHome");
        jndiRefs.add("java:global/raceApp/ejbTeam/TeamInfoEJB!jetty.demo.ejb.TeamInfoHome/");
        jndiRefs.add("java:openejb/local/TeamInfoEJBRemoteHome");
        jndiRefs.add("java:openejb/local/TeamInfoEJBRemoteHome/");
        jndiRefs.add("java:openejb/remote/TeamInfoEJBRemoteHome");
        jndiRefs.add("java:openejb/remote/TeamInfoEJBRemoteHome/");
        jndiRefs.add("java:openejb/global/global/raceApp/ejbTeam/TeamInfoEJB!jetty.demo.ejb.TeamInfoHome/");
        jndiRefs.add("java:openejb/global/global/raceApp/ejbTeam/TeamInfoEJB");
        jndiRefs.add("java:openejb/global/global/raceApp/ejbTeam/TeamInfoEJB/");
        jndiRefs.add("java:openejb/Deployment/TeamInfoEJB");
        jndiRefs.add("java:openejb/Deployment/TeamInfoEJB/");
        jndiRefs.add("java:openejb/Deployment/TeamInfoEJB/jetty.demo.ejb.TeamInfoHome!Home");
        jndiRefs.add("java:openejb/Deployment/TeamInfoEJB/jetty.demo.ejb.TeamInfoHome!Home/");
        jndiRefs.add("java:openejb/Deployment/TeamInfoEJB/jetty.demo.ejb.TeamInfo");
        jndiRefs.add("java:openejb/Deployment/TeamInfoEJB/jetty.demo.ejb.TeamInfo/");
        jndiRefs.add("java:openejb/Deployment/TeamInfoEJB/jetty.demo.ejb.TeamInfo!Remote");
        jndiRefs.add("java:openejb/Deployment/TeamInfoEJB/jetty.demo.ejb.TeamInfo!Remote/");

        InitialContext context = new InitialContext();

        for (int i = 0; i < jndiRefs.size(); i++)
        {
            String ref = (String)jndiRefs.get(i);
            out.print("lookup(\"" + ref + "\")\t");
            try
            {
                boolean valid = false;

                Object val = context.lookup(ref);
                if (val == null)
                {
                    out.println("<null>");
                    continue;
                }
                else
                {
                    out.println("(" + val.getClass().getName() + ")");
                }

                if (val instanceof TeamInfo)
                {
                    TeamInfo info = (TeamInfo)val;
                    out.println("   TeamInfo => " + info.toString());
                    valid = true;
                }

                if (val instanceof TeamInfoBean)
                {
                    TeamInfoBean info = (TeamInfoBean)val;
                    out.println("   TeamInfoBean => " + info.toString());
                    valid = true;
                }

                if (val instanceof TeamInfoHome)
                {
                    TeamInfoHome info = (TeamInfoHome)val;
                    out.println("   TeamInfoHome => " + info.toString());
                    valid = true;
                }

                if (!valid)
                {
                    out.println("   <not a valid TeamInfo>");
                }
            }
            catch (NamingException e)
            {
                out.println("<lookup error:" + e.getMessage() + ">");
            }
        }
    }
}
