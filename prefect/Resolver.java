package prefect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private final Stack<Map<String, Token>> usages = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;
    private boolean insideLoop = false;

    Resolver(Interpreter interpreter)
    {
        this.interpreter = interpreter;
    }

    private enum FunctionType
    {
        NONE,
        FUNCTION,
        //LAMBDA,
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt)
    {
        beginScope();
        resolve(stmt.statements);
        endScope();

        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt)
    {
        declare(stmt.name);
        define(stmt.name);
        resolveFunction(stmt, FunctionType.FUNCTION);

        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt)
    {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt)
    {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if(stmt.elseBranch != null)
            resolve(stmt.elseBranch);

        return null;
    }

    public Void visitPrintStmt(Stmt.Print stmt)
    {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt)
    {
        if (currentFunction == FunctionType.NONE)
        {
            Main.error(stmt.keyword, "Can't return from outside of function.");
        }

        if (stmt.value != null)
        {
            resolve(stmt.value);
        }

        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        if(!insideLoop)
            Main.error(stmt.keyword, "Can't break outside of loop.");

        return null;
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        if(!insideLoop)
            Main.error(stmt.keyword, "Can't continue outside of loop.");
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt)
    {
        resolveLoop(stmt, true);
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt)
    {
        declare(stmt.name);
        if (stmt.initializer != null)
        {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitDictionaryStmt(Stmt.Dictionary stmt) {
        declare(stmt.name);
        //resolve(stmt);
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr)
    {
        if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE)
        {
            Main.error(expr.name, "Can't read local variable in its own initializer.");
        }

        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitDictionaryExpr(Expr.Dictionary expr) {
        resolve(expr.index);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr)
    {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitDictionaryAssignExpr(Expr.DictionaryAssign expr) {
        resolve(expr.value);
        resolve(expr.index);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitTernaryExpr(Expr.Ternary expr) {
        resolve(expr.condition);
        resolve(expr.left);
        resolve(expr.right);

        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr)
    {
        resolve(expr.left);
        resolve(expr.right);

        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr)
    {
        resolve(expr.callee);

        for (Expr argument : expr.arguments)
        {
            resolve(argument);
        }

        return null;
    }

    @Override
    public Void visitLambdaExpr(Expr.Lambda expr) {
        Stmt.Function fn = new Stmt.Function(new Token(TokenType.LAMBDA, "lambda", "", 0), expr.params, expr.body);

        resolveFunction(fn, FunctionType.FUNCTION);

        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr)
    {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr)
    {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr)
    {
        resolve(expr.left);
        resolve(expr.right);

        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr)
    {
        resolve(expr.right);

        return null;
    }

    void resolve(List<Stmt> statements)
    {
        for (Stmt statement : statements)
        {
            resolve(statement);
        }
    }

    private void resolve(Stmt stmt)
    {
        stmt.accept(this);
    }

    private void resolve(Expr expr)
    {
        expr.accept(this);
    }

    private void resolveFunction(Stmt.Function function, FunctionType type)
    {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope();
        for (Token param : function.params)
        {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();
        currentFunction = enclosingFunction;
    }

    private void resolveLoop(Stmt.While loop, boolean isLoop)
    {
        boolean currentState = insideLoop;
        insideLoop = isLoop;

        resolve(loop.condition);
        resolve(loop.body);

        insideLoop = currentState;
    }

    private void beginScope()
    {
        scopes.push(new HashMap<String, Boolean>());
        usages.push(new HashMap<String, Token>());
    }

    private void endScope()
    {
        scopes.pop();
        usages.peek().forEach((k, v) ->
        {
            if(v != null)
            {
                Main.warning(v.line, "Local variable " + k + " is never used.");
            }
        });
        usages.pop();
    }

    private void declare(Token name)
    {
        if (scopes.isEmpty())
            return;

        Map<String, Boolean> scope = scopes.peek();
        Map<String, Token> usage = usages.peek();
        if(scope.containsKey(name.lexeme))
        {
            Main.error(name, "Already a variable with this name in this scope.");
        }
        scope.put(name.lexeme, false);
        usage.put(name.lexeme, name);
    }

    private void define(Token name)
    {
        if (scopes.isEmpty())
            return;
        scopes.peek().put(name.lexeme, true);
    }

    private void resolveLocal(Expr expr, Token name)
    {
        for (int i = scopes.size() - 1; i >= 0; i--)
        {
            if (scopes.get(i).containsKey(name.lexeme))
            {
                usages.get(i).put(name.lexeme, null);
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }
}
