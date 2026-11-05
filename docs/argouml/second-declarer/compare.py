#!/usr/bin/env python3
"""Compare two declarations of ArgoUML's context (two Weland files over the same packages) and their Puck logs.

Usage: python -I compare.py A.wld B.wld packages.txt [A-without.log B-without.log [A-with.log B-with.log]]

Reports, for the two files: the axes and how they map onto each other; the agreement of the two partitions of the
packages (Rand index, adjusted Rand index, the packages placed differently); the agreement of the two orders on
the ordered pairs of packages, read as "q is hidden from p" (p must not use q) under each file, with Cohen's kappa,
with the optional block ("with") and without it; and, when logs are given, the violated uses of each run and the
overlap of the two sets (Jaccard). Only the statements the two files use are interpreted: axis, speed, apart,
hide X from Y (set references by name), friend; set and difficulty are read but a set is not an axis.
"""
import io, re, sys
from itertools import combinations


def strip_optional(text, keep):
    if keep:
        return text
    out, skip = [], False
    for line in text.split('\n'):
        if line.strip().startswith('// BEGIN optional'):
            skip = True
        elif line.strip().startswith('// END optional'):
            skip = False
        elif not skip:
            out.append(line)
    return '\n'.join(out)


def parse(path, keep_optional):
    text = io.open(path, encoding='utf-8').read()
    text = strip_optional(text, keep_optional)
    text = re.sub(r'//[^\n]*', '', text)
    axes, hidden, friends = {}, set(), set()
    chains = []
    # axis and set definitions take no semicolon: read them first, then the other statements
    for m in re.finditer(r'\b(axis|set)\s+(\w+)\s*=\s*\[([^\]]*)\]\s*;?', text):
        if m.group(1) == 'axis':
            axes[m.group(2)] = [p.strip() for p in m.group(3).split(',') if p.strip()]
    text = re.sub(r'\b(axis|set)\s+(\w+)\s*=\s*\[([^\]]*)\]\s*;?', '', text)
    for stmt in re.split(r';', text):
        stmt = stmt.strip()
        if not stmt:
            continue
        m = re.match(r'speed\s+(.*)', stmt, re.S)
        if m:
            groups = [[a.strip() for a in g.split(',')] for g in m.group(1).split('<')]
            chains.append(groups)
            continue
        m = re.match(r'apart\s+(.*)', stmt, re.S)
        if m:
            names = [a.strip() for a in m.group(1).split(',')]
            for a in names:
                for b in names:
                    if a != b:
                        hidden.add((a, b))      # a hidden from b
            continue
        m = re.match(r'hide\s+(.*?)\s+from\s+(.*)', stmt, re.S)
        if m:
            for x in [a.strip() for a in m.group(1).split(',')]:
                for y in [a.strip() for a in m.group(2).split(',')]:
                    hidden.add((x, y))
            continue
        m = re.match(r'friend\s+(.*?)\s+of\s+(.*)', stmt, re.S)
        if m:
            friends.add((m.group(1).strip(), m.group(2).strip()))
            continue
        if stmt.startswith('difficulty'):
            print('note: difficulty statement ignored:', stmt[:60])
    # speed: a < b generates hide b from a, transitively
    slower = set()
    for groups in chains:
        for i in range(len(groups)):
            for j in range(i + 1, len(groups)):
                for a in groups[i]:
                    for b in groups[j]:
                        slower.add((a, b))      # a slower than b
    changed = True
    while changed:
        changed = False
        for (a, b) in list(slower):
            for (c, d) in list(slower):
                if b == c and (a, d) not in slower:
                    slower.add((a, d))
                    changed = True
    for (a, b) in slower:
        hidden.add((b, a))                      # b hidden from a
    return axes, hidden, friends, slower


def axis_of(axes):
    ax = {}
    for a, pkgs in axes.items():
        for p in pkgs:
            ax.setdefault(p, []).append(a)
    return {p: (l[0] if len(l) == 1 else None) for p, l in ax.items()}


def rand(axA, axB, pkgs):
    n11 = n00 = 0
    pairs = list(combinations(pkgs, 2))
    for p, q in pairs:
        sa, sb = axA[p] == axA[q], axB[p] == axB[q]
        if sa and sb:
            n11 += 1
        elif not sa and not sb:
            n00 += 1
    ri = (n11 + n00) / len(pairs)
    # adjusted Rand
    from collections import Counter
    cont = Counter((axA[p], axB[p]) for p in pkgs)
    rowsA, rowsB = Counter(axA[p] for p in pkgs), Counter(axB[p] for p in pkgs)
    comb2 = lambda n: n * (n - 1) / 2
    sum_ij = sum(comb2(v) for v in cont.values())
    sum_a, sum_b = sum(comb2(v) for v in rowsA.values()), sum(comb2(v) for v in rowsB.values())
    total = comb2(len(pkgs))
    expected = sum_a * sum_b / total
    maxi = (sum_a + sum_b) / 2
    ari = (sum_ij - expected) / (maxi - expected) if maxi != expected else 1.0
    return ri, ari


