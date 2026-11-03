package fr.lip6.move.puck.puckgraph;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.*;
import android.util.SparseIntArray;
import fr.lip6.move.gal.util.MatrixCol;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;

import static java.lang.System.exit;

/**
 * The PuckGraph is the central operational Puck graph model object.
 *
 * It stores : the nodes of the graph (as DependencyNodes), the "use" graph
 *  and the "composition" graphs (as DependencyGraph), the named set declarations
 *  parsed to a set of integer indexes, the Puck rules resolved to a "hide" and
 *  a "from" set of integer indexes.
 *
 * @author Yann
 *
 */
public class PuckGraph {
	private final Map<String, Set<Integer>> setDeclarations = new HashMap<>();
	private final List<Rule> rules = new ArrayList<>();
	/** friend F of X: uses by the closure of F of the closure of X (of any node when secrets is null) are never violations, whatever the rule. */
	public record Friend(Set<Integer> clients, Set<Integer> secrets) {}
	private final List<Friend> friends = new ArrayList<>();
	public void addFriend(Set<Integer> clients, Set<Integer> secrets) { friends.add(new Friend(clients, secrets)); }
	public List<Friend> getFriends() { return friends; }
	public boolean isExempt(int interloper, int secret) {
		for (Friend f : friends) {
			if (f.clients().contains(interloper) && (f.secrets() == null || f.secrets().contains(secret))) return true;
		}
		return false;
	}
	/** The axis of each node: the declared set whose effective closure holds it; null until the sets are read. */
	private Map<Integer, String> axisOf = null;
	public void setAxes(Map<Integer, String> axisOf) { this.axisOf = axisOf; }
	/** A containment edge joining two axes: an axis mismatch, a foreign entity in its container \(paper, Proposition 1\). */
	public record AxisMismatch(int container, int contained) {}
	/** The name under which the containment check between axes reports, in the log and the dot files. */
	public static final String AXES_RULE = "containment between axes";
	private final List<AxisMismatch> mismatches = new ArrayList<>();
	/** The mismatches that no hide clause reported: those in the direction the orders permit, which Clause 3 alone forbids. */
	private final List<AxisMismatch> unreportedMismatches = new ArrayList<>();
	public List<AxisMismatch> getAxisMismatches() { return mismatches; }
	public List<AxisMismatch> getUnreportedMismatches() { return unreportedMismatches; }
	private final DependencyNodes nodes;
	private final DependencyGraph useGraph;
	private final DependencyGraph composeGraph;

	private int nbViolationsTotal;
	private int	nbViolations;
	private PrintWriter log, out;
	/** Whether the complete .dot files are written; see {@link #fullDotWanted(int)}. */
	private boolean fullDot = true;

	private boolean errorInWLDFile = false;

	public boolean noErrorInWLDFile() {return (errorInWLDFile == false);}
	public void setErrorInWLDFile() {errorInWLDFile = true;}
	private List<String> nodeNames;
	public record ViolationStub(PsiElement element, String message) {
	}
	public HashMap<PsiElement, Set<ViolationStub>> violations = new HashMap<>();
	// containsViolations est une hashmap qui trie les violations par secret
	private HashMap<PsiElement, Set<ViolationStub>> containsViolations = new HashMap<>();

	public HashMap<PsiElement, Set<ViolationStub>> getContainsViolations() { return containsViolations;}
	private HashMap<PsiElement, List<PsiElement>> registeredContainsProblems = new HashMap<>();

	public HashMap<PsiElement, Set<ViolationStub>> getViolations() {
		return violations;
	}

	public boolean isRegistredProblem(PsiElement interloper, PsiElement secret) {
		return (registeredContainsProblems.containsKey(interloper) && registeredContainsProblems.get(interloper).contains(secret));
	}

	public void registerProblem(PsiElement interloper, PsiElement secret) {
		registeredContainsProblems.putIfAbsent(interloper, new ArrayList<>());
		registeredContainsProblems.get(interloper).add(secret);
	}
	public PuckGraph(DependencyNodes nodes) {
		this.nodes = nodes;
		this.useGraph = new DependencyGraph(nodes.size());
		this.composeGraph = new DependencyGraph(nodes.size());
	}

