<#
.SYNOPSIS
    Releases a new version to Maven Central via GitHub Actions.

.DESCRIPTION
    Updates the version in pom.xml and README.md, commits, tags, and pushes.
    The GitHub Actions workflow then builds and publishes to Maven Central.

.PARAMETER Version
    The version to release, e.g. "0.1.0-beta.1", "0.2.0", "1.0.0"

.EXAMPLE
    .\release.ps1 0.1.0-beta.1
#>
param(
    [Parameter(Mandatory=$true, Position=0)]
    [ValidatePattern('^\d+\.\d+\.\d+')]
    [string]$Version
)

$ErrorActionPreference = 'Stop'
$tag = "v$Version"

# --- Preflight checks ---
if (-not (Test-Path 'pom.xml')) {
    Write-Error "pom.xml not found. Run this script from the repository root."
}

$status = git status --porcelain
if ($status) {
    Write-Warning "Working tree has uncommitted changes:`n$status"
    $answer = Read-Host "Continue anyway? (y/N)"
    if ($answer -ne 'y') { exit 1 }
}

$existingTag = git tag -l $tag
if ($existingTag) {
    Write-Error "Tag $tag already exists. Choose a different version."
}

# --- Update version in pom.xml ---
Write-Host "[1/5] Updating pom.xml to $Version ..." -ForegroundColor Cyan
$pom = Get-Content pom.xml -Raw -Encoding UTF8
$pom = $pom -replace '<version>[^<]+</version>([\s\S]*?<packaging>)', "<version>$Version</version>`$1"
[System.IO.File]::WriteAllText("$PWD\pom.xml", $pom, [System.Text.UTF8Encoding]::new($false))

# --- Update version in README.md ---
Write-Host "[2/5] Updating README.md ..." -ForegroundColor Cyan
$readme = Get-Content README.md -Raw -Encoding UTF8
$readme = $readme -replace '(<version>)[^<]+(</version>)', "`${1}$Version`${2}"
$readme = $readme -replace "(implementation\s+'com\.aresstack:mermaid-java:)[^']+'", "`${1}$Version'"
[System.IO.File]::WriteAllText("$PWD\README.md", $readme, [System.Text.UTF8Encoding]::new($false))

# --- Commit ---
Write-Host "[3/5] Committing ..." -ForegroundColor Cyan
git add pom.xml README.md
git commit -m "release $Version"

# --- Tag ---
Write-Host "[4/5] Creating tag $tag ..." -ForegroundColor Cyan
git tag $tag

# --- Push ---
Write-Host "[5/5] Pushing to origin ..." -ForegroundColor Cyan
git push origin HEAD --tags

Write-Host ""
Write-Host "Done! Tag $tag pushed." -ForegroundColor Green
Write-Host "GitHub Actions workflow will now build and publish to Maven Central."
Write-Host "Monitor: https://github.com/aresstack/mermaid-java/actions" -ForegroundColor Yellow

