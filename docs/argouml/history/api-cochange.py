#!/usr/bin/env python3
"""Were the GUI classes that use the model directly the ones touched when the model's API changed? (One
paragraph of Section 5.6 of the paper reports the result.)

The model's API is the top-level package org.argouml.model (the facade and the factory and helper interfaces). An
*API commit* is a commit that adds or removes a method signature, or a type declaration, in a file of that package
(a changed signature counts, being one removal and one addition; private methods do not count; mass commits, more
than 200 classes, are left out, as in history.py). For each class, over the commits within its lifetime: the share
of the API commits in which the class changes, against the share of the other commits in which it changes; the
ratio of the two is how much more a class follows the API than it follows everything else.

Groups of classes compared:
  V024   the GUI classes of the violated gui -> model uses of version 0.24 (the exact encoding, puck-exact.log);
  Vhead  the GUI classes of the violated uses of the written GUI clause whose target is in the model, at head;
  Phead  the classes outside the GUI and the low-level axes that import org.argouml.model at head (permitted users);
  Ghead0 the GUI classes at head that do not import org.argouml.model.
Per group: classes, mean share of API commits followed, mean share of other commits followed, ratio of the means
with a 95 % bootstrap interval over the classes (10,000 resamples, seed 2026), and the share of the classes that
change in at least one API commit.

Usage: python -I api-cochange.py argouml-log.txt <clone> ../decouple-iteration2.wld ../packages.txt
       ../puck-iteration2-without-optional.log ../0.24/puck-exact.log ../0.24/argouml.sysmdl ../0.24/packages.txt
       <subject>/src
The log is git log --name-only --no-renames --format='%x01%H%x09%ad%x09%an' --date=short in the clone; the subject
is git archive be952fc src/argouml-app/src src/argouml-core-model/src (history/README.md)."""
import re, sys, os, random, statistics, subprocess, datetime
from collections import defaultdict

(log_file, clone, wld_file, packages_file, puck_log, exact_log, sysmdl_file, packages024_file,
 src_root) = sys.argv[1:10]
MASS = 200
BOOT = 10000

# --- the context at head: axes by package
axes = {}
text = open(wld_file, encoding='utf-8').read()
for m in re.finditer(r'^axis\s+(\w+)\s*=\s*\[(.*?)\]', text, re.M | re.S):
    for pkg in re.split(r'[\s,]+', m.group(2).strip()):
        if pkg: axes[pkg] = m.group(1)

def packages_of(f):
    return sorted((l.strip() for l in open(f, encoding='utf-8') if l.strip()), key=len, reverse=True)
packages = packages_of(packages_file); packages024 = packages_of(packages024_file)

def class_of(qualified, pkgs):
    for p in pkgs:
        if qualified == p: return None
        if qualified.startswith(p + '.'):
            rest = qualified[len(p) + 1:]
            return p + '.' + rest.split('.')[0].split('(')[0]
    return None

def axis_of_class(cls): return axes.get(cls.rsplit('.', 1)[0])

# --- the history
commits = []
rx = re.compile(r'(?:^|/)org/argouml/(.+)\.java$')
rx_api_file = re.compile(r'(?:^|/)org/argouml/model/[^/]+\.java$')
cur = None; touches_api_file = set()
for line in open(log_file, encoding='utf-8', errors='replace'):
    line = line.rstrip('\n')
    if line.startswith('\x01'):
        h, date, author = line[1:].split('\t', 2)
        cur = (h, date, set()); commits.append(cur); continue
    if not line or cur is None: continue
    if '/tests/' in '/' + line or line.startswith('tests/'): continue
    mm = rx.search(line)
    if not mm: continue
    cur[2].add('org.argouml.' + mm.group(1).replace('/', '.'))
    if rx_api_file.search(line): touches_api_file.add(cur[0])
commits = [c for c in commits if c[2] and len(c[2]) <= MASS]
date_of = {h: datetime.date.fromisoformat(d).toordinal() for h, d, _ in commits}
class_commits = defaultdict(set); class_first = {}; class_last = {}
for h, date, cls in commits:
    d = date_of[h]
    for c in cls:
        class_commits[c].add(h)
        class_first[c] = min(class_first.get(c, d), d); class_last[c] = max(class_last.get(c, d), d)

# --- the API commits: a method signature or a type declaration added or removed in org/argouml/model/*.java
KEYWORDS = {'if', 'for', 'while', 'switch', 'catch', 'return', 'new', 'else', 'synchronized', 'throw', 'super', 'this'}
rx_sig = re.compile(r'^[+-](?!\+\+|--)\s*(?:@\w+(?:\([^)]*\))?\s*)*'
                    r'(?:(?:public|protected|abstract|static|final|synchronized|default|native)\s+)*'
                    r'(?:<[^>]+>\s*)?[\w.$<>\[\]?,]+(?:\s*<[^>]*>)?\s+(\w+)\s*\(([^;{}]*)\)\s*'
                    r'(?:throws\s+[\w.,\s]+)?\s*[;{]?\s*$')
