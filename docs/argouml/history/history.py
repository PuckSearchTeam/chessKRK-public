#!/usr/bin/env python3
"""ArgoUML's history against the declared context (paper, Section 5.6).

Two questions. (1) Does each axis change as often as the declared speed order says? For every axis: the commits
that touch it, the distinct classes it ever held, the file-touches, and the touches per class; then, for every pair
the chain orders (a < b, "a changes less often than b"), whether the history agrees. (2) Do the classes on the
violating edges change together? For the class pairs of the violated uses (Puck's log) and, as a baseline, for the
permitted cross-axis dependencies read from the import statements of the subject: the Jaccard co-change of the two
classes over the commits, and how often the user changes in a commit that changes the used class ("follows").

Inputs: the git log with file names (git log --name-only --no-renames --format='%x01%H%x09%ad%x09%an' --date=short),
the context file (axes and the speed chain), the package list of the subject, Puck's log of the violated uses, and
the subject's main source root (for the imports). Paths are mapped to classes by the part after org/argouml/, so a
move between source roots (src_new, src/argouml-app/src, ...) is the same class; test roots are left out.
"""
import re, sys, os, random, statistics
from collections import defaultdict

log_file, wld_file, packages_file, puck_log, src_root = sys.argv[1:6]
MASS = 200  # a commit touching more than this many main classes is a mass edit (license headers, reformatting)

# --- the context: axes and the speed chain
axes = {}            # package -> axis
chain = []           # list of groups, slowest first
text = open(wld_file, encoding='utf-8').read()
for m in re.finditer(r'^axis\s+(\w+)\s*=\s*\[(.*?)\]', text, re.M | re.S):
    for pkg in re.split(r'[\s,]+', m.group(2).strip()):
        if pkg: axes[pkg] = m.group(1)
m = re.search(r'^speed\s+(.*?);', text, re.M | re.S)
for group in m.group(1).split('<'):
    chain.append([x.strip() for x in group.split(',') if x.strip()])
axis_names = [a for g in chain for a in g]
packages = sorted((l.strip() for l in open(packages_file, encoding='utf-8') if l.strip()), key=len, reverse=True)

def class_of(qualified):
    """The top-level class of a qualified entity name, by the longest package prefix of the subject."""
    for p in packages:
        if qualified == p: return None
        if qualified.startswith(p + '.'):
            rest = qualified[len(p) + 1:]
            return p + '.' + rest.split('.')[0].split('(')[0]
    return None

def axis_of_class(cls):
    return axes.get(cls.rsplit('.', 1)[0])

# --- the history
commits = []         # (hash, date, set of classes)
rx = re.compile(r'(?:^|/)org/argouml/(.+)\.java$')
cur = None
for line in open(log_file, encoding='utf-8', errors='replace'):
    line = line.rstrip('\n')
    if line.startswith('\x01'):
        h, date, author = line[1:].split('\t', 2)
        cur = (h, date, set())
        commits.append(cur)
        continue
    if not line or cur is None: continue
    if '/tests/' in '/' + line or line.startswith('tests/'): continue
    mm = rx.search(line)
    if not mm: continue
    cur[2].add('org.argouml.' + mm.group(1).replace('/', '.'))
commits = [c for c in commits if c[2]]
first, last = min(c[1] for c in commits), max(c[1] for c in commits)
mass = [c for c in commits if len(c[2]) > MASS]
print(f"commits touching main Java code under org/argouml: {len(commits)} ({first} to {last}); "
      f"mass commits (> {MASS} classes): {len(mass)}, left out of the co-change figures")

# lifetime of a class: from the first to the last commit touching it, floored at one year
import datetime
def ordinal(date): return datetime.date.fromisoformat(date).toordinal()
class_first = {}; class_last = {}
for h, date, cls in commits:
    d = ordinal(date)
    for c in cls:
        class_first[c] = min(class_first.get(c, d), d); class_last[c] = max(class_last.get(c, d), d)
def years(c_set):
    """Years from the first commit touching any of the classes to the last, floored at one year."""
    lo = min(class_first[c] for c in c_set); hi = max(class_last[c] for c in c_set)
    return max(1.0, (hi - lo) / 365.25)

# --- (1) change frequency per axis
touch_commits = defaultdict(set); touches = defaultdict(int); classes_ever = defaultdict(set)
for h, date, cls in commits:
    for c in cls:
        a = axis_of_class(c)
        if a is None: continue
        touch_commits[a].add(h); touches[a] += 1; classes_ever[a].add(c)
print("\n## Change frequency per axis (whole history)\n")
print("| axis | commits touching it | distinct classes ever | file-touches | touches per class | touches per class-year | share of commits |")
print("|---|---|---|---|---|---|---|")
rate_commits = {}; rate_per_class = {}; rate_per_class_year = {}
for a in axis_names:
    n, k, t = len(touch_commits[a]), len(classes_ever[a]), touches[a]
    class_years = sum(years({c}) for c in classes_ever[a])
    rate_commits[a] = n / len(commits); rate_per_class[a] = t / k if k else 0
    rate_per_class_year[a] = t / class_years if class_years else 0
    print(f"| {a} | {n} | {k} | {t} | {rate_per_class[a]:.1f} | {rate_per_class_year[a]:.2f} | {100 * rate_commits[a]:.1f} % |")

