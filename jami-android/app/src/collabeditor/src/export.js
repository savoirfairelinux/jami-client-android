/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA.
 */

/*
 * Writing the document out in a format that is not Jami's.
 *
 * A document lives only inside Jami, and the pictures in it live further in
 * still: they are attachments of the conversation, named by an id that means
 * nothing to any other reader. So every format that can hold a picture is
 * given it whole, as a data: URL, and the one that cannot is written without.
 *
 * The bytes are not read here. The page is served over a scheme of its own,
 * which makes it an opaque origin: a canvas it draws an image on is tainted
 * and refuses to hand the pixels back, and its policy forbids fetching them.
 * Both are deliberate -- a document is written by someone else. The pictures
 * are therefore left as `jami-attachment:<id>`, and the application, which
 * holds the bytes already, puts them in.
 *
 * Rendering is shared: the delta is walked once into lines of formatted runs,
 * and each format is a way of writing those down.
 */

import { normalizeBlock } from './jamiformat.js'

/**
 * What an unresolved picture is written as, for the application to replace.
 *
 * The tail is drawn afresh for every export. The document's text belongs to
 * whoever writes it, and one of them writing `jami-attachment:<id>` as ordinary
 * words would otherwise have a picture's bytes spliced into their sentence: a
 * placeholder that cannot be guessed cannot be written by hand.
 */
export function attachmentScheme() {
    // Nothing stands in for the platform's own randomness here: a name drawn
    // from a source somebody could follow is a name they could write, which is
    // the whole of what this guards against.
    if (!globalThis.crypto || !globalThis.crypto.getRandomValues) {
        throw new Error('no source of randomness to name pictures under')
    }
    const bytes = new Uint8Array(16)
    globalThis.crypto.getRandomValues(bytes)
    return 'jami-attachment-'
        + Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('') + ':'
}

const INLINE_KEYS = ['bold', 'italic', 'underline', 'strike', 'link']

/** Only what a Jami document is allowed to say about a stretch of characters. */
function inlineOf(attrs) {
    const out = {}
    if (!attrs) return out
    for (const key of INLINE_KEYS) {
        const value = attrs[key]
        if (value === undefined || value === null || value === false) continue
        if (key === 'link') {
            if (typeof value === 'string' && value !== '') out.link = value
        } else {
            out[key] = true
        }
    }
    return out
}

/**
 * The document as lines, each holding its paragraph attributes and the runs
 * that make it up.
 *
 * Quill puts a heading, a list marker or an alignment on the newline that ends
 * a line, and inline attributes on the characters themselves. An op carrying
 * both is read for both rather than guessed at, which is why each side is
 * filtered to the keys that belong to it.
 */
export function linesOf(delta) {
    const lines = []
    let runs = []
    const push = (attributes) => {
        lines.push({ attributes: normalizeBlock(attributes), runs })
        runs = []
    }
    for (const op of (delta && delta.ops) || []) {
        const insert = op.insert
        if (typeof insert !== 'string') {
            const image = insert && insert.image
            if (image) {
                const attrs = op.attributes || {}
                const width = parseInt(attrs.width, 10)
                runs.push({
                    image: typeof image === 'string' ? { id: image } : image,
                    width: width > 0 ? width : 0,
                })
            }
            continue
        }
        const inline = inlineOf(op.attributes)
        let rest = insert
        for (;;) {
            const stop = rest.indexOf('\n')
            if (stop === -1) {
                if (rest) runs.push({ text: rest, attributes: inline })
                break
            }
            if (stop > 0) runs.push({ text: rest.slice(0, stop), attributes: inline })
            push(op.attributes)
            rest = rest.slice(stop + 1)
        }
    }
    // Quill's document always ends with a newline, so anything left here is a
    // last line that the caller built without one.
    if (runs.length) push(null)
    return lines
}

/* -------------------------------------------------------------------- html */

function escapeHtml(text) {
    return text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
}

function htmlRun(run) {
    if (run.image) {
        const width = run.width > 0 ? ` width="${run.width}"` : ''
        return `<img src="${escapeHtml(run.src)}"${width}>`
    }
    let out = escapeHtml(run.text)
    const attrs = run.attributes
    if (attrs.bold) out = `<strong>${out}</strong>`
    if (attrs.italic) out = `<em>${out}</em>`
    if (attrs.underline) out = `<u>${out}</u>`
    if (attrs.strike) out = `<s>${out}</s>`
    if (attrs.link) out = `<a href="${escapeHtml(attrs.link)}">${out}</a>`
    return out
}

function htmlLine(line) {
    const body = line.runs.map(htmlRun).join('')
    const style = line.attributes.align ? ` style="text-align: ${line.attributes.align}"` : ''
    if (line.attributes.list) return `<li${style}>${body}</li>`
    if (line.attributes.header) {
        const level = line.attributes.header
        return `<h${level}${style}>${body}</h${level}>`
    }
    // An empty paragraph collapses to nothing without something to hold it open.
    return `<p${style}>${body || '<br>'}</p>`
}