	public DependencyGraph getUseGraph() {
		return useGraph;
	}

	public DependencyNodes getNodes() {
		return nodes;
	}

	public DependencyGraph getComposeGraph() {
		return composeGraph;
	}

	public void addSetDeclaration(String name, Set<Integer> nodes) {
		setDeclarations.put(name, nodes);
	}

	public Set<Integer> getSetDeclaration (String name) {
		return setDeclarations.getOrDefault(name, Collections.emptySet());
	}
	public static class Rule {
		public String text;
		public final Set<Integer> hide;
		public final Set<Integer> from;
		public final Set<Integer> orighide;
		public final Set<Integer> origfrom;
		/** What generated the clause: speed, difficulty, apart or written; picks the repair of a violation. */
		public final String origin;
		public Rule(Set<Integer> hide, Set<Integer> from, Set<Integer> orighide, Set<Integer> origfrom,String text, String origin) {
			this.hide = hide;
			this.from = from;
			this.orighide = orighide;
			this.origfrom = origfrom;
			this.text = text;
			this.origin = origin;
		}
	}
	public void addRule (Set<Integer> hide, Set<Integer> from, Set<Integer> orighide, Set<Integer> origfrom, String text, String origin) {
		this.rules.add (new Rule(hide,from, orighide, origfrom, text, origin));
	}

	public int findIndex(PsiElement key) {
		return nodes.findIndex(key);
	}

	public int findIndex(String key) {
		return nodes.findIndex(key);
	}

	public List<Rule> getRules() {
		return rules;
	}

	public static int findSingleIncomingNeighbor(MatrixCol graph, int node) {
		if (node < 0 || node >= graph.getColumnCount()) {
			throw new IllegalArgumentException("Node index out of bounds.");
		}

		int incomingNeighbor = -1;
		for (int row = 0; row < graph.getRowCount(); row++) {
			if (graph.get(row, node) != 0) {
				if (incomingNeighbor != -1) {
					// Found more than one incoming neighbor.
					return -1;
				}
				incomingNeighbor = row;
			}
		}
		return incomingNeighbor;
	}

	public int findRelevantParent(int node, Set<Integer> elements){
		while (!elements.contains(node)) {
			node = findSingleIncomingNeighbor(composeGraph.getGraph(),node);
		}
		return node;
	}

	/**
	 * A visual representation for our graphs.
	 * @param path where will we build this dot file 
	 * @throws IOException if we couldn't write to that place.
	 */

