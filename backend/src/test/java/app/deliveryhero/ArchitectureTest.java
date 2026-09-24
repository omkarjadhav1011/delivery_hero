package app.deliveryhero;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.data.repository.Repository;

/**
 * The package dependency rules of LLD section 5.1 (DEC-149). Empty rules are allowed while the packages have no
 * classes yet; ArchUnit would otherwise fail a rule that matches nothing.
 */
@AnalyzeClasses(packages = "app.deliveryhero", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule engineDoesNotDependOnApiOrRepositories = noClasses()
            .that()
            .resideInAPackage("app.deliveryhero.engine..")
            .should()
            .dependOnClassesThat(resideInAPackage("app.deliveryhero.api..")
                    .or(resideInAPackage("app.deliveryhero.content..").and(assignableTo(Repository.class)))
                    .or(resideInAPackage("app.deliveryhero.lifecycle..").and(assignableTo(Repository.class))))
            .allowEmptyShould(true)
            .because("the engine stays pure and fast (LLD section 5.1)");

    @ArchTest
    static final ArchRule scoringDependsOnlyOnCommon = classes()
            .that()
            .resideInAPackage("app.deliveryhero.scoring..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage("app.deliveryhero.scoring..", "app.deliveryhero.common..", "java..", "org.jspecify..")
            .allowEmptyShould(true)
            .because("scoring is testable in isolation (LLD section 5.1)");

    @ArchTest
    static final ArchRule nothingDependsOnApi = noClasses()
            .that()
            .resideOutsideOfPackage("app.deliveryhero.api..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("app.deliveryhero.api..")
            .allowEmptyShould(true)
            .because("controllers are the outer layer (LLD section 5.1)");

    // TODO(EN-05): the remaining rules of document 13, section 6.4 (no I/O in engine, time and randomness only in
    // config, no @Autowired fields, no entities from controllers, no engine types in entities)
}
