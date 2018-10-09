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


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import graphql.language.Argument;
import graphql.language.Definition;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.Node;
import graphql.language.ObjectValue;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.language.Value;
import graphql.language.VariableDefinition;
import graphql.language.VariableReference;
import graphql.parser.Parser;
import no.entur.graphql.transformer.argument.ArgumentValue;
import no.entur.graphql.transformer.argument.GraphQLArgumentValue;
import no.entur.graphql.transformer.argument.JsonNodeArgumentValue;
import no.entur.graphql.transformer.argument.NullArgumentValue;
import no.entur.graphql.transformer.json.JsonMessageSingle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class GraphQLRequest extends JsonMessageSingle {

    public Document query;

    private Parser parser = new Parser();

    private GraphQLPrinter graphQLPrinter = new GraphQLPrinter();

    public GraphQLRequest(String payload, ObjectMapper objectMapper) {
        super(payload, objectMapper);
        parseQueryDocument();
    }


    public GraphQLRequest(ObjectMapper objectMapper, ObjectNode element) {
        super(objectMapper, element);
        parseQueryDocument();
    }

    private GraphQLRequest(ObjectMapper objectMapper, ObjectNode element, Document query) {
        super(objectMapper, element);
        this.query = query;
    }

    public boolean isQuery() {
        return query != null;
    }

    private void parseQueryDocument() {
        JsonNode queryNode = element.get("query");
        if (queryNode != null && queryNode.isTextual()) {
            this.query = parser.parseDocument(queryNode.asText());
        }
    }

    @Override
    public GraphQLRequest deepCopy() {
        Document queryCopy = query == null ? null : query.deepCopy();
        return new GraphQLRequest(objectMapper, element.deepCopy(), queryCopy);
    }

    @Override
    public String writeValueAsString() {
        removeUnusedVariables();
        if (query != null) {
            element.set("query", TextNode.valueOf(graphQLPrinter.print(query)));
        }
        return super.writeValueAsString();
    }


    public ArgumentValue getArgumentValue(String fieldName, String argumentName, String... path) {
        if (query != null) {
            for (Definition definition : query.getDefinitions()) {
                if (definition instanceof OperationDefinition) {

                    for (Selection selection : ((OperationDefinition) definition).getSelectionSet().getSelections()) {
                        if (selection instanceof Field) {
                            Field field = (Field) selection;
                            if (fieldName.equals(field.getName())) {
                                for (Argument argument : field.getArguments()) {
                                    if (argumentName.equals(argument.getName())) {
                                        return getValue(argument.getValue(), path);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return NullArgumentValue.INSTANCE;
    }


    /**
     * Add selection field if not already included. Will also add any missing fields on path.
     *
     * @return path to the first added field
     */
    public List<String> addSelectionField(String operationName, String... path) {
        return addSelectionField(operationName, Arrays.asList(path));
    }


    /**
     * Add selection field if not already included. Will also add any missing fields on path.
     *
     * @return path to the first added field
     */
    public List<String> addSelectionField(String operationName, List<String> path) {
        List<String> addedPath = new ArrayList<>();
        Field field = getOperationField(operationName);

        boolean added = false;

        if (field != null) {
            for (String fieldName : path) {
                if (!added) {
                    addedPath.add(fieldName);
                }

                SelectionSet selectionSet = field.getSelectionSet();
                field = getField(selectionSet, fieldName);
                if (field == null) {
                    field = addField(selectionSet, fieldName);
                    added = true;
                }
            }
        }

        if (added) {
            return addedPath;
        }

        return new ArrayList<>();
    }

    public boolean containsField(String operationName, List<String> path) {
        Field field = getOperationField(operationName);

        if (field == null) {
            return false;
        }
        for (String fieldName : path) {
            SelectionSet selectionSet = field.getSelectionSet();
            field = getField(selectionSet, fieldName);
            if (field == null) {
                return false;
            }
        }
        return true;
    }

    private Field getOperationField(String operationName) {
        if (query != null) {
            return query.getDefinitions().stream().filter(OperationDefinition.class::isInstance).map(OperationDefinition.class::cast)
                           .map(od -> getField(od.getSelectionSet(), operationName)).filter(Objects::nonNull).findFirst().orElse(null);
        }
        return null;
    }


    private FragmentDefinition getFragmentDefinition(String fragmentName) {
        if (query != null) {
            return query.getDefinitions().stream().filter(FragmentDefinition.class::isInstance).map(FragmentDefinition.class::cast)
                           .filter(fd -> Objects.equals(fragmentName, fd.getName())).filter(Objects::nonNull).findFirst().orElse(null);
        }
        return null;
    }

    private Field getField(SelectionSet selectionSet, String fieldName) {
        for (Selection selection : selectionSet.getSelections()) {

            if (selection instanceof Field) {
                Field field = (Field) selection;
                if (Objects.equals(fieldName, field.getName())) {
                    return field;
                }
            } else if (selection instanceof FragmentSpread) {
                FragmentSpread fragmentSpread = (FragmentSpread) selection;

                FragmentDefinition fragment = getFragmentDefinition(fragmentSpread.getName());
                if (fragment != null) {
                    Field field = getField(fragment.getSelectionSet(), fieldName);
                    if (field != null) {
                        return field;
                    }
                }

            }
        }

        return null;

    }

    private Field addField(SelectionSet selectionSet, String fieldName) {
        Field field = new Field(fieldName, new SelectionSet());
        selectionSet.getSelections().add(field);
        return field;
    }

    public <T extends Object> void setArgumentValue(String fieldName, String argumentName, Value value) {
        boolean added = false;
        if (query != null) {
            for (Definition definition : query.getDefinitions()) {

                if (definition instanceof OperationDefinition) {
                    OperationDefinition operation = (OperationDefinition) definition;

                    for (Selection selection : operation.getSelectionSet().getSelections()) {
                        if (selection instanceof Field) {
                            Field field = (Field) selection;
                            if (fieldName.equals(field.getName())) {
                                Argument newArgument = new Argument(argumentName, value);

                                // Remove argument if already exists
                                Iterator<Argument> argumentItr = field.getArguments().iterator();
                                while (argumentItr.hasNext()) {
                                    Argument existingArg = argumentItr.next();
                                    if (existingArg.getName().equals(newArgument.getName())) {
                                        argumentItr.remove();
                                    }
                                }
                                field.getArguments().add(newArgument);
                                added = true;
                            }
                        }
                    }
                }
            }

        }
        if (!added) {
            throw new IllegalArgumentException("Unable to set argument value for unknown field: " + fieldName);
        }
    }

    /**
     * Remove any json elements in the variables object that no longer has references from the query.
     * <p>
     * Unused variables may be left after overwriting arguments in the query.
     */
    private void removeUnusedVariables() {
        ObjectNode variables = getVariables();
        if (query != null && variables != null && variables.isObject()) {

            List<OperationDefinition> operationDefinitions = query.getDefinitions().stream()
                                                                     .filter(OperationDefinition.class::isInstance)
                                                                     .map(OperationDefinition.class::cast).collect(Collectors.toList());

            Set<String> variableRefs = collectVariableReferences(operationDefinitions);

            // Remove any variable definitions not in set of active variable refs
            for (OperationDefinition operationDefinition : operationDefinitions) {
                Iterator<VariableDefinition> variableDefinitionItr = operationDefinition.getVariableDefinitions().iterator();
                while (variableDefinitionItr.hasNext()) {
                    VariableDefinition variableDefinition = variableDefinitionItr.next();
                    if (!variableRefs.contains(variableDefinition.getName())) {
                        variableDefinitionItr.remove();
                    }
                }
            }

            // Remove any variable values not in set of active variable refs
            Iterator<String> variableNames = variables.fieldNames();
            while (variableNames.hasNext()) {
                String variableName = variableNames.next();
                if (!variableRefs.contains(variableName)) {
                    variableNames.remove();
                }
            }


        }

    }

    private Set<String> collectVariableReferences(Collection<? extends Node> nodes) {
        Set<String> variableRefs = new HashSet<>();
        for (Node node : nodes) {
            if (node instanceof VariableReference) {
                variableRefs.add(((VariableReference) node).getName());
            } else if (node instanceof FragmentSpread) {
                FragmentSpread fragmentSpread = (FragmentSpread) node;

                FragmentDefinition fragment = getFragmentDefinition(fragmentSpread.getName());
                if (fragment != null) {
                    variableRefs.addAll(collectVariableReferences(fragment.getChildren()));
                }
            } else {
                variableRefs.addAll(collectVariableReferences(node.getChildren()));
            }
        }
        return variableRefs;
    }


    private ArgumentValue getValue(Value node, String... path) {
        Value current = node;

        int i = 0;
        for (String attr : path) {
            if (current instanceof VariableReference) {
                String ref = ((VariableReference) current).getName();
                List<String> remainingPath = new ArrayList();
                remainingPath.add(ref);
                remainingPath.addAll(Arrays.asList(Arrays.copyOfRange(path, i, path.length)));
                return getVariableValue(getVariables(), remainingPath);
            } else if (current instanceof ObjectValue) {
                ObjectValue objectValue = (ObjectValue) current;
                if (objectValue.getObjectFields() == null || objectValue.getObjectFields().size() == 0) {
                    current = null;
                } else {
                    current = objectValue.getObjectFields().stream().filter(of -> attr.equals(of.getName())).map(of -> of.getValue()).findFirst().orElse(null);
                }
            } else {
                current = null;
            }
            i++;
        }

        if (current instanceof VariableReference) {
            String ref = ((VariableReference) current).getName();
            return getVariableValue(getVariables(), Arrays.asList(ref));
        }

        return new GraphQLArgumentValue(current);
    }

    private ObjectNode getVariables() {
        JsonNode variablesNode = element.get("variables");
        if (variablesNode!=null && variablesNode.isObject()) {
            return (ObjectNode) variablesNode;
        }
        return null;
    }


    ArgumentValue getVariableValue(JsonNode node, Iterable<String> path) {
        JsonNode current = node;
        JsonNode parent = null;
        String fieldName = null;
        for (String attr : path) {
            fieldName = attr;
            parent = current;
            current = current.get(attr);
            if (current == null) {
                break;
            }
        }
        return new JsonNodeArgumentValue(current, parent, fieldName);
    }

}