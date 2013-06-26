/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.integration.syndication;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.fcrepo.services.ObjectService;
import org.junit.Test;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration({"/spring-test/repo.xml", "/spring-test/eventing.xml",  "/spring-test/test-container.xml"})
public class RSSIT extends AbstractResourceIT {

    final private Logger logger = LoggerFactory.getLogger(RSSIT.class);
    
    @Autowired
    Repository repo;

	@Autowired
	ObjectService objectService;

    
    JcrTools jcrTools = new JcrTools(true);
    
    @Test
    public void testRSS() throws Exception {

        final Session session = repo.login();
        objectService.createObject(session, "/RSSTESTPID");
        session.save();
        session.logout();

        HttpGet getRSSMethod = new HttpGet(serverAddress + "/fcr:rss");
        HttpResponse response = client.execute(getRSSMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        String content = EntityUtils.toString(response.getEntity());
        logger.debug("Retrieved RSS feed:\n" + content);
        assertTrue("Didn't find the test PID in RSS!", compile("RSSTESTPID",
                DOTALL).matcher(content).find());
    }
}