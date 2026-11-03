package fr.lip6.move.puck.parse;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.PsiFile;
import com.intellij.ide.highlighter.JavaFileType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class JavaFileFinder {

	/** Build output directories, left out at the project root only: a package may be called {@code out} or {@code build}. */
	public static final Set<String> OUTPUT_NAMES = Set.of("target", "build", "out");
	/** Directories never analysed, at any depth: the IDE's and the build tools' own files (and every dot-directory). */
	public static final Set<String> TOOL_NAMES = Set.of("idea-sandbox", "node_modules");
	/** A directory holding one of these is a project of its own, not part of the one being checked. */
	public static final List<String> PROJECT_MARKERS = List.of("decouple.wld", "pom.xml", "build.gradle", "build.gradle.kts",
			"settings.gradle", "settings.gradle.kts", ".idea");

	/** Whether a directory below the project is left out of the analysis: a tool directory, a build output at the top, or a nested project. */
	public static boolean isExcluded(Path dir, boolean topLevel) {
		String name = dir.getFileName().toString();
		if (name.startsWith(".") || TOOL_NAMES.contains(name) || (topLevel && OUTPUT_NAMES.contains(name))) {
			return true;
		}
		for (String marker : PROJECT_MARKERS) {
			if (Files.exists(dir.resolve(marker))) {
				return true;
			}
		}
		return false;
	}

	/** The excluded directories below a project root, at most four levels down, for the module's exclusions. */
	public static List<Path> excludedDirectories(Path root) {
		List<Path> result = new ArrayList<>();
		collectForeign(root, 0, result);
		return result;
	}

	private static void collectForeign(Path dir, int depth, List<Path> result) {
		try (var children = Files.newDirectoryStream(dir, Files::isDirectory)) {
			for (Path child : children) {
				if (isExcluded(child, depth == 0)) {
					result.add(child);
				} else if (depth < 3) {
					collectForeign(child, depth + 1, result);
				}
			}
		} catch (java.io.IOException ignored) {
			// an unreadable directory holds nothing to analyse
		}
	}

	/**
	 * Collects all Java files in a given project, including those in physical sub-directories not part of
	 * the project scope, but not those of nested projects or tool directories ({@link #isExcluded}).
	 *
	 * @param project The IntelliJ project
	 * @return A list of PsiJavaFile representing all Java files in the project and its sub-projects
	 */
	public static Set<PsiJavaFile> collectJavaFiles(Project project) {
		Set<PsiJavaFile> javaFiles = new HashSet<>();

		String projectBasePath = project.getBasePath();
		VirtualFile projectRoot = projectBasePath == null ? null : LocalFileSystem.getInstance().findFileByPath(projectBasePath);

		// Collect Java files within the project scope; the index may not yet apply the module's exclusions
		// (a project just written by the headless runner), so the excluded directories are filtered here too.
		Collection<VirtualFile> virtualFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.projectScope(project));
		for (VirtualFile virtualFile : virtualFiles) {
			if (projectRoot != null && underExcludedDirectory(virtualFile, projectRoot)) {
				continue;
			}
			PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
			if (psiFile instanceof PsiJavaFile) {
				javaFiles.add((PsiJavaFile) psiFile);
			}
		}

		// Collect Java files outside the project scope but within the project's directory
		if (projectRoot != null) {
			collectJavaFilesRecursively(project, projectRoot, javaFiles);
		}

		return javaFiles;
	}

	/** Whether a excluded directory lies between the project root and the file. */
	private static boolean underExcludedDirectory(VirtualFile file, VirtualFile projectRoot) {
		for (VirtualFile dir = file.getParent(); dir != null && !dir.equals(projectRoot); dir = dir.getParent()) {
			if (isExcluded(dir.toNioPath(), projectRoot.equals(dir.getParent()))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Recursively collects Java files from the given VirtualFile and its children.
	 *
	 * @param project The IntelliJ project
	 * @param file The current VirtualFile
	 * @param javaFiles The list to collect Java files into
	 */
	private static void collectJavaFilesRecursively(Project project, VirtualFile file, Set<PsiJavaFile> javaFiles) {
		if (file.isDirectory()) {
			for (VirtualFile child : file.getChildren()) {
				if (child.isDirectory() && isExcluded(child.toNioPath(), project.getBasePath() != null && file.getPath().equals(project.getBasePath()))) {
					continue;
				}
				collectJavaFilesRecursively(project, child, javaFiles);
			}
		} else if ("java".equals(file.getExtension())) {
			PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
			if (psiFile instanceof PsiJavaFile) {
				javaFiles.add((PsiJavaFile) psiFile);
			}
		}
	}
}
