package fr.lip6.move.puck.parse;

import android.util.SparseIntArray;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.psi.*;
import fr.lip6.move.puck.actions.PuckAction;
import fr.lip6.move.puck.puckgraph.DependencyGraph;
import fr.lip6.move.puck.puckgraph.PuckGraph;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PuckInspectionTool extends LocalInspectionTool {
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        PuckGraph graph = PuckAction.getTheGraph();
        if (graph == null && !isOnTheFly) {
            // Batch run (Inspect Code, or the command-line inspector) without a previous Tools > Puck:
            // build the graph and check the rules now.
            graph = PuckAction.ensureGraph(holder.getProject());
        }
        if (graph != null && PuckAction.getReadyForInspection() && graph.noErrorInWLDFile()) {
            return new ProblemMarkerManager(holder);
        } else if (PuckAction.getTheProject() != null && !ApplicationManager.getApplication().isHeadlessEnvironment()) {
            Editor editor = FileEditorManager.getInstance(PuckAction.getTheProject()).getSelectedTextEditor();
            if (editor != null) {
                HintManager.getInstance().showErrorHint(editor, "Not ready for inspection Puck inspection!");
            }
        }
        return new PsiElementVisitor(){};
    }
    @NotNull
    @Override
    public String getGroupDisplayName() {
        return "Puck Inspections";
    }
    /**
     * This utility class manages building actual markers corresponding
     * to the information computed in a PuckGraph.
     * @author Yann
     *
     */
    static class ProblemMarkerManager extends PsiElementVisitor {
        private Map<PsiElement, List<String>> errors = new HashMap<>();
        public ProblemMarkerManager(ProblemsHolder holder) {
            this.holder = holder;
            addErrorMarkers(PuckAction.getTheGraph());
        }
        ProblemsHolder holder;

        @Override
        public void visitElement(PsiElement element) {
            super.visitElement(element);
            List<String> errorsList = errors.get(element);
            if (errorsList != null) {
                for (String msg : errorsList) {
                    if (msg != null) {
                        holder.registerProblem(element, msg, ProblemHighlightType.WARNING);
                    }
                }
            }
        }
        public void addErrorMarkers(PuckGraph graph)  {
            for (PuckGraph.Rule rule : graph.getRules()) {

 /*               SparseIntArray tohideorig = new SparseIntArray();
                for (int index : rule.orighide) {
                    tohideorig.put(index, 1);
                } */
                SparseIntArray tohide = new SparseIntArray();
                for (int index : rule.hide) {
                    tohide.put(index, 1);
                }
// Pour les violations Use, on cherche tous les arcs qui:
// - partent d'un noeud de la fermeture transitive des interlopers explicites de la règle
// - vers un noeud parmi de les secrets explicites de la règle
// exemple: hide classA from packageP => A a.m() ne doit pas être comptée dans les violations mais A a doit être interdit y compris dans les fils de packageP

// Pour les violations Contains, on cherche tous les arcs qui:
// - partent d'un noeud de la fermeture transitive des interlopers explicites de la règle
// - vers un noeud parmi les secrets explicites de la règle
// exemple: hide classA from packageP => si classA n'est  pas DIRECTEMENT contenue dans packageP mais dans un élément X (classe, package?) contenu dans packageP:
// - il n'existe pas d'arc de contenance de packageP vers classA (dans le graphe)
// - on veut compter la violation de X vers classA (dans le code)

                for (Integer interloper : rule.from) {
//                    addViolations(graph.getUseGraph(), ("Violates Puck rule :"+ rule.text).replaceAll("\n", ""), tohideorig, interloper);
//                   addViolations(graph.getComposeGraph(), ("Violates Puck containment rule :"+ rule.text).replaceAll("\n", ""), tohideorig, interloper);
                    addViolations(graph.getUseGraph(), ("Violates Puck rule :"+ rule.text).replaceAll("\n", ""), tohide, interloper);
                    addViolations(graph.getComposeGraph(), ("Violates Puck composition rule :"+ rule.text).replaceAll("\n", ""), tohide, interloper);
                }
            }
            // The mismatches no hide clause reported: the containment check between axes (Clause 3).
            for (PuckGraph.AxisMismatch r : graph.getUnreportedMismatches()) {
                addMarker(graph.getComposeGraph().getReasons(r.contained(), r.container()),
                        "Violates Puck composition rule :" + PuckGraph.AXES_RULE);
            }
        }


        private void addViolations(DependencyGraph dg, String message, SparseIntArray tohide, int interloper) {
            SparseIntArray arcs = dg.getGraph().getColumn(interloper);
            forEachIntersect(tohide, arcs, interloper, dg, message);
        }

        public void forEachIntersect (SparseIntArray s1, SparseIntArray s2, int interloper, DependencyGraph dg, String message) {
            if (s1.size() == 0 || s2.size() == 0) {
                return;
            }

            for (int j = 0, i = 0 , ss1 =  s1.size() , ss2 = s2.size() ; i < ss1 && j < ss2 ; ) {
                int sk1 = s1.keyAt(i);
                int sk2 = s2.keyAt(j);
                if (sk1 == sk2) {
                    // do it
                    List<PsiElement> explains = dg.getReasons(sk1, interloper);
                    if (!PuckAction.getTheGraph().isExempt(interloper, sk1)) {
                        addMarker(explains, message);
                    }
                    // end action
                    i++;
                    j++;
                } else if (sk1 > sk2) {
                    j++;
                } else {
                    i++;
                }
            }
        }

        private void addMarker(List<PsiElement> explains, String message)  {
            for (PsiElement reason : explains) {
                errors.compute(reason, (k, list) -> {
                    if (list == null) {
                        list = new ArrayList<>();
                    }
                    list.add(message);
                    return list;
                });
            }
        }
    }

}
