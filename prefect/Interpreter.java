package prefect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.lang.Math;

public class Interpreter implements Expr.Visitor<Object>,
                                    Stmt.Visitor<Void>{
    final Environment globals = new Environment();
    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();

    Interpreter()
    {
        globals.define("clock", new PrefectCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString()
            {
                return "<native fn>";
            }
        });

        globals.define("sqrt", new PrefectCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                double x = (Double)arguments.get(0);
                double aprox = 1.0;
                int steps = 0;
                while(Math.abs(aprox * aprox - x) > 0.001 && steps < 1000)
                {
                    aprox = 0.5 * (aprox + x / aprox);
                    steps = steps + 1;
                }

                return aprox - aprox % 0.001;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
    }

    void interpret(List<Stmt> statements) {
        try {
            if(statements.size() == 1 && statements.get(0) instanceof Stmt.Expression )
            {
                Object out = evaluate(((Stmt.Expression) statements.get(0)).expression);
                if(out != null)
                    System.out.println(stringify(out));
            }
            else
            {
                for (Stmt statement : statements)
                {
                    execute(statement);
                }
            }
        } catch (RuntimeError error) {
            Main.runtimeError(error);
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt)
    {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {

        PrefectFunction function = new PrefectFunction(stmt, environment);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if(isTrue(evaluate(stmt.condition)))
        {
            execute(stmt.thenBranch);
        }
        else if (stmt.elseBranch != null)
        {
            execute(stmt.elseBranch);
        }

        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt)
    {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null)
            value = evaluate(stmt.value);

        throw new Return(value);
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        throw new Break();
        //return null;
    }

    public Void visitContinueStmt(Stmt.Continue stmt)
    {
        throw new Continue();
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if(stmt.initializer != null)
        {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitDictionaryStmt(Stmt.Dictionary stmt) {
        environment.defineDictionary(stmt.name.lexeme);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while(isTrue(evaluate(stmt.condition)))
        {
            try {
                execute(stmt.body);
            }
            catch (Break breakPoint)
            {
                break;
            }
            catch (Continue continuePoint)
            {
                continue;
            }
        }

        return null;
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr)
    {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR)
        {
            if(isTrue(left))
                return left;
        }
        else
        {
            if(!isTrue(left))
                return left;
        }

        return evaluate(expr.right);

    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr)
    {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr)
    {
        Object right = evaluate(expr.right);

        switch(expr.operator.type)
        {
            case BANG:
                return !isTrue(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
        }

        // Unreachable.
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        //if(environment.get(expr.name) != null)
            return lookUpVariable(expr.name, expr);

        //throw new RuntimeError(expr.name, "Variable has not been initialized.");
    }

    @Override
    public Object visitDictionaryExpr(Expr.Dictionary expr) {
        //System.out.println(environment.dictionaries);
        //System.out.println(indexValue);
        return lookUpDictionary(expr.name, expr.index, expr);
    }

    private Object lookUpVariable(Token name, Expr expr)
    {
        Integer distance = locals.get(expr);
        if (distance != null)
        {
            return environment.getAt(distance, name.lexeme);
        }
        else
        {
            return globals.get(name);
        }
    }

    private Object lookUpDictionary(Token name, Expr index, Expr expr)
    {
        Integer distance = locals.get(expr);
        Object indexValue = evaluate(index);

        if (distance != null)
        {
            return environment.getDictionaryAt(distance, indexValue, name.lexeme);
        }
        else
        {
            return globals.getDictionary(indexValue, name);
        }
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr)
    {
        Object value = evaluate(expr.value);

        Integer distance = locals.get(expr);
        if (distance != null)
        {
            environment.assignAt(distance, expr.name, value);
        }
        else
        {
            globals.assign(expr.name, value);
        }

        return value;
    }

    @Override
    public Object visitDictionaryAssignExpr(Expr.DictionaryAssign expr) {
        Object value = evaluate(expr.value);
        Object indexValue = evaluate(expr.index);

        environment.defineDictionaryIndex(expr.name.lexeme, indexValue, value);

        Integer distance = locals.get(expr);

        if (distance != null)
        {
            environment.assignDictionaryAt(distance, expr.name, indexValue, value);
        }
        else
        {
            globals.assignDictionary(expr.name, indexValue, value);
        }

        return value;
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        Object condition = evaluate(expr.condition);

        if(isTrue(condition))
        {
            return evaluate(expr.left);
        }
        else
        {
            return evaluate(expr.right);
        }
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr)
    {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch(expr.operator.type)
        {
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);
            case GREATER:
                checkNumberOperand(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperand(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperand(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperand(expr.operator, left, right);
                return (double)left <= (double)right;
            case PLUS:
            {
                if(left instanceof Double && right instanceof Double)
                {
                    return (double)left + (double)right;
                }

                if(left instanceof String && right instanceof String)
                {
                    return (String)left + (String)right;
                }

                if((left instanceof String || right instanceof String) &&
                   (left instanceof Double || right instanceof Double))
                {
                    return (String)left + (String)right;
                }

                throw new RuntimeError(expr.operator, "Operands must be numbers or strings.");
            }
            case MINUS:
                checkNumberOperand(expr.operator, left, right);
                return (double)left - (double)right;
            case STAR:
                checkNumberOperand(expr.operator, left, right);
                return (double)left * (double)right;
            case SLASH:
                checkNumberOperand(expr.operator, left, right);
                return (double)left / (double)right;
        }

        // Unreachable.
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments)
        {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof PrefectCallable))
        {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        PrefectCallable function = (PrefectCallable)callee;

        if (arguments.size() != function.arity())
        {
            throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
        }
        return function.call(this, arguments);
    }

    @Override
    public Object visitLambdaExpr(Expr.Lambda expr) {
        Stmt.Function anonFunction = new Stmt.Function(new Token(TokenType.LAMBDA, "lambda", "", 0), expr.params, expr.body);
        PrefectFunction function = new PrefectFunction(anonFunction, environment);
        return function;
    }

    private void checkNumberOperand(Token operator, Object operand)
    {
        if (operand instanceof Double)
            return;

        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperand(Token operator, Object leftOperand, Object rightOperand)
    {
        if (leftOperand instanceof Double && rightOperand instanceof Double)
            return;

        throw new RuntimeError(operator, "Operands must both be numbers.");

    }

    private boolean isTrue(Object object)
    {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;

        return true;
    }

    private boolean isEqual(Object a, Object b)
    {
        if (a == null && b == null)
            return true;
        if (a == null)
            return false;


        //return a == b; So this follows IEE 754, even though it's weird
        // (0 / 0) == (0 / 0) meaning NaN == NaN, returns false according to IEE 754 ._.
        return a.equals(b); // This doesn't follow IEE 754

    }

    private String stringify(Object object)
    {
        if (object == null)
            return "nil";

        if (object instanceof Double)
        {
            String text = object.toString();
            if (text.endsWith(".0"))
            {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    void executeBlock(List<Stmt> statements, Environment environment)
    {
        Environment previous = this.environment;

        try {
            this.environment = environment;

            for (Stmt statement : statements)
            {
                execute(statement);
            }
        } finally
        {
            this.environment = previous;
        }
    }

    private Object evaluate(Expr expr)
    {
        return expr.accept(this);
    }

    private void execute(Stmt stmt)
    {
        stmt.accept(this);
    }

    void resolve(Expr expr, int depth)
    {
        locals.put(expr, depth);
    }
}
