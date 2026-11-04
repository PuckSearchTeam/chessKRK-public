#!/usr/bin/env python3
"""Violated uses of the exact encoding (puck-exact.log) counted per module pair of the published architecture
(argouml.sysmdl), source module -> target module, most frequent first. The modules are assigned by the regular
expressions of the sysmdl's mapping section, which pairs-count.pl uses too; a plain prefix match would put
org.argouml.application.api and .modules, which belong to moduleLoader, into application (the earlier
"application -> application 46", 2026-11-04). A package outside the mapping is reported as "other".
Usage: python -I pairs-exact.py puck-exact.log argouml.sysmdl > pairs-exact.txt"""
import re, sys, collections

log, sysmdl = sys.argv[1], sys.argv[2]
rx = {}; section = None
for line in open(sysmdl, encoding='utf-8', errors='replace'):
    line = line.rstrip('\r\n')
    m = re.match(r'^#\s*(\w+)', line)
    if m: section = m.group(1); continue
    if not line.strip() or line.startswith('#'): continue
    if section == 'mapping':
        name, r = line.split(None, 1); rx[name] = r

def module(entity):
    for name, r in rx.items():
        if re.match(r'^(?:' + r + r')', entity): return name
    return 'other'

pairs = collections.Counter()
for line in open(log, encoding='utf-8', errors='replace'):
    if '--u-->' not in line: continue
    left, right = line.split('--u-->')
    source = left.strip().lstrip('+ ').strip(); target = right.split('[')[0].strip()
    pairs[(module(source), module(target))] += 1
for (s, t), n in sorted(pairs.items(), key=lambda kv: (-kv[1], kv[0])):
    print(f"{n:7d} {s} -> {t}")
print(f"{sum(pairs.values()):7d} total")
