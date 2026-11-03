package fr.lip6.move.puck.actions;

import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.actions.RunInspectionIntention;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import fr.lip6.move.gal.util.GraphUtils;
import fr.lip6.move.gal.util.MatrixCol;
import fr.lip6.move.puck.puckgraph.DotToCytoscapeJs;
import fr.lip6.move.puck.puckgraph.PuckGraph;
import fr.lip6.move.puck.parse.GraphBuilder;
import fr.lip6.move.puck.parse.JavaFileFinder;
import fr.lip6.move.puck.parse.WelandPsiVisitor;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class PuckAction extends AnAction {
    private static PuckGraph theGraph = null;
    public static PuckGraph getTheGraph() { return theGraph; };
    private static Project project = null;
    public static Project getTheProject() { return project; };
    PrintWriter pw_wlderrors = null;
    private static boolean readyForInspection = false;
    public static boolean getReadyForInspection() { return readyForInspection; }
    public static void setReadyForInspection() { readyForInspection = true; }
    private static boolean readyForWelandInspection = false;
    public static boolean getReadyForWelandInspection() { return readyForWelandInspection; }
    public static void setReadyForWelandInspection() { readyForWelandInspection = true; }

    public PuckGraph checkForViolations(Project project) {
        Set<PsiJavaFile> files = JavaFileFinder.collectJavaFiles(project);
        System.out.println("Found " + files.size() + " Java files");
        for (PsiJavaFile f : files) {
            System.out.println("File: " + f.getName() + " in Package: " + f.getPackageName());

        }
        PuckGraph graph = GraphBuilder.collectGraph(files);
        String projectPath = project.getBasePath();
        VirtualFile wf = createVirtualWldFile(projectPath);
        if (wf == null) {
            return graph;
        }
        WelandPsiVisitor visitor = createWldVisitor(graph, projectPath);
        PsiManager psiManager = PsiManager.getInstance(project);
        PsiFile psiFile = psiManager.findFile(wf);
        assert psiFile != null;
        psiFile.accept(visitor);
        pw_wlderrors.close();
        PuckAction.setReadyForWelandInspection();
        runWelandInspection(project, wf);
        if (graph.noErrorInWLDFile()) {
            addRules(graph, visitor.getListOfRules(), visitor.sets, visitor.axisNames());
        }
        try {
            graph.exportDot(projectPath);
            DotToCytoscapeJs.main(new String[]{projectPath});
            PuckAction.setReadyForInspection();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return graph;
    }
    private  VirtualFile createVirtualWldFile(String projectPath) {
        VirtualFile wf = LocalFileSystem.getInstance().findFileByIoFile(new File(projectPath, "decouple.wld"));
        if (wf == null) {
            System.out.println("File decouple.wld not found in the project");
            Editor editor = FileEditorManager.getInstance(PuckAction.getTheProject()).getSelectedTextEditor();
            if (editor != null) {
                HintManager.getInstance().showErrorHint(editor, "File decouple.wld not found in the project");
            }
        }
        return wf;
    }

    /**
     * Turns the rules of the file into rules of the graph, i.e. pairs of index sets (hide, from) plus the
     * friend exemptions. A set reference in a rule is a list of set names whose union is meant, or
     * {@code all}. Semantics (paper, Sections 4.2 and 4.3), with cl(X) the effective closure of a reference:
     * the named entities and everything they contain, minus the entities that a set outside the reference
     * names inside them, which are mismatches and belong to that other set with their own scope.
     * <ul>
     * <li>{@code hide X except X' from Y but-not-from Y'}: hidden = cl(X) minus cl(X'), interlopers =
     *     cl(Y) minus cl(Y') minus cl(X) (we do not hide from ourselves); {@code from} absent means every node;</li>
     * <li>{@code separate X}: for each element z named by X, cl({z}) is hidden from the closure of the other
     *     elements, one rule per element;</li>
     * <li>{@code friend F of X}: uses by cl(F) of cl(X) are never violations, whatever the rule.</li>
     * </ul>
     * The axes of the context (paper, Section 4.3) are the sets declared with {@code axis}, or every set but
     * {@code tests} in a file without any; the graph gets the axis of every node, for the containment check
     * between axes (Clause 3, Proposition 1).
     */
    private static void addRules(PuckGraph graph, List<WelandPsiVisitor.Rule> rules, Map<String, List<String>> sets,
                                 Collection<String> axisNames) {
        Closures closures = new Closures(graph, sets);
        for (String name : sets.keySet()) {
            List<String> ref = List.of(name);
            System.out.println("Set " + name + ": " + closures.explicit(ref).size() + " named entities, "
                    + closures.effective(ref).size() + " in the effective closure");
        }
        for (WelandPsiVisitor.Rule r : rules) {
            switch (r.kind()) {
                case HIDE -> {
                    Set<Integer> orighide = closures.explicit(r.hide());
                    Set<Integer> hide = closures.effective(r.hide());
                    hide.removeAll(closures.effective(r.except()));
                    Set<Integer> origfrom = closures.explicit(r.from());
                    Set<Integer> from = closures.effective(r.from());
                    from.removeAll(closures.effective(r.butNotFrom()));
                    from.removeAll(closures.effective(r.hide()));
                    graph.addRule(hide, from, orighide, origfrom, r.ruleText(), r.origin().name().toLowerCase());
                }
                case SEPARATE -> {
                    List<Integer> elements = new ArrayList<>(closures.explicit(r.hide()));
                    for (int z : elements) {
                        Set<Integer> hide = closures.effectiveElement(z, r.hide());
                        Set<Integer> others = new HashSet<>(elements);
                        others.remove(z);
                        Set<Integer> from = new HashSet<>();
                        for (int other : others) {
                            from.addAll(closures.effectiveElement(other, r.hide()));
                        }
                        from.removeAll(hide);
                        if (!from.isEmpty()) {
                            graph.addRule(hide, from, Set.of(z), others,
                                    r.ruleText() + " (" + graph.getNodes().getNodeNames().get(z) + ")", r.origin().name().toLowerCase());
                        }
                    }
                }
                case FRIEND -> {
                    Set<Integer> clients = closures.effective(r.from());
                    Set<Integer> secrets = Closures.isAll(r.hide()) ? null : closures.effective(r.hide());
                    System.out.println("Friend rule " + r.ruleText() + ": " + clients.size() + " clients, "
                            + (secrets == null ? "all" : secrets.size()) + " secrets");
                    graph.addFriend(clients, secrets);
                }
            }
        }
        graph.setAxes(closures.axes(axisNames));
    }

    /** The explicit and effective closures of the sets of a file, over node indexes. */
    private static final class Closures {
        private final PuckGraph graph;
        private final MatrixCol containment;
        private final Map<String, Set<Integer>> explicit = new LinkedHashMap<>();
        private final Map<String, Set<Integer>> effective = new HashMap<>();

        Closures(PuckGraph graph, Map<String, List<String>> sets) {
            this.graph = graph;
            this.containment = graph.getComposeGraph().getGraph();
            for (Map.Entry<String, List<String>> set : sets.entrySet()) {
                Set<Integer> indexes = new HashSet<>();
                for (String name : set.getValue()) {
                    int index = graph.findIndex(name);
                    if (index >= 0) {
                        indexes.add(index);
                    }
                }
                explicit.put(set.getKey(), indexes);
            }
        }

        static boolean isAll(List<String> ref) {
            return ref != null && ref.size() == 1 && WelandPsiVisitor.ALL.equals(ref.get(0));
        }

        /** The nodes a reference names; every node for {@code all}; nothing for {@code null}. */
        Set<Integer> explicit(List<String> ref) {
            Set<Integer> result = new HashSet<>();
            if (ref == null) {
                return result;
            }
            if (isAll(ref)) {
                return allNodes();
            }
            for (String set : ref) {
                result.addAll(explicit.getOrDefault(set, Set.of()));
            }
            return result;
        }

        /** A copy of the effective closure of a reference: the union of those of its sets. */
        Set<Integer> effective(List<String> ref) {
            Set<Integer> result = new HashSet<>();
            if (ref == null) {
                return result;
            }
            if (isAll(ref)) {
                return allNodes();
            }
            for (String set : ref) {
                Set<Integer> one = effective.get(set);
                if (one == null) {
                    one = closure(explicit.getOrDefault(set, Set.of()));
                    removeForeignScopes(one, Set.of(set));
                    effective.put(set, one);
                }
                result.addAll(one);
            }
            return result;
        }

        /** The effective closure of one named element: its scope minus the mismatches of sets outside the reference. */
        Set<Integer> effectiveElement(int element, List<String> ref) {
            Set<Integer> result = closure(Set.of(element));
            removeForeignScopes(result, ref == null ? Set.of() : new HashSet<>(ref));
            return result;
        }

        /** Removes from a closure the scopes of the foreign entities inside it: those that sets outside the given ones name. */
        private void removeForeignScopes(Set<Integer> result, Collection<String> own) {
            Set<Integer> ownExplicit = new HashSet<>();
            for (String set : own) {
                ownExplicit.addAll(explicit.getOrDefault(set, Set.of()));
            }
            for (Map.Entry<String, Set<Integer>> other : explicit.entrySet()) {
                if (own.contains(other.getKey())) {
                    continue;
                }
                for (int foreign : other.getValue()) {
                    if (!ownExplicit.contains(foreign) && result.contains(foreign)) {
                        result.removeAll(closure(Set.of(foreign)));
                    }
                }
            }
        }

        /**
         * The axis of every node that the effective closure of an axis holds; a node held by two axes
         * (overlapping declarations) has none.
         */
        Map<Integer, String> axes(Collection<String> axisNames) {
            Map<Integer, String> result = new HashMap<>();
            Set<Integer> ambiguous = new HashSet<>();
            for (String set : axisNames) {
                for (int node : effective(List.of(set))) {
                    String previous = result.put(node, set);
                    if (previous != null && !previous.equals(set)) {
                        ambiguous.add(node);
                    }
                }
            }
            result.keySet().removeAll(ambiguous);
            if (!ambiguous.isEmpty()) {
                System.out.println(ambiguous.size() + " nodes are named by two sets and get no axis");
            }
            return result;
        }

        private Set<Integer> closure(Set<Integer> nodes) {
            Set<Integer> result = new HashSet<>(nodes);
            GraphUtils.collectSuffix(result, containment);
            return result;
        }

        private Set<Integer> allNodes() {
            Set<Integer> result = new HashSet<>();
            for (int i = 0; i < graph.getNodes().size(); i++) {
                result.add(i);
            }
            return result;
        }
    }

    private WelandPsiVisitor createWldVisitor(PuckGraph graph, String projectPath) {
        try {
            pw_wlderrors = new PrintWriter(projectPath + "/decouple.err");
        } catch (FileNotFoundException e) {
            System.out.println("Error creating decouple.err file");
        }
        return new WelandPsiVisitor(graph, pw_wlderrors);
    }
    private static void runWelandInspection(Project project, VirtualFile wf) {
        // Rerunning an inspection from inside a batch inspection run makes no sense; in a headless run there is no UI at all.
        if (batchRun || ApplicationManager.getApplication().isHeadlessEnvironment()) return;
        AnalysisScope wldscope = new AnalysisScope(project, Collections.singleton(wf));
        // Run inspection
        InspectionToolWrapper<?, ?> wrapper = LocalInspectionToolWrapper.findTool2RunInBatch(project, null, "WldInspection");
        if (wrapper != null)
            RunInspectionIntention.rerunInspection(wrapper, (InspectionManagerEx) InspectionManager.getInstance(project), wldscope, null);
    }

    private static boolean batchRun = false;

    /**
     * Builds the graph and checks the rules for a project when no Tools > Puck action has run yet.
     * Used by the Puck inspection in batch mode (Inspect Code, or the command-line inspector), so that
     * the check can run without any user interaction. The result is also written to graph.json.
     */
    public static synchronized PuckGraph ensureGraph(Project p) {
        if (theGraph == null || project != p) {
            project = p;
            batchRun = true;
            theGraph = new PuckAction().checkForViolations(p);
            try {
                theGraph.exportJson(p.getBasePath());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return theGraph;
    }

    /** Builds the graph and checks the rules afresh, whatever is cached: for the headless runner, which may check a project twice. */
    public static synchronized PuckGraph rebuildGraph(Project p) {
        theGraph = null;
        return ensureGraph(p);
    }

    public void actionPerformed(AnActionEvent e) {
        project = e.getProject();
        if (project != null) {
            theGraph = checkForViolations(project);
            // quit if the WLD file contains at least one error
            if (!theGraph.noErrorInWLDFile()) {
                JOptionPane.showMessageDialog(null, "The WLD file contains at least one error!");
                return;
            }
            // count the violations recorded in theGraph
            int nbrv = 0;
            for (Set<PuckGraph.ViolationStub> lv : theGraph.getViolations().values()) {
                nbrv+=lv.size();
            }
            System.out.println("Number of recorded violations: "+nbrv);

            // Run the PuckInspection programmatically
            Collection<VirtualFile> virtualJavaFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.projectScope(project));
            // Create a new AnalysisScope for the project
            AnalysisScope scope = new AnalysisScope(project, virtualJavaFiles);
            // Run inspection
            InspectionToolWrapper<?, ?> wrapper = LocalInspectionToolWrapper.findTool2RunInBatch(project, null, "PuckInspection");
            if (wrapper != null)
                RunInspectionIntention.rerunInspection(wrapper, (InspectionManagerEx)InspectionManager.getInstance(project), scope, null);

            // generate the JSON file with all infos
            String projectPath = project.getBasePath();
            try {
                theGraph.exportJson(projectPath);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
