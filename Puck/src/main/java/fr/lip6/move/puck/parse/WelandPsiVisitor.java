package fr.lip6.move.puck.parse;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.tree.IElementType;
import fr.lip6.move.puck.puckgraph.PuckGraph;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.util.*;

import static fr.lip6.move.puck.parse.WelandTypes.*;

/**
 * Reads a parsed decouple.wld: collects the set and axis definitions, checks that every qualified name is a
 * node of the graph and every set used is declared, turns each written rule into a {@link Rule} over set
 * names, and generates the hide clauses of the context (paper, Section 4.4): from {@code speed a < b;} (b
 * changes more often than a) {@code hide b from a;}, from {@code difficulty X < Y;} (Y is harder to change
 * than X) {@code hide X from Y;}, both along the transitive closure of the chains, and from {@code apart a,
 * b, c;} each axis hidden from the others. The generated clauses come first in {@link #getListOfRules()},
 * speed then difficulty, grouped by hidden set; the written statements follow in file order.
 */
public class WelandPsiVisitor extends PsiElementVisitor {

    public enum Kind { HIDE, SEPARATE, FRIEND }

    /**
     * What a rule comes from: an order of the context, an {@code apart} statement, or a clause the architect
     * wrote. With the kind of edge, it picks the repair of a violation (paper, Table 3).
     */
    public enum Origin { SPEED, DIFFICULTY, APART, WRITTEN }

    /** The set name standing for every node of the program, written {@code all} in the file. */
    public static final String ALL = "*all*";

    /** The predefined set of the nodes under the test source roots; not an axis. */
    public static final String TESTS = "tests";

    /**
     * A rule over set references. A reference is a list of set names, their union being meant; the single
     * name {@link #ALL} stands for every node; {@code null} is an absent optional part.
     * <ul>
     * <li>HIDE: {@code hide} is hidden, minus {@code except} (may be null), from {@code from} (never null:
     *     {@link #ALL} when absent), minus {@code butNotFrom} (may be null);</li>
     * <li>SEPARATE: {@code hide} names the sets whose elements are separated from one another;</li>
     * <li>FRIEND: {@code from} names the friends, {@code hide} what they are friends of ({@link #ALL} allowed).</li>
     * </ul>
     */
    public record Rule(Kind kind, String ruleText, List<String> hide, List<String> except, List<String> from, List<String> butNotFrom, Origin origin) {}

    private static final List<String> ALL_REF = List.of(ALL);

    /** The sets of the file, by name, as lists of qualified names; "tests" is predefined; the axes are among them. */
    public final Map<String, List<String>> sets = new LinkedHashMap<>();
    /** The axes of the context, declared with {@code axis}; empty when the file declares none. */
    public final Map<String, List<String>> axes = new LinkedHashMap<>();
    private final List<Rule> listOfRules = new ArrayList<>();
    /** The pairs (a, b) of each order, a < b as written; closed transitively when the rules are generated. */
    private final List<String[]> speedPairs = new ArrayList<>(), difficultyPairs = new ArrayList<>();
    private List<Rule> generated = null;
    private final PuckGraph theGraph;
    private final PrintWriter errorsPrinter;

    public WelandPsiVisitor(PuckGraph graph, PrintWriter pw) {
        theGraph = graph;
        errorsPrinter = pw;
        // The predefined set "tests": every node declared under a test source root. A definition in the file overrides it.
        sets.put(TESTS, graph.getNodes().getTestNodeNames());
    }

    /** The generated clauses, then the written ones. */
    public List<Rule> getListOfRules() {
        if (generated == null) {
            generated = new ArrayList<>();
            generateChainRules(speedPairs, "speed", true, Origin.SPEED);
            generateChainRules(difficultyPairs, "difficulty", false, Origin.DIFFICULTY);
        }
        List<Rule> all = new ArrayList<>(generated);
        all.addAll(listOfRules);
        return all;
    }

    /**
     * The names of the axes: those declared with {@code axis}, or, in a file without any, every set but
     * {@code tests}. The containment check between axes (Clause 3) runs on them.
     */
    public Collection<String> axisNames() {
        if (!axes.isEmpty()) {
            return axes.keySet();
        }
        List<String> names = new ArrayList<>(sets.keySet());
        names.remove(TESTS);
        return names;
    }

