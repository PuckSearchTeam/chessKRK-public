# ArgoUML as a subject (2026-10-19)

Subject: ArgoUML (`argouml-tigris-org/argouml`, head `be952fc`), the application module `src/argouml-app` and the
Model subsystem `src/argouml-core-model`: 1,322 classes in 78 packages (233 kLOC), 180 test classes. Assembled in a
scratch directory as `src/main/java` (the two modules' `src/org` trees merged) and `src/test/java`
(`argouml-app/tests/org`), then `scripts/puck-both.sh <dir>`. Puck's graph has 18,081 nodes; a run (both checks) takes
about 90 seconds once the complete `.dot` exports are skipped, which Puck now does above 3,000 nodes
(`-Dpuck.fullDot=true` forces them).

## The documented architecture

The developers' Cookbook, migrated into the project wiki in 2008-2009
(https://argouml-tigris-org.github.io/tigris/wiki-argouml/wiki/Cookbook.html, chapters 4 "ArgoUML Design, The Big
Picture" and 5 "Inside the subsystems"): subsystems, each in a package, in four layers with a rule each:

- low-level (Internationalization, TaskManagement, Configuration, Swing extensions, the Model): "infrastructure
  subsystems that just are there for every other subsystem to use - but they are not using any other part of ArgoUML";
- view and control (Diagrams, GUI, Property panels, Explorer, Notation, Code Generation, Reverse Engineering, Module
  loader, Persistence, Profile): "rely on the information in the model subsystems";
- loadable (Critics and checklists, OCL, the languages): "connected only through the interfaces provided by other
  subsystems";
- top level (Application): "depends on other ArgoUML subsystems, but no other ArgoUML subsystem depends on the top level".

Two further statements: "Everything within ArgoUML should access the Model subsystem through the interfaces in the
org.argouml.model package"; the GUI "has no knowledge of UML, Critics, Diagrams, or Model". The wiki also admits that
the layering "is more of a base for discussion and ambition" and that ArgoUML "is traditionally built as a whole and no
subsystems were clearly defined".

## The contexts and the results

| File | What it declares | Without the optional block | With it |
|---|---|---|---|
| `decouple-draft.wld` | an order guessed before reading the wiki (application module only) | 203 | (no block) |
| `decouple.wld` (iteration 1) | the wiki's layering taken literally: 16 axes, the four layers as the speed order, the GUI statement as a clause, the view-and-control subsystems apart as the option | 2,890 | 5,529 |
| `decouple-iteration2.wld` | the same, with three mapping decisions the wiki leaves open: the Explorer a subsystem of its own (the wiki says so), `org.argouml.application.{api,events,helpers}` infrastructure rather than top level (they are the plug-in interfaces every subsystem uses), the kernel (no wiki page) with the view-and-control subsystems | 894 | 5,089 |

Logs: `puck-iteration1-*.log`, `puck-iteration2-*.log`. No axis mismatch in any run: the tags are packages. `packages.txt`
lists the 78 packages, `axes.map` the prefix-to-subsystem assignment of iteration 1 and `axis-lines-generated.txt` its
result (a package name does not include its subpackages in Weland, so every package is listed).

What the 894 of iteration 2 are, by clause:

- 525: the GUI (`org.argouml.ui`, `ui.cmd`, `ui.targetmanager`) uses the model (205), the property panels (132), the
  diagrams (131), the critics (24) and `org.argouml.uml` (17): the documented statement describes an intention, not the
  code;
- 325: the critics, a loadable subsystem, named by `profile.internal` (116), `uml.diagram` (51), `profile` (48),
  `persistence` (43), `ui.cmd` (28), `ui` (12) through `org.argouml.uml.cognitive` (105), `Critic` (55), `ToDoItem` (49),
  `Designer` (48): no interface stands between the loadable layer and the rest;
- 27: the infrastructure uses the model, 20 from `application.helpers` (moved to the infrastructure by iteration 2) and
  7 from `util`;
- 16: OCL used directly by `uml.ui` (12) and `persistence` (4); 1: the application used by `uml.ui`.

With the optional block (the view-and-control subsystems and the kernel kept apart, which the wiki does not state),
5,093 (5,089 on the run of 2026-10-19; the four edges that differ come from anonymous classes, whose owner the graph
builder resolves differently between runs, `null.run()`, `null.targetAdded(TargetEvent)`): the GUI used by the others 1,295 times, the kernel 1,157, the diagrams 554, the notation 402, the panels 288: the
view-and-control layer uses itself freely, the kernel (`Project`) and the GUI being its hubs.

Iteration 1's 2,890 differ from iteration 2's 894 by: 739 uses of the model by the explorer (the GUI clause no longer
covers it), 965 uses of `application.{api,events,helpers}` by everyone (now infrastructure), about 250 uses by the
kernel of the diagrams (111), the notation (74), the profiles (51), `uml`, persistence and the GUI (now unordered with them).

## Version 0.24 under the published architecture (`0.24/`)

Brunet, Bittencourt, Serey and Figueiredo (WCRE 2012) wrote an intended architecture and a mapping of ArgoUML 0.24
for a reflexion-model study over nineteen versions; it is distributed with the s4rdm3x tool suite (Olsson, Ericsson
and Wingkvist, JOSS 2021) as `data/systems/argouml/argouml.sysmdl`, copied here: 19 modules, a mapping by regular
expression on package names, and the relations each module is allowed to have. Subject: tag `VERSION_0_24`,
`src_new` and `src/model/src` (1,408 classes, 76 packages), tests from `tests`. `sysmdl-to-weland.pl` writes two
contexts from the file and the package list; a third adds `apart` within each layer.

| Encoding | What it states | Violated uses |
|---|---|---|
| `exact.wld` | the published architecture as a reflexion model: one clause per module, listing every module it may not use | 1,547 |
| `ordered.wld` | the layering the allowed relations imply (longest chain of allowed uses), as a speed chain, nothing else | 624 |
| `ordered-apart.wld` | the same chain, the modules of each layer kept apart | 1,184 |

The 363 uses only the exact encoding reports are decisions neither an order nor acyclicity expresses; `pairs-exact.txt`
has the counts per module pair: `gui -> model` 174 (the wiki's statement) and `critics -> gui` 104 account for 278 of
them. Twenty-five packages are outside the published mapping (the kernel, `org.argouml.uml` and its property panels,
the utilities, the critics of `org.argouml.uml.cognitive`); four modules of the architecture have no package in 0.24
(profile, help, taskManagement, configuration). Runs take about 35 seconds each.

## The published architecture measured on module pairs (2026-10-20)

The proportions above (624, 1,184 and 1,547) are of one version's violations. `pairs-count.pl argouml.sysmdl packages.txt`
measures the architecture itself: of the 210 ordered pairs of the 15 modules present in 0.24, 49 are allowed and 161
forbidden; the layering alone states 82 of the forbidden pairs (51 %), an `apart` decision within each layer 46 (29 %),
and 33 (20 %) can only be written, a faster module forbidden to use a slower one (`gui -> model`, `critics -> gui`,
`ocl -> notation`, `javaCodeGeneration -> diagrams`, …).

## Repair labels (2026-10-20)

Every violation line ends with the repair of the paper's Table 3. A violation of a written clause, such as the GUI
clause here (525 lines in `puck-iteration2-without-optional.log`) or every clause of `0.24/exact.wld`, is labelled
`[written clause, a decision of the architect (ADP when it directs an unordered pair): the same inversion, or revise the
decision]`; until 2026-10-20 the tool labelled them `ADP`, which was wrong for clauses that direct ordered pairs. The
logs were regenerated; the counts are unchanged but for the four edges noted above.

## The history (2026-10-20)

`history/`: the change frequency of each axis over the 10,810 commits that touch the main code, against the declared
order, and the co-change of the classes on the violating edges against permitted dependencies; see `history/README.md`.
