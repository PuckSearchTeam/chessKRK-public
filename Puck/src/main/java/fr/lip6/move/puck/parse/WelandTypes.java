package fr.lip6.move.puck.parse;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import fr.lip6.move.puck.parse.impl.*;

/** Element and token types of Weland; see weland.bnf for the grammar. */
public interface WelandTypes {

  // Composite elements
  IElementType SETDEF = new WelandElementType("SETDEF");
  IElementType AXISDEF = new WelandElementType("AXISDEF");
  IElementType SET_NAME = new WelandElementType("SET_NAME");
  IElementType A_SET = new WelandElementType("A_SET");
  IElementType QUALIFIED_NAME = new WelandElementType("QUALIFIED_NAME");
  IElementType HIDE_RULE = new WelandElementType("HIDE_RULE");
  IElementType SEPARATE_RULE = new WelandElementType("SEPARATE_RULE");
  IElementType FRIEND_RULE = new WelandElementType("FRIEND_RULE");
  IElementType SPEED_RULE = new WelandElementType("SPEED_RULE");           // speed g1 < g2 < ... ;
  IElementType DIFFICULTY_RULE = new WelandElementType("DIFFICULTY_RULE"); // difficulty g1 < g2 < ... ;
  IElementType APART_RULE = new WelandElementType("APART_RULE");           // apart g ;
  IElementType GROUP = new WelandElementType("GROUP");                     // a group of a chain: SET_NAMEs

  // The set references of a rule, one element type per role; each holds a SET_NAME or the token ALL.
  IElementType HIDDEN_SET = new WelandElementType("HIDDEN_SET");           // hide X
  IElementType EXCEPTED_SET = new WelandElementType("EXCEPTED_SET");       // except X'
  IElementType FROM_SET = new WelandElementType("FROM_SET");               // from Y
  IElementType BUT_NOT_FROM_SET = new WelandElementType("BUT_NOT_FROM_SET"); // but-not-from Y'
  IElementType SEPARATED_SET = new WelandElementType("SEPARATED_SET");     // separate X
  IElementType FRIEND_SET = new WelandElementType("FRIEND_SET");           // friend F
  IElementType OF_SET = new WelandElementType("OF_SET");                   // of X

  // Tokens
  IElementType SET = new WelandTokenType("SET");
  IElementType AXIS = new WelandTokenType("AXIS");
  IElementType SPEED = new WelandTokenType("SPEED");
  IElementType DIFFICULTY = new WelandTokenType("DIFFICULTY");
  IElementType APART = new WelandTokenType("APART");
  IElementType LT = new WelandTokenType("LT");
  IElementType HIDE = new WelandTokenType("HIDE");
  IElementType EXCEPT = new WelandTokenType("EXCEPT");
  IElementType FROM = new WelandTokenType("FROM");
  IElementType BUT_NOT_FROM = new WelandTokenType("BUT_NOT_FROM");
  IElementType SEPARATE = new WelandTokenType("SEPARATE");
  IElementType FRIEND = new WelandTokenType("FRIEND");
  IElementType OF = new WelandTokenType("OF");
  IElementType ALL = new WelandTokenType("ALL");
  IElementType EQUALS = new WelandTokenType("EQUALS");
  IElementType LSQUAREBRACKET = new WelandTokenType("LSQUAREBRACKET");
  IElementType RSQUAREBRACKET = new WelandTokenType("RSQUAREBRACKET");
  IElementType COMMA = new WelandTokenType("COMMA");
  IElementType SEMICOLON = new WelandTokenType("SEMICOLON");
  IElementType DOT = new WelandTokenType("DOT");
  IElementType IDENTIFIER = new WelandTokenType("IDENTIFIER");
  IElementType COMMENT = new WelandTokenType("COMMENT");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == SETDEF || type == AXISDEF) {
        return new WelandSetdefImpl(node);
      } else if (type == SET_NAME) {
        return new WelandSetNameImpl(node);
      } else if (type == A_SET) {
        return new WelandASetImpl(node);
      } else if (type == QUALIFIED_NAME) {
        return new WelandQualifiedNameImpl(node);
      } else if (type == HIDE_RULE) {
        return new WelandHideRuleImpl(node);
      } else if (type instanceof WelandElementType) {
        return new WelandElementImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
