package fr.lip6.move.puck.puckgraph;

import com.intellij.psi.*;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DependencyNodes {
	private final List<PsiElement> nodes = new ArrayList<>();
	private final List<PsiPackage> packages = new ArrayList<>();
	private final List<PsiClass> types = new ArrayList<>();
	private final List<PsiMethod> methods = new ArrayList<>();
	private final List<PsiField> attributes = new ArrayList<>();
	private final List<String> nodeNames = new ArrayList<>();
	private final List<String> packageNames = new ArrayList<>();
	/** Names of the nodes declared under a test source root: the predefined set "tests" of Weland. */
	private final List<String> testNodeNames = new ArrayList<>();

	public List<String> getNodeNames() { return nodeNames; }
	public List<String> getPackageNames() { return packageNames; }
	public List<String> getTestNodeNames() { return testNodeNames; }

	/** Records which nodes come from a test source root; packages, shared by both roots, never do. */
	public void markTestNodes(com.intellij.openapi.roots.ProjectFileIndex index) {
		for (int i = 0; i < nodes.size(); i++) {
			PsiElement element = nodes.get(i);
			if (element instanceof PsiPackage) continue;
			PsiFile file = element.getContainingFile();
			com.intellij.openapi.vfs.VirtualFile virtualFile = file == null ? null : file.getVirtualFile();
			if (virtualFile != null && index.isInTestSourceContent(virtualFile)) {
				testNodeNames.add(nodeNames.get(i));
			}
		}
	}

	public List<PsiElement> getNodes() { return nodes; }

	public record LegendItem(String text, String color) {
	}

	public void addType(PsiClass type) {
		nodes.add(type);
		types.add(type);
		nodeNames.add(type.getQualifiedName());
	}

	public void addMethod(PsiMethod method) {
		nodes.add(method);
		methods.add(method);
		PsiParameterList psiParams = method.getParameterList();
		StringBuilder params = new StringBuilder("(");
		if (psiParams.isEmpty()) {
			params.append(")");
		} else {
			for (int i = 0 ;  i < psiParams.getParametersCount(); i++) {
				PsiParameter param = psiParams.getParameter(i);
				params.append(param.getType().getPresentableText()).append(",");
			}
			params.replace(params.lastIndexOf(","),params.length(),")");
		}
		nodeNames.add(method.getContainingClass().getQualifiedName() + "." + method.getName() + params);
	}

	public void addAttribute(PsiField field) {
		nodes.add(field);
		attributes.add(field);
		nodeNames.add(field.getContainingClass().getQualifiedName() + "." + field.getName());
	}

	public void addPackage(PsiPackage pkg) {
		if (!packages.contains(pkg)) {
			packages.add(pkg);
			nodes.add(pkg);
			nodeNames.add(pkg.getQualifiedName());
			packageNames.add(pkg.getQualifiedName());
		}
	}

	public int size() {
		return nodes.size();
	}

	@Override
	public String toString() {
		return printNodes(nodes);
	}

	public int findIndex(PsiElement element) { return nodes.indexOf(element); }

	public int findIndex(String name) {
		return nodeNames.indexOf(name);
	}

	// color nodes of a dot graph in the given color
	public static void colorize(PrintWriter out, String color, Set<Integer> nodeSet) {
		for (int i : nodeSet) {
			out.println(" n" + i + " [style = filled fillcolor=\""+ color +"\"];");
		}
	}

	public static void legendify(PrintWriter out, List<LegendItem> legend) {
		out.println("subgraph clusterLegend {");
		out.println("label=\"LEGEND\";");
		out.println("style=filled fillcolor =\"grey97\"" +"color=\"navyblue\";");
	int ind = 0;
		for (LegendItem li : legend) {
			out.println("legend"+ind+ " [label=\"\" style=filled fillcolor =\""+ li.color +"\""+"color =\""+ li.color +"\"];");
			out.println(li.text+" [shape=none];");
			out.println("legend"+ind+" -> "+li.text+" [dir=both];");
			ind++;
		}
		out.println("}");
	}
	private static String printNodes(List<? extends PsiElement> nodes) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (PsiElement e : nodes) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			if (e instanceof PsiClass) {
				sb.append("TYPE:" + ((PsiClass) e).getQualifiedName());
			} else if (e instanceof PsiMethod) {
				sb.append("METHOD:" + ((PsiMethod) e).getContainingClass().getQualifiedName() + "::" + ((PsiMethod) e).getName());
			} else if (e instanceof PsiPackage) {
				sb.append("PACKAGE:" + ((PsiPackage) e).getName());
			} else if (e instanceof PsiField) {
				sb.append("FIELD:" + ((PsiField) e).getContainingClass().getQualifiedName() + "::" + ((PsiField) e).getName());
			}
		}
		return sb.toString();
	}

	public void dotExport(PrintWriter out) {
		int index = 0;
		for (PsiElement element : nodes) {
			if (element instanceof PsiClass) {
				out.println("  n" + index + " [shape=box,label=\"" + ((PsiClass) element).getQualifiedName() + "\"];");
			} else if (element instanceof PsiField) {
				out.println("  n" + index + " [shape=doubleoctagon,label=\"" + ((PsiField) element).getContainingClass().getQualifiedName() + "." + ((PsiField) element).getName() + "\"];");
			} else if (element instanceof PsiMethod) {
				out.println("  n" + index + " [shape=octagon,label=\"" + ((PsiMethod) element).getContainingClass().getQualifiedName() + "." + ((PsiMethod) element).getName() + "\"];");
			} else if (element instanceof PsiPackage) {
				out.println("  n" + index + " [shape=folder,label=\"" + ((PsiPackage) element).getName() + "\"];");
			}
			index++;
		}
	}

	// color "hide" nodes and "from" nodes
	public void coloredDotExport(PrintWriter out, Set<Integer> hide, Set<Integer> from) {
		int index = 0;
		for (PsiElement element : nodes) {
			if (element instanceof PsiClass) {
				out.println("  n" + index + " [shape=box,label=\"" + ((PsiClass) element).getQualifiedName() + "\"];");
			} else if (element instanceof PsiField) {
				out.println("  n" + index + " [shape=doubleoctagon,label=\"" + ((PsiField) element).getContainingClass().getQualifiedName() + "." + ((PsiField) element).getName() + "\"];");
			} else if (element instanceof PsiMethod) {
				out.println("  n" + index + " [shape=octagon,label=\"" + ((PsiMethod) element).getContainingClass().getQualifiedName() + "." + ((PsiMethod) element).getName() + "\"];");
			} else if (element instanceof PsiPackage) {
				out.println("  n" + index + " [shape=folder,label=\"" + ((PsiPackage) element).getName() + "\"];");
			}
			index++;
		}
		List<LegendItem> legend = new ArrayList<>();
		legend.add(new LegendItem("Secret","gold"));
		legend.add(new LegendItem("Interloper","mediumspringgreen"));
		legendify(out,legend);
		colorize(out,"gold", hide);
		colorize(out,"mediumspringgreen", from);

	}
}
