
package org.fcrepo.integration.syndication;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static org.fcrepo.services.PathService.OBJECT_PATH;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_OBJECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration({"/spring-test/repo.xml", "/spring-test/test-container.xml"})
public class RSSIT extends AbstractResourceIT {

    final private Logger logger = LoggerFactory.getLogger(RSSIT.class);
    
    @Autowired
    Repository repo;
    
    JcrTools jcrTools = new JcrTools(true);
    
    @Test
    public void testRSS() throws Exception {

        final Session session = repo.login();
        final Node object =
                jcrTools.findOrCreateChild(session.getNode(OBJECT_PATH), "RSSTESTPID");
        object.addMixin(FEDORA_OBJECT);
        session.save();
        session.logout();

        HttpGet getRSSMethod = new HttpGet(serverAddress + "/rss");
        HttpResponse response = client.execute(getRSSMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        String content = EntityUtils.toString(response.getEntity());
        logger.debug("Retrieved RSS feed:\n" + content);
        assertTrue("Didn't find the test PID in RSS!", compile("RSSTESTPID",
                DOTALL).matcher(content).find());
    }
}