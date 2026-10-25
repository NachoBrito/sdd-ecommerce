# Static Code Analysis & Architecture Verification

This document provides a comprehensive reference of all static code analysis tools, architectural compliance checks, and quality gates configured in the **SDD E-Commerce** platform.

---

## 1. Quality Philosophy & Overview

The project enforces a **zero-tolerance quality gate** across all codebases. Any violation, style discrepancy, potential bug, architectural leak, or unhandled compiler warning immediately fails the build:

- **Compiler warnings are treated as errors** (`-Werror`).
- **Linters fail on any violation** (`maxWarnings = 0`, `ignoreFailures = false`).
- **Static analysis runs on maximum sensitivity** (SpotBugs `effort = MAX`, `reportLevel = LOW`).
- **Architectural boundaries are strictly enforced** via ArchUnit tests in the JUnit 5 test suite.
- **Minimum 80% test coverage is enforced** via JaCoCo instruction and line coverage verification.
- **Git commits must adhere to Conventional Commits** via an automated git hook.

### Quality Tools Summary

| Tool | Version / Plugin | Purpose | Configuration File(s) | Gradle Task | HTML Report Location |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Java Compiler (`javac`)** | Java 25 (Toolchain) | Strict compilation, parameter reflection, lint warnings as errors | `build.gradle` (`JavaCompile`) | `compileJava`, `compileTestJava` | Console output |
| **Checkstyle** | 10.21.4 | Code style, formatting, import ordering, naming hygiene | `config/checkstyle/checkstyle.xml` | `checkstyleMain`, `checkstyleTest` | `build/reports/checkstyle/` |
| **PMD** | 7.28.0 | AST analysis for bad practices, complex design, and code smells | `config/pmd/pmd.xml` | `pmdMain`, `pmdTest` | `build/reports/pmd/` |
| **SpotBugs** | 6.1.1 (Tool 4.10.5) | Bytecode analysis for security vulnerabilities and bug patterns | `config/spotbugs/exclude.xml` | `spotbugsMain`, `spotbugsTest` | `build/reports/spotbugs/` |
| **JaCoCo** | 0.8.13 | Code coverage measurement and mandatory ≥80% threshold verification | `build.gradle` (`jacoco`) | `jacocoTestReport`, `jacocoTestCoverageVerification` | `build/reports/jacoco/test/html/` |
| **ArchUnit** | 1.4.0 | Hexagonal architecture boundary and dependency enforcement | `src/test/resources/archunit.properties`, `HexagonalArchitectureTest.java` | `test` | `build/reports/tests/test/` |
| **Git Hook** | Custom bash script | Enforces Conventional Commits specification on `git commit` | `.git/hooks/commit-msg` | `installGitHooks` | Console output |

---

## 2. Java Compiler Warnings (`javac`)

The Java compiler is configured to enforce clean compilation under modern Java 25 standards.

### Configuration (`build.gradle`)

```groovy
tasks.withType(JavaCompile).configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll([
        "-parameters",        // Preserves method parameter reflection metadata (used by Micronaut)
        "-Xlint:all",          // Enables all standard Java compiler lint warnings
        "-Xlint:-processing",  // Disables verbose annotation-processor messages
        "-Xlint:-classfile",   // Suppresses classfile version mismatch warnings from dependencies
        "-Werror"              // Fails compilation if any warning is triggered
    ])
}
```

### Key Enforcements
- All unused variables, deprecated API calls, missing serial version UIDs, or unchecked casts must either be resolved or cleanly suppressed with targeted `@SuppressWarnings`.
- Any dangling compiler warning will cause the build to fail immediately.

---

## 3. Checkstyle

Checkstyle verifies source code formatting, spacing, braces, naming conventions, and import hygiene.

### Configuration (`build.gradle`)

```groovy
checkstyle {
    toolVersion = "10.21.4"
    configFile = file("config/checkstyle/checkstyle.xml")
    maxWarnings = 0
    ignoreFailures = false
}
```

### Rule Highlights (`config/checkstyle/checkstyle.xml`)

1. **File-Level & Exclusions**:
   - Encoding: `UTF-8`.
   - Maximum line length: **140 characters** (`LineLength`), ignoring package/import statements and URLs.
   - Tab characters: strictly disallowed (`FileTabCharacter`).
   - File endings: files must end with a newline (`NewlineAtEndOfFile`).
   - Exclusions: `module-info.java`, files in `resources/`, and generated code (`generated/`).