def hidden_pairs(axes, hidden, pkgs):
    ax = axis_of(axes)
    out = set()
    for p in pkgs:
        for q in pkgs:
            if p != q and ax.get(p) and ax.get(q) and ax[p] != ax[q] and (ax[q], ax[p]) in hidden:
                out.add((p, q))             # p must not use q
    return out


def kappa(setA, setB, universe):
    n = len(universe)
    a = len(setA & setB)
    b = len(setA - setB)
    c = len(setB - setA)
    d = n - a - b - c
    po = (a + d) / n
    pe = ((a + b) * (a + c) + (c + d) * (b + d)) / (n * n)
    return po, (po - pe) / (1 - pe) if pe != 1 else 1.0, a, b, c, d


def violations(path):
    uses, cur, per = set(), None, {}
    for line in io.open(path, encoding='utf-8', errors='replace'):
        if line.startswith('- '):
            cur = line.strip()
            per.setdefault(cur, 0)
        elif line.startswith('   + ') and cur:
            edge = line.split('[')[0].strip()[2:]
            uses.add(edge)
            per[cur] += 1
    return uses, per


def main():
    a_path, b_path, pk_path = sys.argv[1:4]
    logs = sys.argv[4:]
    pkgs = [l.strip() for l in io.open(pk_path, encoding='utf-8') if l.strip()]
    for keep in (False, True):
        tag = 'with the optional block' if keep else 'without the optional block'
        A, hA, fA, sA = parse(a_path, keep)
        B, hB, fB, sB = parse(b_path, keep)
        if not keep:
            axA, axB = axis_of(A), axis_of(B)
            placed = [p for p in pkgs if axA.get(p) and axB.get(p)]
            print('## Axes\n')
            print('A: %d axes, %d packages placed; B: %d axes, %d packages placed; %d placed in both of the %d.' % (
                len(A), sum(1 for p in pkgs if axA.get(p)), len(B), sum(1 for p in pkgs if axB.get(p)), len(placed), len(pkgs)))
            missing = [p for p in pkgs if not axA.get(p) or not axB.get(p)]
            if missing:
                print('Not placed by both:', ', '.join('%s (A: %s, B: %s)' % (p, axA.get(p), axB.get(p)) for p in missing))
            print('\nMapping of B\'s axes onto A\'s (packages shared):')
            for b in B:
                from collections import Counter
                c = Counter(axA[p] for p in B[b] if axA.get(p))
                print('- %s (%d): %s' % (b, len(B[b]), ', '.join('%s %d' % kv for kv in c.most_common())))
            ri, ari = rand(axA, axB, placed)
            print('\nPartition agreement on the %d packages placed by both: Rand index %.3f, adjusted Rand index %.3f.' % (len(placed), ri, ari))
            diff = []
            for p, q in combinations(placed, 2):
                if (axA[p] == axA[q]) != (axB[p] == axB[q]):
                    diff.append((p, q))
            print('Package pairs grouped by one and split by the other: %d of %d.' % (len(diff), len(list(combinations(placed, 2)))))
        print('\n## Hidden pairs, %s\n' % tag)
        universe = [(p, q) for p in placed for q in placed if p != q]
        HA, HB = hidden_pairs(A, hA, placed), hidden_pairs(B, hB, placed)
        po, k, a, b, c, d = kappa(HA, HB, universe)
        print('Ordered package pairs (p must not use q): A hides %d, B hides %d, both %d, A only %d, B only %d, neither %d of %d.' % (
            len(HA), len(HB), a, b, c, d, len(universe)))
        print('Agreement %.3f, Cohen\'s kappa %.3f, Jaccard of the hidden sets %.3f.' % (po, k, a / (a + b + c) if a + b + c else 1))
    if len(logs) >= 2:
        for i, name in enumerate(['without the optional block', 'with the optional block']):
            if len(logs) < 2 * i + 2:
                break
            uA, pA = violations(logs[2 * i])
            uB, pB = violations(logs[2 * i + 1])
            print('\n## Violated uses, %s\n' % name)
            print('A: %d lines, %d distinct uses; B: %d lines, %d distinct uses; in both %d; Jaccard %.3f.' % (
                sum(pA.values()), len(uA), sum(pB.values()), len(uB), len(uA & uB), len(uA & uB) / len(uA | uB) if uA | uB else 1))
            print('A per clause: ' + '; '.join('%s %d' % (k[2:].rstrip(';'), v) for k, v in pA.items()))
            print('B per clause: ' + '; '.join('%s %d' % (k[2:].rstrip(';'), v) for k, v in pB.items()))


if __name__ == '__main__':
    main()
