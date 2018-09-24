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

import java.util.List;

public class NullArgumentValue implements ArgumentValue {

    public static NullArgumentValue INSTANCE = new NullArgumentValue();

    private NullArgumentValue() {
    }

    @Override
    public Integer asInt() {
        return null;
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public Double asDouble() {
        return null;
    }

    @Override
    public String asString() {
        return null;
    }

    @Override
    public Boolean asBoolean() {
        return null;
    }

    @Override
    public List<ArgumentValue> asList() {
        return null;
    }

    @Override
    public void setIterable(Iterable values) {
        setValue(values);
    }

    @Override
    public void setString(String string) {
        setValue(string);
    }

    @Override
    public <T> void setValue(T value) {
        throw new IllegalArgumentException("Cannot set value for null argument");
    }

    @Override
    public String toString() {
        return "NullArgumentValue";
    }

    @Override
    public NullArgumentValue deepCopy() {
        return INSTANCE;
    }
}
