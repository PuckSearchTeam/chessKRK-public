package fr.lip6.move.puck.cli;

import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationStarter;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import fr.lip6.move.puck.actions.PuckAction;
import fr.lip6.move.puck.parse.JavaFileFinder;
import fr.lip6.move.puck.puckgraph.PuckGraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Command-line entry point: {@code idea puck [--jdk=<name or home>] [--both] <project directory>...}, or the
 * original form {@code idea puck <project directory> [<jdk name or home>]}.
 *
 * <p>Opens each directory as a project and runs the same check as Tools &gt; Puck: the graph is built from
 * every Java file below the directory (nested projects and tool directories left out, see
 * {@link JavaFileFinder#isExcluded}), {@code decouple.wld} is read, and the violations are written to
 * {@code puck.log} and {@code graph.json} next to it. {@code puck.log} is also printed. With {@code --both}
 * the check runs twice, with the file as written and with its optional block (the lines between
 * {@code // BEGIN optional} and {@code // END optional}) commented out, and keeps both logs as
 * {@code puck-with-optional.log} and {@code puck-without-optional.log}. Several directories are checked in the
 * one IDE session, which is what makes a batch fast. The IDE then exits with 0 when every check ran, 2 when a
 * project could not be opened, 3 when a constraint file has errors.</p>
 *
 * <p>When a directory has no {@code .idea} folder, one is written first (with one module whose content root
 * is the directory, whose source roots are {@code src/main/java} and {@code src/test/java} when they exist,
 * and which excludes the excluded directories), exactly as if the directory had been opened in the IDE and
 * configured by hand. No Maven or Gradle import is performed: the only dependencies resolved are those between
 * the sources of the project itself, which is all the graph records anyway.</p>
 */
public class PuckStarter implements ApplicationStarter {

    private static final List<String> SOURCE_ROOTS = List.of("src/main/java", "src/test/java", "src/test/groovy", "src/integration-test/java", "src/integration-test/groovy");
    private static final String BEGIN_OPTIONAL = "// BEGIN optional", END_OPTIONAL = "// END optional";

    @Override
    public int getRequiredModality() {
        return NOT_IN_EDT;
    }

    @Override
    public boolean isHeadless() {
        return true;
    }

    @Override
    public void main(List<String> args) {
        int code;
        try {
            code = run(args.subList(1, args.size()));
        } catch (Throwable t) {
            t.printStackTrace();
            code = 1;
        }
        System.out.flush();
        System.err.flush();
        System.exit(code);
    }

    private int run(List<String> args) throws IOException {
        List<String> positional = new ArrayList<>();
        String jdkNameOrHome = null;
        boolean both = false;
        for (String arg : args) {
            if (arg.equals("--both")) {
                both = true;
            } else if (arg.startsWith("--jdk=")) {
                jdkNameOrHome = arg.substring("--jdk=".length());
            } else if (!arg.isBlank()) {
                positional.add(arg);
            }
        }
        // The original form: one directory, then the JDK.
        if (positional.size() == 2 && jdkNameOrHome == null && !Files.exists(Paths.get(positional.get(1), "decouple.wld"))) {
            jdkNameOrHome = positional.remove(1);
        }
        if (positional.isEmpty()) {
            System.err.println("usage: puck [--jdk=<name or home>] [--both] <project directory>...");
            return 1;
        }
        Sdk[] sdk = new Sdk[1];
        String jdk = jdkNameOrHome;
        ApplicationManager.getApplication().invokeAndWait(() -> sdk[0] = WriteAction.compute(() -> findOrCreateJdk(jdk)));

        int code = 0;
        for (String dir : positional) {
            code = Math.max(code, check(Paths.get(dir).toAbsolutePath().normalize(), sdk[0], both));
        }
        return code;
    }

    /** Opens one project, checks it once or twice, closes it. */
    private static int check(Path projectPath, Sdk sdk, boolean both) throws IOException {
        if (!Files.isDirectory(projectPath)) {
            System.err.println("puck: not a directory: " + projectPath);
            return 2;
        }
        if (!Files.isDirectory(projectPath.resolve(".idea"))) {
            writeProjectFiles(projectPath, sdk);
        }
        Project project = ProjectManagerEx.getInstanceEx().openProject(projectPath, OpenProjectTask.build());
        if (project == null) {
            System.err.println("puck: cannot open project " + projectPath);
            return 2;
        }
        try {
            configure(project, projectPath, sdk);
            int code = checkOnce(project, projectPath);
            if (code != 0 || !both) {
                return code;
            }
            Path wld = projectPath.resolve("decouple.wld");
            Path log = projectPath.resolve("puck.log");
            String original = Files.readString(wld);
            String without = withoutOptionalBlock(original);
            if (without.equals(original)) {
                System.out.println("puck: no optional block in " + wld + ", one run only");
                return 0;
            }
            Files.copy(log, projectPath.resolve("puck-with-optional.log"), StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.writeString(wld, without);
                refresh(wld);
                code = checkOnce(project, projectPath);
            } finally {
                Files.writeString(wld, original);
                refresh(wld);
            }
            Files.copy(log, projectPath.resolve("puck-without-optional.log"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(projectPath.resolve("puck-with-optional.log"), log, StandardCopyOption.REPLACE_EXISTING);
            return code;
        } finally {
            ApplicationManager.getApplication().invokeAndWait(() -> ProjectManagerEx.getInstanceEx().forceCloseProject(project));
        }
    }

    private static int checkOnce(Project project, Path projectPath) throws IOException {
        // Indexing restarts asynchronously after the module is configured or the constraint file rewritten; a
        // check that overlaps such a restart may fail to resolve references. So the check is redone when the
        // IDE entered dumb mode while it ran.
        java.util.concurrent.atomic.AtomicInteger dumbModes = new java.util.concurrent.atomic.AtomicInteger();
        project.getMessageBus().connect().subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
            @Override
            public void enteredDumbMode() {
                dumbModes.incrementAndGet();
            }
        });
        PuckGraph graph = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            waitForIndexing(project, projectPath);   // a rewritten constraint file, too, starts a re-index
            int before = dumbModes.get();
            graph = ReadAction.compute(() -> PuckAction.rebuildGraph(project));
            if (dumbModes.get() == before && !DumbService.getInstance(project).isDumb()) {
                break;
            }
            System.out.println("puck: indexing restarted during the check, checking again");
        }
        Path log = projectPath.resolve("puck.log");
        if (Files.exists(log)) {
            System.out.println("---- " + log + " ----");
            System.out.print(Files.readString(log));
        }
        if (!graph.noErrorInWLDFile()) {
            System.out.println("puck: decouple.wld has errors, see " + projectPath.resolve("decouple.err"));
            return 3;
        }
        return 0;
    }

    /** The file with every line of its optional block turned into a comment, the markers kept. */
    static String withoutOptionalBlock(String text) {
        StringBuilder result = new StringBuilder();
        boolean inside = false;
        for (String line : text.split("\n", -1)) {
            if (line.contains(BEGIN_OPTIONAL)) {
                inside = true;
            } else if (line.contains(END_OPTIONAL)) {
                inside = false;
            } else if (inside && !line.startsWith("//")) {
                line = "// " + line;
            }
            result.append(line).append('\n');
        }
        result.setLength(result.length() - 1);
        return result.toString();
    }

    /** Makes the IDE see a file written behind its back. */
    private static void refresh(Path file) {
        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(file);
        if (vf != null) {
            VfsUtil.markDirtyAndRefresh(false, false, false, vf);
        }
    }

    /** The project files the IDE would write for a directory opened and configured by hand. */
    private static void writeProjectFiles(Path projectPath, Sdk sdk) throws IOException {
        String name = projectPath.getFileName().toString();
        Path idea = projectPath.resolve(".idea");
        Files.createDirectories(idea);

        StringBuilder iml = new StringBuilder();
        iml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        iml.append("<module type=\"JAVA_MODULE\" version=\"4\">\n");
        iml.append("  <component name=\"NewModuleRootManager\" inherit-compiler-output=\"true\">\n");
        iml.append("    <exclude-output />\n");
        iml.append("    <content url=\"file://$MODULE_DIR$\">\n");
        for (String folder : SOURCE_ROOTS) {
            if (Files.isDirectory(projectPath.resolve(folder))) {
                iml.append("      <sourceFolder url=\"file://$MODULE_DIR$/").append(folder)
                   .append("\" isTestSource=\"").append(!folder.startsWith("src/main")).append("\" />\n");
            }
        }
        for (Path dir : JavaFileFinder.excludedDirectories(projectPath)) {
            iml.append("      <excludeFolder url=\"file://$MODULE_DIR$/").append(projectPath.relativize(dir).toString().replace('\\', '/')).append("\" />\n");
        }
        iml.append("    </content>\n");
        iml.append("    <orderEntry type=\"inheritedJdk\" />\n");
        iml.append("    <orderEntry type=\"sourceFolder\" forTests=\"false\" />\n");
        iml.append("  </component>\n");
        iml.append("</module>\n");
        Files.writeString(projectPath.resolve(name + ".iml"), iml.toString());

        Files.writeString(idea.resolve("modules.xml"),
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<project version=\"4\">\n"
                + "  <component name=\"ProjectModuleManager\">\n"
                + "    <modules>\n"
                + "      <module fileurl=\"file://$PROJECT_DIR$/" + name + ".iml\" filepath=\"$PROJECT_DIR$/" + name + ".iml\" />\n"
                + "    </modules>\n"
                + "  </component>\n"
                + "</project>\n");

        String jdkAttributes = sdk == null ? "" : " project-jdk-name=\"" + sdk.getName() + "\" project-jdk-type=\"JavaSDK\"";
        Files.writeString(idea.resolve("misc.xml"),
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<project version=\"4\">\n"
                + "  <component name=\"ProjectRootManager\" version=\"2\" languageLevel=\"JDK_21\"" + jdkAttributes + " />\n"
                + "</project>\n");
        System.out.println("puck: wrote " + idea + " and " + name + ".iml (one module over the directory)");
    }

    /** Fallback for a directory that already had a .idea folder: a module when there is none, a JDK, the language level. */
    private static void configure(Project project, Path projectPath, Sdk sdk) {
        ApplicationManager.getApplication().invokeAndWait(() -> WriteAction.run(() -> {
            VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(projectPath);
            if (root == null) {
                throw new IllegalStateException("puck: cannot find " + projectPath + " in the VFS");
            }
            VfsUtil.markDirtyAndRefresh(false, true, true, root);

            ProjectRootManager rootManager = ProjectRootManager.getInstance(project);
            if (sdk != null && rootManager.getProjectSdk() == null) {
                rootManager.setProjectSdk(sdk);
            }
            LanguageLevelProjectExtension.getInstance(project).setLanguageLevel(LanguageLevel.HIGHEST);

            ModuleManager moduleManager = ModuleManager.getInstance(project);
            if (moduleManager.getModules().length == 0) {
                Path iml = projectPath.resolve(projectPath.getFileName() + ".iml");
                Module module = moduleManager.newModule(iml, "JAVA_MODULE");
                ModuleRootModificationUtil.updateModel(module, model -> {
                    ContentEntry entry = model.addContentEntry(root);
                    for (String folder : SOURCE_ROOTS) {
                        VirtualFile dir = root.findFileByRelativePath(folder);
                        if (dir != null) {
                            entry.addSourceFolder(dir, !folder.startsWith("src/main"));
                        }
                    }
                    for (Path excluded : JavaFileFinder.excludedDirectories(projectPath)) {
                        VirtualFile dir = LocalFileSystem.getInstance().findFileByNioFile(excluded);
                        if (dir != null) {
                            entry.addExcludeFolder(dir);
                        }
                    }
                    model.inheritSdk();
                });
                System.out.println("puck: module " + module.getName() + " over " + root.getPath());
            }
        }));
    }

    /**
     * Waits until indexing is over and every source root is known as a package root, which is what the graph
     * builder needs to create package nodes; root changes are indexed asynchronously, so smart mode alone is
     * not enough right after a configuration change.
     */
    private static void waitForIndexing(Project project, Path projectPath) {
        DumbService dumbService = DumbService.getInstance(project);
        for (int attempt = 0; attempt < 120; attempt++) {
            dumbService.waitForSmartMode();
            boolean ready = ReadAction.compute(() -> {
                for (String folder : SOURCE_ROOTS) {
                    Path dir = projectPath.resolve(folder);
                    if (!Files.isDirectory(dir)) {
                        continue;
                    }
                    VirtualFile file = LocalFileSystem.getInstance().findFileByNioFile(dir);
                    PsiDirectory directory = file == null ? null : PsiManager.getInstance(project).findDirectory(file);
                    if (directory == null || JavaDirectoryService.getInstance().getPackage(directory) == null) {
                        return false;
                    }
                }
                return true;
            });
            if (ready && !dumbService.isDumb()) {
                return;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        System.err.println("puck: the source roots are still not indexed after a minute, continuing anyway");
    }

    private static Sdk findOrCreateJdk(String nameOrHome) {
        ProjectJdkTable table = ProjectJdkTable.getInstance();
        if (nameOrHome != null) {
            Sdk sdk = table.findJdk(nameOrHome);
            if (sdk != null) {
                return sdk;
            }
            if (Files.isDirectory(Paths.get(nameOrHome))) {
                sdk = JavaSdk.getInstance().createJdk(Paths.get(nameOrHome).getFileName().toString(), nameOrHome, false);
                table.addJdk(sdk);
                return sdk;
            }
            System.err.println("puck: no JDK named " + nameOrHome + ", using the first one registered");
        }
        Sdk[] all = table.getAllJdks();
        if (all.length > 0) {
            return all[0];
        }
        Sdk sdk = JavaSdk.getInstance().createJdk("puck-runtime", System.getProperty("java.home"), false);
        table.addJdk(sdk);
        return sdk;
    }
}
