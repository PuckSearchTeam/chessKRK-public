package fr.lip6.move.puck.parse;

import com.intellij.psi.tree.TokenSet;
import static fr.lip6.move.puck.parse.WelandTypes.*;

public interface WelandTokenSets {

    TokenSet KEYWORDS = TokenSet.create(SET, AXIS, SPEED, DIFFICULTY, APART, HIDE, EXCEPT, FROM, BUT_NOT_FROM, SEPARATE, FRIEND, OF, ALL);

    TokenSet IDENTIFIERS = TokenSet.create(IDENTIFIER);

    TokenSet PUNCTUATION = TokenSet.create(DOT, COMMA, SEMICOLON);
}