	public void exportDot (String path) throws IOException {
//		getComposeGraph();
		out = new PrintWriter(new File(path+"/graph.dot"));
		log = new PrintWriter(new File(path+"/puck.log"));
		fullDot = fullDotWanted(nodes.getNodeNames().size());
		if (!fullDot) {
			log.println("Complete .dot exports skipped (" + nodes.getNodeNames().size() + " nodes; -Dpuck.fullDot=true forces them); the _RedOnly.dot files are written.");
		}
		out.println("digraph  G {");
		if (fullDot) {
			nodes.dotExport(out);
			useGraph.dotExport(out, "");
			composeGraph.dotExport(out, "[style=dotted]");
		}

		boolean doRedArcs=true;
		if (! doRedArcs) {
			// named sets
			for (Entry<String, Set<Integer>> ent : setDeclarations.entrySet()) {
				out.println("  "+ent.getKey()+ " [color=blue] ;");
				for (Integer i : ent.getValue()) {
					out.println("  "+ent.getKey()+ " -> n" + i + " [color=blue] ;");
				}
			}
		} else {
			log.println("Violations:");
			nbViolationsTotal = 0;
			nodeNames = nodes.getNodeNames();
			Set<AxisMismatch> reportedContainments = new HashSet<>();
			for (int idx = 0;  idx < rules.size() ; idx++) {
				Rule rule = rules.get(idx);
				nbViolations = 0;
				// One graph constructed for each rule
				Set<Integer> from = new HashSet<>(rule.from);
				Set<Integer> hide = new HashSet<>(rule.hide);
				Set<Integer> origfrom = new HashSet<>(rule.origfrom);
				Set<Integer> orighide = new HashSet<>(rule.orighide);
				PrintWriter ruleOut = new PrintWriter(new File(path+"/"+rule.text.replace(' ', '_').replace(";", "")+".dot"));
				ruleOut.println("digraph  G {");
				PrintWriter ruleOutRedOnly = new PrintWriter(new File(path+"/"+rule.text.replace(' ', '_').replace(";", "")+"_RedOnly.dot"));
				Set<Integer> redNodesIndexes = new HashSet<>();
				ruleOutRedOnly.println("digraph  G {");

				// choose what sets to show in dot files
				Boolean showOrigSets = true;
				if (fullDot) {
					// in dot files, only the nodes explicitly named in hide and from are colored (showOrigSets)
					nodes.coloredDotExport(ruleOut, showOrigSets ? orighide : hide, showOrigSets ? origfrom : from);
					useGraph.dotExport(ruleOut, "");
					composeGraph.dotExport(ruleOut, "[style=dotted]");
				}
				log.println("- "+rule.text);
				for (Integer interloper : from) {
//					for (Integer secret : orighide) {
					for (Integer secret : hide) {
						if (isExempt(interloper, secret)) continue;
						if (useGraph.hasEdge(secret, interloper)) {
							String message = printViolation(ruleOut, nodeNames, rule.text, interloper, secret, " --u--> ", repairFor(" --u--> ", rule.origin));
							createViolationStub(secret, interloper, message);
							ruleOutRedOnly.println("  n" + interloper + " -> n" + secret + " [color=red] ;");
							redNodesIndexes.add(interloper);
							redNodesIndexes.add(secret);
						}
						if (composeGraph.hasEdge(secret, interloper)) {
//							if (!orighide.contains(secret)) {
//								continue;
//							}
							String message = printViolation(ruleOut, nodeNames, rule.text, interloper, secret, " --c--> ", repairFor(" --c--> ", rule.origin));
							createViolationStub(secret, interloper, message);
							ruleOutRedOnly.println("  n" + interloper + " -> n" + secret + " [color=red, style=dotted] ;");
							redNodesIndexes.add(interloper);
							redNodesIndexes.add(secret);
							addContainsViolation(interloper, secret, message);
							reportedContainments.add(new AxisMismatch(interloper, secret));
						}
					}
				}
				log.println("Number of violations for the rule: "+nbViolations);
				ruleOut.println("}");
				ruleOut.close();
				exportRedDots(ruleOutRedOnly, redNodesIndexes);
				ruleOutRedOnly.println("}");
				ruleOutRedOnly.close();
			}
			checkContainmentBetweenAxes(path, reportedContainments);
			log.println("Total Number of violations: "+nbViolationsTotal);
		}
		out.println("}");
		out.close();
		log.close();
	}

