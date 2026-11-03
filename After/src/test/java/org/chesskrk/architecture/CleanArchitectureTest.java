package org.chesskrk.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

/** Enforces the Dependency Rule: source code dependencies only point inwards. */
@AnalyzeClasses(packages = "org.chesskrk", importOptions = ImportOption.DoNotIncludeTests.class)
class CleanArchitectureTest {

    @ArchTest
    static final ArchRule layers = onionArchitecture()
            .domainModels("org.chesskrk.domain..")
            .domainServices("org.chesskrk.domain..")
            .applicationServices("org.chesskrk.application..")
            .adapter("uci", "org.chesskrk.adapter.uci..")
            .adapter("tablebase", "org.chesskrk.adapter.tablebase..")
            .withOptionalLayers(true)
            .ignoreDependency(
                    com.tngtech.archunit.base.DescribedPredicate.describe("infrastructure",
                            (com.tngtech.archunit.core.domain.JavaClass c) -> c.getPackageName().startsWith("org.chesskrk.infrastructure")),
                    com.tngtech.archunit.base.DescribedPredicate.alwaysTrue());

    @ArchTest
    static final ArchRule domainIsPure = noClasses().that().resideInAPackage("org.chesskrk.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.chesskrk.application..", "org.chesskrk.adapter..", "org.chesskrk.infrastructure..");

    @ArchTest
    static final ArchRule applicationKnowsNoAdapters = noClasses().that().resideInAPackage("org.chesskrk.application..")
            .should().dependOnClassesThat().resideInAnyPackage("org.chesskrk.adapter..", "org.chesskrk.infrastructure..");

    @ArchTest
    static final ArchRule onlyMainWiresThings = noClasses().that().resideOutsideOfPackage("org.chesskrk.infrastructure..")
            .should().dependOnClassesThat().resideInAPackage("org.chesskrk.infrastructure..");
}
