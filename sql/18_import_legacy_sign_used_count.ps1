<#!
One-time pre-cutover importer for the legacy Redis authority hash.

It preserves the old total `usedCount` in `sign_supplement_legacy_used`. Runtime
uses max(detail-derived count, imported total), so a historical Redis-first/MQ
failure cannot restore a consumed supplement quota. Run after sql/17 and before
enabling the MySQL-authoritative signup service. The script is idempotent.
#>
[CmdletBinding()]
param(
    [string]$RedisContainer = 'simlect-redis',
    [string]$MySqlContainer = 'simlect-mysql',
    [string]$MySqlUser = 'root',
    [Parameter(Mandatory = $true)]
    [string]$MySqlPassword
)

$keyPrefix = 'mall:sign:userId:'
$keys = & docker exec $RedisContainer redis-cli --scan --pattern "$keyPrefix*"
if ($LASTEXITCODE -ne 0) {
    throw "无法扫描 Redis 容器 $RedisContainer 中的历史签到 Hash"
}

$imported = 0
foreach ($key in $keys) {
    if (-not $key.StartsWith($keyPrefix)) {
        continue
    }
    $userId = $key.Substring($keyPrefix.Length)
    if ($userId -notmatch '^[A-Za-z0-9_-]{1,32}$') {
        throw "签到 Redis key 包含不安全 userId，已中止：$key"
    }
    $usedCountRaw = & docker exec $RedisContainer redis-cli HGET $key usedCount
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($usedCountRaw)) {
        continue
    }
    $usedCount = 0
    if (-not [int]::TryParse($usedCountRaw, [ref]$usedCount) -or $usedCount -lt 0) {
        throw "Redis usedCount 非法，已中止：key=$key value=$usedCountRaw"
    }
    if ($usedCount -eq 0) {
        continue
    }
    $statement = "INSERT INTO sign_supplement_legacy_used " +
        "(user_id, used_count, detail_count_at_cutover, source, update_time) " +
        "SELECT '$userId', GREATEST($usedCount, COUNT(1)), COUNT(1), 'redis_snapshot', NOW() " +
        "FROM sign_supplement_used WHERE user_id='$userId' " +
        "ON DUPLICATE KEY UPDATE " +
        "source=IF(VALUES(used_count)>=sign_supplement_legacy_used.used_count, " +
        "VALUES(source), sign_supplement_legacy_used.source), " +
        "detail_count_at_cutover=sign_supplement_legacy_used.detail_count_at_cutover, " +
        "used_count=GREATEST(sign_supplement_legacy_used.used_count, VALUES(used_count)), update_time=NOW();"
    & docker exec $MySqlContainer mysql "-u$MySqlUser" "-p$MySqlPassword" simlect_user -e $statement
    if ($LASTEXITCODE -ne 0) {
        throw "导入 Redis usedCount 失败：userId=$userId"
    }
    $imported++
}

Write-Host "已导入 $imported 个历史 Redis usedCount 快照。请执行 sql/18_verify_legacy_sign_used_count.sql 后再切流。"
