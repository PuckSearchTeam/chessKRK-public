package org.chesskrk.oracles;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import org.jmolecules.archunit.JMoleculesArchitectureRules;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two codifications of the architecture written by others, used as oracles on the compiled classes of the
 * engine (repository root) and of its refactored naive version (After/):
 * <ul>
 * <li>jMolecules classical onion: domain model, application services, infrastructure; dependencies inward
 *     only. Checked on every class.</li>
 * <li>jMolecules hexagonal, strict: ports and adapters, adapters reaching the application through its ports
 *     only. Checked on every class but the composition root (org.chesskrk.infrastructure): the released rules
 *     have no role for a class that assembles adapters, ports and application, and report each of its
 *     dependencies as a violation. The same gap exists in ArchUnit's onion recipe, which the engine's own
 *     CleanArchitectureTest works around with an ignoreDependency on that package; the paper's context writes
 *     it as "friend wiring of all;".</li>
 * </ul>
 * The rings and roles are the package-info.java annotations of each project. The subjects are compiled for
 * Java 17 by scripts/oracles.sh, since the jMolecules rules need ArchUnit 0.23, which ignores newer class files.
 */
class ExternalOraclesTest {

    private static final Map<String, String> SUBJECTS = Map.of(
            "engine", "target/engine17",
            "After", "target/after17");

    private static final String COMPOSITION_ROOT = "/org/chesskrk/infrastructure/";

    @TestFactory
    List<DynamicTest> oracles() {
        List<DynamicTest> tests = new ArrayList<>();
        for (Map.Entry<String, String> subject : SUBJECTS.entrySet()) {
            Path classes = Paths.get(subject.getValue()).toAbsolutePath().normalize();
            tests.add(check(subject.getKey(), classes, "jMolecules classical onion, all classes",
                    JMoleculesArchitectureRules.ensureOnionClassical(), false));
            tests.add(check(subject.getKey(), classes, "jMolecules hexagonal (strict), all classes but the composition root",
                    JMoleculesArchitectureRules.ensureHexagonal(), true));
        }
        return tests;
    }

    private static DynamicTest check(String subject, Path classes, String oracle, ArchRule rule, boolean withoutCompositionRoot) {
        return DynamicTest.dynamicTest(subject + " satisfies " + oracle, () -> {
            assertTrue(Files.isDirectory(classes), "run scripts/oracles.sh first; it compiles the subjects for Java 17 into " + classes);
            ClassFileImporter importer = new ClassFileImporter();
            if (withoutCompositionRoot) {
                importer = importer.withImportOption(location -> !location.contains(COMPOSITION_ROOT));
            }
            JavaClasses imported = importer.importPath(classes);
            assertTrue(imported.size() > 0, "no class imported from " + classes);
            EvaluationResult result = rule.evaluate(imported);
            System.out.println(subject + " / " + oracle + ": " + imported.size() + " classes, "
                    + (result.hasViolation() ? "VIOLATIONS" : "no violation"));
            assertTrue(!result.hasViolation(), () -> String.join("\n", result.getFailureReport().getDetails()));
        });
    }
}
