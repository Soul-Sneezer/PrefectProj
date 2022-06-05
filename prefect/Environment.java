package prefect;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    final Environment enclosing;

    Environment()
    {
        enclosing = null;
    }

    Environment(Environment enclosing)
    {
        this.enclosing = enclosing;
    }
    private final Map<String, Object> values = new HashMap<>();
    final Map<String, Map<Object, Object>> dictionaries = new HashMap<>();

    Object get(Token name)
    {
        if (values.containsKey(name.lexeme))
        {
            return values.get(name.lexeme);
        }

        if (enclosing != null)
            return enclosing.get(name);

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'."); // This makes it a runtime error
        // Making it a syntax error would be helpful but it would make recursion
        // difficult, unless we did it C style :)
        // Using a variable isn't the same as referring to it.

    }

    Object getDictionary(Object index, Token name)
    {
        if(dictionaries.containsKey(name.lexeme))
        {
            return dictionaries.get(name.lexeme).get(index);
        }

        if (enclosing != null)
        {
            return enclosing.getDictionary(index, name);
        }

        throw new RuntimeError(name, "Undefined dictionary '" + name.lexeme + "'.");
    }

    void assign(Token name, Object value)
    {
        if (values.containsKey(name.lexeme))
        {
            values.put(name.lexeme, value);
            return;
        }

        if (enclosing != null)
        {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    void assignDictionary(Token name, Object index, Object value)
    {
        if (dictionaries.containsKey(name.lexeme))
        {
            dictionaries.get(name.lexeme).put(index, value);
            return;
        }

        if (enclosing != null)
        {
            enclosing.assignDictionary(name, index, value);
            return;
        }

        throw new RuntimeError(name, "Undefined dictionary '" + name.lexeme + "'.");
    }

    void define(String name, Object value)
    {
        values.put(name, value); // this means we allow redefining global variables
                                 // might change it in the future, or not
    }

    void defineDictionary(String name)
    {
        dictionaries.put(name, new HashMap<>());
    }

    void defineDictionaryIndex(String name, Object index, Object value)
    {
        if(dictionaries.get(name) == null)
            dictionaries.put(name, new HashMap<>());
        dictionaries.get(name).put(index, value);
    }

    Environment ancestor(int distance)
    {
        Environment environment = this;
        for (int i = 0; i < distance; i++)
        {
            environment = environment.enclosing;
        }

        return environment;
    }

    Object getAt(int distance, String name)
    {
        return ancestor(distance).values.get(name);

    }

    Object getDictionaryAt(int distance, Object index, String name)
    {
        return ancestor(distance).dictionaries.get(name).get(index);
    }

    void assignAt(int distance, Token name, Object value)
    {
        ancestor(distance).values.put(name.lexeme, value);
    }
    void assignDictionaryAt(int distance, Token name, Object index, Object value) { ancestor(distance).dictionaries.get(name).put(index, value); }
}
