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

package no.entur.graphql.transformer.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JsonMessageArray extends JsonMessage {

    private List<JsonNode> elements;

    public JsonMessageArray(String payload, ObjectMapper objectMapper) {
        super(objectMapper);

        TypeReference<List<ObjectNode>> listType = new TypeReference<List<ObjectNode>>() {
        };
        try {
            elements = getObjectMapper().readValue(payload, listType);
        } catch (IOException ioE) {
            throw new RuntimeException("Failed to parseJson payload as JsonArray: " + ioE.getMessage(), ioE);
        }
    }

    public JsonMessageArray(ObjectMapper objectMapper, List<JsonNode> elements) {
        super(objectMapper);
        this.elements = elements;
    }

    @Override
    public JsonMessage deepCopy() {
        List<JsonNode> copy = elements.stream().map(e -> (JsonNode) e.deepCopy()).collect(Collectors.toList());
        return new JsonMessageArray(objectMapper, copy);
    }

    @Override
    public void transformNode(String elementName, Function<JsonNode, JsonNode> mappingFunction) {
        elements.forEach(m -> transformNode(m, elementName, mappingFunction));
    }

    @Override
    public <T extends JsonNode> List<T> findChildNodes(Function<Map.Entry<String, JsonNode>, Boolean> matcher) {
        throw new RuntimeException("findChildNodes not yet implemented for Array messages");
    }

    @Override
    protected List<JsonNode> getValue() {
        return elements;
    }

    @Override
    public void mergeElements(JsonMessage other, String elementName) {
        List<JsonNode> otherResults = (List<JsonNode>) other.getValue();

        // TODO - no good

        if (otherResults != null) {

            for (int i = 0; i < elements.size(); i++) {

                ObjectNode res = (ObjectNode) elements.get(i);

                if (otherResults.size() > i) {
                    // TODO check same?
                    ObjectNode resOther = (ObjectNode) otherResults.get(i);
                    res.set(elementName, mergeArrayNodes(res.get(elementName), resOther.get(elementName)));
                }

            }

        }

// TODO if other has more res?
    }
}