	/**
	 * Clause 3 of the paper: no entity of an axis contains an entity of another, whatever the orders. By
	 * Proposition 1 its violations are the mismatches, one containment edge each. The hide clauses already report
	 * the mismatches against the order (the hidden axis inside the hiding one); this reports the others, those in
	 * the direction the orders permit, under the rule {@link #AXES_RULE}, with the count of all mismatches.
	 */
	private void checkContainmentBetweenAxes(String path, Set<AxisMismatch> reported) throws IOException {
		mismatches.clear();
		unreportedMismatches.clear();
		if (axisOf == null) {
			return;
		}
		MatrixCol compose = composeGraph.getGraph();
		for (int container = 0; container < compose.getColumnCount(); container++) {
			SparseIntArray contained = compose.getColumn(container);
			for (int i = 0; i < contained.size(); i++) {
				int node = contained.keyAt(i);
				String a = axisOf.get(container), b = axisOf.get(node);
				if (a != null && b != null && !a.equals(b) && !isExempt(container, node)) {
					mismatches.add(new AxisMismatch(container, node));
				}
			}
		}
		nbViolations = 0;
		String stem = path + "/" + AXES_RULE.replace(' ', '_');
		PrintWriter ruleOut = new PrintWriter(new File(stem + ".dot"));
		PrintWriter ruleOutRedOnly = new PrintWriter(new File(stem + "_RedOnly.dot"));
		ruleOut.println("digraph  G {");
		ruleOutRedOnly.println("digraph  G {");
		if (fullDot) {
			nodes.coloredDotExport(ruleOut, Set.of(), Set.of());
			useGraph.dotExport(ruleOut, "");
			composeGraph.dotExport(ruleOut, "[style=dotted]");
		}
		Set<Integer> redNodesIndexes = new HashSet<>();
		log.println("- " + AXES_RULE);
		for (AxisMismatch r : mismatches) {
			if (reported.contains(r)) {
				continue;
			}
			String message = printViolation(ruleOut, nodeNames, AXES_RULE, r.container(), r.contained(), " --c--> ", repairFor(" --c--> ", "axes"));
			createViolationStub(r.contained(), r.container(), message);
			ruleOutRedOnly.println("  n" + r.container() + " -> n" + r.contained() + " [color=red, style=dotted] ;");
			redNodesIndexes.add(r.container());
			redNodesIndexes.add(r.contained());
			addContainsViolation(r.container(), r.contained(), message);
			unreportedMismatches.add(r);
		}
		log.println("Number of violations for the rule: " + nbViolations);
		log.println("Axis mismatches: " + mismatches.size() + " (" + (mismatches.size() - nbViolations) + " reported above by hide clauses, "
				+ nbViolations + " here)");
		ruleOut.println("}");
		ruleOut.close();
		exportRedDots(ruleOutRedOnly, redNodesIndexes);
		ruleOutRedOnly.println("}");
		ruleOutRedOnly.close();
	}

	private void addContainsViolation(int interloper, int secret, String message) {
		PsiElement interloperPsi = nodes.getNodes().get(interloper);
		PsiElement secretPsi = nodes.getNodes().get(secret);
		containsViolations.computeIfAbsent(secretPsi, k -> new HashSet<>()).add(new ViolationStub(interloperPsi, message));
	}

	private void exportRedDots(PrintWriter ruleOutRedOnly, Set<Integer> redNodes) {
		for (int index : redNodes) {
			PsiElement element = nodes.getNodes().get(index);
			if (element instanceof PsiClass) {
				ruleOutRedOnly.println("  n" + index + " [shape=box,label=\"" + ((PsiClass) element).getQualifiedName() + "\"];");
			} else if (element instanceof PsiField) {
				ruleOutRedOnly.println("  n" + index + " [shape=doubleoctagon,label=\"" + ((PsiField) element).getContainingClass().getQualifiedName() + "." + ((PsiField) element).getName() + "\"];");
			} else if (element instanceof PsiMethod) {
				ruleOutRedOnly.println("  n" + index + " [shape=octagon,label=\"" + ((PsiMethod) element).getContainingClass().getQualifiedName() + "." + ((PsiMethod) element).getName() + "\"];");
			} else if (element instanceof PsiPackage) {
				ruleOutRedOnly.println("  n" + index + " [shape=folder,label=\"" + ((PsiPackage) element).getName() + "\"];");
			}
		}
	}

	private void createViolationStub(int secret, int interloper, String message) {
		ViolationStub v = new ViolationStub(nodes.getNodes().get(secret), message);
		PsiElement interlop = nodes.getNodes().get(interloper);
		if (violations.containsKey(interlop)) {
			violations.get(interlop).add(v);
		} else {
			Set<ViolationStub> vs = new HashSet<>();
			vs.add(v);
			violations.put(interlop, vs);
		}
	}

