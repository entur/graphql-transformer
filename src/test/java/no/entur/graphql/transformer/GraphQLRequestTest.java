/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package no.entur.graphql.transformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import graphql.language.IntValue;
import graphql.language.StringValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileReader;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class GraphQLRequestTest {


    private ObjectMapper objectMapper = new ObjectMapper();

    private GraphQLRequest req;

    @Before
    public void setup() throws Exception {
        String msg = CharStreams.toString(new FileReader("src/test/resources/no/entur/grapqhl/transformer/request_with_variables.json"));
        req = new GraphQLRequest(msg, objectMapper);
    }


    @Test
    public void testGetArgumentValueForInlineOneLevelArgument() throws Exception {
        Assert.assertEquals(3, req.getArgumentValue("trip", "numTripPatterns").asInt().intValue());
    }

    @Test
    public void testGetArgumentValueForInlineMultiLevelArgument() throws Exception {
        Assert.assertEquals(1.0, req.getArgumentValue("trip", "from", "coordinates", "longitude").asDouble().doubleValue(), 0.1);
    }

    @Test
    public void testGetArgumentValueForVariableOneLevelArgument() throws Exception {
        Assert.assertEquals("Oslo s, Oslo", req.getArgumentValue("trip", "from", "name").asString());
    }

    @Test
    public void testGetArgumentValueForVariableMultilevelLevelArgument() throws Exception {
        Assert.assertEquals(1.0, req.getArgumentValue("trip", "to", "coordinates", "longitude").asDouble(), 0.1);
    }

    @Test
    public void testGetUnknownArgumentIsNull() throws Exception {
        Assert.assertTrue(req.getArgumentValue("trip", "xxx").isNull());
    }

    @Test
    public void testGetArgumentForUnknownFieldIsNull() throws Exception {
        Assert.assertTrue(req.getArgumentValue("xxx", "numTripPatterns").isNull());
    }

    @Test
    public void testGetArgumentForUnknownVariableIsNull() throws Exception {
        Assert.assertTrue(req.getArgumentValue("trip", "to", "juks").isNull());
    }

    @Test
    public void testSetArgumentExisting() {
        Assert.assertEquals(3, req.getArgumentValue("trip", "numTripPatterns").asInt().intValue());
        req.setArgumentValue("trip", "numTripPatterns", new IntValue(BigInteger.TEN));
        Assert.assertEquals(10, req.getArgumentValue("trip", "numTripPatterns").asInt().intValue());
    }

    @Test
    public void testSetArgumentNew() {
        String newVal = "newVal!";
        Assert.assertTrue(req.getArgumentValue("trip", "newArg").isNull());
        req.setArgumentValue("trip", "newArg", new StringValue(newVal));
        Assert.assertEquals(newVal, req.getArgumentValue("trip", "newArg").asString());

        Assert.assertTrue(req.writeValueAsString().contains(newVal));
    }

    @Test
    public void testAddSelectionField() {

        List<String> path = Arrays.asList("tripPatterns", "newParent", "newChild");
        Assert.assertFalse("Request should not contain new field prior to adding it", req.containsField("trip", path));

        List<String> addedPath = req.addSelectionField("trip", path);
        Assert.assertEquals(Arrays.asList("tripPatterns", "newParent"), addedPath);

        Assert.assertTrue(req.containsField("trip", path));

        req.writeValueAsString().contains("newChild");
    }


    @Test
    public void testAddSelectionFieldWithFragments() throws Exception {
        String msg = CharStreams.toString(new FileReader("src/test/resources/no/entur/grapqhl/transformer/request_with_fragments.json"));
        GraphQLRequest fragmentReq = new GraphQLRequest(msg, objectMapper);

        List<String> path = Arrays.asList("tripPatterns", "legs", "rentedBike");
        Assert.assertFalse("Request should not contain new field prior to adding it", fragmentReq.containsField("trip", path));

        List<String> addedPath = fragmentReq.addSelectionField("trip", path);
        Assert.assertEquals(Arrays.asList("tripPatterns", "legs", "rentedBike"), addedPath);

        Assert.assertTrue(fragmentReq.containsField("trip", path));

        fragmentReq.writeValueAsString().contains("rentedBike");
    }

    @Test
    public void testRemoveUnusedVariablesBeforeWritingValueAsString() throws Exception {

        String valueFromReplacedVariable = "$to";
        Assert.assertTrue("Value should be present before replacement", req.writeValueAsString().contains(valueFromReplacedVariable));

        req.setArgumentValue("trip", "to", new StringValue("replaceToValue"));

        Assert.assertFalse("Value should not be present after replacement", req.writeValueAsString().contains(valueFromReplacedVariable));
    }


}
