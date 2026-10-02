import { build } from 'esbuild'
import { mkdirSync, copyFileSync, readdirSync } from 'node:fs'
import { basename, dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

import { DOCUMENT_FONTS, FONT_STYLES, fontFile } from './src/fonts.js'

const here = dirname(fileURLToPath(import.meta.url))

// Gradle passes the directory it has declared as an asset source, so that the
// bundle is a build product rather than something committed next to what it is
// built from.
const flag = process.argv.indexOf('--outdir')
const outdir = flag === -1
    ? resolve(here, 'build/collab')
    : resolve(process.argv[flag + 1], 'collab')

mkdirSync(outdir, { recursive: true })

await build({
    entryPoints: [resolve(here, 'src/index.js')],
    bundle: true,
    outfile: resolve(outdir, 'editor.js'),
    format: 'iife',
    // Every WebView the application runs on is a modern Chromium, so there is
    // nothing to transpile down to.
    target: ['chrome100'],
    minify: true,
    sourcemap: false,
    legalComments: 'external',
    logLevel: 'info',
})

copyFileSync(resolve(here, 'editor.html'), resolve(outdir, 'editor.html'))

// The fonts a document may name, which the page loads from next to itself.
// Their licenses travel with them, as the SIL Open Font License asks.
const fonts = resolve(outdir, 'fonts')
mkdirSync(fonts, { recursive: true })
for (const font of DOCUMENT_FONTS) {
    for (const style of FONT_STYLES) {
        const file = basename(fontFile(font, style))
        copyFileSync(resolve(here, 'fonts', file), resolve(fonts, file))
    }
}
for (const license of readdirSync(resolve(here, 'fonts')).filter((name) => name.endsWith('.txt'))) {
    copyFileSync(resolve(here, 'fonts', license), resolve(fonts, license))
}