def agreement(rate, label):
    agree = disagree = 0; bad = []
    for i, g in enumerate(chain):
        for a in g:
            for g2 in chain[i + 1:]:
                for b in g2:
                    if rate[a] < rate[b]: agree += 1
                    else: disagree += 1; bad.append(f"{a} ({rate[a]:.2f}) not < {b} ({rate[b]:.2f})")
    print(f"\n{label}: the history agrees with {agree} of the {agree + disagree} ordered pairs of the chain.")
    for x in bad: print("  disagrees: " + x)
agreement(rate_commits, "Share of commits touching the axis")
agreement(rate_per_class, "Touches per class")
agreement(rate_per_class_year, "Touches per class-year (each class's lifetime, floored at one year)")
print("\nRanking by share of commits: " + " < ".join(sorted(axis_names, key=lambda a: rate_commits[a])))
print("Ranking by touches per class: " + " < ".join(sorted(axis_names, key=lambda a: rate_per_class[a])))
print("Ranking by touches per class-year: " + " < ".join(sorted(axis_names, key=lambda a: rate_per_class_year[a])))

# --- (2) co-change
class_commits = defaultdict(set)
for h, date, cls in commits:
    if len(cls) > MASS: continue
    for c in cls: class_commits[c].add(h)

def cochange(pairs):
    """Conditional measures (Jaccard; 'follows', the share of the commits changing the used class in which the user
    changes too) and absolute ones (commits changing both, in all and per year of the pair's life)."""
    jac = []; follow = []; absolute = []; per_year = []; ever = 0; usable = 0
    for s, t in pairs:
        cs, ct = class_commits.get(s), class_commits.get(t)
        if not cs or not ct: continue
        usable += 1
        both = len(cs & ct)
        jac.append(both / len(cs | ct)); follow.append(both / len(ct))
        absolute.append(both); per_year.append(both / years({s, t}))
        if both: ever += 1
    if not usable: return "no usable pair"
    return (f"{usable} | {100 * ever / usable:.0f} % | {statistics.mean(jac):.3f} / {statistics.median(jac):.3f} | "
            f"{100 * statistics.mean(follow):.0f} % | {statistics.mean(absolute):.2f} | {statistics.mean(per_year):.3f}")

violating = set()
for line in open(puck_log, encoding='utf-8', errors='replace'):
    if '--u-->' not in line: continue
    left, right = line.split('--u-->')
    s = class_of(left.strip().lstrip('+ ').strip()); t = class_of(right.split('[')[0].strip())
    if s and t and s != t: violating.add((s, t))
print(f"\n## Co-change\n\nViolated uses of the context (iteration 2, without the optional block) as class pairs: {len(violating)}")

imports = set()
for root, dirs, files in os.walk(src_root):
    for f in files:
        if not f.endswith('.java'): continue
        path = os.path.join(root, f).replace('\\', '/')
        mm = rx.search(path)
        if not mm: continue
        src = 'org.argouml.' + mm.group(1).replace('/', '.')
        for line in open(path, encoding='utf-8', errors='replace'):
            im = re.match(r'\s*import\s+(?!static\s)(org\.argouml\.[\w.]+)\s*;', line)
            if im:
                t = class_of(im.group(1))
                if t and t != src: imports.add((src, t))
cross = {(s, t) for s, t in imports if axis_of_class(s) and axis_of_class(t) and axis_of_class(s) != axis_of_class(t)}
permitted = cross - violating
same_axis = {(s, t) for s, t in imports if axis_of_class(s) and axis_of_class(s) == axis_of_class(t)}
print(f"Import edges between classes of the subject: {len(imports)}; across axes: {len(cross)}, of which not violating (permitted): {len(permitted)}; inside an axis: {len(same_axis)}")
random.seed(1)
all_classes = sorted(c for c in class_commits if axis_of_class(c))
unrelated = set()
while len(unrelated) < 5000:
    s, t = random.sample(all_classes, 2)
    if axis_of_class(s) != axis_of_class(t) and (s, t) not in imports: unrelated.add((s, t))
gui = {(s, t) for s, t in violating if axis_of_class(s) == 'gui'}   # the written clause, not against the speed order
print("\n| pairs | n | co-changed at least once | Jaccard mean / median | user follows the used class | commits changing both, mean | per year of the pair's life |")
print("|---|---|---|---|---|---|---|")
print(f"| violating uses against the speed order (the generated clauses) | {cochange(violating - gui)} |")
print(f"| violating uses of the written GUI clause (not against the order) | {cochange(gui)} |")
print(f"| all violating uses | {cochange(violating)} |")
print(f"| permitted dependencies across axes (imports) | {cochange(permitted)} |")
print(f"| dependencies inside an axis (imports) | {cochange(same_axis)} |")
print(f"| random cross-axis pairs without dependency | {cochange(unrelated)} |")