2. **Import Ordering & Hygiene**:
   - Wildcard imports (`import es.nachobrito.*`) are forbidden (`AvoidStarImport`).
   - Enforces `CustomImportOrder` with the following strict grouping, separated by single blank lines and alphabetically sorted within each group:
     1. Static imports (`STATIC`)
     2. Third-party libraries (`THIRD_PARTY_PACKAGE`)
     3. Standard Java libraries (`STANDARD_JAVA_PACKAGE`)

3. **Braces & Formatting**:
   - Mandatory curly braces for `if`, `else`, `for`, `while`, and `do` blocks (`NeedBraces`).
   - `LeftCurly` on the same line (K&R style) for classes, records, methods, and blocks.
   - `RightCurly` on the same line for `catch`, `finally`, `else` (`RightCurlySame`), but on a new line for standalone closures (`RightCurlyAlone`).
   - Indentation: **4 spaces** for basic blocks, case labels, throws, and line wrapping (`Indentation`).

4. **Naming Conventions**:
   - Packages: lowercase words separated by dots (`^[a-z]+(\.[a-z][a-z0-9]*)*$`).
   - Types (Classes, Interfaces, Enums, Records, Annotations): UpperCamelCase (`TypeName`).
   - Variables & Parameters: lowerCamelCase (`MemberName`, `ParameterName`, `LocalVariableName`).
   - Generic Type Parameters: single uppercase letter or UpperCamelCase ending in `T` (`^[A-Z][0-9]?$|^[A-Z][a-zA-Z0-9]*[T]$`).

5. **Code Hygiene**:
   - One statement per line (`OneStatementPerLine`).
   - No multiple variable declarations on a single line (`MultipleVariableDeclarations`).
   - Switches require a default case (`MissingSwitchDefault`) and explicit fall-through handling (`FallThrough`).
   - Catch blocks cannot be empty unless the exception variable is explicitly named `expected` or `ignore` (`EmptyCatchBlock`).

---

## 4. PMD (Programming Mistake Detector)

PMD performs Abstract Syntax Tree (AST) scanning of the Java source code to identify code smells, potential bugs, suboptimal design, and performance traps.

### Configuration (`build.gradle`)

```groovy
pmd {
    toolVersion = "7.28.0"
    consoleOutput = true
    ruleSetConfig = resources.text.fromFile("config/pmd/pmd.xml")
    ruleSets = []
    ignoreFailures = false
}
```

### Rule Categories & Customizations (`config/pmd/pmd.xml`)

The project uses PMD 7 with the strict industry-standard ruleset tailored for modern Java and Micronaut:

- **Best Practices (`category/java/bestpractices.xml`)**:
  - Enforces proper use of assertions, avoidance of unused private fields/methods, and defensive programming.
  - *Excluded*: `GuardLogStatement` (modern SLF4J parameterized logging handles lazy formatting without manual guards) and `LooseCoupling` (records and specific collections).
- **Code Style (`category/java/codestyle.xml`)**:
  - Enforces proper modifier ordering, avoidance of redundant keywords, and clean variable definitions.
  - *Excluded*: `AtLeastOneConstructor` (redundant with Java records and static factory methods), `CommentDefaultAccessModifier` (package-private constructors and test methods), `OnlyOneReturn` (early returns/guard clauses preferred), `ShortVariable`, `LongVariable`, `TooManyStaticImports`, and `LinguisticNaming`.
- **Design (`category/java/design.xml`)**:
  - Enforces high cohesion, avoidance of deep nesting, cyclomatic complexity thresholds, and absence of god classes.
  - *Excluded*: `LawOfDemeter` (fluent builders, streams, and DTO mappings) and `LoosePackageCoupling`.
- **Error Prone (`category/java/errorprone.xml`)**:
  - Flags potential `NullPointerException`s, broken equals/hashCode contracts, missing break statements, and improper comparisons.
  - *Excluded*: `NullAssignment` (common in defensive copy fallbacks).
- **Multithreading (`category/java/multithreading.xml`)**:
  - Guards against thread-safety issues, improper synchronization, and non-thread-safe singletons.
  - *Excluded*: `UseConcurrentHashMap`.
- **Performance (`category/java/performance.xml`)**:
  - Guards against string concatenations inside loops, inefficient collections, and wasteful memory allocations.
- **Security (`category/java/security.xml`)**:
  - Flags insecure hardcoded values, weak cryptography, or potential injection vectors.

