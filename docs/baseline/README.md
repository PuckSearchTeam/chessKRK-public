# Baseline: what context-free tools say about the subjects

Run on 2026-10-19 on the naive engine (`Before/`), the clean engine (repository root) and the SDP example
(`Examples/Martin/sdp/before`), as a baseline for the constraints of the paper. Nothing here needs a context; that is the point of the comparison.

## PMD's design rules (a smell detector)

Command, from a throwaway Maven project whose `sourceDirectory` is the subject (`pom.xml` and `design-rules.xml` in this
directory): `mvn org.apache.maven.plugins:maven-pmd-plugin:3.26.0:pmd -Dsubject=<src/main/java> -Drules=design-rules.xml`.
Rules: GodClass, CouplingBetweenObjects, ExcessiveImports, TooManyMethods, TooManyFields, DataClass, CyclomaticComplexity,
NcssCount (PMD 7, the plugin's default version). Reports: `pmd-before.xml`, `pmd-engine.xml`.

| Subject | Design findings (cyclomatic-complexity lines aside) |
|---|---|
| naive engine (`Before/`) | `Engine`: God Class (WMC 105, ATFD 6, TCC 9.4 %), too many methods. `Main`, `Tablebase`: nothing. |
| clean engine | `RetrogradeTablebase`: God Class (WMC 88, ATFD 24, TCC 17.5 %), too many methods. `Position`: too many methods. `StandardPieces`: data class. |

Reading: the detector measures size and internal cohesion. It flags `Engine`, which the context also finds mixed, but
cannot say which of its members belong elsewhere nor where; and it flags `RetrogradeTablebase` in the clean engine by
the same thresholds, a class of one axis that the context accepts. A conformance checker (ArchUnit, DCL) has nothing
to say on the naive engine at all: one package, so no layering to state.

## Martin's instability metric, I = Ce / (Ca + Ce)

- SDP example, violating version, by JDepend 2.9.1 with `java.*` ignored (`jdepend-sdp.txt`):
  `core` Ca 2, Ce 1, I 0.33; `experimental` Ca 1, Ce 0, I 0; `app1`, `app2` I 1. By the metric, `core` depends on a more
  stable package: the design conforms to SDP as Martin measures it. The violation reported in the paper exists only under
  the declared difficulty order (`difficulty experimental < core;`), which says that `experimental` is easier to change
  than its single dependent suggests. This is the paper's case for declaring difficulty rather than measuring it.
- The clean engine, from `jdeps -verbose:package` on `target/classes` with `java.*` and `jdk.*` excluded (JDepend misreads
  Java 21 class files), `jdeps-engine.txt`:

  | Package | Ca | Ce | I |
  |---|---|---|---|
  | domain | 8 | 0 | 0.00 |
  | application.port.out | 4 | 1 | 0.20 |
  | application.port.in | 3 | 1 | 0.25 |
  | adapter.pieces | 1 | 1 | 0.50 |
  | adapter.heuristic | 1 | 2 | 0.67 |
  | adapter.tablebase | 1 | 2 | 0.67 |
  | adapter.uci | 1 | 2 | 0.67 |
  | application.usecase | 1 | 3 | 0.75 |
  | infrastructure | 0 | 8 | 1.00 |

  The metric follows the declared speed order except on the strategies: it puts them (0.75) above the adapter axes
  (0.67 and 0.50), less stable, whereas the order ranks them below the adapters since 2026-10-19, as Clean Architecture ranks
  use cases below adapters. The disagreement is expected: the metric is a snapshot count of dependents, which says
  nothing of how often a search method changes (paper, Sections 4.3 and 5.1).
- The naive engine has one package, so the metric is undefined for it.
