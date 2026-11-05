# A second, independent declaration of ArgoUML's context (2026-11-05)

How much do two declarers agree when each writes ArgoUML's context from the same documentation? This directory
holds a second declaration made blind, the run of Puck on it, and the comparison with the authors' declarations
(`../decouple.wld`, the first one, and `../decouple-iteration2.wld`, the one the paper reports). The second
declarer was a fresh session of the assistant (Claude), not a human: the figures below are assistant-to-assistant
agreement, a dry run of the protocol and of the comparison, not the human replication a reader may ask for.

## Protocol

- Inputs, and nothing else: the 31 pages of the developers' Cookbook in the project wiki (chapter 4, the design and
  the four layers; chapter 5, one page per subsystem), fetched as text on 2026-11-05 by `fetchwiki.py` (the page
  list is in the script); `../packages.txt`, the 78 packages of the subject; the Weland grammar
  (`Puck/src/main/java/fr/lip6/move/puck/parse/weland.bnf`); the engine's `decouple.wld` as a syntax example. No
  code, no access to the repository or the paper, no other web page.
- The brief, verbatim: `brief.md`. It explains what a context is (axes, speed order, written clauses, the
  optional block for implied decisions, `friend tests of all`) and asks for a constraint file and notes.
- The declarer read, decided and wrote in about three quarters of an hour (`notes.md`, section 7), with no
  revision after seeing results: its file is a first declaration, like `../decouple.wld`, where
  `../decouple-iteration2.wld` is the authors' after one revision informed by the first run.
- Puck ran on the subject of the paper (head `be952fc`, the application module and the Model subsystem,
  rebuilt with `git archive` as `src/main/java` and `src/test/java`; `scripts/puck-both.sh`), with and without
  the optional block: `puck-without-optional.log`, `puck-with-optional.log`. The authors' declaration was rerun on
  the same rebuilt subject in the same session: 894 without the block, as kept; 5,090 with it (5,093 kept, the
  known run-to-run drift).
- `compare.py` compares two files and their logs: the partitions (Rand and adjusted Rand index over the 78
  packages, the mapping of the axes), the orders (for every ordered pair of packages, whether one must not use
  the other under each file: agreement, Cohen's kappa, Jaccard of the two hidden sets), and the violated uses (the
  distinct `A --u--> B` lines of the logs: overlap, Jaccard, per clause). `comparison.md` is its output, against
  both of the authors' declarations.

## The second declaration (`decouple.wld`, `notes.md`)

Twenty-three axes, all 78 packages placed: one per documented subsystem present in the subject, the low-level
layer kept as eight axes (internationalisation, task management, configuration, Swing extensions, the Model,
logging, the utilities, the GEF extensions), the kernel and the UML helpers as axes of their own, the explorer as a
subsystem of its own. One speed chain, the four layers. Written clauses from the wiki's sentences: the low-level
subsystems apart ("no dependencies ... nor between them"), persistence hidden from the kernel, the GUI statement
read as hiding the model, the diagrams, the critics and, for its "UML", the property panels, the code generator,
the reverse engineering and the UML helpers; the target manager hidden from everything but the GUI and the
explorer ("should not be used anywhere outside the GUI subsystem"). The optional block holds what the declarer read
as implied: the kernel and the UML helpers slower than the view-and-control subsystems that use them, the notation
slower than the diagrams, two "less desired" dependencies as clauses, the utilities and GEF extensions apart from
the other low-level axes. The packages placed by judgement (no page claims them) and every ambiguity decided are
listed in `notes.md`, sections 4 and 6.

One side effect the declarer did not intend: `set targetmanager = [org.argouml.ui.targetmanager]` takes that
package out of the closure of the `gui` axis (a set that is not an axis removes its entities from their axis,
paper, Section 4.3), so the GUI clauses do not count the target manager's own uses. The figures are as run.

## Agreement

| | against the authors' final declaration | against the authors' first declaration | the authors' first against their final |
|---|---|---|---|
| Axes | 23 against 17 | 23 against 16 | 16 against 17 |
| Partition of the 78 packages: Rand index, adjusted Rand index | 0.981, 0.891 | 0.987, 0.924 | 0.988, 0.932 |
| Package pairs grouped by one and split by the other, of 3,003 | 56 | 39 | 37 |
| Hidden ordered package pairs, without the optional block: agreement, kappa, Jaccard | 0.912, 0.778, 0.722 | 0.964, 0.911, 0.880 | 0.911, 0.779, 0.726 |
| The same with the optional block | 0.580, 0.241, 0.399 | 0.662, 0.385, 0.482 | 0.917, 0.819, 0.880 |
| Violated uses without the block (lines, distinct) | 2,473 (2,394) against 894 (849) | 2,473 against 2,890 (2,829) | 2,890 against 894 |
| Distinct violated uses in both, Jaccard | 822, 0.340 | 2,330, 0.804 | 829, 0.291 |

The mapping of the axes is one-to-one up to the split of the low-level layer and three packages: the GEF
extensions (the authors: GUI; the second declarer: low-level), `org.argouml.language.ui` (GUI; code generation),
and the application's `api`, `events` and `helpers` packages (the authors' revision moved them to the
infrastructure, the second declarer kept them in the application, as the authors' first declaration did). Two of
the authors' three revision decisions were made by the second declarer on its own: the explorer as an axis of its
own, and the kernel as an axis; the third, the application's plug-in packages as infrastructure, was not.

Per clause, the counts coincide where the wiki is explicit: the critics named by the other subsystems, 325 in
both; the OCL module, 16 in both; the GUI statement, 525 in both (376 + 149 in the second declaration, which
splits it in two clauses); the uses of the application's packages, 967, as in the authors' first declaration. The
second declaration adds the 633 uses of the target manager from outside the GUI and the explorer, a wiki sentence
the authors did not encode, and 7 uses of persistence by the kernel; it lacks the 27 uses of the model by the
application's plug-in packages, which its mapping permits. Of the 849 distinct uses the paper reports, 822
(97 %) are violations under the second declaration too.

## Reading

Where the wiki states something, the two declarers wrote the same thing: the subsystems, the four layers as the
speed order, the GUI statement, the critics' isolation. They differ where the wiki is silent: the granularity of
the low-level layer, three package placements, which implied decisions go in the optional block, and one sentence
of the wiki that one declarer encoded as a clause and the other did not. On the pairs of packages that one
declaration hides and the other does not, the disagreement between the two declarers (kappa 0.78 against the
authors' final file) is the size of the authors' own revision (0.78 between their first and final files), and
smaller against the authors' first, unrevised file (0.91). The optional blocks, where each declarer wrote what the
wiki implies rather than says, agree little (kappa 0.24): the authors kept the view-and-control subsystems apart,
the second declarer ordered some of them. The violation count moves by the factor the paper's Threats section
calls a sensitivity (2,473 against 894), mostly through the three mapping decisions and the extra clause; the
violations the paper reports are reported by the second declaration too, nearly all of them.

## Reproducing

Build the subject (`../README.md`), copy `decouple.wld` next to its `src/`, run `scripts/puck-both.sh <dir>`, then
from `docs/argouml/`:

    python -I second-declarer/compare.py decouple-iteration2.wld second-declarer/decouple.wld packages.txt \
        <ours-without.log> second-declarer/puck-without-optional.log <ours-with.log> second-declarer/puck-with-optional.log

`fetchwiki.py` refetches the wiki pages (they are not kept here: copyright of the ArgoUML project).
