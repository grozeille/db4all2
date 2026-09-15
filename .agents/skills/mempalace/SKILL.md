---
name: mempalace-guide
description: >-
  Use this skill to query, index, and manage long-term agent memories and project context using MemPalace.
---

# MemPalace Agent Guide

MemPalace is a local-first, hierarchical memory system (Wings -> Rooms -> Halls -> Drawers) that stores long-term facts and context for AI coding agents.

## Core Workflows

### 1. Ingesting Project Context (Mining)
If the project structure or codebase changes significantly, run the mine command to update the memory base:
```bash
mempalace mine .
```

### 2. Consulting Memory via MCP Tools
MemPalace is configured as an MCP server in this workspace. When connected, the agent automatically exposes custom tools to query or search the local memory palace.
Use these tools to retrieve past developer context, guidelines, or troubleshooting history when stuck.
