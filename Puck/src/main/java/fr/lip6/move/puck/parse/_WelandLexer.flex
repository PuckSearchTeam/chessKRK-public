package fr.lip6.move.puck.parse;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static fr.lip6.move.puck.parse.WelandTypes.*;

%%

%{
  public _WelandLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _WelandLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

WHITE_SPACE=\s+
LINE_COMMENT="//"[^\r\n]*
/* A qualified name is a sequence of identifiers separated by dots. A method name carries its parameter
   list, which may hold commas, spaces, brackets and generics: Class.m(int,List<String>,int[]) */
IDENTIFIER=[a-zA-Z_$][a-zA-Z_0-9$]*(\([^)]*\))?

%%
<YYINITIAL> {
  {WHITE_SPACE}       { return WHITE_SPACE; }
  {LINE_COMMENT}      { return COMMENT; }

  "set"               { return SET; }
  "axis"              { return AXIS; }
  "speed"             { return SPEED; }
  "difficulty"        { return DIFFICULTY; }
  "apart"             { return APART; }
  "hide"              { return HIDE; }
  "except"            { return EXCEPT; }
  "from"              { return FROM; }
  "but-not-from"      { return BUT_NOT_FROM; }
  "separate"          { return SEPARATE; }
  "friend"            { return FRIEND; }
  "of"                { return OF; }
  "all"               { return ALL; }
  "="                 { return EQUALS; }
  "<"                 { return LT; }
  "["                 { return LSQUAREBRACKET; }
  "]"                 { return RSQUAREBRACKET; }
  ","                 { return COMMA; }
  ";"                 { return SEMICOLON; }
  "."                 { return DOT; }
  {IDENTIFIER}        { return IDENTIFIER; }
}

[^] { return BAD_CHARACTER; }
