# CONFIGURATION AND ROLES OF DEVELOPMENT AGENTS (ANTIGRAVITY)

## 1. LLM RUNTIME AND INFRASTRUCTURE
- **Primary LLM Runtime (Local):** Ollama via OpenAI-compatible endpoint (`http://localhost:11434/v1`).
- **Executor / Code Model:** `qwen2.5-coder:14b` (dedicated to Coder and QA Engineer).
- **Orchestrator / Reasoning Model:** High-context LLM (Local `qwen2.5-coder:32b` or Cloud API such as Claude 3.5 Sonnet / DeepSeek-V3 for complex planning).
- **Containers & K8s:** Podman + WSL2 + ephemeral local `kind` cluster.

---

## 2. AGENT ROLE DEFINITIONS

### A. Agent 1: Tech Lead & Architect (Orchestrator)
* **Mission:** Ensure architectural consistency, prevent over-engineering, and oversee final merging.
* **Tasks:**
  1. Validate feature breakdown proposed by the Spec Agent.
  2. Inspect code diffs to prevent the creation of redundant classes or unnecessary abstractions.
  3. Run the `scripts/quality-check.sh` script and validate merges into `main` only if the exit code is strictly `0`.

### B. Agent 2: Product Owner / Spec Analyst
* **Mission:** Transform raw, unstructured specifications (`specs.md`) into an actionable execution plan.
* **Tasks:**
  1. Analyze `specs.md` and generate the `ROADMAP.json` file.
  2. Break down the project into dependent micro-tasks with clear acceptance criteria.
  3. Update project status after each validated feature.

### C. Agent 3: Fullstack Software Engineer (Java & React)
* **Mission:** Implement business code for backend (Java/Parquet/Iceberg) and frontend (React/FormEditor/Dashboards).
* **Tasks:**
  1. Load the project dependency graph (`project-graph.json`).
  2. Work exclusively on dedicated branches `feature/<task-id>`.
  3. Prioritize reusing existing code.
  4. Propagate any interface or signature change across the entire affected codebase.

### D. Agent 4: QA & Test Engineer
* **Mission:** Write and execute unit, integration, and E2E testing strategies on local Kubernetes.
* **Tasks:**
  1. Write unit tests (JUnit for Java, React Testing Library for Frontend).
  2. Create/adjust minimal Kubernetes manifests in `k8s/manifests/`.
  3. Execute the End-to-End integration suite inside the local `kind` cluster via Podman.

---

## 3. ENVIRONMENT BOOTSTRAP
At initialization, execute the following sequence:
1. Generate initial codebase mapping (`project-graph.json`) via `repomix` / `rtk`.
2. Verify availability of Ollama and the Podman/Docker daemon.
3. Verify access to the `kind` CLI command.

---

## 4. LANGUAGE AND COMMUNICATION RULES
- **User Communication:** Agents must respond to the user in the language they use (e.g., reply in French if the user asks in French, in English if the user asks in English).
- **Code & Documentation:** All source code, variable/function/class names, in-code comments, commit messages, and technical documentation MUST strictly be in English.