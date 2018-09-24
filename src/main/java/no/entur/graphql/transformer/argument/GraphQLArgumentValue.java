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

import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.EnumValue;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.NullValue;
import graphql.language.StringValue;
import graphql.language.Value;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Wrapper around an inline graphql argument value
 */
public class GraphQLArgumentValue implements ArgumentValue {

    private Value graphQLValue;

    public GraphQLArgumentValue(Value graphQLValue) {
        this.graphQLValue = graphQLValue;
    }


    @Override
    public Integer asInt() {
        if (graphQLValue instanceof IntValue) {
            BigInteger bigInteger = ((IntValue) graphQLValue).getValue();
            if (bigInteger != null) {
                return bigInteger.intValue();
            }
        }
        return null;
    }

    @Override
    public Boolean asBoolean() {
        if (graphQLValue instanceof BooleanValue) {
            return ((BooleanValue) graphQLValue).isValue();
        }
        return null;
    }

    public boolean isNull() {
        if (graphQLValue != null && !graphQLValue.equals(NullValue.Null)) {
            return false;
        }
        return true;
    }

    public Double asDouble() {
        if (graphQLValue instanceof FloatValue) {
            BigDecimal gd = ((FloatValue) graphQLValue).getValue();
            if (gd != null) {
                return gd.doubleValue();
            }
        }
        return null;
    }

    public String asString() {
        if (graphQLValue instanceof StringValue) {
            return ((StringValue) graphQLValue).getValue();
        } else if (graphQLValue instanceof EnumValue) {
            return ((EnumValue) graphQLValue).getName();
        }
        return null;
    }

    public List<ArgumentValue> asList() {
        if (graphQLValue instanceof ArrayValue) {
            return ((ArrayValue) graphQLValue).getValues().stream().map(v -> new GraphQLArgumentValue(v)).collect(Collectors.toList());
        }
        return null;
    }


    public void setIterable(Iterable values) {
        if (graphQLValue instanceof ArrayValue) {

            List<Value> graphQLValues = new ArrayList<>();

            for (Object o : values) {
                if (o instanceof String) {
                    graphQLValues.add(new StringValue((String) o));
                } else if (o instanceof EnumValue) {
                    graphQLValues.add((EnumValue) o);
                } else if (o instanceof Enum) {
                    graphQLValues.add(new EnumValue(((Enum) o).name()));
                }

            }

            ((ArrayValue) graphQLValue).setValues(graphQLValues);
        } else {
            throw new IllegalArgumentException("Unable to set list value for value: " + this);
        }
    }

    public void setString(String string) {
        if (graphQLValue instanceof EnumValue) {
            ((EnumValue) graphQLValue).setName(string);

        } else if (graphQLValue instanceof StringValue) {
            ((StringValue) graphQLValue).setValue(string);

        } else {
            throw new IllegalArgumentException("Unable to set string value for value: " + this);
        }
    }

    public void setInteger(Integer integer) {
        if (graphQLValue instanceof IntValue) {
            ((IntValue) graphQLValue).setValue(BigInteger.valueOf(integer));
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
    public GraphQLArgumentValue deepCopy() {
        if (graphQLValue == null) {
            return new GraphQLArgumentValue(NullValue.Null);
        }
        return new GraphQLArgumentValue(graphQLValue.deepCopy());
    }

    @Override
    public String toString() {
        return "GraphQLArgumentValue{" +
                       "graphQLValue=" + graphQLValue +
                       '}';
    }
}