package jetty.demo.ejb;

import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class TesterServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String command = req.getPathInfo();

        resp.setContentType("text/plain");
        PrintWriter out = resp.getWriter();

        try
        {
            if (command.startsWith("/team-info/"))
            {
                String id = command.substring("/team-info/".length());
                dumpTeamInfo(out,id);
            }
            else
            {
                out.println("ERROR: Unknown command: \"" + command + "\"");
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

    private void dumpTeamInfo(PrintWriter out, String teamId) throws NamingException, RemoteException
    {
        InitialContext context = new InitialContext();
        TeamInfo teamInfo = (TeamInfo)context.lookup(TeamInfo.class.getName());
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
}
