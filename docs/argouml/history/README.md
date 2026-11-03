# ArgoUML's history against the declared context (2026-10-20)

Does each declared axis change as often as the speed order says, and do the classes on the violating edges change
together?
Paper, Section 5.6, Threats and the conclusion.

## Method (`history.py`)

- History: the full clone of `argouml-tigris-org/argouml` (`D:\repos\argouml`, 17,843 commits, 1998 to 2022), read as
  `git log --name-only --no-renames --format='%x01%H%x09%ad%x09%an' --date=short > argouml-log.txt` (7 MB, not kept).
  A path is mapped to a class by the part after `org/argouml/`, so a move between source roots (`src_new`,
  `src/argouml-app/src`, `src/model/src`, ...) is the same class; test roots are left out. 10,810 commits touch main
  Java code under `org.argouml` (2000-09-04 to 2022-08-01). Sixteen commits touch more than 200 classes (license
  headers, reformatting) and are left out of the co-change figures.
- Context: `../decouple-iteration2.wld` (17 axes, the speed chain `lowlevel < model < {kernel, gui, explorer,
  diagrams, panels, uml, notation, codegen, reveng, persistence, profile, modules} < cognitive, ocl < application`),
  69 ordered pairs. A class belongs to the axis of its package; packages outside the context are ignored.
- Change frequency per axis: the commits touching the axis, the distinct classes it ever held, the file-touches, the
  touches per class (file-touches / distinct classes), the share of commits. For each ordered pair a < b ("a changes
  less often than b") the history agrees when a's rate is lower than b's.
- Co-change: for a class pair (user, used), over the commits of the whole history, the Jaccard index of the two
  classes' commit sets, and "follows": the share of the commits changing the used class in which the user changes
  too. Pairs: the 209 class pairs of the 894 violated uses (`../puck-iteration2-without-optional.log`, member edges
  lifted to their top-level classes); as baselines, the 2,135 permitted dependencies across axes read from the import
  statements of the subject (`import org.argouml...;` in the checked sources, 3,706 edges, 2,336 across axes), the
  1,370 dependencies inside an axis, and 5,000 random cross-axis pairs without an import between them.

Run: `python -I history.py argouml-log.txt ../decouple-iteration2.wld ../packages.txt ../puck-iteration2-without-optional.log <subject>/src/main/java > results.md`.

## Results (`results.md`)

- Change frequency, three readings of "changes less often" (2026-10-20):
  touches per class, the order holds for 56 of the 69 pairs (a random order: about 34); touches per class-year
  (each class's lifetime from its first to its last commit, floored at one year), 34; share of commits touching the
  axis, 24. Per class and per class-year the low-level subsystems (8.9 touches per class, 2.94 per class-year) are among
  the four least-touched axes and the application (30.2, 6.32) and the code generator (32.8, 6.52) the two most; by
  share of commits the low-level subsystems (7.1 %) and the model (7.3 %) lie above the median, the application
  (2.7 %) below. The per-class-year reading is at chance because of the loadable layer: cognitive (2.58) and ocl
  (2.53), declared faster than the view-and-control subsystems, change less than they do once lifetimes count (per
  class and by share, the critics change more than nine of the twelve); 26 of that reading's 35 disagreements involve
  them, and the order holds on 30 of the 39 other pairs. The wiki's layers are a dependency statement, read by us
  as a speed statement. The diagrams (26 %) and the panels (21 %) are the largest
  axes and touch the most commits.
- Co-change does not single out the violations, on the conditional measures (Jaccard; "follows") nor on the absolute
  ones (commits changing both, in all and per year of the pair's life): the 95 pairs of the uses against the
  generated clauses, the ones against the speed order, Jaccard 0.020, follows 7 %, 1.12 commits, 0.108 per year;
  the 114 pairs of the written GUI clause, which the order permits, 0.016, 9 %, 1.67, 0.149; permitted dependencies
  across axes 0.021, 9 %, 1.29, 0.128; dependencies inside an axis 0.044, 13 %, 2.33, 0.229; unrelated pairs 0.002,
  0 %, 0.07, 0.008.

Reading: a dependency shows in co-change (ten times the unrelated pairs), its direction against the declared order
does not. The premise the paper's Related Work section states, that a dependency against the direction of change
costs, is not confirmed on ArgoUML at the granularity of a commit. The one positive signal: dependencies inside an axis co-change twice as often as dependencies across axes, evidence
that the declared axes group what changes together (confounded by package proximity). Limits: a change spread over
several commits, or never made because the violation was tolerated (the GUI has used the model for twenty years), is
invisible to commit-level co-change; the context is the wiki's layering declared by us, not the developers' own
ranking; the whole history is pooled, the code of 2000 and of 2022 alike, which the per-class-year reading only
partly corrects; the violating pairs come from Puck's graph and the permitted ones from import statements, two
extractors.
