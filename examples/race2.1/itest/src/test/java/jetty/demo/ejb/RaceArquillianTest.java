package jetty.demo.ejb;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;

import org.eclipse.jetty.toolchain.test.FS;
import org.eclipse.jetty.toolchain.test.IO;
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
    static
    {
        System.out.printf("logging = %s%n",System.getProperty("java.util.logging.config.file"));
    }

    @Deployment
    public static EnterpriseArchive createDeployment() throws IOException
    {
        File pomFile = MavenTestingUtils.getProjectFile("/pom.xml");

        MavenResolverSystem resolver = Maven.resolver();
        PomEquippedResolveStage pomStage = resolver.loadPomFromFile(pomFile);
        MavenStrategyStage mvnStage = pomStage.resolve("org.eclipse.jetty.demo:raceApp:ear:1.0-SNAPSHOT");
        File[] ears = mvnStage.withoutTransitivity().asFile();
        assertThat("ears found",ears,notNullValue());
        assertThat("ear",ears.length,greaterThanOrEqualTo(1));

        File tmpDir = MavenTestingUtils.getTargetTestingDir(RaceArquillianTest.class.getName());
        FS.ensureEmpty(tmpDir);
        File mainEarFile = new File(tmpDir,"raceApp.ear");

        IO.copyFile(ears[0],mainEarFile);

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

    @Test
    public void testFindTeamInfo() throws UnknownHostException, IOException, URISyntaxException
    {
        String resp = new SimpleRequest(deploymentUrl.toURI()).getString("/webTester/tester/find-team-info/");
        assertThat("response",resp,containsString("cherry"));
    }

    @Test
    public void saveAllResults() throws UnknownHostException, IOException, URISyntaxException
    {
        File outputDir = MavenTestingUtils.getTargetTestingDir("webtester-reports");
        FS.ensureDirExists(outputDir);
        String reports[][] = new String[][] {
                // [path to report], [output report filename]
                { "/webTester/tester/jndi-dump/java:", "tomee-jndi-dump-java.html" }, //
                { "/webTester/tester/jndi-dump-native/java:", "tomee-jndi-dump-native-java.txt" }, //
                { "/webTester/tester/jndi-dump/openejb:", "tomee-jndi-dump-openejb.html" }, //
                { "/webTester/tester/jndi-dump-native/openejb:", "tomee-jndi-dump-native-openejb.txt" }, //
                { "/webTester/tester/find-team-info/", "tomee-find-team-info.txt" }, //
        };
        for (String[] req : reports)
        {
            String resp = new SimpleRequest(deploymentUrl.toURI()).getString(req[0]);
            File outputFile = new File(outputDir,req[1]);
            StringReader reader = null;
            FileWriter writer = null;
            try
            {
                reader = new StringReader(resp);
                writer = new FileWriter(outputFile,false);
                IO.copy(reader,writer);
                System.out.println("Wrote file: " + outputFile);
            }
            finally
            {
                IO.close(reader);
                IO.close(writer);
            }
        }
    }
}