	/**
	 * The repair of a violation (paper, Table 3), from the kind of edge and what generated the clause: a containment
	 * across axes is repaired by moving the foreign entity; a use against the speed order by an inversion through an
	 * interface owned by the user; one against the difficulty order by the same inversion or by moving the
	 * dependency; one against an apart statement by the same inversion (the axes being unordered, either side may own the interface); one against a written clause, which is a decision of the architect
	 * that no order generated, by the same inversion or by revising the decision.
	 */
	static String repairFor(String arrow, String origin) {
		if (arrow.equals(" --c--> ")) {
			return "SRP, CCP: Move Method or Move Class into a container of the entity's axis, or Extract Class";
		}
		return switch (origin == null ? "written" : origin) {
			case "speed" -> "OCP, DIP: Extract Interface into the user's axis and redirect the use; Introduce Parameter or a factory for an instantiation";
			case "difficulty" -> "SDP: the same inversion, or move the dependency to an entity that is cheaper to change";
			case "apart" -> "OCP, DIP, between axes kept apart: Extract Interface into the user's axis and redirect the use";
			default -> "written clause, a decision of the architect (ADP when it directs an unordered pair): the same inversion, or revise the decision";
		};
	}

	/**
	 * The complete .dot of the graph, written once and once per clause, is what makes a run on thousands of nodes
	 * take minutes; it is written for graphs of at most 3000 nodes unless the system property puck.fullDot says
	 * true or false. The violation-only _RedOnly.dot files and puck.log do not depend on it.
	 */
	static boolean fullDotWanted(int nodes) {
		String p = System.getProperty("puck.fullDot", "auto");
		if (p.equalsIgnoreCase("true")) return true;
		if (p.equalsIgnoreCase("false")) return false;
		return nodes <= 3000;
	}

	private String printViolation(PrintWriter ruleOut, List<String> nms, String ruleText, int interloper, int secret, String arrow, String repair) {
		String type = "";
		if (arrow.equals(" --c--> ")) {
			type+=", style=dotted";
		}
		out.println("  n" + interloper + " -> n" + secret + " [color=red"+type+"] ;");
		ruleOut.println("  n" + interloper + " -> n" + secret + " [color=red"+type+"] ;");
		log.println("   + " + nms.get(interloper) + arrow + nms.get(secret) + "   [" + repair + "]");
		String message = nms.get(interloper) + " -> " + nms.get(secret) + " violates Puck rule: " + ruleText + " Repair: " + repair + ".";
		nbViolations++;
		nbViolationsTotal++;
		return message;
	}

	private void printoutContainsPath(Integer node, Set<Integer> elements, PrintWriter pw) {
		if (node == -1) exit(0);
		int parent = findRelevantParent(node,elements);
		while (parent != -1) {
			pw.println("  n" + parent + " -> n" + node + " [color=red] ;");
			System.out.println("n" + parent + " : " + "n" + node);
			node = parent;
			parent = findRelevantParent(node,elements);
		}
	}

	public void exportJson(String path) throws IOException {
		Map<Integer, NodeInfo> nodeInfos = collectNodeInfos();
		List<Edge> edges = collectEdges();
		List<RuleInfo> rules = collectRules();
		Map<String, List<Violation>> violations = collectViolations();

		Map<String, Object> graphData = new HashMap<>();
		graphData.put("nodes", nodeInfos);
		graphData.put("edges", edges);
		graphData.put("rules", rules);
		graphData.put("violations", violations);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(graphData);

		try (PrintWriter out = new PrintWriter(new File(path + "/graph.json"))) {
			out.println(json);
		}
	}

