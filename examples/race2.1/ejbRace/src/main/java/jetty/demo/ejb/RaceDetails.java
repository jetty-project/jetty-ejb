package jetty.demo.ejb;

import java.util.ArrayList;
import java.util.List;

import jetty.demo.ejb.TeamDetails;


public class RaceDetails
{
    private int kilometerLength;
    private List standings = new ArrayList();

    public int getKilometerLength()
    {
        return kilometerLength;
    }

    public void setKilometerLength(int kilometerLength)
    {
        this.kilometerLength = kilometerLength;
    }

    public List getStandings()
    {
        return standings;
    }

    public void setStandings(List standings)
    {
        this.standings = standings;
    }
    
    public void clearStandings()
    {
        this.standings.clear();
    }
    
    public void addStanding(TeamDetails team)
    {
        this.standings.add(team);
    }
}
