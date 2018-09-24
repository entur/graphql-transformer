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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class JsonMessageSingle extends JsonMessage {

    protected ObjectNode element;

    public JsonMessageSingle(String payload, ObjectMapper objectMapper) {
        super(objectMapper);
        try {
            element =
                    getObjectMapper().readValue(payload, ObjectNode.class);
        } catch (IOException ioE) {
            throw new RuntimeException("Failed to transform elements: " + ioE.getMessage(), ioE);
        }
    }

    public JsonMessageSingle(ObjectMapper objectMapper, ObjectNode element) {
        super(objectMapper);
        this.element = element;
    }

    @Override
    public void transformNode(String elementName, Function<JsonNode, JsonNode> mappingFunction) {
        transformNode(element, elementName, mappingFunction);
    }

    @Override
    public <T extends JsonNode> List<T> findChildNodes(Function<Map.Entry<String, JsonNode>, Boolean> matcher) {
        return findChildNodes(element, matcher);
    }

    @Override
    public JsonMessage deepCopy() {
        return new JsonMessageSingle(objectMapper, element.deepCopy());
    }

    @Override
    protected ObjectNode getValue() {
        return element;
    }

    @Override
    public void mergeElements(final JsonMessage other, String elementName) {

        final List<JsonNode> otherResultElements = other.findChildNodes(child -> child.getKey().equals(elementName));

        if (!otherResultElements.isEmpty()) {
            transformNode(elementName, thisArray -> mergeArrayNodes(thisArray, otherResultElements.get(0)));
        }

    }

}
