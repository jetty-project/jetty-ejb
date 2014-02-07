package jetty.demo.ejb;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;

import jetty.demo.ejb.logging.LoggingUtil;

import org.eclipse.jetty.toolchain.test.MavenTestingUtils;
import org.eclipse.jetty.toolchain.test.SimpleRequest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolverSystem;
import org.jboss.shrinkwrap.resolver.api.maven.MavenStrategyStage;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class RaceArquillianTest
{
    static {
        System.out.printf("logging = %s%n",System.getProperty("java.util.logging.config.file"));
    }
    
    @Deployment
    public static EnterpriseArchive createDeployment()
    {
        File pomFile = MavenTestingUtils.getProjectFile("/pom.xml");
        
        MavenResolverSystem resolver = Maven.resolver();
        PomEquippedResolveStage pomStage = resolver.loadPomFromFile(pomFile);
        MavenStrategyStage mvnStage = pomStage.resolve("org.eclipse.jetty.demo:raceApp:ear:1.0-SNAPSHOT");
        File[] ears = mvnStage.withoutTransitivity().asFile();
        assertThat("ears found", ears, notNullValue());
        assertThat("ear", ears.length, greaterThanOrEqualTo(1));

        File mainEarFile = ears[0];

        EnterpriseArchive ear = ShrinkWrap.createFromZipFile(EnterpriseArchive.class,mainEarFile);
        return ear;
    }

    @ArquillianResource
    private URL deploymentUrl;

    @Test
    public void testJndiLookupJava() throws UnknownHostException, IOException, URISyntaxException
    {
        String resp = new SimpleRequest(deploymentUrl.toURI()).getString("/webTester/tester/jndi-lookup/java:");
        assertThat("response",resp,containsString("cherry"));
    }
}
