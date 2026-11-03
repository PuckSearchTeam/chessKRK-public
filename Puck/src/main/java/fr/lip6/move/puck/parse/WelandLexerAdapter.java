package fr.lip6.move.puck.parse;

import com.intellij.lexer.FlexAdapter;

import java.io.Reader;

public class WelandLexerAdapter extends FlexAdapter {

    public WelandLexerAdapter() {
        super(new _WelandLexer((Reader) null));
    }

}