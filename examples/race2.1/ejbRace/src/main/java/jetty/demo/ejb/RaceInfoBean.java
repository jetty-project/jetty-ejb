package jetty.demo.ejb;

import java.rmi.RemoteException;

import javax.ejb.SessionBean;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import jetty.demo.common.AbstractSessionBean;


/**
 * @ejb.bean
 *      display-name="RaceEJB"
 *      name="RaceEJB"
 *      view-type="remote"
 *      impl-class-name="jetty.demo.ejb.RaceInfoBean"
 * @ejb.home
 *      generate="remote"
 *      remote-class="jetty.demo.ejb.RaceInfoHome"
 * @ejb.interface
 *      generate="remote"
 *      remote-class="jetty.demo.ejb.RaceInfo"
 *
 */
public class RaceInfoBean extends AbstractSessionBean implements SessionBean
{
    private static final long serialVersionUID = 4595804217975092966L;

    /**
     * @ejb.interface-method
     *  view-type="remote"
     */
    public RaceDetails getDetails()
    {
        RaceDetails details = new RaceDetails();
        details.setKilometerLength(101);
        details.clearStandings();

        try
        {
            InitialContext context = new InitialContext();
            TeamInfo info = (TeamInfo)context.lookup(TeamInfo.class.getName());

            details.addStanding(info.getTeamDetails("skuttles"));
            details.addStanding(info.getTeamDetails("app-less"));
            details.addStanding(info.getTeamDetails("doop-sloop"));
        }
        catch (NamingException e)
        {
            e.printStackTrace();
        }
        catch (RemoteException e)
        {
            e.printStackTrace();
        }
        return details;
    }
}
