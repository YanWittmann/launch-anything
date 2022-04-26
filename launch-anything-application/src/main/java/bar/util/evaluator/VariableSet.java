package bar.util.evaluator;

import com.fathzer.soft.javaluator.AbstractVariableSet;

import java.util.HashMap;
import java.util.Map;

public class VariableSet<T> implements AbstractVariableSet<T> {

    private final Map<String, T> variables;

    public VariableSet() {
        this.variables = new HashMap<>();
    }

    public VariableSet(VariableSet<?> staticVariableSet) {
        this.variables = new HashMap<>((Map<? extends String, ? extends T>) staticVariableSet.variables);
    }

    @Override
    public T get(String variableName) {
        return this.variables.get(variableName);
    }

    public void set(String variableName, T value) {
        this.variables.put(variableName, value);
    }

    public void remove(String variableName) {
        this.variables.remove(variableName);
    }

    public Map<String, T> getVariables() {
        return variables;
    }

    @Override
    public String toString() {
        return variables.toString();
    }
}
