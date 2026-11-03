package fr.lip6.move.puck.parse;

import com.intellij.psi.*;
import fr.lip6.move.puck.puckgraph.DependencyNodes;
import fr.lip6.move.puck.puckgraph.PuckGraph;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Traverses a set of input Java files to build the underlying containment and usage graphs.
 */
public final class GraphBuilder {

	/**
	 * Visits the parsed AST of these PSI files to first collect a set of nodes,
	 * then add all usage and containment edges to this graph.
	 * @param psiFiles A list of parsed PSI representations of sources.
	 * @return a graph containing the analysis results.
	 */
	public static PuckGraph collectGraph(Set<PsiJavaFile> psiFiles) {
		// The nodes we will collect in a first pass.
		DependencyNodes nodes = new DependencyNodes();
		// Sort psiFiles (PSI version)
		// semble inutile pour un Set<PsiFile> qui a remplacé la List<PsiFile> Collections.sort(psiFiles, Comparator.comparing(file -> file.getVirtualFile().getPath()));
		// First pass with a visitor.
		for (PsiJavaFile psiFile : psiFiles) {

			psiFile.accept(new JavaRecursiveElementVisitor() {

				@Override
				public void visitPackageStatement(PsiPackageStatement packageStatement) {
					PsiPackage psiPackage = JavaPsiFacade.getInstance(psiFile.getProject()).findPackage(packageStatement.getPackageName());
					if (psiPackage != null) {
						nodes.addPackage(psiPackage);
						super.visitPackageStatement(packageStatement);
					}
				}

				@Override
				public void visitClass(PsiClass aClass) {
					nodes.addType(aClass);

					for (PsiMethod psiMethod : aClass.getMethods()) {
						nodes.addMethod(psiMethod);
					}

					for (PsiField psiField : aClass.getFields()) {
						nodes.addAttribute(psiField);
					}

					// Call the parent method to ensure nested classes are visited
					super.visitClass(aClass);
				}
			});
		}

		if (!psiFiles.isEmpty()) {
			nodes.markTestNodes(com.intellij.openapi.roots.ProjectFileIndex.getInstance(psiFiles.iterator().next().getProject()));
		}
		System.out.println("Found " + nodes.size() + " nodes : " + nodes);
		System.out.println("Found " + nodes.getTestNodeNames().size() + " nodes under test source roots: " + nodes.getTestNodeNames());

		// Step 2 : we traverse the AST again, but this time we add edges to the picture
		// if either source or destination for a potential edge is not in the set of nodes
		// that we have collected above, the edge is discarded.

		// Let's initialize the graph with the context nodes we have collected.
		PuckGraph graph = new PuckGraph(nodes);

		// Now the graph builder; because it is stateful (it keeps track of which node is current owner) it is implemented as a separate Visitor class.
		EdgeCollector edgeCollector = new EdgeCollector(graph);

		// now build the graph dependency links using our visitor.
		for (PsiJavaFile psiFile : psiFiles) {
			psiFile.accept(edgeCollector);
		}

		return graph;
	}

	/**
	 * A visitor for AST that collects edges for any usage or containment
	 * where both nodes belong to the graph.
	 */
	private static class EdgeCollector extends JavaRecursiveElementVisitor {
		private final PuckGraph graph;

		private final Stack<Integer> currentOwner = new Stack<>();

		public EdgeCollector(PuckGraph graph) {
			this.graph = graph;
		}

		@Override
		public void visitElement(@NotNull PsiElement element) {
			if (element instanceof PsiReference ref) {
				PsiElement referencedElement = ref.resolve();
				if (referencedElement != null && !(referencedElement instanceof PsiPackage)) {	// a qualifier naming a package is not a use of it
					addDependency(referencedElement, element);
				}
			}
			if (element instanceof PsiNewExpression newExp) {
				PsiMethod constructor = newExp.resolveConstructor();
				if (constructor != null) {
					addDependency(constructor, element);
				}
			}
			super.visitElement(element);
		}

		private void addDependency(PsiElement referencedElement, PsiElement sourceElement) {
			if (!currentOwner.isEmpty()) {
				int indexSrc = currentOwner.peek();
				int indexDst = graph.getNodes().findIndex(referencedElement);
				if (indexDst != indexSrc && indexDst >= 0 && indexSrc >= 0) {
					graph.getUseGraph().addEdge(indexDst, indexSrc, sourceElement);
				}
			}
		}

		@Override
		public void visitClass(PsiClass psiClass) {
			// is there a containment edge ?
			int cur = graph.getNodes().findIndex(psiClass);
			if (!currentOwner.isEmpty() && cur != -1) {
				graph.getComposeGraph().addEdge(cur, currentOwner.peek(), psiClass);
			} else if (cur != -1) {
				// also add that this is contained in its package
				// so a direct containment link from package to any type definition.
				PsiFile containingFile = psiClass.getContainingFile();
				if (containingFile instanceof PsiJavaFile) {
					PsiPackageStatement packageStatement = ((PsiJavaFile) containingFile).getPackageStatement();
					if (packageStatement != null) {
						PsiPackage psiPackage = JavaPsiFacade.getInstance(containingFile.getProject()).findPackage(packageStatement.getPackageName());
						if (psiPackage != null) {
							int parent = graph.findIndex(psiPackage);
							graph.getComposeGraph().addEdge(cur, parent, psiClass);
						}
					}
				}
			} else {
				System.err.println("Class "+psiClass.toString()+" is not a graph node");
			}
			// we are new owner
			currentOwner.push(cur);
			super.visitClass(psiClass);
			currentOwner.pop();
		}

		@Override
		public void visitMethod(PsiMethod psiMethod) {
			int cur = graph.getNodes().findIndex(psiMethod);
			if (!currentOwner.isEmpty() && cur != -1) {
				graph.getComposeGraph().addEdge(cur, currentOwner.peek(), psiMethod);
			}
			currentOwner.push(cur);
			super.visitMethod(psiMethod);
			currentOwner.pop();
		}

		@Override
		public void visitField(PsiField psiField) {
			int cur = graph.getNodes().findIndex(psiField);
			if (!currentOwner.isEmpty() && cur != -1) {
				graph.getComposeGraph().addEdge(cur, currentOwner.peek(), psiField);
			}
			currentOwner.push(cur);
			super.visitField(psiField);
			currentOwner.pop();
		}

		@Override
		public void visitPackageStatement(PsiPackageStatement statement) {
			// The qualifier of a package statement resolves to the enclosing packages; that is not a use of them.
			// Containment of a class in its package is recorded in visitClass, so nothing to do here.
		}
	}

	private GraphBuilder() {}
}
