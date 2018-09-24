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


import graphql.language.Argument;
import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.Definition;
import graphql.language.Directive;
import graphql.language.Document;
import graphql.language.EnumValue;
import graphql.language.Field;
import graphql.language.FloatValue;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.IntValue;
import graphql.language.ListType;
import graphql.language.Node;
import graphql.language.NonNullType;
import graphql.language.NullValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.language.StringValue;
import graphql.language.TypeName;
import graphql.language.Value;
import graphql.language.VariableDefinition;
import graphql.language.VariableReference;

import java.util.List;

// TODO unlcear license. Lifted from https://gist.github.com/dminkovsky/b5efd2df4a13c6c89a01c5c49fbdffc8
public class GraphQLPrinter {

    private int indentWidth = 4;

    public String print(Document document) {
        GraphQLPrinterImpl printer = new GraphQLPrinterImpl(document, indentWidth);
        return printer.print();
    }

    private static class GraphQLPrinterImpl {

        private final StringBuilder builder = new StringBuilder();
        private final Node root;
        private final int indentWidth;
        private int indentLevel = 0;

        GraphQLPrinterImpl(Node root, int indentWidth) {
            this.root = root;
            this.indentWidth = indentWidth;
        }

        public String print() {
            print(root);
            return builder.toString();
        }

        private <T extends Node> void join(List<T> nodes, String delimeter) {
            int size = nodes.size();
            for (int i = 0; i < size; i++) {
                T node = nodes.get(i);
                print(node);
                if (i + 1 != size) {
                    builder.append(delimeter);
                }
            }
        }

        private <T> void wrap(String start, T thing, String end) {
            wrap(start, thing, end, builder::append);
        }

        private <T> void wrap(String start, T thing, String end, java.util.function.Consumer<T> inner) {
            if (thing instanceof List && ((List) thing).isEmpty()) {
                return;
            }
            builder.append(start);
            inner.accept(thing);
            builder.append(end);
        }

        private String getNewLine() {
            StringBuilder builder = new StringBuilder();
            builder.append("\n");
            for (int i = 0; i < indentLevel; i++) {
                for (int j = 0; i < indentWidth; i++) {
                    builder.append(" ");
                }
            }
            return builder.toString();
        }

        private void line() {
            line(1);
        }

        private void line(int count) {
            for (int i = 0; i < count; i++) {
                builder.append(getNewLine());
            }
        }

        private void print(Node node) {
            if (node instanceof Document) {
                print((Document) node);
            } else if (node instanceof OperationDefinition) {
                print((OperationDefinition) node);
            } else if (node instanceof FragmentDefinition) {
                print((FragmentDefinition) node);
            } else if (node instanceof VariableDefinition) {
                print((VariableDefinition) node);
            } else if (node instanceof ArrayValue) {
                print((ArrayValue) node);
            } else if (node instanceof BooleanValue) {
                print((BooleanValue) node);
            } else if (node instanceof EnumValue) {
                print((EnumValue) node);
            } else if (node instanceof FloatValue) {
                print((FloatValue) node);
            } else if (node instanceof IntValue) {
                print((IntValue) node);
            } else if (node instanceof ObjectValue) {
                print((ObjectValue) node);
            } else if (node instanceof StringValue) {
                print((StringValue) node);
            } else if (node instanceof VariableReference) {
                print((VariableReference) node);
            } else if (node instanceof ListType) {
                print((ListType) node);
            } else if (node instanceof NonNullType) {
                print((NonNullType) node);
            } else if (node instanceof TypeName) {
                print((TypeName) node);
            } else if (node instanceof Directive) {
                print((Directive) node);
            } else if (node instanceof Argument) {
                print((Argument) node);
            } else if (node instanceof ObjectField) {
                print((ObjectField) node);
            } else if (node instanceof SelectionSet) {
                print((SelectionSet) node);
            } else if (node instanceof Field) {
                print((Field) node);
            } else if (node instanceof InlineFragment) {
                print((InlineFragment) node);
            } else if (node instanceof FragmentSpread) {
                print((FragmentSpread) node);
            } else if (node instanceof NullValue) {
                print((NullValue) node);
            }
            else {
                throw new RuntimeException("unknown type");
            }
        }

        private void print(Document node) {
            for (Definition defintition : node.getDefinitions()) {
                print(defintition);
                line(2);
            }
            line();
        }

