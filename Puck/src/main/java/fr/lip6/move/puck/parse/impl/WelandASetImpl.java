// This is a generated file. Not intended for manual editing.
package fr.lip6.move.puck.parse.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import fr.lip6.move.puck.parse.*;

public class WelandASetImpl extends ASTWrapperPsiElement implements WelandASet {

  public WelandASetImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull WelandVisitor visitor) {
    visitor.visitASet(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof WelandVisitor) accept((WelandVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<WelandQualifiedName> getQualifiedNameList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, WelandQualifiedName.class);
  }

}
