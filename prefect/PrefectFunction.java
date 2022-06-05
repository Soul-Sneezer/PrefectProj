package prefect;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class PrefectFunction implements PrefectCallable {
    private final Stmt.Function declaration;
    private final Environment closure;
    PrefectFunction(Stmt.Function declaration, Environment closure)
    {
        this.closure = closure;
        this.declaration = declaration;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments)
    {
        Environment environment = new Environment(closure);
        for (int i = 0; i < declaration.params.size(); i++)
        {
            if(arguments.get(i) instanceof Map<?, ?>)
            {
                environment.defineDictionary(declaration.params.get(i).lexeme, (Map<Object, Object>)arguments.get(i));
            }
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            return returnValue.value;
        }

        return null;
    }

    @Override
    public int arity()
    {
        return declaration.params.size();
    }

    @Override
    public String toString()
    {
        return "<fn " + declaration.name.lexeme + ">";
    }
}
