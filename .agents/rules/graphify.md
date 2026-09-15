# Codebase Navigation and Graphify Rules

- **Use Graphify for Code Navigation:** Before running grep search or listing directories recursively, run `graphify query` to locate specific classes, interfaces, or functions. This is much faster and token-efficient.
- **Update the Graph:** Whenever you make significant changes to signatures or dependencies, run `graphify .` to update the AST graph.
- **Log Lessons Learnt:** When you debug a complex error or receive feedback from the compiler/tests, run `graphify save-result --question "<issue>" --answer "<solution>" --outcome useful` followed by `graphify reflect` to record it in `graphify-out/reflections/LESSONS.md` so that future agent invocations can benefit from it.
