/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 * 
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 * 
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.cloud.deployer.process;

import fish.payara.cloud.deployer.process.ConfigurationSerializer;
import fish.payara.cloud.deployer.inspection.contextroot.ContextRootConfiguration;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author jonathan coustick
 */
public class JsonSerilizationTest {
    
    @Test
    public void testConfigBeanSerialization() throws IOException {
        DeploymentProcessState state = new DeploymentProcessState(new Namespace("foo", "bar"), "foobar", File.createTempFile("abc", null));
        SimpleConfiguration config = new SimpleConfiguration();
        ProcessAccessor.addConfiguration(state, config);
        Jsonb jsonb = JsonbBuilder.create();
        String serializedState = jsonb.toJson(state);
        JsonParser jsonParser = Json.createParser(new StringReader(serializedState));
        jsonParser.next();
        JsonObject deploymentState = jsonParser.getObject();
        System.out.println(deploymentState.toString());
        Assert.assertNotNull(deploymentState.getString("id")); //will be a UUID

        JsonObject jsonConfig = deploymentState.getJsonObject("configurations");
        JsonArray order = jsonConfig.getJsonArray("order");
        Assert.assertEquals(1, order.size());
        JsonObject orderObject = order.getJsonObject(0);
        Assert.assertEquals("TEST", orderObject.getString("kind"));
        Assert.assertEquals("TESTID", orderObject.getString("id"));
        JsonObject configObject = jsonConfig.getJsonObject("kind").getJsonObject("TEST").getJsonObject("TESTID");
        Assert.assertNotNull(configObject);
        JsonArray keyArray = configObject.getJsonArray("keys");
        Assert.assertEquals(3, keyArray.size());
        JsonObject aKeyObject = keyArray.getJsonObject(0);
        Assert.assertTrue(aKeyObject.getString("name").contains("key"));
        Assert.assertFalse(aKeyObject.getBoolean("required"));
        Assert.assertEquals("DEFAULT", aKeyObject.getString("default"));
        JsonObject valuesObject = configObject.getJsonObject("values");
        Assert.assertEquals("Value1", valuesObject.getString("key1"));
        Assert.assertEquals("value2", valuesObject.getString("key2"));
        Assert.assertEquals("value3", valuesObject.getString("key3"));
    }


    @Test
    public void testConfigBeanDeserialization() throws IOException {
        DeploymentProcessState state = new DeploymentProcessState(new Namespace("foo", "bar"), "foobar", File.createTempFile("abc", null));
        SimpleConfiguration config = new SimpleConfiguration();
        ProcessAccessor.addConfiguration(state, config);
        Jsonb jsonb = JsonbBuilder.create();
        String serializedState = jsonb.toJson(state);
        JsonParser jsonParser = Json.createParser(new StringReader(serializedState));
        jsonParser.next();
        JsonObject deploymentState = jsonParser.getObject();
        System.out.println(deploymentState.toString());
        Assert.assertNotNull(deploymentState.getString("id")); //will be a UUID

        JsonObject jsonConfig = deploymentState.getJsonObject("configurations");


        ConfigBean deserialized = jsonb.fromJson(jsonConfig.toString(), ConfigBean.class);

        Assert.assertEquals(1, deserialized.getOrder().size());
        ConfigBean.ConfigId orderObject = deserialized.getOrder().get(0);
        Assert.assertEquals("TEST", orderObject.getKind());
        Assert.assertEquals("TESTID", orderObject.getId());
        ConfigBean.Config configObject = deserialized.getKind().get("TEST").get("TESTID");
        Assert.assertNotNull(configObject);
        List<ConfigBean.Key> keyArray = configObject.getKeydetails();
        Assert.assertEquals(3, keyArray.size());
        ConfigBean.Key aKeyObject = keyArray.get(0);
        Assert.assertTrue(aKeyObject.getName().contains("key"));
        Assert.assertFalse(aKeyObject.isRequired());
        Assert.assertEquals("DEFAULT", aKeyObject.getDefaultValue().get());
        Map<String, String> valuesObject = configObject.getValues();
        Assert.assertEquals("Value1", valuesObject.get("key1"));
        Assert.assertEquals("value2", valuesObject.get("key2"));
        Assert.assertEquals("value3", valuesObject.get("key3"));
    }
    
    private class SimpleConfiguration extends Configuration {
        
        Map<String, String> MAP = Map.of("key1", "Value1", "key2", "value2", "key3", "value3");
        
        public SimpleConfiguration() {
            super("TESTID");
            ProcessAccessor.updateConfiguration(this, MAP);
        }

        @Override
        public String getKind() {
            return "TEST";
        }

        @Override
        public Set<String> getKeys() {
            return MAP.keySet();
        }

        @Override
        public Optional<String> getDefaultValue(String key) {
            return Optional.of("DEFAULT");
        }
    
    }   
    
}