### PMD Suppressions
When domain or infrastructure requirements necessitate bypassing a PMD check (e.g., flattened persistence entities having numerous fields), suppressions must be declared explicitly with targeted scopes:
```java
@SuppressWarnings({"PMD.ExcessivePublicCount", "PMD.TooManyFields"})
public class OrderEntity { ... }
```

---

## 5. SpotBugs

SpotBugs analyzes compiled Java bytecode (`.class` files) to find potential bugs, concurrency flaws, security vulnerabilities, and bad practices that source-level linters cannot see.

### Configuration (`build.gradle`)

```groovy
spotbugs {
    toolVersion = "4.10.5"
    effort.set(com.github.spotbugs.snom.Effort.valueOf("MAX"))
    reportLevel.set(com.github.spotbugs.snom.Confidence.valueOf("LOW"))
    ignoreFailures = false
    excludeFilter.set(file("config/spotbugs/exclude.xml"))
}

tasks.withType(com.github.spotbugs.snom.SpotBugsTask).configureEach {
    reports {
        html.required = true
        xml.required = true
    }
}
```

- **Effort**: `MAX` executes all analyses, including expensive inter-procedural dataflow.
- **Report Level**: `LOW` flags issues at the highest sensitivity (reporting High, Medium, and Low confidence warnings).

### Exclusions (`config/spotbugs/exclude.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<FindBugsFilter>
    <!-- Exclude generated bytecode from Micronaut annotation processors -->
    <Match>
        <Package name="~.*\.generated.*"/>
    </Match>
    <Match>
        <Class name="~.*\$.*"/>
    </Match>
    <Match>
        <Class name="~.*\$Definition.*"/>
    </Match>
    <!-- Micronaut AOP proxies require non-final classes which triggers false-positive CT_CONSTRUCTOR_THROW -->
    <Match>
        <Bug pattern="CT_CONSTRUCTOR_THROW"/>
    </Match>
</FindBugsFilter>
```

### Common SpotBugs Patterns in this Project
- **`EI_EXPOSE_REP` / `EI_EXPOSE_REP2`**: Triggered when mutable objects or lists are accepted into or returned from records. Handled by defensive copying in compact constructors and overriding accessor methods:
  ```java
  public record OrderResponse(..., List<OrderLineDto> lines) {
      public OrderResponse {
          lines = lines == null ? List.of() : List.copyOf(lines);
      }
      @Override
      public List<OrderLineDto> lines() {
          return List.copyOf(lines);
      }
  }
  ```

---

## 6. ArchUnit (Architectural & Hexagonal Compliance)

ArchUnit validates architectural structure, layer isolation, package hygiene, and dependency direction as automated unit tests on every build.

### Global Configuration (`src/test/resources/archunit.properties`)

```properties
# ArchUnit configuration
archRule.failOnEmptyShould=false
```

### Test Suite (`src/test/java/es/nachobrito/sdd/ecommerce/architecture/HexagonalArchitectureTest.java`)

ArchUnit analyzes all production bytecode in package `es.nachobrito.sdd.ecommerce`, excluding test classes (`ImportOption.Predefined.DO_NOT_INCLUDE_TESTS`).

```
                    ┌───────────────────────────────┐
                    │        Infrastructure         │
                    │   (REST, JDBC, Messaging)     │
                    └───────────────┬───────────────┘
                                    │ depends on
                                    ▼
                    ┌───────────────────────────────┐
                    │          Application          │
                    │     (Services, AOP, UseCases) │
                    └───────────────┬───────────────┘
                                    │ depends on
                                    ▼
                    ┌───────────────────────────────┐
                    │            Domain             │
                    │   (Pure Java, Models, Ports)  │
                    └───────────────────────────────┘
```

The test suite enforces 5 non-negotiable architectural rules:

### Rule 1: Domain Layer Independence from Outer Layers
Domain classes must never import or depend on Application services or Infrastructure adapters.
```java
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
```

### Rule 2: Pure Domain (Zero Framework Dependencies)
The domain layer must remain pure Java with absolutely zero imports from frameworks, ORMs, or drivers.
```java
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
```

### Rule 3: Application Layer Independence from Infrastructure
Application use cases orchestrate domain logic through outbound ports; they cannot directly reference concrete infrastructure adapters.
```java
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
```

### Rule 4: Inbound Port Placement
Any driving use case interface ending in `UseCase` must reside strictly in `domain.port.inbound`.
```java
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
```

### Rule 5: Outbound Port Placement
Any driven port interface ending in `Port` must reside strictly in `domain.port.outbound`.
```java
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
```

---

---

## 7. JaCoCo Test Coverage Verification

The project enforces automated test coverage verification via the **JaCoCo** Gradle plugin. Any pull request or commit that causes total instruction or line coverage to fall below **80%** will fail the build (`./gradlew check` / `make check`).

### Configuration (`build.gradle`)

```groovy
jacoco {
    toolVersion = "0.8.13"
}

