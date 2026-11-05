# The brief given to the second declarer (2026-11-05)

Verbatim, the only instructions the second declarer received. The folder it names held the 31 wiki pages as text
(`fetchwiki.py` lists them), `packages.txt`, `weland.bnf` and the engine's `decouple.wld` as `example-engine.wld`.
The declarer was a fresh session of the assistant (Claude), with no access to the repository, the paper, the first
declaration or its results.

---

You are a software architect asked to declare the *context* of ArgoUML, the UML modelling tool, for a
coupling-constraint checker called Puck, from the project's own documentation and nothing else. Your declaration
will be compared with another one, made independently from the same documentation, to measure how much two
declarers agree. So work only from the inputs below, decide every open point yourself, and record every decision.

Your folder, the ONLY place you may read from or write to: `<folder>`
- wiki\*.txt: 31 pages of the ArgoUML developers' Cookbook as migrated into the project wiki (chapter 4, "ArgoUML
  Design, The Big Picture", with the definition of a subsystem, the relationships between subsystems, the four
  layers; chapter 5, one page per subsystem). Read all of them.
- packages.txt: the 78 Java packages of the subject, which is the ArgoUML application module plus the Model
  subsystem at the head of the repository (1,322 classes). Every package in this list must be assigned to exactly
  one axis, unless you deliberately leave it out and say why.
- weland.bnf: the grammar and semantics of Weland, the constraint language of Puck.
- example-engine.wld: a complete constraint file for another program (a chess engine), as an example of the
  syntax and the style.
Do not read any other file, directory or web page: not the ArgoUML source code, not anything under D:\repos,
nothing on the web. If a page is missing something, decide and note it.

What a context is. The checker works from three things that are specific to a project and that its code does
not contain:
1. Its *axes of change*: a partition of the program's packages into sets of entities that share one reason to
   change (Martin's responsibilities). In Weland: `axis name = [org.argouml.pkg, org.argouml.other.pkg];`.
   Packages are NOT nested for the checker: listing `org.argouml.ui` does not include `org.argouml.ui.cmd`; every
   package of an axis must be listed by its full name.
2. Its *speed of change*: a partial order on the axes, `speed a < b < c, d;` reading "a changes less often than
   b, which changes less often than c and d" (c and d unordered between themselves); several chains may be
   written, their union must be a partial order (no cycle). From `a < b` the checker generates `hide b from a`:
   the slower axis must not use the faster one (Clean Architecture's Dependency Rule, with layers as speeds of
   change).
3. Optionally, a *difficulty to change* order (`difficulty X < Y;`), only if the documentation gives a reason.
Besides the generated clauses, the documentation may state explicit rules between subsystems: write each as a
`hide X from Y;` clause (X hidden from Y: Y must not use X). Decisions the documentation does not state but you
consider implied (for instance keeping unordered axes apart from one another with `apart a, b, c;`) go in a
separate block between the lines `// BEGIN optional` and `// END optional`, so that the file can be checked with
and without them. The tests are under a separate test source root; `friend tests of all;` exempts them, which is
the usual choice.

Method: read the documentation; map each documented subsystem to the packages of packages.txt (name the packages
that no page claims and decide where they go, saying why); turn the layering into speed chains; turn any explicit
statement about who may use whom into a written clause; keep what the wiki does not say out of the main part. Do
not consult code; where the documentation is silent, decide from the names and say so.

Deliverables, written in your folder:
1. decouple.wld: the complete constraint file, commented like the example (one comment per decision that the
   wiki does not make for you).
2. notes.md: your declaration explained: the list of axes with their packages and the wiki page each comes from;
   the speed order and the wiki sentences it rests on (quote them briefly, with the page's file name); the written
   clauses and their sources; every package you placed by judgement rather than by the documentation, with the
   reason; every point where the wiki is ambiguous or silent and what you decided; and how long the reading and
   the writing took you, roughly.
Reply with a short summary: the number of axes, the chains, the written clauses, and the packages decided by
judgement.
