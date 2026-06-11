# Comments are claims, not facts
- Comments describe the code as it was when they were written; they can be obsolete
- Never base an analysis, a fix or a commit message on a comment alone:
  verify it against the code actually executed on the revision under review
- Minimum verification before relying on a stated flow:
  - trace the real call sites (who calls this, with which arguments)
  - check default parameter values and overload resolution
  - `git log -S`/`git blame` the area to see if behavior changed after the comment
- If a comment contradicts the code, the code is the truth: fix the comment
  in a dedicated commit
- Any behavioral claim in a commit message must be backed by call-site
  tracing, not by comments or by memory of an older version of the codebase

# Precedent
The bootstrap comment in daemon `src/jamidht/conversation.cpp` described a
repository-based DRT bootstrap that had been replaced by presence-based
injection (daemon commit 47ce3de86). Trusting it led to a change built on a
wrong premise, abandoned in review (jami-daemon change 34276).