        private void print(OperationDefinition node) {
            String name = node.getName();
            OperationDefinition.Operation operation = node.getOperation();
            List<VariableDefinition> variableDefinitions = node.getVariableDefinitions();
            List<Directive> directives = node.getDirectives();
            SelectionSet selectionSet = node.getSelectionSet();
            if (name == null && variableDefinitions.isEmpty() && directives.isEmpty() && operation == OperationDefinition.Operation.QUERY) {
                print(selectionSet);
            } else {
                if (operation == OperationDefinition.Operation.QUERY) {
                    builder.append("query ");
                } else if (operation == OperationDefinition.Operation.MUTATION) {
                    builder.append("mutation ");
                } else {
                    throw new RuntimeException("unsupported operation");
                }
                if (name != null) {
                    builder.append(name);
                }
                wrap("(", variableDefinitions, ")", definitions -> {
                    join(definitions, ", ");
                });
                wrap(" ", directives, " ", dirs -> {
                    join(dirs, " ");
                });
                builder.append(" ");
                print(selectionSet);
            }
        }

        private void print(VariableDefinition node) {
            // Entur: Prefixed var reference with $
            builder.append("$").append(node.getName());
            builder.append(": ");
            print(node.getType());
            Value defaultValue = node.getDefaultValue();
            if (defaultValue != null) {
                builder.append(" = ");
                print(defaultValue);
            }
        }

        private void print(Directive node) {
            builder.append("@");
            builder.append(node.getName());
            wrap("(", node.getArguments(), ")", arguments -> {
                join(arguments, ", ");
            });
        }

        private void print(SelectionSet node) {
            List<Selection> selections = node.getSelections();
            if (selections.isEmpty()) {
                return;
            }
            builder.append("{");
            indentLevel++;
            line();
            join(selections, getNewLine());
            indentLevel--;
            line();
            builder.append("}");
        }

        private void print(FragmentDefinition node) {
            builder.append("fragment ");
            builder.append(node.getName());
            builder.append(" on ");
            print(node.getTypeCondition());
            wrap(" ", node.getDirectives(), " ", directives -> {
                join(directives, " ");
            });
            print(node.getSelectionSet());
        }

        private void print(ArrayValue node) {
            wrap("[", node.getValues(), "]", values -> {
                join(values, ", ");
            });
        }

        private void print(BooleanValue node) {
            builder.append(node.isValue());
        }

        private void print(EnumValue node) {
            builder.append(node.getName());
        }

        private void print(FloatValue node) {
            builder.append(node.getValue());
        }

        private void print(IntValue node) {
            builder.append(node.getValue());
        }

        private void print(ObjectValue node) {
            wrap("{", node.getObjectFields(), "}", fields -> {
                join(fields, ", ");
            });
        }

        private void print(StringValue node) {
            wrap("\"", node.getValue(), "\"");
        }

        private void print(NullValue node) {
            builder.append("null");
        }

        private void print(VariableReference node) {
            // Entur: Prefixed var reference with $
            builder.append("$").append(node.getName());
        }

        private void print(ListType node) {
            wrap("[", node.getType(), "]", this::print);
        }

        private void print(NonNullType node) {
            print(node.getType());
            builder.append("!");
        }

        private void print(TypeName node) {
            builder.append(node.getName());
        }

        private void print(Argument node) {
            builder.append(node.getName());
            builder.append(": ");
            print(node.getValue());
        }

        private void print(Field node) {
            String alias = node.getAlias();
            if (alias != null) {
                builder.append(alias);
                builder.append(": ");
            }
            builder.append(node.getName());
            List<Argument> arguments = node.getArguments();
            wrap("(", arguments, ")", args -> {
                join(args, ", ");
            });
            join(node.getDirectives(), " ");
            SelectionSet selectionSet = node.getSelectionSet();
            if (selectionSet != null) {
                print(selectionSet);
            }
        }

        private void print(ObjectField node) {
            builder.append(node.getName());
            builder.append(": ");
            print(node.getValue());
        }

        private void print(InlineFragment node) {
            builder.append("... on ");
            builder.append(node.getTypeCondition().getName());
            builder.append(" ");
            join(node.getDirectives(), " ");
            print(node.getSelectionSet());
        }

        private void print(FragmentSpread node) {
            builder.append("...");
            builder.append(node.getName());
            join(node.getDirectives(), " ");
        }

    }
}