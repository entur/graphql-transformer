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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class JsonMessage {

    protected ObjectMapper objectMapper;

    public JsonMessage(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public abstract void transformNode(String elementName, Function<JsonNode, JsonNode> mappingFunction);

    public abstract <T extends JsonNode> List<T> findChildNodes(Function<Map.Entry<String, JsonNode>, Boolean> matcher);

    public abstract JsonMessage deepCopy();

    public String writeValueAsString() {
        try {
            return getObjectMapper().writeValueAsString(getValue());
        } catch (IOException ioE) {
            throw new RuntimeException("Failed to write payload as string: " + ioE.getMessage(), ioE);
        }
    }

    public abstract void mergeElements(JsonMessage other, String elementName);

    protected abstract Object getValue();

    protected ObjectMapper getObjectMapper() {
        return objectMapper;
    }


    protected <T extends JsonNode> List<T> findChildNodes(JsonNode jsonNode, Function<Map.Entry<String, JsonNode>, Boolean> matcher) {
        List<T> nodes = new ArrayList<>();
        jsonNode.fields().forEachRemaining(child -> {

            if (matcher.apply(child)) {
                nodes.add((T) child.getValue());
            } else {
                nodes.addAll(findChildNodes(child.getValue(), matcher));
            }

        });
        return nodes;
    }

    protected void transformNode(JsonNode jsonNode, String elementName, Function<JsonNode, JsonNode> mappingFunction) {
        jsonNode.fields().forEachRemaining(child -> {

            if (elementName.equals(child.getKey())) {
                child.setValue(mappingFunction.apply(child.getValue()));

            } else {
                transformNode(child.getValue(), elementName, mappingFunction);
            }
        });
    }

    protected JsonNode mergeArrayNodes(JsonNode o1, JsonNode o2) {
        if (o1 == null) {
            return o2;
        } else if (o2 == null) {
            return o1;
        }

        if (!(o1 instanceof ArrayNode) || !(o2 instanceof ArrayNode)) {
            throw new IllegalArgumentException("Unable to merge ");
        }

        ArrayNode a1 = (ArrayNode) o1;
        ArrayNode a2 = (ArrayNode) o2;

        ArrayNode merged = new ArrayNode(JsonNodeFactory.instance);

        a1.elements().forEachRemaining(a -> merged.add(a));
        a2.elements().forEachRemaining(a -> merged.add(a));

        return merged;
    }


}
