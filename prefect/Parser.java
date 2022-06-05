package prefect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static prefect.TokenType.*;

public class Parser {
    private static class ParseError extends RuntimeException {}
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens)
    {
        this.tokens = tokens;
    }

    /*
    Expr parse()
    {
        try {
            return expression();
        } catch (ParseError error)
        {
            return null;
        }
    }
    */

    List<Stmt> parse()
    {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd())
        {
            statements.add(declaration());
        }

        return statements;
    }

    private Stmt declaration()
    {
        try {
            if (match(FUN))
                return function("function");
            if (match(VAR))
                return varDeclaration();
            if (match(DICTIONARY))
                return dictionaryDeclaration();

            return statement();
        } catch (ParseError error)
        {
            synchronize();
            return null;
        }
    }

    private Stmt statement()
    {
        if(match(BREAK))
            return breakStatement();
        if(match(CONTINUE))
            return continueStatement();
        if (match(IF))
            return ifStatement();
        if (match(FOR))
            return forStatement();
        if (match(WHILE))
            return whileStatement();
        if (match(PRINT))
            return printStatement();
        if (match(RETURN))
            return returnStatement();
        if (match(LEFT_BRACE))
            return new Stmt.Block(block());

        return expressionStatement();
    }

    private Stmt forStatement()
    {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;
        if (match(SEMICOLON))
        {
            initializer = null;
        }
        else if(match(VAR))
        {
            initializer = varDeclaration();
        }
        else
        {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON))
        {
            condition = expression();
        }

        consume(SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;

        if(!check(RIGHT_PAREN))
        {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after increment.");

        Stmt body = statement();

        if (increment != null)
        {
            body = new Stmt.Block(
                    Arrays.asList(
                            body,
                            new Stmt.Expression(increment)));
        }

        if (condition == null)
            condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null)
        {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt whileStatement()
    {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after 'while'.");

        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private Stmt ifStatement()
    {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE))
        {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt.Function function(String kind)
    {
        Token name = consume(IDENTIFIER, "Expect " + kind + " name.");

        consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();

        if (!check(RIGHT_PAREN))
        {
            do {
               if (parameters.size() >= 255)
               {
                   error(peek(), "Can't have more than 255 parameters.");
               }
               parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }

        consume(RIGHT_PAREN, "Expect ')' after parameters.");
        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }

    private Stmt varDeclaration()
    {
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL))
        {
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");

        return new Stmt.Var(name, initializer);
    }

    private Stmt dictionaryDeclaration()
    {
        Token name = consume(IDENTIFIER, "Expect dictionary name.");

        consume(SEMICOLON, "Expect ';' after variable declaration.");

        return new Stmt.Dictionary(name);
    }

    private Stmt printStatement()
    {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt returnStatement()
    {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON))
        {
            value = expression();
        }

        consume(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    private Stmt breakStatement()
    {
        Token keyword = previous();
        consume(SEMICOLON, "Expect ';' after 'break' statement.");
        return new Stmt.Break(keyword);
    }

    private Stmt continueStatement()
    {
        Token keyword = previous();
        consume(SEMICOLON, "Expect ';' after 'continue' statement.");
        return new Stmt.Continue(keyword);
    }

    private Stmt expressionStatement()
    {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");

        return new Stmt.Expression(expr);
    }

    private List<Stmt> block()
    {
        List<Stmt> statements = new ArrayList<>();

        while(!check(RIGHT_BRACE) && !isAtEnd())
        {
            statements.add(declaration());
        }


        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Expr expression()
    {
        return assignment();
    }

    private Expr assignment()
    {
        Expr expr = comma();

        if (match(EQUAL))
        {
            Token equals = previous();
            //System.out.println(expr);
            Expr value = assignment();

            if (expr instanceof Expr.Variable)
            {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            if (expr instanceof Expr.Dictionary)
            {
                Token name = ((Expr.Dictionary)expr).name;
                Expr index = ((Expr.Dictionary)expr).index;

                return new Expr.DictionaryAssign(name, index, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr comma()
    {
        Expr expr = ternary();
        //while(match(COMMA))
        //{
        //    expr = ternary();
        //}

        return expr;
    }

    private Expr ternary()
    {
        Expr expr = or();
        while(match(QMARK))
        {
            Expr left = or();
            consume(TokenType.COLON, "Expected ':' for ternary operator.");
            Expr right = ternary();
            expr = new Expr.Ternary(expr, left, right);
        }

        return expr;
    }

    private Expr or()
    {
        Expr expr = and();

        while(match(OR))
        {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and()
    {
        Expr expr = equality();

        while (match(AND))
        {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr equality()
    {
        if(match(BANG_EQUAL, EQUAL_EQUAL))
        {
            error(previous(), "Missing left operand.");
        }
        Expr expr = comparison();
        while (match(BANG_EQUAL, EQUAL_EQUAL))
        {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison()
    {
        if (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL))
        {
            error(previous(), "Missing left operand.");
        }
        Expr expr = term();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL))
        {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term()
    {
        Expr expr = factor();
        while (match(MINUS, PLUS))
        {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor()
    {
        if(match(SLASH, STAR, MODULUS))
        {
            error(previous(), "Missing left operand.");
        }
        Expr expr = unary();
        while (match(SLASH, STAR, MODULUS))
        {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary()
    {
        if(match(BANG, MINUS, PLUS))
        {
            Token operator = previous();
            if(operator.type == PLUS)
            {
                Main.error(operator.line, "Unary '+' expressions are not supported.");
            }
            Expr right = unary();

            return new Expr.Unary(operator, right);

        }

        return call();
    }

    private Expr finishCall(Expr callee)
    {
        List<Expr> arguments = new ArrayList<>();

        if (!check(RIGHT_PAREN))
        {
            do {
                if (arguments.size() >= 255)
                {
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");

        return new Expr.Call(callee, paren, arguments);
    }

    private Expr call()
    {
        Expr expr = primary();

        while(true)
        {
            if (match(LEFT_PAREN))
            {
                expr = finishCall(expr);
            }
            else
            {

                break;
            }
        }

        return expr;
    }

    private Expr lambda()
    {
        consume(LEFT_PAREN, "Expected '(' after lambda.");

        List<Token> parameters = new ArrayList<>();

        if(!check(RIGHT_PAREN))
        {
            do {
                if (parameters.size() >= 255)
                {
                    error(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(consume(IDENTIFIER, "Expected parameter name."));
            } while(match(COMMA));
        }

        consume(RIGHT_PAREN, "Expect ')' after parameters.");
        consume(LEFT_BRACE, "Expect '{' before body.");

        List<Stmt> body = block();

        return new Expr.Lambda(parameters, body);
    }

    private Expr primary()
    {
        if(match(FALSE))
        {
            return new Expr.Literal(false);
        }
        if(match(TRUE))
        {
            return new Expr.Literal(true);
        }
        if(match(NIL))
        {
            return new Expr.Literal(null);
        }

        if(match(NUMBER, STRING))
        {
            return new Expr.Literal(previous().literal);
        }

        if(match(IDENTIFIER))
        {
            Token name = previous();

            if(match(LEFT_BRACKET))
            {
                Expr index = comma();
                consume(RIGHT_BRACKET, "Expected ']' after index.");
                return new Expr.Dictionary(name, index);
            }
            return new Expr.Variable(name);
        }

        if(match(LAMBDA))
        {
            return lambda();
        }

        if(match(LEFT_PAREN))
        {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        // check for potential erroneous(heh) errors here
        switch(previous().type)
        {
            case SLASH:
            case DOT:
            case MINUS:
            case PLUS:
            case EQUAL_EQUAL:
            case BANG_EQUAL:
            case GREATER_EQUAL:
            case GREATER:
            case LESS_EQUAL:
            case LESS:
            case MODULUS:
                throw error(previous(), "Missing valid right operand.");
        }
        throw error(peek(), "Expect expression.");
    }

    private boolean match(TokenType... types)
    {
        for (TokenType type : types)
        {
            if (check(type))
            {
                advance();
                return true;
            }
        }

        return false;
    }

    private Token consume(TokenType type, String message)
    {
        if (check(type))
            return advance();

        throw error(peek(), message);
    }

    private ParseError error(Token token, String message)
    {
        Main.error(token, message);
        return new ParseError();
    }

    private void synchronize()
    {
        advance();
        while (!isAtEnd())
        {
            if (previous().type == SEMICOLON)
                return;

            switch(peek().type)
            {
                case CLASS:
                case FOR:
                case FUN:
                case IF:
                case PRINT:
                case RETURN:
                case VAR:
                case WHILE:
                    return;
            }

            advance();
        }
    }

    private boolean check(TokenType type)
    {
        if (isAtEnd())
            return false;

        return peek().type == type;
    }

    private Token advance()
    {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd()
    {
        return peek().type == EOF;
    }

    private Token peek()
    {
        return tokens.get(current);
    }

    private Token previous()
    {
        return tokens.get(current - 1);
    }
}
