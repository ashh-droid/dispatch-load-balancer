package com.freightfox.dispatch.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.freightfox.dispatch")
class LayerArchitectureTest {
    @ArchTest
    static final ArchRule controllersDoNotAccessPersistence = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule algorithmsDoNotKnowHttpOrPersistence = noClasses()
            .that().resideInAnyPackage("..strategy..", "..distance..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..controller..", "..dto..", "..repository..", "org.springframework.web..");

    @ArchTest
    static final ArchRule otherLayersDoNotDependOnControllers = noClasses()
            .that().resideOutsideOfPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..controller..");
}
