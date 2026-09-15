# Bootstrap Agents Environment Script for Windows/PowerShell
$ErrorActionPreference = "Stop"

$workspaceRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$binDir = Join-Path $workspaceRoot ".bin"
$agentsDir = Join-Path $workspaceRoot ".agents"

Write-Host "=== 1. INSTALLING AI PRODUCTIVITY TOOLS ===" -ForegroundColor Cyan

# Install graphifyy using uv with sql support
Write-Host "Installing Graphify via uv..."
uv tool install --force "graphifyy[sql]"

# Install mempalace using uv
Write-Host "Installing MemPalace via uv..."
uv tool install mempalace

# Download RTK (Rust Token Killer)
if (-not (Test-Path $binDir)) {
    New-Item -ItemType Directory -Path $binDir | Out-Null
}

$rtkZip = Join-Path $binDir "rtk.zip"
$rtkExe = Join-Path $binDir "rtk.exe"

if (-not (Test-Path $rtkExe)) {
    Write-Host "Downloading RTK (Rust Token Killer) v0.45.0..."
    $rtkUrl = "https://github.com/rtk-ai/rtk/releases/download/v0.45.0/rtk-x86_64-pc-windows-msvc.zip"
    Invoke-WebRequest -Uri $rtkUrl -OutFile $rtkZip
    
    Write-Host "Extracting RTK..."
    Expand-Archive -Path $rtkZip -DestinationPath $binDir -Force
    Remove-Item $rtkZip
    Write-Host "RTK installed successfully in .bin/rtk.exe" -ForegroundColor Green
} else {
    Write-Host "RTK is already downloaded in .bin/rtk.exe" -ForegroundColor Green
}

Write-Host "`n=== 2. INITIALIZING TOOL DATA ===" -ForegroundColor Cyan

# Generate project-graph.json via repomix
Write-Host "Generating codebase map (project-graph.json)..."
npx.cmd --yes repomix --style json -o "$workspaceRoot\project-graph.json" --no-files

# Initialize MemPalace
Write-Host "Initializing MemPalace..."
uv tool run mempalace init "$workspaceRoot"

# Build Graphify graph with code-only to avoid requiring an LLM API key
Write-Host "Building Graphify knowledge graph..."
uv tool run --from graphifyy graphify "$workspaceRoot" --code-only

Write-Host "`n=== 3. VERIFYING DESKTOP PLATFORM RUNTIMES ===" -ForegroundColor Cyan

# Check Ollama
try {
    $ollamaCheck = & ollama list 2>&1
    Write-Host "[OK] Ollama is running and responding." -ForegroundColor Green
} catch {
    Write-Host "[WARNING] Ollama check failed. Please ensure Ollama service is running." -ForegroundColor Yellow
}

# Check Podman
try {
    $podmanCheck = & podman version 2>&1
    Write-Host "[OK] Podman is installed and responding." -ForegroundColor Green
} catch {
    Write-Host "[WARNING] Podman check failed. Please ensure Podman Desktop / VM is running." -ForegroundColor Yellow
}

# Check kind
try {
    $kindCheck = & kind --version 2>&1
    Write-Host "[OK] kind is installed and responding." -ForegroundColor Green
} catch {
    Write-Host "[WARNING] kind is not installed or not in PATH." -ForegroundColor Yellow
}

Write-Host "`n=== AGENT SETUP COMPLETE ===" -ForegroundColor Green
Write-Host "All configured tools are now ready for coding agents." -ForegroundColor Green
