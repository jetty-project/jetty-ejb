package jetty.demo.ejb;

import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinates;
import org.junit.Test;

public class MavenCoordTest
{
    @Test
    public void testFindEar()
    {
        MavenCoordinate coord = MavenCoordinates.createCoordinate("org.eclipse.jetty.demo:raceApp:ear:1.0-SNAPSHOT");
        System.out.println(coord);
        System.out.printf("groupId    = %s%n",coord.getGroupId());
        System.out.printf("artifactId = %s%n",coord.getArtifactId());
        System.out.printf("version    = %s%n",coord.getVersion());
        System.out.printf("type       = %s%n",coord.getType());
        System.out.printf("packaging  = %s%n",coord.getPackaging());
        System.out.printf("classifier = %s%n",coord.getClassifier());
    }
}
