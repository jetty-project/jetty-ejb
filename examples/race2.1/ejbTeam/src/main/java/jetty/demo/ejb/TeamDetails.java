package jetty.demo.ejb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TeamDetails implements Serializable
{
    private static final long serialVersionUID = -8487988991235706786L;
    
    private String id;
    private String teamName;
    private String boatName;
    private List members = new ArrayList();

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getTeamName()
    {
        return teamName;
    }

    public void setTeamName(String teamName)
    {
        this.teamName = teamName;
    }

    public String getBoatName()
    {
        return boatName;
    }

    public void setBoatName(String boatName)
    {
        this.boatName = boatName;
    }

    public List getMembers()
    {
        return members;
    }

    public void setMembers(List members)
    {
        this.members = members;
    }
}
