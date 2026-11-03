package fr.lip6.move.puck.parse.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/** PSI element of the composites that need no accessor of their own: the rules and the set references. */
public class WelandElementImpl extends ASTWrapperPsiElement {

  public WelandElementImpl(@NotNull ASTNode node) {
    super(node);
  }

}
