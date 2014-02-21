package jetty.demo.ejb.commands;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletResponse;

import jetty.demo.ejb.Command;
import jetty.demo.ejb.TeamInfo;
import jetty.demo.ejb.TeamInfoBean;
import jetty.demo.ejb.TeamInfoHome;

public class FindTeamInfoCommand implements Command
{
    public void exec(HttpServletResponse resp, String arg) throws Throwable
    {
        resp.setContentType("text/plain");
        PrintWriter out = resp.getWriter();

        List jndiRefs = new ArrayList();
        jndiRefs.add("java:global/raceApp/ejbTeam/TeamInfoEJB");
        jndiRefs.add("java:global/raceApp/ejbTeam/TeamInfoEJB!jetty.demo.ejb.TeamInfoHome");
        jndiRefs.add("java:openejb/local/TeamInfoEJBRemoteHome");
        jndiRefs.add("java:openejb/remote/TeamInfoEJBRemoteHome");
        jndiRefs.add("java:openejb/global/global/raceApp/ejbTeam/TeamInfoEJB!jetty.demo.ejb.TeamInfoHome/");
        jndiRefs.add("java:openejb/global/global/raceApp/ejbTeam/TeamInfoEJB");
        jndiRefs.add("java:openejb/Deployment/TeamInfoEJB/jetty.demo.ejb.TeamInfoHome!Home");
        jndiRefs.add("java:openejb/Deployment/TeamInfoEJB/jetty.demo.ejb.TeamInfo");
        jndiRefs.add("java:openejb/Deployment/TeamInfoEJB/jetty.demo.ejb.TeamInfo!Remote");

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
                    out.println("   TeamInfoHome (direct) => " + info.toString());
                    valid = true;
                }

                TeamInfoHome home = (TeamInfoHome)javax.rmi.PortableRemoteObject.narrow(val,TeamInfoHome.class);
                out.println("   TeamInfoHome (narrowed) => " + home.toString());

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

    public String getName()
    {
        return "find-team-info";
    }
}
