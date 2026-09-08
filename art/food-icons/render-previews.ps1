$ErrorActionPreference = 'Stop'
node (Join-Path $PSScriptRoot 'render-and-check.mjs')
if ($LASTEXITCODE -ne 0) {
    throw '图标渲染或边界检查失败，请查看 validation-results.json。'
}
