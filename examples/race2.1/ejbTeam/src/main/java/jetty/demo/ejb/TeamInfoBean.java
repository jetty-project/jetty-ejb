package jetty.demo.ejb;

import javax.ejb.SessionBean;

import jetty.demo.common.AbstractSessionBean;


/**
 * @ejb.bean
 *    display-name="TeamInfoEJB" 
 *    name="TeamInfoEJB" 
 *    view-type="remote" 
 *    impl-class-name="jetty.demo.ejb.TeamInfoBean"
 * @ejb.home 
 *    generate="remote"
 *    remote-class="jetty.demo.ejb.TeamInfoHome"
 * @ejb.interface 
 *    generate="remote"
 *    remote-class="jetty.demo.ejb.TeamInfo"
 */
public class TeamInfoBean extends AbstractSessionBean implements SessionBean
{
    private static final long serialVersionUID = 4565923843053835408L;

    /**
     * @ejb.interface-method
     *   view-type="remote"
     */
    public TeamDetails getTeamDetails(String teamId)
    {
        TeamDetails details = new TeamDetails();
        details.setId(teamId);
        details.setTeamName("Example Team");
        details.setBoatName("Example Boat");
        details.getMembers().add("Captain");
        details.getMembers().add("First Mate");
        return details;
    }
}
