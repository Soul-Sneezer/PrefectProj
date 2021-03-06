package prefect;

import java.util.List;

abstract class Expr {
    interface Visitor<R>
    {
        R visitTernaryExpr(Ternary expr);
        R visitBinaryExpr(Binary expr);
        R visitCallExpr(Call expr);
        R visitLambdaExpr(Lambda expr);
        R visitGroupingExpr(Grouping expr);
        R visitLiteralExpr(Literal expr);
        R visitLogicalExpr(Logical expr);
        R visitUnaryExpr(Unary expr);
        R visitVariableExpr(Variable expr);
        R visitDictionaryExpr(Dictionary expr);
        R visitAssignExpr(Assign expr);
        R visitDictionaryAssignExpr(DictionaryAssign expr);
    }

    abstract <R> R accept(Visitor<R> visitor);

    static class Ternary extends Expr
    {
        Ternary(Expr condition, Expr left, Expr right)
        {
            this.condition = condition;
            this.left = left;
            this.right = right;
        }

        <R> R accept(Visitor<R> visitor) { return visitor.visitTernaryExpr(this); }

        final Expr condition;
        final Expr left;
        final Expr right;
    }

    static class Binary extends Expr
    {
        Binary(Expr left, Token operator, Expr right)
        {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        <R> R accept(Visitor<R> visitor)
        {
            return visitor.visitBinaryExpr(this);
        }

        final Expr left;
        final Token operator;
        final Expr right;
    }

    static class Call extends Expr
    {
        Call(Expr callee, Token paren, List<Expr> arguments)
        {
            this.callee = callee;
            this.paren = paren;
            this.arguments = arguments;
        }

        @Override
        <R> R accept(Visitor<R> visitor)
        {
            return visitor.visitCallExpr(this);
        }

        final Expr callee;
        final Token paren;
        final List<Expr> arguments;
    }

    static class Lambda extends Expr
    {
        Lambda(List<Token> params, List<Stmt> body)
        {
            this.params = params;
            this.body = body;
        }

        <R> R accept(Visitor<R> visitor)
        {
            return visitor.visitLambdaExpr(this);
        }

        final List<Token> params;
        final List<Stmt> body;
    }

    static class Grouping extends Expr
    {
        Grouping(Expr expression)
        {
            this.expression = expression;
        }

        @Override
        <R> R accept(Visitor<R> visitor)
        {
            return visitor.visitGroupingExpr(this);
        }

        final Expr expression;
    }

    static class Literal extends Expr
    {
        Literal(Object value)
        {
            this.value = value;
        }

        @Override
        <R> R accept(Visitor<R> visitor)
        {
            return visitor.visitLiteralExpr(this);
        }

        final Object value;
    }

    static class Logical extends Expr
    {
        Logical(Expr left, Token operator, Expr right)
        {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        <R> R accept(Visitor<R> visitor)
        {
            return visitor.visitLogicalExpr(this);
        }

        final Expr left;
        final Token operator;
        final Expr right;
    }

    static class Unary extends Expr
    {
        Unary(Token operator, Expr right)
        {
            this.operator = operator;
            this.right = right;
        }

        @Override
        <R> R accept(Visitor<R> visitor)
        {
            return visitor.visitUnaryExpr(this);
        }

        final Token operator;
        final Expr right;
    }

    static class Variable extends Expr
    {
        Variable(Token name)
        {
            this.name = name;
        }

        @Override
        <R> R accept(Visitor<R> visitor)
        {
            return visitor.visitVariableExpr(this);
        }

        final Token name;
    }

    static class Assign extends Expr
    {
        Assign(Token name, Expr value)
        {
            this.name = name;
            this.value = value;
        }

        @Override
        <R> R accept(Visitor<R> visitor)
        {
            return visitor.visitAssignExpr(this);
        }

        final Token name;
        final Expr value;
    }

    static class Dictionary extends Expr
    {
        Dictionary(Token name, Expr index)
        {
            this.name = name;
            this.index = index;
        }

        <R> R accept(Visitor<R> visitor) { return visitor.visitDictionaryExpr(this); };

        final Token name;
        final Expr index;
    }

    static class DictionaryAssign extends Expr
    {
        DictionaryAssign(Token name, Expr index, Expr value)
        {
            this.name = name;
            this.index = index;
            this.value = value;
        }

        @Override
        <R> R accept(Visitor<R> visitor) { return visitor.visitDictionaryAssignExpr(this); }

        final Token name;
        final Expr index;
        final Expr value;
    }
}