    /**
     * The hide clauses of one order. With {@code hideRight}, the right side of each pair of the closure is
     * hidden from the left (speed: the faster from the slower); otherwise the left from the right
     * (difficulty: the easier from the harder). One clause per hidden set, the others listed in
     * declaration order, so that the clauses read as the files used to write them.
     */
    private void generateChainRules(List<String[]> pairs, String order, boolean hideRight, Origin origin) {
        Map<String, Set<String>> above = new LinkedHashMap<>();   // x -> every y with x < y, transitively
        for (String name : sets.keySet()) {
            above.put(name, new LinkedHashSet<>());
        }
        for (String[] pair : pairs) {
            above.get(pair[0]).add(pair[1]);
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Set<String> ys : above.values()) {
                for (String y : new ArrayList<>(ys)) {
                    if (ys.addAll(above.get(y))) {
                        changed = true;
                    }
                }
            }
        }
        for (Map.Entry<String, Set<String>> e : above.entrySet()) {
            if (e.getValue().contains(e.getKey())) {
                error("the " + order + " order has a cycle through " + e.getKey());
                return;
            }
        }
        for (String hidden : sets.keySet()) {
            List<String> from = new ArrayList<>();
            for (String other : sets.keySet()) {
                boolean related = hideRight ? above.get(other).contains(hidden) : above.get(hidden).contains(other);
                if (related) {
                    from.add(other);
                }
            }
            if (!from.isEmpty()) {
                generated.add(new Rule(Kind.HIDE, "hide " + hidden + " from " + String.join(", ", from) + ";",
                        List.of(hidden), null, from, null, origin));
            }
        }
    }

    @Override
    public void visitElement(@NotNull PsiElement element) {
        IElementType type = element.getNode().getElementType();
        if (type == SETDEF) {
            visitSetdef(element, false);
        } else if (type == AXISDEF) {
            visitSetdef(element, true);
        } else if (type == SPEED_RULE) {
            visitChain(element, speedPairs, true);
        } else if (type == DIFFICULTY_RULE) {
            visitChain(element, difficultyPairs, false);
        } else if (type == APART_RULE) {
            visitApart(element);
        } else if (type == HIDE_RULE) {
            listOfRules.add(new Rule(Kind.HIDE, text(element),
                    setRef(element, HIDDEN_SET, null), setRef(element, EXCEPTED_SET, null),
                    setRef(element, FROM_SET, ALL_REF), setRef(element, BUT_NOT_FROM_SET, null), Origin.WRITTEN));
        } else if (type == SEPARATE_RULE) {
            listOfRules.add(new Rule(Kind.SEPARATE, text(element), setRef(element, SEPARATED_SET, null), null, null, null, Origin.WRITTEN));
        } else if (type == FRIEND_RULE) {
            listOfRules.add(new Rule(Kind.FRIEND, text(element), setRef(element, OF_SET, ALL_REF), null, setRef(element, FRIEND_SET, null), null, Origin.WRITTEN));
        } else {
            for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
                child.accept(this);
            }
        }
    }

    private void visitSetdef(PsiElement setdef, boolean axis) {
        PsiElement nameElement = child(setdef, SET_NAME);
        PsiElement aSet = child(setdef, A_SET);
        if (nameElement == null || aSet == null) {
            error("incomplete set definition: " + text(setdef));
            return;
        }
        List<String> names = new ArrayList<>();
        for (PsiElement child = aSet.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNode().getElementType() == QUALIFIED_NAME) {
                String name = child.getText();
                if (!theGraph.getNodes().getNodeNames().contains(name)) {
                    error(name + " is not the qualified name of an element of the project !");
                }
                names.add(name);
            }
        }
        sets.put(nameElement.getText(), names);
        if (axis) {
            axes.put(nameElement.getText(), names);
        }
    }

    /** The groups of a chain, in order; a speed chain must name axes when the file declares any. */
    private void visitChain(PsiElement rule, List<String[]> pairs, boolean onAxes) {
        List<List<String>> groups = new ArrayList<>();
        for (PsiElement child = rule.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNode().getElementType() == GROUP) {
                groups.add(groupNames(child, rule, onAxes));
            }
        }
        for (int i = 0; i < groups.size(); i++) {
            for (int j = i + 1; j < groups.size(); j++) {
                for (String x : groups.get(i)) {
                    for (String y : groups.get(j)) {
                        pairs.add(new String[] {x, y});
                    }
                }
            }
        }
    }

    /** apart a, b, c;  becomes  hide a from b, c;  hide b from a, c;  hide c from a, b;  in place. */
    private void visitApart(PsiElement rule) {
        PsiElement group = child(rule, GROUP);
        List<String> names = group == null ? List.of() : groupNames(group, rule, true);
        if (names.size() < 2) {
            error("apart needs at least two sets (in: " + text(rule) + ")");
        }
        for (String hidden : names) {
            List<String> from = new ArrayList<>(names);
            from.remove(hidden);
            listOfRules.add(new Rule(Kind.HIDE, "hide " + hidden + " from " + String.join(", ", from) + ";",
                    List.of(hidden), null, from, null, Origin.APART));
        }
    }

    private List<String> groupNames(PsiElement group, PsiElement rule, boolean onAxes) {
        List<String> names = new ArrayList<>();
        for (PsiElement child = group.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNode().getElementType() == SET_NAME) {
                String name = child.getText();
                if (!sets.containsKey(name)) {
                    error(name + " is not the name of a known set (in: " + text(rule) + ")");
                } else if (onAxes && !axes.isEmpty() && !axes.containsKey(name)) {
                    error(name + " is a set, not an axis (in: " + text(rule) + ")");
                } else {
                    names.add(name);
                }
            }
        }
        return names;
    }

    /**
     * The set names referenced in the given role of a rule, {@link #ALL_REF} for {@code all}, or
     * {@code whenAbsent} when the role is absent. An undeclared set is an error and is left out.
     */
    private List<String> setRef(PsiElement rule, IElementType role, List<String> whenAbsent) {
        PsiElement ref = child(rule, role);
        if (ref == null) {
            return whenAbsent;
        }
        if (child(ref, ALL_TOKEN) != null) {
            return ALL_REF;
        }
        List<String> names = new ArrayList<>();
        for (PsiElement child = ref.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNode().getElementType() == SET_NAME) {
                String name = child.getText();
                if (sets.containsKey(name)) {
                    names.add(name);
                } else {
                    error(name + " is not the name of a known set (in: " + text(rule) + ")");
                }
            }
        }
        if (names.isEmpty()) {
            error("set name missing in: " + text(rule));
        }
        return names;
    }

    private static final IElementType ALL_TOKEN = WelandTypes.ALL;

    private static PsiElement child(PsiElement parent, IElementType type) {
        for (PsiElement child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNode().getElementType() == type) {
                return child;
            }
        }
        return null;
    }

    private static String text(PsiElement element) {
        return element.getText().replaceAll("\\s+", " ").trim();
    }

    private void error(String message) {
        errorsPrinter.println(message);
        theGraph.setErrorInWLDFile();
    }
}
