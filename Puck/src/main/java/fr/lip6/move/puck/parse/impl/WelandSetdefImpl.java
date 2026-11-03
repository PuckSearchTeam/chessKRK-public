// This is a generated file. Not intended for manual editing.
package fr.lip6.move.puck.parse.impl;

import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import fr.lip6.move.puck.parse.*;

public class WelandSetdefImpl extends ASTWrapperPsiElement implements WelandSetdef {

  public WelandSetdefImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull WelandVisitor visitor) {
    visitor.visitSetdef(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof WelandVisitor) accept((WelandVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public WelandASet getASet() {
    return findNotNullChildByClass(WelandASet.class);
  }

  @Override
  @NotNull
  public WelandSetName getSetName() {
    return findNotNullChildByClass(WelandSetName.class);
  }

}
