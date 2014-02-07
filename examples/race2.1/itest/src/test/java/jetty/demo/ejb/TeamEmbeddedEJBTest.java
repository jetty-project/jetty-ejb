package jetty.demo.ejb;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import javax.ejb.embeddable.EJBContainer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TeamEmbeddedEJBTest
{
    private static EJBContainer ejbContainer;

    @BeforeClass
    public static void setUp() throws Exception
    {
        ejbContainer = EJBContainer.createEJBContainer();
    }

    @AfterClass
    public static void tearDown()
    {
        if (ejbContainer != null)
        {
            ejbContainer.close();
        }
    }

    @Test
    public void testCreate() throws Exception
    {
        TeamInfoHome team = (TeamInfoHome)ejbContainer.getContext().lookup("java:global/raceApp/ejbTeam/TeamInfoEJB!jetty.demo.ejb.TeamInfoHome");
        TeamInfo info = team.create();
        assertThat(info,notNullValue());
    }
}