def jacocoExcludes = [
    "es/nachobrito/sdd/ecommerce/Application.*",
    "**/generated/**",
    "**/*\$*"
]

tasks.named("jacocoTestReport", JacocoReport) {
    dependsOn(tasks.named("test"))
    reports {
        xml.required = true
        html.required = true
    }
    classDirectories.setFrom(
        sourceSets.main.output.classesDirs.collect {
            fileTree(dir: it, exclude: jacocoExcludes)
        }
    )
}

tasks.named("jacocoTestCoverageVerification", JacocoCoverageVerification) {
    dependsOn(tasks.named("test"))
    classDirectories.setFrom(
        sourceSets.main.output.classesDirs.collect {
            fileTree(dir: it, exclude: jacocoExcludes)
        }
    )
    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = 0.80
            }
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = 0.80
            }
        }
    }
}

// Wire coverage verification into the standard check lifecycle
tasks.named("check") {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}
```

### Coverage Rules & Invariants
- **Minimum 80% Instruction Coverage**: At the aggregate bundle level, covered bytecode instructions must equal or exceed 80%.
- **Minimum 80% Line Coverage**: At the aggregate bundle level, executable source lines covered must equal or exceed 80%.
- **Exclusions**:
  - `Application.java`: Main framework bootstrap entrypoint (`Micronaut.run(...)`).
  - `**/generated/**`: Bytecode generated by Micronaut sourcegen or annotation processors.
  - `**/*$*`: Synthetic and inner classes generated by compilers or processors.

---

## 8. Git Commit Hook (Conventional Commits)

The repository enforces standardized commit messages conforming to the [Conventional Commits](https://www.conventionalcommits.org/) specification via a Git hook.

### Hook Definition (`build.gradle`)

The `installGitHooks` Gradle task installs `.git/hooks/commit-msg`:

```bash
#!/usr/bin/env bash
commit_msg=$(cat "$1")
pattern="^(feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert)(\([a-zA-Z0-9_-]+\))?: .+"
if ! echo "$commit_msg" | grep -qE "$pattern"; then
    echo "ERROR: Invalid commit message format."
    echo "Commit message MUST follow Conventional Commits:"
    echo "  <type>(<optional scope>): <subject>"
    echo "Allowed types: feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert"
    echo "Example: feat(catalog): add product creation endpoint"
    exit 1
fi
```

To install or refresh the hook locally:
```bash
./gradlew installGitHooks
```

---

## 9. Running Quality Checks Locally

All static analysis tools, linters, coverage verification, and architectural compliance checks are tied directly into Gradle's standard `check` task.

### Running All Quality Gates

```bash
# Using the Makefile
make check

# Or directly with Gradle
./gradlew check
```

This single command triggers:
1. `checkstyleMain` & `checkstyleTest`
2. `pmdMain` & `pmdTest`
3. `spotbugsMain` & `spotbugsTest`
4. `test` (including all JUnit 5 unit, integration, and ArchUnit compliance tests)
5. `jacocoTestCoverageVerification` (enforcing ≥80% instruction and line coverage)
6. `jacocoTestReport` (generating HTML and XML coverage reports)

### Running Individual Tools

```bash
# Run Checkstyle only
./gradlew checkstyleMain checkstyleTest

# Run PMD only
./gradlew pmdMain pmdTest

# Run SpotBugs only
./gradlew spotbugsMain spotbugsTest

# Run ArchUnit architecture tests only
./gradlew test --tests "es.nachobrito.sdd.ecommerce.architecture.*"

# Run JaCoCo coverage verification only
./gradlew jacocoTestCoverageVerification

# Generate JaCoCo coverage report only
./gradlew jacocoTestReport
```

---

## 10. Inspecting Reports

When a quality check fails or when reviewing detailed telemetry, HTML reports are available in the `build/reports` directory:

- **Checkstyle Report**: `build/reports/checkstyle/main.html`
- **PMD Report**: `build/reports/pmd/main.html`
- **SpotBugs Report**: `build/reports/spotbugs/main.html`
- **JaCoCo Coverage Report**: `build/reports/jacoco/test/html/index.html`
- **Test & ArchUnit Report**: `build/reports/tests/test/index.html`

