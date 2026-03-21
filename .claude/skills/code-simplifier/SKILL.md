---
name: code-simplifier
description: Review changed code for reuse, quality, and efficiency, then fix any issues found. Use when code needs cleanup, refactoring, or quality review.
disable-model-invocation: false
allowed-tools: Read, Grep, Glob, Edit, Write, Bash, Agent
argument-hint: [file-or-directory]
---

# Code Simplifier

Review and improve code quality across the project or specified files.

## Steps

1. **Identify recent changes** — check `git diff` and `git log` for recently modified files
2. **Analyze code quality** — look for:
   - Duplicated logic that can be extracted
   - Overly complex methods (high cyclomatic complexity)
   - Unused imports, variables, or dead code
   - Inconsistent naming or patterns
   - Potential null pointer or exception handling issues
3. **Check efficiency** — identify:
   - N+1 query problems
   - Unnecessary object creation
   - Inefficient collection operations
   - Missing database indexes (based on query patterns)
4. **Review reuse opportunities** — find repeated patterns that could be shared utilities
5. **Implement fixes** — apply improvements while preserving existing behavior

If $ARGUMENTS is provided, focus analysis on that file or directory.
Otherwise, analyze recently changed files.
