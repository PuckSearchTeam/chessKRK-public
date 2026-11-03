package fr.lip6.move.puck.parse;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiElement;

public class WelandVisitor extends PsiElementVisitor {

  public void visitASet(@NotNull WelandASet o) {
    visitPsiElement(o);
  }

  public void visitHideRule(@NotNull WelandHideRule o) {
    visitPsiElement(o);
  }

  public void visitQualifiedName(@NotNull WelandQualifiedName o) {
    visitPsiElement(o);
  }

  public void visitSetName(@NotNull WelandSetName o) {
    visitPsiElement(o);
  }

  public void visitSetdef(@NotNull WelandSetdef o) {
    visitPsiElement(o);
  }

  public void visitPsiElement(@NotNull PsiElement o) {
    visitElement(o);
  }

}
