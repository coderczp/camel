/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.scr;

import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(JUnit4.class)
public class AbstractCamelRunnerTest {

    @Rule
    public TestName testName = new TestName();

    private Logger log = LoggerFactory.getLogger(getClass());

    @Before
    public void setUp() throws Exception {
        log.info("*******************************************************************");
        log.info("Test: " + testName.getMethodName());
        log.info("*******************************************************************");
    }

    @Test
    public void testDeepConfigure() throws Exception {
        ConcreteCamelRunner integration = new ConcreteCamelRunner();

        integration.activate(null, integration.getDefaultProperties());
        assertEquals("Overriding camelContextId failed (deep configure)", integration.getDefaultProperties().get("camelContextId"), integration.getContext().getName());
    }

    @Test
    public void testActivateDeactivate() {
        ConcreteCamelRunner integration = new ConcreteCamelRunner();
        try {
            integration.activate(null, integration.getDefaultProperties());
            Thread.sleep(AbstractCamelRunner.START_DELAY + 1000);
            integration.deactivate();
            assertTrue("Camel context has not started.", integration.camelContextStarted == 1);
            assertTrue("Camel context has not stopped.", integration.camelContextStopped == 1);
            assertTrue("Not enough routes added.", integration.routeAdded == 2);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testPrepareRunStop() {
        ConcreteCamelRunner integration = new ConcreteCamelRunner();
        try {
            integration.prepare(null, integration.getDefaultProperties());
            integration.run();
            do {
                Thread.sleep(500);
            } while (integration.getContext().isStartingRoutes());
            integration.stop();
            assertTrue("Camel context has not started.", integration.camelContextStarted == 1);
            assertTrue("Camel context has not stopped.", integration.camelContextStopped == 1);
            assertTrue("Not enough routes added.", integration.routeAdded == 2);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testDelayedStart() {
        ConcreteCamelRunner integration = new ConcreteCamelRunner();
        try {
            integration.activate(null, integration.getDefaultProperties());
            Thread.sleep(2000);
            integration.gotCamelComponent(null);
            Thread.sleep(AbstractCamelRunner.START_DELAY - 1000);
            assertTrue("Camel context has started too early", integration.camelContextStarted == 0);
            Thread.sleep(2000);
            assertTrue("Camel context has not started.", integration.camelContextStarted == 1);
            integration.deactivate();
            assertTrue("Camel context has not stopped.", integration.camelContextStopped == 1);
            assertTrue("Not enough routes added.", integration.routeAdded == 2);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testDelayedStartCancel() {
        ConcreteCamelRunner integration = new ConcreteCamelRunner();

        Map<String, String> properties = integration.getDefaultProperties();
        properties.put("from", "notfound:something");
        properties.put("camelroute.id", "test/notfound-mock");

        try {
            integration.activate(null, properties);
            Thread.sleep(AbstractCamelRunner.START_DELAY - 1000);
            integration.deactivate();
            assertTrue("Routes have been added.", integration.routeAdded == 0);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}