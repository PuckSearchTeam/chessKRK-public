#!/usr/bin/env python3
"""Permutation test for the three readings of "changes less often" against the declared speed order (paper,
Section 5.6, RQ5). The declared order is the `speed` chain of decouple-iteration2.wld (69 ordered pairs of
axes in its transitive closure); each reading ranks the seventeen axes (results.md, "Ranking by ..."). The test
asks how many of the 69 pairs a random total order of the axes agrees with, and where the observed agreement sits.
Usage: python permutation.py [samples]   (default 100000; seed fixed)"""
import random, re, sys, statistics
from pathlib import Path

here = Path(__file__).resolve().parent
wld = (here.parent / "decouple-iteration2.wld").read_text(encoding="utf-8")
results = (here / "results.md").read_text(encoding="utf-8")

chain = re.search(r"^speed\s+(.*?);", wld, re.M).group(1)
levels = [[a.strip() for a in level.split(",")] for level in chain.split("<")]
pairs = [(a, b) for i, lo in enumerate(levels) for a in lo for hi in levels[i + 1:] for b in hi]
axes = sorted({a for level in levels for a in level})
assert len(pairs) == 69 and len(axes) == 17, (len(pairs), len(axes))

readings = {}
for m in re.finditer(r"^Ranking by (.*?): (.*)$", results, re.M):
    order = [a.strip() for a in m.group(2).split("<")]
    assert sorted(order) == axes, m.group(1)
    readings[m.group(1)] = {a: i for i, a in enumerate(order)}

def agreement(rank):
    return sum(1 for a, b in pairs if rank[a] < rank[b])

n = int(sys.argv[1]) if len(sys.argv) > 1 else 100000
random.seed(2026)
dist = []
for _ in range(n):
    perm = axes[:]
    random.shuffle(perm)
    dist.append(agreement({a: i for i, a in enumerate(perm)}))
mean, sd = statistics.fmean(dist), statistics.pstdev(dist)
print(f"random total orders of the 17 axes, {n} samples: agreement with the 69 declared pairs "
      f"mean {mean:.1f}, sd {sd:.1f}, min {min(dist)}, max {max(dist)}")
for name, rank in readings.items():
    obs = agreement(rank)
    ge = sum(1 for d in dist if d >= obs) / n
    le = sum(1 for d in dist if d <= obs) / n
    print(f"{name}: agrees on {obs} of 69; P(random >= {obs}) = {ge:.4f}; P(random <= {obs}) = {le:.4f}")
