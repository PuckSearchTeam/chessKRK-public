package fr.lip6.move.puck.parse;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import fr.lip6.move.puck.actions.PuckAction;
import fr.lip6.move.puck.puckgraph.PuckGraph;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static fr.lip6.move.puck.parse.WelandTypes.*;

public class WelandInspectionTool extends LocalInspectionTool {

    List<String> welandSetNames = new ArrayList<>(List.of("tests")); // "tests" is predefined
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        if (PuckAction.getReadyForWelandInspection()) {
            return new PsiElementVisitor() {
                @Override
                public void visitElement(PsiElement element) {
                    super.visitElement(element);
                    String wsn = null;
                    // Condition to check if the element is problematic
                    IElementType elementType = element.getNode().getElementType();
                    if (elementType.equals(QUALIFIED_NAME)) {
                        String txt = element.getText();
                        PuckGraph graph = PuckAction.getTheGraph();
                        if (!graph.getNodes().getNodeNames().contains(txt)) {
                            holder.registerProblem(element, element.getText() + " is not the qualified name of an existing node.", ProblemHighlightType.WARNING);
                        }
                    } else if (elementType.equals(SETDEF) || elementType.equals(AXISDEF)) {
                        wsn = ((WelandSetdef) element).getSetName().getText();
                        welandSetNames.add(wsn);
                    } else if (elementType.equals(SET_NAME)) {
                        String txt = element.getText();
                        if (!welandSetNames.contains(txt)) {
                            holder.registerProblem(element, element.getText() + " is not the name of a known set.", ProblemHighlightType.WARNING);
                        }
                    }
                }
            };
        } else if (PuckAction.getTheProject() != null) {
            //System.out.println("Not ready for inspection");
            Editor editor = FileEditorManager.getInstance(PuckAction.getTheProject()).getSelectedTextEditor();
            if (editor != null) {
                HintManager.getInstance().showErrorHint(editor, "Not ready for inspection Weland inspection!");
            }
        }
        return new PsiElementVisitor(){};
    }
    @NotNull
    @Override
    public String getGroupDisplayName() {
        return "Puck Inspections";
    }
}