function toHtml(lines, title) {
    const body = []
    let list = null
    for (const line of lines) {
        const wanted = line.attributes.list
            ? (line.attributes.list === 'ordered' ? 'ol' : 'ul')
            : null
        if (wanted !== list) {
            if (list) body.push(`</${list}>`)
            if (wanted) body.push(`<${wanted}>`)
            list = wanted
        }
        body.push(htmlLine(line))
    }
    if (list) body.push(`</${list}>`)
    return [
        '<!DOCTYPE html>',
        '<html>',
        '<head>',
        '<meta charset="utf-8">',
        `<title>${escapeHtml(title || '')}</title>`,
        '</head>',
        '<body>',
        body.join('\n'),
        '</body>',
        '</html>',
        '',
    ].join('\n')
}

/* ---------------------------------------------------------------- markdown */

/*
 * Only the characters that would otherwise start something are escaped, and
 * only where they would: a backslash before every possible marker turns
 * ordinary prose into a thicket.
 *
 * @param start whether this text begins its line, the markers that open a
 *              block meaning nothing anywhere else.
 */
function escapeMarkdown(text, start) {
    const out = text.replace(/([\\`*_[\]])/g, '\\$1')
    if (!start) return out
    return out
        .replace(/^(\s*)([#>\-+=~])/, '$1\\$2')
        // A number and a dot or a bracket open a list. Escaping the delimiter
        // leaves the number reading as it was written.
        .replace(/^(\s*\d{1,9})([.)])(?=\s|$)/, '$1\\$2')
}

/*
 * A destination is written between parentheses, so one holding a space or a
 * parenthesis of its own would end the link early and spill the rest of the
 * address into the text as prose. The bracketed form takes both.
 */
function markdownUrl(url) {
    const clean = url.replace(/[\u0000-\u0020\u007f]+/g, ' ').trim()
    if (!/[ ()<>]/.test(clean)) return clean
    return `<${clean.replace(/</g, '%3C').replace(/>/g, '%3E')}>`
}

function markdownRun(run, start) {
    if (run.image) return `![](${markdownUrl(run.src)})`
    let out = escapeMarkdown(run.text, start)
    const attrs = run.attributes
    // Markdown has no underline, and inventing one as raw HTML would leave a
    // tag in a file meant to be read as it is.
    if (attrs.bold) out = `**${out}**`
    if (attrs.italic) out = `*${out}*`
    if (attrs.strike) out = `~~${out}~~`
    if (attrs.link) out = `[${out}](${markdownUrl(attrs.link)})`
    return out
}

function toMarkdown(lines) {
    const out = []
    let ordinal = 0
    for (const line of lines) {
        const body = line.runs.map((run, i) => markdownRun(run, i === 0)).join('')
        const attrs = line.attributes
        if (attrs.list === 'ordered') {
            ordinal += 1
            out.push(`${ordinal}. ${body}`)
            continue
        }
        ordinal = 0
        if (attrs.list) {
            out.push(`- ${body}`)
        } else if (attrs.header) {
            out.push(`${'#'.repeat(attrs.header)} ${body}`)
        } else {
            out.push(body)
        }
    }
    // A paragraph ends at a blank line, so the lines are separated by one.
    return out.join('\n\n') + '\n'
}

/* -------------------------------------------------------------- plain text */

function toPlainText(lines) {
    // A picture has nowhere to go, and is taken out rather than left as the one
    // character it occupies, which would be written as a stray glyph.
    return lines
        .map((line) => line.runs.filter((run) => !run.image).map((run) => run.text).join(''))
        .join('\n') + '\n'
}

/* ------------------------------------------------------------------ export */

const FORMATS = {
    html: toHtml,
    md: toMarkdown,
    txt: toPlainText,
}

/**
 * The document written in @p format, and the pictures the application still
 * has to put into it.
 *
 * @param without ids of pictures to leave out, for those whose bytes never
 *        arrived: written as a reference nobody can follow, they would be a
 *        hole in a file that is supposed to stand on its own.
 * @return { text, attachments, scheme } where attachments lists, once each and
 *         in the order they appear, the ids left to be put in, and scheme is
 *         what they are written under, this time; or null if the format is not
 *         one of "html", "md" or "txt".
 */
export function exportDocument(delta, format, title, without) {
    const write = FORMATS[format]
    if (!write) return null
    const dropped = without || []
    const scheme = attachmentScheme()
    const lines = linesOf(delta).map((line) => ({
        attributes: line.attributes,
        runs: line.runs
            // A picture with no id is left out like one whose bytes never
            // arrived, and without asking: there is nothing to ask for. Kept,
            // it would be written under a name the application cannot be told
            // about, and go out as an address no reader can follow.
            .filter((run) => !run.image
                || (typeof run.image.id === 'string' && run.image.id !== ''
                    && !dropped.includes(run.image.id)))
            .map((run) => (run.image
                ? { ...run, src: scheme + encodeURIComponent(run.image.id) }
                : run)),
    }))
    const attachments = []
    if (format !== 'txt') {
        for (const line of lines)
            for (const run of line.runs) {
                if (!run.image) continue
                if (!attachments.includes(run.image.id)) attachments.push(run.image.id)
            }
    }
    return { text: write(lines, title), attachments, scheme }
}
