# Declaring the context of ArgoUML from the Cookbook wiki: notes

Inputs: the 31 wiki pages (`wiki/01` to `wiki/31`), `packages.txt` (78 packages), `weland.bnf`,
`example-engine.wld`. Nothing else was read. Page numbers below are the file prefixes in `wiki/`.

## 1. The axes (23) and where each comes from

The wiki's own partition is "one subsystem, one package, sub-parts in sub-packages" (02: "A single
directory/Java package where it resides. Subparts of the subsystem can reside in subdirectories of this
directory"). I used one axis per documented subsystem present in the subject, assigned every sub-package to its
parent's subsystem, and added axes only for packages no page claims.

| Axis | Packages | Source |
|---|---|---|
| `i18n` | org.argouml.i18n | 17, "Located org.argouml.i18n", layer Low-level |
| `taskmgmt` | org.argouml.taskmgmt | 29 gives a purpose and the layer (Low-level) but no package: **by name** |
| `configuration` | org.argouml.configuration | 12, "Located org.argouml.blablabla" (placeholder), layer Low-level: **by name** |
| `swingext` | org.argouml.swingext | 06 lists "Swing extensions" among the low-level subsystems; no page of its own: **by name** |
| `model` | org.argouml.model | 20, "Located org.argouml.model", layer Low-level. Only the interface package is in the subject; the MDR and eUML implementations are not in `packages.txt` |
| `logging` | org.argouml.util.logging | 19 (layer Low-level) names no package; **by name**, see 4.3 |
| `util` | org.argouml.util, org.argouml.util.osdep | no page: **by judgement**, see 4.3 |
| `gefext` | org.argouml.gefext | no page: **by judgement**, see 4.4 |
| `kernel` | org.argouml.kernel | no page; named as "the kernel subsystem" (25), "the kernel package" (26), `org.argouml.kernel.Project` (10), `org.argouml.kernel.Wizard` (13): see 4.1 |
| `umlcommon` | org.argouml.uml, org.argouml.uml.util, org.argouml.uml.util.namespace | no page: **by judgement**, see 4.2 |
| `gui` | org.argouml.ui, org.argouml.ui.cmd, org.argouml.ui.targetmanager | 30, "Located org.argouml.ui", "Plug-in interface Target Manager org.argouml.ui.targetmanager", layer V&C. `ui.cmd`: sub-package rule |
| `explorer` | org.argouml.ui.explorer, org.argouml.ui.explorer.rules | 15, "Located org.argouml.ui.explorer and sub-packages", layer V&C, listed as its own subsystem in 07 |
| `diagrams` | org.argouml.uml.diagram and its 16 sub-packages | 14, "Located org.argouml.uml.diagram", layer V&C |
| `proppanels` | org.argouml.uml.ui and its 8 sub-packages | 27 says "Located org.argouml.uml.?": **by judgement**, see 4.5 |
| `notation` | org.argouml.notation, .providers, .providers.java, .providers.uml, .ui | 22, "Located org.argouml.notation and its sub-folders", layer V&C |
| `codegen` | org.argouml.uml.generator, org.argouml.uml.generator.ui, org.argouml.language.ui | 11, "Located org.argouml.language", but its API classes are `org.argouml.uml.generator.CodeGenerator` and `GeneratorManager`; layer V&C. See 4.6 |
| `reveng` | org.argouml.uml.reveng, org.argouml.uml.reveng.ui | 28, "Package Name org.argouml.uml.reveng"; layer V&C from 07 (28 gives none) |
| `moduleloader` | org.argouml.moduleloader | 21, "Located org.argouml.moduleloader"; layer "?" on 21, View and Control on 07 |
| `persistence` | org.argouml.persistence | 25, "Located org.argouml.persistence", layer V&C (25; the layer page 07 does not list it) |
| `profile` | org.argouml.profile, .init, .internal, .internal.ocl, .internal.ocl.uml14, .internal.ui | 26, "located in the package org.argouml.profile and its sub-packages", layer V&C (26; absent from 07's list) |
| `critics` | org.argouml.cognitive, .ui, .critics, .critics.ui, .checklist, .checklist.ui, org.argouml.uml.cognitive, .critics, .checklist, org.argouml.pattern.cognitive.critics | 13, "Located org.argouml.cognitive", critics "currently located in" org.argouml.cognitive.critics, org.argouml.uml.cognitive.critics, org.argouml.pattern.cognitive.critics; checklists in org.argouml.cognitive.checklist; helpers in org.argouml.cognitive.ui. 31: To Do items are "a set of classes in the Cognitive subsystem, not a subsystem of its own". Layer Loadable. `org.argouml.uml.cognitive` and `.checklist`: **by name** (the UML-specific checklists, by analogy with `uml.cognitive.critics`) |
| `ocl` | org.argouml.ocl | 23, "Located org.argouml.ocl", layer Loadable |
| `application` | org.argouml.application, .api, .events, .helpers, .security | 10, "Located org.argouml.application", layer Top Level. Sub-packages by the sub-package rule; see 4.7 |

Subsystems documented but absent from the subject (no package in `packages.txt`): Help System (16, "will be
located in org.argouml.help", "not yet implemented"), Java code generation and reverse engineering (18,
"org.argouml.language.java in the argouml-java project"), Other source languages (24, separate projects), the
external subsystems (04: gef, swidgets, toolbar, antlr, tudresden.ocl, log4j), and the new property panels
(27, "org.argouml.core.propertypanels.module in Eclipse project argouml-core-umlpropertypanels"). Nothing is
written for them.

## 2. The speed order and the sentences it rests on

One chain, the four layers of chapter 4, slowest first:

```
speed i18n, taskmgmt, configuration, swingext, model, logging, util, gefext
    < kernel, umlcommon, gui, explorer, diagrams, proppanels, notation, codegen, reveng, moduleloader, persistence, profile
    < critics, ocl
    < application;
```

- Low-level slowest: 06, "infrastructure subsystems that just are there for every other subsystem to use - but
  they are not using any other part of ArgoUML ... All other ArgoUML packages may depend on them, but there are
  no dependencies 'upwards'".
- View and Control above them: 07, "These subsystems rely on the information in the model subsystems in order to
  do their work."
- Loadable above View and Control: 08, "These subsystems should be connected only through the interfaces provided
  by other subsystems. This means that they can be individually enabled and disabled using the module loader."
  Something that can be disabled cannot be depended on, so nothing below uses them.
- Application fastest: 09, "It initialises other subsystems, and hence depends on other ArgoUML subsystems, but
  no other ArgoUML subsystem depends on the top level."

Decisions inside the chain: `kernel` and `umlcommon` (no layer page places them) are put in the View-and-Control
group; see 4.1 and 4.2. `logging`, `util`, `gefext` are put in the low-level group; see 4.3 and 4.4.

Within a layer the wiki gives no order; the View-and-Control subsystems use one another in documented ways
(Diagrams uses the GUI and Notation, 14; the Explorer uses `org.argouml.uml.ui.UMLTreeRenderer`, 15; the GUI's
settings tabs use the Profile subsystem, 26; `org.argouml.uml.ui.ActionImportFromSources` starts reverse
engineering, 28), so they stay unordered in the main part. The few orders I consider implied are in the optional
block (section 5).

No `difficulty` order: the only stability statement in the wiki is about Facade classes ("The Facade class should
be very much more stable than the subsystem itself", 03), a class-level matter, not an order between subsystems.

## 3. The written clauses and their sources

Main part:

1. `apart i18n, taskmgmt, configuration, swingext, model, logging;` — 06: low-level subsystems have "no
   dependencies 'upwards', nor between them. This latter statement is the way to recognize a low level subsystem:
   it does not depend on any other argouml package." The six are the ones 06 and 19 document as low-level
   (Help has no package). `util` and `gefext` are low-level by my naming only, so their independence is optional.
2. `hide persistence from kernel;` — 25: "the persistence subsystem depends on the kernel subsystem, and not vice
   versa."
3. `hide model, diagrams, critics from gui;` — 30: "This subsystem has no knowledge of UML, Critics, Diagrams, or
   Model."
4. `hide proppanels, codegen, reveng, umlcommon from gui;` — the "UML" of the same sentence. Decision: since
   "Model" is listed separately, "UML" is read as ArgoUML's UML-specific code, i.e. the `org.argouml.uml.*`
   subsystems other than Diagrams (named on its own). `critics` holds `org.argouml.uml.cognitive.*` and is already
   hidden by clause 3.
5. `set targetmanager = [org.argouml.ui.targetmanager]` and `hide targetmanager from all but-not-from gui,
   explorer;` — 30: "it has been decided that the TargetManager is GUI state, and hence shall be a part of the GUI
   subsystem, and should not be used anywhere outside the GUI subsystem. However, currently the TargetManager is
   used in other subsystems, e.g. the Project. Hence, this needs refactoring." A set, not an axis, because the
   wiki makes it part of the GUI. Decision: the Explorer is exempted too, because 30 lists "The Explorer ...
   Located in org.argouml.ui.explorer" among the GUI's own parts and plug-in interfaces, and 15 keeps the Explorer
   Pane in `org.argouml.ui`. The wiki says this rule is currently broken; the checker will report by whom.
6. `friend tests of all;` — the usual choice.

Statements that are permissions, not rules, and so generate nothing: 13 "This subsystem uses ... Model ... To Do
items"; 14 "uses ... Model, The GUI, Notation"; 11 "All modules must implement org.argouml.moduleloader.
ModuleInterface"; 06 "All other ArgoUML packages may depend on them". They are all allowed by the speed order
(or within one group), so the declaration does not contradict a documented use except where noted in section 6.

## 4. Packages placed by judgement

4.1 `org.argouml.kernel` → axis `kernel`, View-and-Control group. No page documents it, but four pages name
it as a subsystem or package: 25 ("the persistence subsystem depends on the kernel subsystem"), 26
("ProfileConfiguration is part of the kernel subsystem"; applying profiles "belongs to the project management in
the kernel package", and it "doesn't know the user models" is said of Profile, so the kernel does), 10
(`org.argouml.kernel.Project` set up by Main), 13 (`org.argouml.kernel.Wizard`, the base class of wizards), 20
(`Project.moveToTrash` is the way to delete a model element). It is not low-level (it knows the user models, so it
uses Model), it is used by a View-and-Control subsystem (Persistence) and by a Loadable one (Critics' wizards), and
the wiki places it nowhere else; the least committal position is the View-and-Control group, with the one
documented order (`hide persistence from kernel`) written explicitly. Orders towards the other V&C axes are optional.

4.2 `org.argouml.uml`, `org.argouml.uml.util`, `org.argouml.uml.util.namespace` → axis `umlcommon`,
View-and-Control group. No page claims them. `org.argouml.uml` is the parent of five documented subsystems
(diagram, ui, generator, reveng, cognitive) and so is no sub-part of any; by its name it holds what they share.
`uml.util` and `uml.util.namespace` are UML utilities (namespaces are a UML notion) and go with it rather than
with `org.argouml.util`. Placed in the V&C group because it is a `uml.*` package like the subsystems it serves;
the order "common below specific" is optional.

4.3 `org.argouml.util`, `org.argouml.util.osdep` → axis `util`, low-level group; `org.argouml.util.logging`
→ axis `logging`, low-level group. No page claims `org.argouml.util`; by its name it is utility code, i.e.
infrastructure, so low-level. `util.logging` is taken out of it against the sub-package rule because Logging is a
documented subsystem (19, layer Low-level) that gives no package, and this is the one package named logging; the
page says "The Logging uses log4j directly", so the axis may be nearly empty, which is harmless. `util.osdep`
(operating-system dependencies, by name) stays with `util`.

4.4 `org.argouml.gefext` → axis `gefext`, low-level group. No page claims it. By name, extensions of GEF, which
04 lists as an external subsystem ("the package which controls most of the graphical aspects"). The parallel
with "Swing extensions" (`swingext`, a documented low-level subsystem extending another external library) decides
its layer. The alternative, folding it into `diagrams` ("The classes in this subsystem are extensions of the GEF
base classes", 14), was rejected: the Diagrams page locates its subsystem in `org.argouml.uml.diagram` only.

4.5 `org.argouml.uml.ui` and its 8 sub-packages → axis `proppanels`. 27 says "Located org.argouml.uml.?" and
describes the old implementation. Four other pages name `org.argouml.uml.ui` for things of the details pane: the
tabs (10, "Create your TabXXX class in org.argouml.uml.ui"), `TabConstraints` (23), `UMLTreeRenderer` (15),
`ActionImportFromSources` (28). The sub-packages (`behavior.*`, `foundation.*`, `model_management`) follow the
chapters of the UML specification, like the property panels of each kind of element. So the whole of
`org.argouml.uml.ui` is the Property panels subsystem. 10 says "It is not clear in what subsystem Documentation,
Style, Source, and Tagged values belong"; 27 treats the Documentation and Tagged Values tabs as views of its own
kind ("contain fewer fields"), which supports the choice.

4.6 `org.argouml.language.ui` → axis `codegen`. 11 locates Code Generation in `org.argouml.language` and says the
languages themselves are separate projects under it; the only `org.argouml.language.*` package of the subject is
this one, a sub-package, so it is the subsystem's. `org.argouml.uml.generator` and `.ui` are in the same axis
because 11 names `org.argouml.uml.generator.CodeGenerator` and `GeneratorManager` as the subsystem's interface and
registry.

4.7 `org.argouml.application.api`, `.events`, `.helpers`, `.security` → axis `application`, by the sub-package
rule of 02. Risk noted: 09 says nothing depends on the top level, while a package named `api` (named by 21 as the
home of the old `Pluggable` interfaces, "replaced") and one named `events` are the kind other subsystems use. I
kept the documented rule; the checker will show whether the top-level claim holds for them.

4.8 `org.argouml.ui.cmd` → `gui` (sub-package of `org.argouml.ui`, no page names it). `org.argouml.uml.cognitive`,
`org.argouml.uml.cognitive.checklist` → `critics` (by name, next to the documented `uml.cognitive.critics`).
`org.argouml.notation.providers.java` → `notation` (sub-folder rule of 22), although 18 says Java support lives in
a separate project. `org.argouml.profile.internal.ocl`, `.ocl.uml14` → `profile` (sub-packages of
`org.argouml.profile`, 26), not `ocl`.

## 5. The optional block (implied, not stated)

- `speed kernel < gui, explorer, diagrams, proppanels, notation, codegen, reveng;` — MVC reading of 10 (the
  ProjectBrowser sets the project), 26 (the settings tabs in `org.argouml.ui` edit `kernel.ProfileConfiguration`),
  30 (the Project's use of the TargetManager "needs refactoring"): views and tools know the project, not the
  reverse. `profile` is left out because 26 says the kernel's ProfileConfiguration holds profiles;
  `moduleloader` and `persistence` because nothing is said (persistence is already handled).
- `speed umlcommon < diagrams, proppanels, codegen, reveng;` — by the names only: common code below specific.
- `speed notation < diagrams;` — 22: "a change has been performed to resolve dependency cycles" and the
  `NotationRenderer` callback interface lets a notation reach its Fig without knowing Diagrams; 14: Diagrams uses
  Notation.
- `hide notation, profile from gui;` — 30: the dependency "between org.argouml.ui and the org.argouml.some-package
  is less desired" for subsystems split into `some-package` and `some-package.ui` (the figure of allowed
  dependencies is missing from the page). Notation and Profile are the two such subsystems not already hidden
  from `gui` by the main clauses.
- `hide reveng from all but-not-from proppanels;` — 28: "no other subsystem needs to access it ... The only entry
  point is the new Import(...) statement in the org.argouml.uml.ui.ActionImportFromSources class."
- `apart util, gefext, i18n, taskmgmt, configuration, swingext, model, logging;` — the independence of the
  low-level layer extended to the two axes I placed there by name.

## 6. Where the wiki is ambiguous or silent, and what I decided

- **Layer of the Module loader**: "?" on 21, View and Control on 07. Followed 07. (Modules implement its
  `ModuleInterface`, 11, so it must be below Loadable, which 07's placement gives.)
- **Persistence and Profile absent from the layer page 07** but given the layer V&C on their own pages (25, 26).
  Followed the subsystem pages.
- **Logging listed both as external (04) and as low-level (19)**: it is log4j plus, in the subject, one package
  named logging; placed low-level (4.3).
- **Explorer: subsystem or part of the GUI?** 07 and 15 make it a subsystem; 30 lists it among the GUI's API and
  plug-in interfaces. Own axis (it has its own page, package and reason to change); exempted from the
  TargetManager rule along with the GUI (section 3, clause 5).
- **"UML" in "no knowledge of UML, Critics, Diagrams, or Model" (30)**: read as the `org.argouml.uml.*` code
  (section 3, clause 4). The narrower reading (UML = the Model) would make the word redundant.
- **The missing figure of allowed dependencies on 30**: only the sentence about the "less desired" direction
  remains; used in the optional block.
- **Documented uses that the documented layering forbids** (the checker will report them; I kept the layering,
  which is the design rule, over the descriptions of the current state): `org.argouml.ui.ProjectBrowser`
  initialises the `org.argouml.cognitive.ui.ToDoPane` (10) although the GUI "has no knowledge of ... Critics" (30);
  `org.argouml.uml.ui.TabConstraints` uses `org.argouml.ocl.ArgoFacade` (23), a V&C package using a Loadable one;
  the Profile subsystem (V&C) defines critics (`CrOCL`, "Java-Critics", 26), which needs the Loadable `critics`;
  the Project uses the TargetManager (30, flagged by the page itself).
- **Property panels**: the page says the main view has a new implementation outside the subject; the old one is
  what `org.argouml.uml.ui` holds (4.5).
- **Diagrams**: "There is an intention (Bob Tarling) to split this subsystem into several smaller subsystems, one
  for each diagram type" (14); an intention only, so one axis of 17 packages.
- **Code Generation located in `org.argouml.language` but implemented in `org.argouml.uml.generator`** (11): one
  axis holding both (4.6).
- **The Model subsystem's implementations** (MDR, eUML) are not in the subject; nothing to say about 20's rule
  "Everything within ArgoUML should access the Model subsystem through the interfaces in the org.argouml.model
  package": the only Model package present is that interface package.
- **To Do items (31)**: not a subsystem; inside `critics`.
- **Difficulty order**: none, no reason given (section 2).

## 7. Time

Reading: the 31 pages (about 130 KB, in nine batches), the grammar, the example and the package list, about 15
minutes. Deciding and writing the two files, with a coverage check (78 of 78 packages, each once), about 30
minutes. The whole, roughly three quarters of an hour (the folder was created at 10:38; the files were written
from 10:45 on).
