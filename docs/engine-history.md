# The engine's own history under the three readings (2026-11-04)

The engine's own git history is the cheapest evidence one could ask for the
declared speed order: apply the three readings of "changes less often" of Section 5.6 to its seven axes. The
history is too short to read a frequency from: six commits touch `src/main/java` between 2026-09-11 and
2026-10-09, three of them on one day, and the engine's 38 classes were written by the authors in that month.
Counted anyway, for the record (`git log --name-only -- src/main/java`, classes mapped to axes by package):

| axis | declared rank | commits touching it | file-touches | classes ever | touches per class |
|---|---|---|---|---|---|
| rules | 1 (slowest) | 5 | 21 | 14 | 1.5 |
| policy | 2 | 4 | 7 | 6 | 1.2 |
| strategies | 3 | 4 | 9 | 5 | 1.8 |
| valuation | 4 (unordered) | 5 | 8 | 5 | 1.6 |
| pieces | 4 (unordered) | 2 | 2 | 2 | 1.0 |
| protocol | 4 (unordered) | 5 | 9 | 4 | 2.2 |
| wiring | 5 (fastest) | 5 | 6 | 3 | 2.0 |

Per class the spread is one touch (1.0 to 2.2) over six commits, and the `pieces` axis exists only since the
redesign commit `9eb6659`, the fourth of the six. No reading can rank seven axes on that; the order of the
touches-per-class column (policy, rules, valuation, strategies, wiring, protocol) agrees with the declared chain on
some pairs and not others by accident. The paper does not report it: such a
study needs a history of years, which the engine does not have, and would in any case measure
the authors' own habits against the authors' own declaration.
