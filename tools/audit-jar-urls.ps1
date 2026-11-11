param([string]$Jar = 'komga/build/libs/komga-1.30.0-plain.jar')

Add-Type -AssemblyName System.IO.Compression.FileSystem
$path = (Resolve-Path $Jar).Path
$zip = [System.IO.Compression.ZipFile]::OpenRead($path)

$hits = @{}
foreach ($entry in $zip.Entries) {
  if ($entry.FullName -match '\.(js|mjs|html|json|css)$' -and $entry.Length -lt 8000000) {
    $reader = New-Object System.IO.StreamReader($entry.Open())
    $text = $reader.ReadToEnd()
    $reader.Close()
    foreach ($m in [regex]::Matches($text, 'https?://[A-Za-z0-9._~:/?#@\!\$\&\*\+\,;\=%\-]{4,120}')) {
      $u = $m.Value
      # trim trailing punctuation that is commonly part of surrounding code
      $u = $u.TrimEnd('\', '"', "'", ',', ')', '}', ';', '.')
      if (-not $hits.ContainsKey($u)) { $hits[$u] = New-Object System.Collections.ArrayList }
      if ($hits[$u] -notcontains $entry.Name) { [void]$hits[$u].Add($entry.Name) }
    }
  }
}
$zip.Dispose()

Write-Output ("DISTINCT URLS: " + $hits.Count)
$sorted = $hits.GetEnumerator() | Sort-Object -Property @{Expression = { $_.Value.Count }; Descending = $true }, @{Expression = { $_.Key }}
$i = 0
foreach ($kv in $sorted) {
  $i++
  if ($i -gt 60) { break }
  $files = ($kv.Value | Select-Object -First 3) -join ','
  Write-Output ("{0,3}  {1}  <= {2}" -f $kv.Value.Count, $kv.Key, $files)
}