rx_type = re.compile(r'^[+-](?!\+\+|--)\s*(?:public\s+)?(?:abstract\s+|final\s+)*(?:interface|class|enum)\s+\w+')
def api_change(h):
    out = subprocess.run(['git', '-C', clone, 'show', '--format=', '--unified=0', '--no-color', h, '--',
                          ':(glob)**/org/argouml/model/*.java'], capture_output=True, text=True,
                         encoding='utf-8', errors='replace').stdout
    n = 0
    for line in out.splitlines():
        if not line or line[0] not in '+-': continue
        if 'private ' in line: continue
        m = rx_sig.match(line)
        if m and m.group(1) not in KEYWORDS and '=' not in line.split('(')[0]: n += 1; continue
        if rx_type.match(line): n += 1
    return n
api = {h for h in touches_api_file if h in date_of and api_change(h) > 0}
non_api = [h for h in date_of if h not in api]
api_dates = sorted(date_of[h] for h in api); other_dates = sorted(date_of[h] for h in non_api)
import bisect
def within(dates, lo, hi): return bisect.bisect_right(dates, hi) - bisect.bisect_left(dates, lo)
print(f"commits touching main code, mass commits left out: {len(date_of)}; touching org.argouml.model's top-level "
      f"package: {len([h for h in touches_api_file if h in date_of])}; of which API commits (a signature or a type "
      f"declaration added or removed): {len(api)}, from {datetime.date.fromordinal(api_dates[0])} to "
      f"{datetime.date.fromordinal(api_dates[-1])}")

# --- the groups
def pairs_from_log(path, pkgs):
    """(source class, target entity) of each violated use; the target is kept raw, so that a target outside the
    package list (the model packages are not in the head's packages.txt) is still seen."""
    pairs = set()
    for line in open(path, encoding='utf-8', errors='replace'):
        if '--u-->' not in line: continue
        left, right = line.split('--u-->')
        s = class_of(left.strip().lstrip('+ ').strip(), pkgs); t = right.split('[')[0].strip()
        if s and t: pairs.add((s, t))
    return pairs
rx_mod = {}; section = None
for line in open(sysmdl_file, encoding='utf-8', errors='replace'):
    line = line.rstrip('\r\n')
    m = re.match(r'^#\s*(\w+)', line)
    if m: section = m.group(1); continue
    if not line.strip() or line.startswith('#'): continue
    if section == 'mapping':
        name, r = line.split(None, 1); rx_mod[name] = r
def module(entity):
    for name, r in rx_mod.items():
        if re.match(r'^(?:' + r + r')', entity): return name
    return None
v024 = {s for s, t in pairs_from_log(exact_log, packages024) if module(s) == 'gui' and module(t) == 'model'
        and not t.startswith(s + '.')}
vhead = {s for s, t in pairs_from_log(puck_log, packages)
         if axis_of_class(s) == 'gui' and t.startswith('org.argouml.model.')}
imports_model = set(); all_head = set()
for root, dirs, files in os.walk(src_root):
    for f in files:
        if not f.endswith('.java'): continue
        path = os.path.join(root, f).replace('\\', '/')
        mm = rx.search(path)
        if not mm: continue
        cls = 'org.argouml.' + mm.group(1).replace('/', '.')
        all_head.add(cls)
        for line in open(path, encoding='utf-8', errors='replace'):
            if re.match(r'\s*import\s+(?:static\s+)?org\.argouml\.model\.', line): imports_model.add(cls); break
phead = {c for c in imports_model if axis_of_class(c) not in (None, 'gui', 'lowlevel', 'model')}
ghead0 = {c for c in all_head if axis_of_class(c) == 'gui' and c not in imports_model}

# --- measures
def rates(c):
    cs = class_commits.get(c)
    if not cs: return None
    lo, hi = class_first[c], class_last[c]
    n_api, n_oth = within(api_dates, lo, hi), within(other_dates, lo, hi)
    if n_api == 0 or n_oth == 0: return None
    a = len(cs & api)
    return a / n_api, (len(cs) - a) / n_oth, a > 0
def summary(name, classes):
    rows = [r for r in (rates(c) for c in sorted(classes)) if r]
    if not rows: return f"| {name} | 0 | | | | | |"
    p_api = [r[0] for r in rows]; p_oth = [r[1] for r in rows]; touched = sum(1 for r in rows if r[2])
    ratio = statistics.mean(p_api) / statistics.mean(p_oth)
    rng = random.Random(2026); boots = []
    for _ in range(BOOT):
        sample = rng.choices(rows, k=len(rows))
        ma = sum(r[0] for r in sample) / len(sample); mo = sum(r[1] for r in sample) / len(sample)
        boots.append(ma / mo if mo else float('inf'))
    boots.sort(); lo, hi = boots[int(0.025 * BOOT)], boots[int(0.975 * BOOT) - 1]
    return (f"| {name} | {len(classes)} | {len(rows)} | {100 * statistics.mean(p_api):.1f} % | "
            f"{100 * statistics.mean(p_oth):.1f} % | {ratio:.2f} ({lo:.2f} to {hi:.2f}) | {100 * touched / len(rows):.0f} % |")
print("\n| classes | n | with a usable history | changes in an API commit | changes in another commit | ratio (95 % bootstrap) | touched by at least one API commit |")
print("|---|---|---|---|---|---|---|")
print(summary("GUI classes of the violated gui -> model uses, version 0.24", v024))
print(summary("GUI classes of the GUI clause's violated uses into the model, head", vhead))
print(summary("permitted users of the model (import it, outside the GUI and the low-level axes), head", phead))
print(summary("GUI classes that do not import the model, head", ghead0))
