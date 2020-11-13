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

package no.entur.graphql.transformer.argument;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Streams;
import graphql.language.EnumValue;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonNodeArgumentValue implements ArgumentValue {

    private JsonNode jsonNode;

    private JsonNode jsonParent;

    private String jsonFieldName;

    public JsonNodeArgumentValue(JsonNode jsonNode, JsonNode parent, String fieldName) {
        this.jsonNode = jsonNode;
        this.jsonParent = parent;
        this.jsonFieldName = fieldName;
    }

    @Override
    public Integer asInt() {
        if (jsonNode != null && jsonNode.isInt()) {
            return jsonNode.asInt();
        }
        return null;
    }

    public boolean isNull() {
        if (jsonNode != null && !jsonNode.isNull()) {
            return false;
        }
        return true;
    }

    @Override
    public Boolean asBoolean() {
        if (jsonNode != null && jsonNode.isBoolean()) {
            return jsonNode.asBoolean();
        }
        return null;
    }

    public Double asDouble() {
        if (jsonNode != null && jsonNode.isNumber()) {
            return jsonNode.asDouble();
        }
        return null;
    }

    public String asString() {
        if (jsonNode != null && jsonNode.isTextual()) {
            return jsonNode.asText();
        }
        return null;
    }

    public List<ArgumentValue> asList() {
        if (jsonNode != null && jsonNode.isArray()) {
            return Streams.stream(jsonNode.elements()).map(jn -> new JsonNodeArgumentValue(jn, null, null)).collect(Collectors.toList());
        }
        return null;
    }

    public Map<String, ArgumentValue> asMap() {
        if (jsonNode != null && jsonNode.isObject()) {
            Map<String, ArgumentValue> mapNode = new HashMap<>();
            Streams.stream(jsonNode.fields()).forEach(e -> mapNode.put(e.getKey(), new JsonNodeArgumentValue(e.getValue(), null, null)));
            return mapNode;
        }
        return null;
    }

    public void setIterable(Iterable values) {
        if (jsonNode != null && jsonNode.isArray()) {

            ArrayNode arrayNode = ((ArrayNode) jsonNode);
            arrayNode.removeAll();

            for (Object o : values) {
                if (o instanceof String) {
                    arrayNode.add((String) o);
                } else if (o instanceof Enum) {
                    arrayNode.add(((Enum) o).name());
                } else if (o instanceof EnumValue) {
                    arrayNode.add(((EnumValue) o).getName());
                } else {
                    throw new IllegalArgumentException("Unsupported type of iterable element: " + o);
                }
            }
        }
    }

    public void setString(String string) {
        if (jsonNode != null && jsonNode.isTextual() && jsonParent instanceof ObjectNode) {
            ((ObjectNode) jsonParent).set(jsonFieldName, TextNode.valueOf(string));
        } else {
            throw new IllegalArgumentException("Unable to set string value for value: " + this);
        }

    }

    public void setInteger(Integer integer) {
        if (jsonNode != null && jsonNode.isInt() && jsonParent instanceof ObjectNode) {
            ((ObjectNode) jsonParent).set(jsonFieldName, BigIntegerNode.valueOf(BigInteger.valueOf(integer)));
        } else {
            throw new IllegalArgumentException("Unable to set integer value for value: " + this);
        }

    }


    public <T extends Object> void setValue(T value) {
        if (value instanceof Iterable) {
            setIterable((Iterable) value);
        } else if (value instanceof String) {
            setString((String) value);
        } else if (value instanceof Integer) {
            setInteger((Integer) value);
        } else {
            throw new IllegalArgumentException("Unable to set value: " + value);
        }
    }

    @Override
    public JsonNodeArgumentValue deepCopy() {
        JsonNode jsonNodeCopy = jsonNode == null ? null : jsonNode.deepCopy();
        JsonNode jsonParentCopy = jsonParent == null ? null : jsonParent.deepCopy();
        return new JsonNodeArgumentValue(jsonNodeCopy, jsonParentCopy, jsonFieldName);
    }

    @Override
    public String toString() {
        return "JsonNodeArgumentValue{" +
                       "jsonNode=" + jsonNode +
                       ", jsonParent=" + jsonParent +
                       ", jsonFieldName='" + jsonFieldName + '\'' +
                       '}';
    }
}
