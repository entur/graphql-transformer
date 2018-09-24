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

package no.entur.graphql.transformer.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.List;

public class JsonNodeUtil {

    /**
     * Remove a field from a JsonNode, if exists.
     *
     * @param node node to remove the field from
     * @param path path to the field
     */
    public static void removeField(JsonNode node, List<String> path) {
        if (node.isArray()) {
            Iterator<JsonNode> elements = node.elements();

            while (elements.hasNext()) {
                JsonNode child = elements.next();
                removeField(child, path);
            }

        } else {

            String fieldName = path.get(0);
            if (path.size() == 1) {
                if (node.isObject()) {
                    ((ObjectNode) node).remove(fieldName);
                }
            } else if (node.isObject()) {
                JsonNode child = node.findPath(fieldName);
                removeField(child, path.subList(1, path.size()));
            }
        }
    }
}