	private Map<Integer, NodeInfo> collectNodeInfos() {
		Map<Integer, NodeInfo> nodeInfos = new HashMap<>();
		for (int i = 0; i < nodes.size(); i++) {
			PsiElement element = nodes.getNodes().get(i);
			if (element != null && element.getContainingFile() != null) {
				PsiFile containingFile = element.getContainingFile();
				if (containingFile.getVirtualFile() != null) {
					String filePath = containingFile.getVirtualFile().getPath();
					String fileName = containingFile.getName();
					Document document = PsiDocumentManager.getInstance(containingFile.getProject()).getDocument(containingFile);

					if (document != null && element.getTextRange() != null) {
						int startLine = document.getLineNumber(element.getTextRange().getStartOffset()) + 1;
						int startOffset = element.getTextRange().getStartOffset() - document.getLineStartOffset(startLine - 1);
						int endLine = document.getLineNumber(element.getTextRange().getEndOffset()) + 1;
						int endOffset = element.getTextRange().getEndOffset() - document.getLineStartOffset(endLine - 1);

						String qualifiedName = "";
						String nodeType = "";

						if (element instanceof PsiClass) {
							qualifiedName = ((PsiClass) element).getQualifiedName();
							nodeType = "Class";
						} else if (element instanceof PsiField) {
							qualifiedName = ((PsiField) element).getContainingClass().getQualifiedName() + "." + ((PsiField) element).getName();
							nodeType = "Attribute";
						} else if (element instanceof PsiMethod) {
							qualifiedName = ((PsiMethod) element).getContainingClass().getQualifiedName() + "." + ((PsiMethod) element).getName();
							nodeType = "Method";
						} else if (element instanceof PsiPackage) {
							qualifiedName = ((PsiPackage) element).getQualifiedName();
							nodeType = "Package";
						} else if (element instanceof PsiClass && ((PsiClass) element).isInterface()) {
							qualifiedName = ((PsiClass) element).getQualifiedName();
							nodeType = "Interface";
						}

						nodeInfos.put(i, new NodeInfo(filePath, fileName, startLine, startOffset, endLine, endOffset, qualifiedName, nodeType));
					}
				}
			}
		}
		return nodeInfos;
	}

	private List<Edge> collectEdges() {
		List<Edge> edges = new ArrayList<>();
		for (int i = 0; i < useGraph.getGraph().getRowCount(); i++) {
			for (int j = 0; j < useGraph.getGraph().getColumnCount(); j++) {
				if (useGraph.getGraph().get(i, j) != 0) {
					edges.add(new Edge(i, j, "use"));
				}
				if (composeGraph.getGraph().get(i, j) != 0) {
					edges.add(new Edge(i, j, "compose"));
				}
			}
		}
		return edges;
	}

	private List<RuleInfo> collectRules() {
		List<RuleInfo> ruleInfos = new ArrayList<>();
		for (Rule rule : rules) {
			ruleInfos.add(new RuleInfo(rule));
		}
		return ruleInfos;
	}

	private Map<String, List<Violation>> collectViolations() {
		Map<String, List<Violation>> violationMap = new HashMap<>();
		for (Entry<PsiElement, Set<ViolationStub>> entry : violations.entrySet()) {
			List<Violation> violationList = new ArrayList<>();
			for (ViolationStub stub : entry.getValue()) {
				violationList.add(new Violation(stub));
			}
			violationMap.put(entry.getKey().toString(), violationList);
		}
		return violationMap;
	}

	class NodeInfo {
		String filePath;
		String fileName;
		int startLine;
		int startOffset;
		int endLine;
		int endOffset;
		String qualifiedName;
		String nodeType;

		NodeInfo(String filePath, String fileName, int startLine, int startOffset, int endLine, int endOffset, String qualifiedName, String nodeType) {
			this.filePath = filePath;
			this.fileName = fileName;
			this.startLine = startLine;
			this.startOffset = startOffset;
			this.endLine = endLine;
			this.endOffset = endOffset;
			this.qualifiedName = qualifiedName;
			this.nodeType = nodeType;
		}
	}

	class Edge {
		int source;
		int target;
		String type;

		Edge(int source, int target, String type) {
			this.source = source;
			this.target = target;
			this.type = type;
		}
	}

	class RuleInfo {
		String text;
		Set<Integer> hide;
		Set<Integer> from;
		Set<Integer> orighide;
		Set<Integer> origfrom;

		RuleInfo(PuckGraph.Rule rule) {
			this.text = rule.text;
			this.hide = rule.hide;
			this.from = rule.from;
			this.orighide = rule.orighide;
			this.origfrom = rule.origfrom;
		}
	}

	class Violation {
		String element;
		String message;

		Violation(PuckGraph.ViolationStub stub) {
			this.element = stub.element().toString();
			this.message = stub.message();
		}
	}
}
