# DIRECTIVES DE PROJET ET WORKFLOW AUTONOME - DB4ALL2

## CONTEXTE & VISION PROJET
**db4all2** est un outil de manipulation de données visant à remplacer Excel/Access/SharePoint List :
- **Backend :** Java avec support natif des formats Parquet et Apache Iceberg (saisie, import CSV/Excel, catalogue de tables, vues, filtres, jointures, agrégations). Exposition de vues comme sources OData pour Excel.
- **Frontend :** React avec éditeur de formulaires (type FormIO), grille de saisie type Excel, et modules de TCD (Pivot Tables) / Graphiques / Dashboards intégrables.

---

## 1. RÈGLES DE DÉVELOPPEMENT STRICTES (ANTI-DEGRADATION)
1. **ZÉRO CODE REDONDANT :** Il est strictement interdit de créer une nouvelle classe Java, un nouveau composant React ou un module utilitaire si un composant existant peut être étendu.
2. **ANALYSE D'IMPACT OBLIGATOIRE :** Avant chaque modification, analyse le graphe des dépendances (`project-graph.json`). Si la signature d'une fonction/méthode change, tu dois mettre à jour TOUS ses appelants dans le projet.
3. **CODE SOBRE :** Pas de pattern de conception sur-ingéniéré (ex: pas d'AbstractFactoryProvider s'il n'y a qu'une seule implémentation).
4. **ISOLATION :** Tout le travail de dev s'effectue sur une branche locale isolée `feature/<task-id>`. Aucun commit direct sur `main`.

---

## 2. WORKFLOW D'EXÉCUTION AUTONOME (BOUCLE D'AGENT)

1. Analyse : specs.md ➔ (Spec Agent) ➔ Génération de ROADMAP.json
2. Branchement : Creation et checkout de feature/<task-id>
3. Implémentation : Dev Code & Écriture des tests Unitaires/E2E
4. Validation : Exécution du gatekeeper scripts/quality-check.sh
5. Boucle de correction :
   - En cas d'échec (<= 3 essais) : Correction ciblée par le Coder Agent et nouvelle tentative.
   - En cas de succès (code 0) : Merge automatique sur main sans Fast-Forward (git merge --no-ff) et passage à la tâche suivante.
---

## 4. SCRIPT DE QUALITÉ LOCAL & GATEKEEPER (`scripts/quality-check.sh`)

Ce script est exécuté par le Tech Lead / QA Agent pour valider de manière automatisée chaque branche avant merge :

```bash
#!/usr/bin/env bash
set -e

echo "=== 1. ANALYSE STATIQUE ET TESTS UNITAIRES (BACKEND) ==="
mvn clean compile test spotbugs:check pmd:check

echo "=== 2. ANALYSE STATIQUE ET TESTS UNITAIRES (FRONTEND) ==="
cd frontend
npm run lint
npx tsc --noEmit
npm test -- --watchAll=false
cd ..

echo "=== 3. DEPLOIEMENT & TEST E2E SUR KUBERNETES EPHEMERE (KIND) ==="
KIND_CLUSTER_NAME="db4all2-e2e-check"

# Nettoyage d'un éventuel cluster résiduel
kind delete cluster --name ${KIND_CLUSTER_NAME} || true
kind create cluster --name ${KIND_CLUSTER_NAME} --config k8s/kind-config.yaml

# Destruction automatique du cluster à la fin de l'exécution
trap "kind delete cluster --name ${KIND_CLUSTER_NAME}" EXIT

# Build des images conteneurisées via Podman
podman build -t db4all2-backend:test ./backend
podman build -t db4all2-frontend:test ./frontend

# Ingestion des images dans le cluster Kind local
kind load docker-image db4all2-backend:test --name ${KIND_CLUSTER_NAME}
kind load docker-image db4all2-frontend:test --name ${KIND_CLUSTER_NAME}

# Application des manifestes Kubernetes
kubectl apply -f k8s/manifests/
kubectl rollout status deployment/backend-deployment --timeout=120s
kubectl rollout status deployment/frontend-deployment --timeout=120s

# Lancement des tests d'intégration End-to-End
npm run test:e2e

echo "=== VÉRIFICATION COMPLÈTE EFFECTUÉE AVEC SUCCÈS ==="