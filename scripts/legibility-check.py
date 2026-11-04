#!/usr/bin/env python3
"""Lists the paragraphs and sentences of a LaTeX paper that exceed the legibility thresholds.

Usage: python scripts/legibility-check.py Article/main.tex [--paragraph 200] [--sentence 60] [--changed]
  --changed   report only paragraphs that differ from the committed version (git HEAD) of the file

Exit code 1 if anything exceeds a threshold (0 otherwise), so a hook can gate on it. The body is what lies between
\\section{Introduction} and the first starred section (CRediT and the rest); floats, lists and displayed maths are
skipped. Word counts are taken after LaTeX commands and citations are stripped, so they undercount slightly.
"""
import argparse, re, subprocess, sys

# Floats, listings and displayed maths are not prose: blanked before the paragraphs are cut (line count kept).
FLOATS = r"\\begin\{(table|figure|lstlisting|verbatim|align|equation)\*?\}.*?\\end\{\1\*?\}"
# A list item, a theorem-like environment or a sectioning command starts a paragraph of its own, as a blank line does.
BREAKS = r"(\n[ \t]*\n|(?=\\(?:sub)*section\{|\\paragraph\{|\\item\b|\\begin\{)|(?<=\\end\{itemize\})|(?<=\\end\{enumerate\})|(?<=\\end\{proof\})|(?<=\\end\{proposition\})|(?<=\\end\{observation\}))"

def strip(tex):
    tex = re.sub(r"(?<!\\)%.*", "", tex)
    tex = re.sub(r"\\(begin|end)\{[^}]*\}", "", tex)
    tex = re.sub(r"\\citet\*?(\[[^\]]*\])*\{[^}]*\}", "Author", tex)  # a textual citation is a word, often a sentence's first
    tex = re.sub(r"\\(cite[p]?|ref|eqref|label)\*?(\[[^\]]*\])*\{[^}]*\}", "", tex)
    tex = re.sub(r"\\[a-zA-Z]+\*?(\[[^\]]*\])?", "", tex)
    tex = re.sub(r"\$[^$]*\$", "X", tex)
    return re.sub(r"[{}~]", " ", tex)

def body_of(src):
    start = src.find("\\section{Introduction}")
    end = src.find("\\section*{", start)
    body = src[start:end if end > 0 else len(src)]
    body = re.sub(FLOATS, lambda m: "\n" * m.group(0).count("\n"), body, flags=re.S)
    return body, src[:start].count("\n") + 1

def paragraphs(src):
    """Yields (first line, text) per paragraph of prose, a list item counting as one."""
    body, first_line = body_of(src)
    line = first_line
    for block in re.split(BREAKS, body):
        if block and strip(block).strip():
            yield line, block
        line += block.count("\n")

def sentences(text):
    text = re.sub(r"\b(e\.g|i\.e|cf|vs|et al|Fig|Sec|Eq)\.", lambda m: m.group(0).replace(".", "·"), text)
    for s in re.split(r"(?<=[.!?:;])\s+(?=[A-Z(`\\])|(?<=[.!?])\s+(?=[(`])", text):
        if s.strip():
            yield s.replace("·", ".")

def committed(path):
    try:
        return subprocess.run(["git", "show", f"HEAD:{path}"], capture_output=True, text=True,
                              encoding="utf-8", check=True).stdout
    except (subprocess.CalledProcessError, FileNotFoundError):
        return ""

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("path")
    ap.add_argument("--paragraph", type=int, default=200)
    ap.add_argument("--sentence", type=int, default=60)
    ap.add_argument("--changed", action="store_true")
    a = ap.parse_args()
    src = open(a.path, encoding="utf-8").read()
    old = {re.sub(r"\s+", " ", p).strip() for _, p in paragraphs(committed(a.path.replace("\\", "/")))} if a.changed else None
    bad = 0
    for line, p in paragraphs(src):
        if old is not None and re.sub(r"\s+", " ", p).strip() in old:
            continue
        plain = strip(p)
        words = plain.split()
        head = " ".join(words[:7])
        if len(words) > a.paragraph:
            bad += 1
            print(f"{a.path}:{line}: paragraph of {len(words)} words (limit {a.paragraph}): {head} ...")
        for s in sentences(plain):
            n = len(s.split())
            if n > a.sentence:
                bad += 1
                print(f"{a.path}:{line}: sentence of {n} words (limit {a.sentence}): {' '.join(s.split()[:7])} ...")
    scope = "changed paragraphs" if a.changed else "whole body"
    print(f"{bad} finding(s) over the thresholds ({scope}; paragraph {a.paragraph} words, sentence {a.sentence} words)")
    sys.exit(1 if bad else 0)

if __name__ == "__main__":
    main()
