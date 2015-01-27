package com.github.fge.grappa.illegal;

import com.github.fge.grappa.parsers.EventBusParser;
import org.parboiled.Rule;

public abstract class IllegalGrammarParser
    extends EventBusParser<Object>
{
    abstract Rule illegal();

    abstract Rule legal();

    Rule empty()
    {
        return EMPTY;
    }

    Rule nonEmpty()
    {
        return ch('x');
    }
}