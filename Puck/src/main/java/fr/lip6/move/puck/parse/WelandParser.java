package fr.lip6.move.puck.parse;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

import static fr.lip6.move.puck.parse.WelandTypes.*;

/**
 * Recursive-descent parser for Weland (the grammar is in weland.bnf):
 * <pre>
 *   statement     ::= setdef | axisdef | speed-rule | difficulty-rule | apart-rule | hide-rule | separate-rule | friend-rule
 *   setdef        ::= set NAME = [ (qualified-name (, qualified-name)*)? ]
 *   axisdef       ::= axis NAME = [ (qualified-name (, qualified-name)*)? ]
 *   speed-rule    ::= speed chain ;
 *   difficulty-rule ::= difficulty chain ;
 *   chain         ::= group (< group)+
 *   group         ::= NAME (, NAME)*
 *   apart-rule    ::= apart group ;
 *   hide-rule     ::= hide setref (except setref)? (from setref (but-not-from setref)?)? ;
 *   separate-rule ::= separate setref ;
 *   friend-rule   ::= friend setref of setref ;
 *   setref        ::= NAME (, NAME)* | all
 * </pre>
 * Statements may come in any order; a set must be defined before a rule uses it, which the Weland
 * inspection checks. Each statement consumes at least its keyword, so an error never stalls the parse.
 */
public class WelandParser implements PsiParser {

    /** After a dot, a keyword is accepted as a name part, so that a package may be called "of" or "all". */
    private static final TokenSet NAME_PART =
            TokenSet.create(IDENTIFIER, SET, AXIS, SPEED, DIFFICULTY, APART, HIDE, EXCEPT, FROM, BUT_NOT_FROM, SEPARATE, FRIEND, OF, ALL);

    @Override
    public @NotNull ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
        Marker file = builder.mark();
        while (!builder.eof()) {
            statement(builder);
        }
        file.done(root);
        return builder.getTreeBuilt();
    }

    private static void statement(PsiBuilder b) {
        IElementType token = b.getTokenType();
        if (token == SET) {
            setdef(b, SETDEF);
        } else if (token == AXIS) {
            setdef(b, AXISDEF);
        } else if (token == SPEED) {
            chainRule(b, SPEED_RULE);
        } else if (token == DIFFICULTY) {
            chainRule(b, DIFFICULTY_RULE);
        } else if (token == APART) {
            apartRule(b);
        } else if (token == HIDE) {
            hideRule(b);
        } else if (token == SEPARATE) {
            separateRule(b);
        } else if (token == FRIEND) {
            friendRule(b);
        } else {
            Marker m = b.mark();
            b.advanceLexer();
            m.error("set, axis, speed, difficulty, apart, hide, separate or friend expected");
        }
    }

    private static void setdef(PsiBuilder b, IElementType type) {
        Marker m = b.mark();
        b.advanceLexer(); // set or axis
        setName(b);
        expect(b, EQUALS, "'=' expected");
        aSet(b);
        m.done(type);
    }

    /** One or more set names separated by commas: the sets a chain leaves unordered, or the sets kept apart. */
    private static void group(PsiBuilder b) {
        Marker m = b.mark();
        setName(b);
        while (b.getTokenType() == COMMA) {
            b.advanceLexer();
            setName(b);
        }
        m.done(GROUP);
    }

    /** speed or difficulty, then groups separated by '<', at least two. */
    private static void chainRule(PsiBuilder b, IElementType type) {
        Marker m = b.mark();
        b.advanceLexer(); // speed or difficulty
        group(b);
        if (expect(b, LT, "'<' expected")) {
            group(b);
            while (b.getTokenType() == LT) {
                b.advanceLexer();
                group(b);
            }
        }
        expect(b, SEMICOLON, "';' expected");
        m.done(type);
    }

    private static void apartRule(PsiBuilder b) {
        Marker m = b.mark();
        b.advanceLexer(); // apart
        group(b);
        expect(b, SEMICOLON, "';' expected");
        m.done(APART_RULE);
    }

    /** A possibly empty list of qualified names between square brackets. */
    private static void aSet(PsiBuilder b) {
        Marker m = b.mark();
        if (expect(b, LSQUAREBRACKET, "'[' expected")) {
            if (b.getTokenType() != RSQUAREBRACKET) {
                qualifiedName(b);
                while (b.getTokenType() == COMMA) {
                    b.advanceLexer();
                    qualifiedName(b);
                }
            }
            expect(b, RSQUAREBRACKET, "']' expected");
        }
        m.done(A_SET);
    }

    private static void qualifiedName(PsiBuilder b) {
        Marker m = b.mark();
        if (b.getTokenType() != IDENTIFIER) {
            m.error("qualified name expected");
            return;
        }
        b.advanceLexer();
        while (b.getTokenType() == DOT) {
            b.advanceLexer();
            if (NAME_PART.contains(b.getTokenType())) {
                b.advanceLexer();
            } else {
                b.error("name expected after '.'");
            }
        }
        m.done(QUALIFIED_NAME);
    }

    private static void setName(PsiBuilder b) {
        if (b.getTokenType() == IDENTIFIER) {
            Marker m = b.mark();
            b.advanceLexer();
            m.done(SET_NAME);
        } else {
            b.error("set name expected");
        }
    }

    /**
     * One or more set names separated by commas, their union being meant, or the keyword {@code all};
     * wrapped in the element type of its role in the rule.
     */
    private static void setRef(PsiBuilder b, IElementType role) {
        Marker m = b.mark();
        if (b.getTokenType() == ALL) {
            b.advanceLexer();
        } else {
            setName(b);
            while (b.getTokenType() == COMMA) {
                b.advanceLexer();
                setName(b);
            }
        }
        m.done(role);
    }

    private static void hideRule(PsiBuilder b) {
        Marker m = b.mark();
        b.advanceLexer(); // hide
        setRef(b, HIDDEN_SET);
        if (b.getTokenType() == EXCEPT) {
            b.advanceLexer();
            setRef(b, EXCEPTED_SET);
        }
        if (b.getTokenType() == FROM) {
            b.advanceLexer();
            setRef(b, FROM_SET);
            if (b.getTokenType() == BUT_NOT_FROM) {
                b.advanceLexer();
                setRef(b, BUT_NOT_FROM_SET);
            }
        }
        expect(b, SEMICOLON, "';' expected");
        m.done(HIDE_RULE);
    }

    private static void separateRule(PsiBuilder b) {
        Marker m = b.mark();
        b.advanceLexer(); // separate
        setRef(b, SEPARATED_SET);
        expect(b, SEMICOLON, "';' expected");
        m.done(SEPARATE_RULE);
    }

    private static void friendRule(PsiBuilder b) {
        Marker m = b.mark();
        b.advanceLexer(); // friend
        setRef(b, FRIEND_SET);
        if (expect(b, OF, "'of' expected")) {
            setRef(b, OF_SET);
        }
        expect(b, SEMICOLON, "';' expected");
        m.done(FRIEND_RULE);
    }

    private static boolean expect(PsiBuilder b, IElementType token, String message) {
        if (b.getTokenType() == token) {
            b.advanceLexer();
            return true;
        }
        b.error(message);
        return false;
    }
}
