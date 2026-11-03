# The Composite pattern: where child management is declared

The second reading of the Interface Segregation Principle in the paper (Section 3.4): the breadth of an abstraction
over a hierarchy. Gamma et al. (1994) discuss, under Composite, "Implementation", where to declare the
child-management operations: in `Component`, so that every client treats every component alike (transparency; their
sample code), or in `Composite` only, so that the type system refuses children on a leaf (safety; Martin's variation
declares the common operations only). Johnson (Wirfs-Brock 2006) says the book "just got that wrong", that C++
frameworks declared `add()` in every component only "as long as C++ didn't have a safe way to downcast".

Two variants, each a small Java program under `src/main/java`, differing in nothing but where `add`, `remove` and
`children` are declared; `render()` is a method of every kind in both, and the clients are an `Editor`, which adds a
leaf to a component it is given, and a `Renderer`. Three axes: the kinds (`composite.kinds`), the editing clients
(`composite.editing`), the rendering clients (`composite.rendering`), the kinds the slower axis. Each variant is checked
under two contexts, the same files for both variants but for the tags:

| Context | What it declares | `transparent` | `safe` |
|---|---|---|---|
| `decouple.wld` ("every") | child management is every component's business; an editor knows only the abstraction: `hide composite.kinds.Composite from editing;` | 0 | 2 uses: `Editor` names `Composite` to add a child |
| `decouple-composite.wld` ("composite") | child management is the composite's business: the `add`, `remove` and `children` of `Component` and `Leaf` serve the editing clients only and are tagged with that axis (nothing to tag in `safe`) | 6 containments: the tagged members, foreign to `Component` and `Leaf` | 0 |

Reading: each variant conforms under the context that declares it and violates under the other. The breadth of the
abstraction is a decision on the partition, which the context records and the clauses enforce; the price each side
pays, a leaf that refuses children at run time against a client that must test the kind, is what no clause sees.
Johnson's story is the two contexts in sequence: the first while C++ had no safe downcast, the second after.

Run: `scripts/puck-both.sh Examples/Composite/transparent Examples/Composite/safe` from the repository root uses
`decouple.wld`; for the other context, copy `decouple-composite.wld` over `decouple.wld` and run again. Kept logs and
context files: `docs/puck-results/composite/<variant>-<context>.{wld,log}`. Every version compiles with `javac` alone.
