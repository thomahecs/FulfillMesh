# Upload FulfillMesh to GitHub

This handoff deliberately excludes `.git`, `node_modules`, `target`, database
volumes, and `.env` credentials. It contains the source and reproducible
benchmark scripts needed to start a fresh Git repository.

1. Create an empty **private or public** GitHub repository named `FulfillMesh`
   without generating a new README or .gitignore in GitHub.
2. From Terminal after unzipping the project:

```bash
cd ~/Desktop/CareerProject/FulfillMesh
git init
git branch -M main
git add .
git status
git commit -m "Complete FulfillMesh local stack and benchmarks"
git remote add origin https://github.com/YOUR_USERNAME/FulfillMesh.git
git push -u origin main
```

Replace `YOUR_USERNAME` with your GitHub username. Do not commit `.env` files,
cloud access keys, Docker volumes, or machine-specific build output. Before
publishing the resume's p95 and coverage measurements, run
`./scripts/verify-all.sh` on your machine and verify the generated reports.

`reports/routing-benchmark.json` is an actual generated benchmark artifact.
`reports/concurrency-500.json` is deliberately absent until the PostgreSQL
load test has actually run.
