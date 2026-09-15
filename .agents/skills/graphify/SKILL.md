---
name: graphify-guide
description: >-
  Use this skill to query, navigate, and analyze the codebase structure using Graphify's AST-based knowledge graph.
  Also use this skill to run graphify reflect and document lessons learned.
---

# Graphify Agent Guide

Graphify compiles and queries AST-based representations of the codebase to help you reason about project structures, dependencies, and code patterns efficiently.

## Core Workflows

### 1. Generating / Updating the Graph
To recompile the knowledge graph for the current repository:
```bash
graphify .
```

### 2. Querying Codebase structure
To search for files, relationships, or classes without scanning files manually:
```bash
graphify query "Find all occurrences of OData controller endpoints"
```

### 3. Recording Feedback & Lessons Learned
Graphify tracks helpful vs failed attempts and crystallizes them into deterministic lessons:
1. Save an outcome or troubleshooting resolution:
```bash
graphify save-result --question "<issue or question>" --answer "<solution or lesson>" --outcome useful
```
*(Outcomes: `useful`, `dead_end`, `corrected`)*

2. Aggregate all recorded memories into `graphify-out/reflections/LESSONS.md`:
```bash
graphify reflect
```
