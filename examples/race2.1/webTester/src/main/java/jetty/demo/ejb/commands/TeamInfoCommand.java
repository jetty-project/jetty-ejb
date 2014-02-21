package jetty.demo.ejb.commands;

import java.io.PrintWriter;
import java.util.List;

import javax.naming.InitialContext;
import javax.servlet.http.HttpServletResponse;

import jetty.demo.ejb.Command;
import jetty.demo.ejb.Locator;
import jetty.demo.ejb.TeamDetails;
import jetty.demo.ejb.TeamInfo;

public class TeamInfoCommand implements Command
{
    public void exec(HttpServletResponse resp, String arg) throws Throwable
    {
        resp.setContentType("text/plain");
        PrintWriter out = resp.getWriter();

        String teamId = arg;

        TeamInfo teamInfo = Locator.getTeamInfo(new InitialContext());
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

    public String getName()
    {
        return "team-info";
    }
}
