/**
 * 构建前同步清空 dist，并**自校验**是否真的清空了。
 *
 * 为什么不是只靠 vite.config.ts 里的 `build.emptyOutDir: true`：
 * Vite 内部用 `fs.rmSync(path, { recursive: true, force: true })` 删旧产物。
 * 实测在受限执行环境（例如 AI 沙箱/带删除拦截的安全软件）里，Node 的这个调用会
 * **不抛异常也不删除**——Vite 于是遍历一圈等于没删，旧 chunk 原地留着：
 * dist 文件数 459 → 488，assets 里同时存在两代 SettingView-*.js / *.css。
 * （同一目录改用 .NET Directory.Delete 或 PowerShell Remove-Item 都能删掉，
 *   所以这不是权限问题，而是 Node 删除调用被静默吞掉了。）
 *
 * 因此这里做两件事：
 *   1. 先试 Node 自己删；
 *   2. 删不掉就回退到系统自带删除命令（Windows rmdir /s /q，其它平台 rm -rf）；
 *   3. 仍然没删掉就直接退出码 1 报错——宁可构建失败，也不要静默产出污染目录。
 */
import { spawnSync } from 'node:child_process'
import { existsSync, rmSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const projectRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const outDir = resolve(projectRoot, 'dist')

if (existsSync(outDir)) {
  rmSync(outDir, { recursive: true, force: true })

  if (existsSync(outDir)) {
    console.warn('[clean-dist] Node 的 fs.rmSync 静默失效（受限环境的已知现象），回退系统命令')
    const [bin, args] =
      process.platform === 'win32'
        ? ['cmd', ['/c', 'rmdir', '/s', '/q', outDir]]
        : ['rm', ['-rf', outDir]]
    const { status, error } = spawnSync(bin, args, { stdio: 'inherit' })

    if (error || (status !== 0 && existsSync(outDir)) || existsSync(outDir)) {
      console.error(`[clean-dist] 清空失败，请手动删除后重试：${outDir}`)
      process.exit(1)
    }
  }

  console.log(`[clean-dist] 已清空 ${outDir}`)
}
