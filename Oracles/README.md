# Oracles — codifications of the architecture written by others

Whether the engine (repository root) and the refactored naive version (`After/`) are Clean Architecture should
not be judged by our own constraint files, nor by the ArchUnit rules we wrote. This module runs two codifications
written by others on the compiled classes of both:

| Oracle | What it checks | Scope | Result |
|---|---|---|---|
| jMolecules classical onion (`JMoleculesArchitectureRules.ensureOnionClassical()`) | domain model, application services, infrastructure; dependencies inward only | every class | passes on both |
| jMolecules hexagonal, strict (`ensureHexagonal()`) | ports and adapters; adapters reach the application through its ports only | every class but the composition root `org.chesskrk.infrastructure` | passes on both |

The rings and roles are declared on the packages of each project, in `package-info.java` files
(`@DomainModelRing`, `@ApplicationServiceRing`, `@InfrastructureRing`; `@PrimaryPort`, `@SecondaryPort`,
`@Application`, `@PrimaryAdapter`, `@SecondaryAdapter`).

The composition root is left out of the hexagonal check because the released jMolecules rules have no role for a
class that assembles adapters, ports and application, and report each of its dependencies as a violation.
ArchUnit's own `onionArchitecture()` recipe, which the engine's `CleanArchitectureTest` uses, has the same gap and
is worked around there with an `ignoreDependency` on that package. The paper's context states that privilege in
one clause, `friend wiring of all;`.

## Run

```sh
scripts/oracles.sh
```

The script compiles both subjects for Java 17 into `Oracles/target/` (the jMolecules rules need ArchUnit 0.23,
which silently ignores class files newer than that) and runs `mvn -q test` here. It is a separate module because
ArchUnit 0.23 and the ArchUnit 1.3 of the engines cannot share a classpath.
