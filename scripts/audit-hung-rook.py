#!/usr/bin/env python3
# Independent audit: drives target/krk-engine.jar over UCI through every legal white-to-move KRK position
# and checks, with its own rules, that the answer is legal and never leaves the rook capturable.
# Usage: scripts/audit-hung-rook.py [path/to/engine.jar]   (about ten seconds; prints "positions 175168 bad 0" when all is well)
import subprocess, sys, itertools
import os
JAR=sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)),"..","target","krk-engine.jar")
def sq(i): return "abcdefgh"[i%8]+str(i//8+1)
def adj(a,b): return max(abs(a%8-b%8),abs(a//8-b//8))==1
def fen(wk,r,bk,side):
    board=[""]*64; board[wk]="K"; board[r]="R"; board[bk]="k"
    rows=[]
    for rank in range(7,-1,-1):
        s="";e=0
        for f in range(8):
            c=board[rank*8+f]
            if c: 
                if e: s+=str(e); e=0
                s+=c
            else: e+=1
        if e: s+=str(e)
        rows.append(s)
    return "/".join(rows)+" "+side+" - - 0 1"
def rook_attacks(r,t,blocker):
    if r==t: return False
    if r%8==t%8:
        lo,hi=sorted((r//8,t//8))
        return not (blocker%8==r%8 and lo<blocker//8<hi)
    if r//8==t//8:
        lo,hi=sorted((r%8,t%8))
        return not (blocker//8==r//8 and lo<blocker%8<hi)
    return False
def white_attacks(wk,r,t): return adj(wk,t) or (r is not None and rook_attacks(r,t,wk))
p=subprocess.Popen(["java","-jar",JAR],stdin=subprocess.PIPE,stdout=subprocess.PIPE,text=True,bufsize=1)
def cmd(s): p.stdin.write(s+"\n"); p.stdin.flush()
cmd("uci"); 
while "uciok" not in p.stdout.readline(): pass
cmd("isready")
while "readyok" not in p.stdout.readline(): pass
bad=0; n=0
for wk,r,bk in itertools.product(range(64),repeat=3):
    if len({wk,r,bk})<3 or adj(wk,bk): continue
    if white_attacks(wk,r,bk): continue   # black in check with white to move: illegal
    n+=1
    cmd("position fen "+fen(wk,r,bk,"w")); cmd("go")
    line=p.stdout.readline()
    while not line.startswith("bestmove"): line=p.stdout.readline()
    mv=line.split()[1]
    if mv=="0000": print("NULL",fen(wk,r,bk,"w")); bad+=1; continue
    fr=("abcdefgh".index(mv[0]))+(int(mv[1])-1)*8; to=("abcdefgh".index(mv[2]))+(int(mv[3])-1)*8
    if fr==wk: nwk,nr=to,r
    elif fr==r: nwk,nr=wk,to
    else: print("NOPIECE",fen(wk,r,bk,"w"),mv); bad+=1; continue
    # legality: not onto own piece / bk, king not adjacent to bk, rook path clear
    legal = to!=bk and to not in (wk,r) and (fr!=wk or (adj(fr,to) and not adj(to,bk))) and (fr!=r or rook_attacks(r,to,wk))
    hang = adj(bk,nr) and not adj(nwk,nr)
    if not legal or hang:
        bad+=1; print("BAD" if not legal else "HANG", fen(wk,r,bk,"w"), mv)
cmd("quit")
print("positions",n,"bad",bad)
