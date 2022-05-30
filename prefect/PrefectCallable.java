package prefect;

import java.util.List;

interface PrefectCallable {
    int arity();
    Object call(Interpreter interpreter, List<Object> arguments);
}
