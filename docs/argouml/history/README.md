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
  too. Pairs: the 255 class pairs of the 894 violated uses (`../puck-iteration2-without-optional.log`, member edges
  lifted to their top-level classes); as baselines, the 2,919 permitted dependencies across axes read from the import
  statements of the subject (`import org.argouml...;` in the checked sources, 4,514 edges, 3,144 across axes), the
  1,370 dependencies inside an axis, and 5,000 random cross-axis pairs without an import between them. Until
  2026-11-04 `../packages.txt` lacked `org.argouml.model` (77 packages for 78), so a use or an import whose target is
  in the model mapped to no class and every dependency into the model was left out: 209 violating pairs (95 + 114)
  and 2,135 permitted ones, the figures of the paper's Table 6 before that date (Jaccard 0.020 / 0.016 / 0.021 for
  the generated clauses, the GUI clause and the permitted pairs). The package added, the counts below are the
  corrected ones; the reading did not change.

- Bootstrap (2026-11-04): each row's pairs are resampled with replacement 10,000 times (seed 2026) and the four means
  recomputed; the 2.5th and 97.5th percentiles are the interval of each mean, and, from the same resamples, of each
  row's difference with the permitted cross-axis dependencies. About ten seconds.

Run, from this directory, with the git log and the subject's sources (not kept; `git archive be952fc src/argouml-app/src
src/argouml-core-model/src | tar -x -C <subject>` from the clone, 1,322 classes), then paste the permutation section back
at the end, as `permutation.py` prints it:
`python -I history.py argouml-log.txt ../decouple-iteration2.wld ../packages.txt ../puck-iteration2-without-optional.log <subject>/src > results.md`.

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
  ones (commits changing both, in all and per year of the pair's life): the 102 pairs of the uses against the
  generated clauses, the ones against the speed order, Jaccard 0.019, follows 6 %, 1.06 commits, 0.102 per year;
  the 153 pairs of the written GUI clause, which the order permits, 0.012, 7 %, 1.28, 0.115; permitted dependencies
  across axes 0.016, 7 %, 0.98, 0.098; dependencies inside an axis 0.044, 13 %, 2.33, 0.229; unrelated pairs 0.002,
  0 %, 0.07, 0.008.

Reading: a dependency shows in co-change (ten times the unrelated pairs), its direction against the declared order
does not. The premise the paper's Related Work section states, that a dependency against the direction of change
costs, is not confirmed on ArgoUML at the granularity of a commit. The one positive signal: dependencies inside an axis co-change nearly three times as often as dependencies across axes, evidence
that the declared axes group what changes together (confounded by package proximity). Limits: a change spread over
several commits, or never made because the violation was tolerated (the GUI has used the model for twenty years), is
invisible to commit-level co-change; the context is the wiki's layering declared by us, not the developers' own
ranking; the whole history is pooled, the code of 2000 and of 2022 alike, which the per-class-year reading only
partly corrects; the violating pairs come from Puck's graph and the permitted ones from import statements, two
extractors.

- `permutation.py` (2026-11-04): a permutation test of the three readings against the declared order, 100,000 random total orders of the seventeen axes; results at the end of `results.md`.
- `api-cochange.py` (2026-11-04, one paragraph of Section 5.6 of the paper):
  were the GUI classes that use the model directly the ones touched when the model's API changed? API commits are
  those that add or remove a method signature or a type declaration in the top-level package `org.argouml.model`
  (491 of the 782 commits touching it, 2002 to 2020, read from `git show` in the clone). Per class, the share of the
  API commits within its lifetime in which it changes, against the share of the other commits; the ratio of the
  two, averaged per group with a bootstrap interval over the classes. The 13 GUI classes of the 174 `gui -> model`
  uses of 0.24: 0.93 (0.55 to 1.67); the 16 GUI classes of the GUI clause's 205 uses into the model at head: 0.40
  (0.16 to 0.64); the 613 permitted users of the model: 1.28 (1.14 to 1.42); the 113 GUI classes that do not import
  the model: 0.10 (0.03 to 0.19). The violating classes do not follow the API more than they follow anything else;
  the permitted users do, a little. Run from this directory with the clone's path as second argument (about twenty
  seconds); results at the end of `results.md`.
- Bootstrap intervals (2026-11-04, `results.md` after the co-change table): the Jaccard mean of the 102 violating pairs
  lies in 0.013 to 0.025 and that of the 2,919 permitted pairs in 0.015 to 0.017; their difference in −0.003 to 0.009
  (follows: −3 to 2 points; commits changing both: −0.23 to 0.42; per year: −0.025 to 0.036), so the violating pairs
  co-change at most about half again as much as the permitted ones on Jaccard, and possibly less. Dependencies
  inside an axis minus across axes: Jaccard 0.025 to 0.032, follows 6 to 8 points, both clear of zero. The 153 GUI
  pairs: Jaccard difference −0.008 to 0.000, the other measures astride zero. (Corrected on 2026-11-04 with the
  model package; before: −0.007 to 0.005 and 0.020 to 0.027.)
