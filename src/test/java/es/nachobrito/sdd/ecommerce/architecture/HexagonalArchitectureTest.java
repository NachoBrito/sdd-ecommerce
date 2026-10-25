package es.nachobrito.sdd.ecommerce.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Architectural compliance tests validating Hexagonal Architecture rules.
 */
class HexagonalArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("es.nachobrito.sdd.ecommerce");
    }

    @Test
    @DisplayName("Domain layer must not depend on Application or Infrastructure layers")
    void domainMustNotDependOnOuterLayers() {
        final ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..application..", "..infrastructure..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Domain layer must be pure Java with zero framework imports")
    void domainMustNotDependOnFrameworks() {
        final ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "io.micronaut..",
                        "jakarta.persistence..",
                        "jakarta.transaction..",
                        "java.sql..",
                        "org.springframework.."
                );

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Application layer must not depend on Infrastructure adapters")
    void applicationMustNotDependOnInfrastructure() {
        final ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..application..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..infrastructure..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Inbound ports must only reside in domain.port.inbound")
    void inboundPortsMustResideInCorrectPackage() {
        final ArchRule rule = classes()
                .that()
                .haveSimpleNameEndingWith("UseCase")
                .should()
                .resideInAPackage("..domain.port.inbound..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Outbound ports must only reside in domain.port.outbound")
    void outboundPortsMustResideInCorrectPackage() {
        final ArchRule rule = classes()
                .that()
                .haveSimpleNameEndingWith("Port")
                .should()
                .resideInAPackage("..domain.port.outbound..");

        rule.check(importedClasses);
    }
}
