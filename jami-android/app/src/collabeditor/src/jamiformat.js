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
 * Translation between the shared document and Quill.
 *
 * The two disagree on two points, and only on those two.
 *
 * 1. Attribute names. The document uses short keys, because every one of them
 *    is stored once per character in a CRDT that is replicated to every
 *    participant and kept forever in the conversation's history.
 *
 * 2. Where a paragraph attribute lives. Quill puts a heading, a list marker or
 *    an alignment on the newline that *ends* the line. The document puts it on
 *    every character *of* the line, which is what the desktop client's
 *    QTextDocument can express and what the CRDT can merge attribute by
 *    attribute. Two participants making the same line a heading then converge
 *    instead of each keeping their own newline.
 *
 * Everything else -- the character sequence itself, including the newlines,
 * and therefore every offset -- is identical on both sides. That is what lets
 * a cursor position travel between an Android client and a desktop one without
 * being translated.
 *
 * One asymmetry is deliberate: Quill's document always ends with a newline,
 * the shared one never does. The desktop client's last paragraph has no
 * separator after it either, so the trailing newline is added when reading and
 * dropped when writing.
 */

const INLINE_TO_QUILL = {
    b: 'bold',
    i: 'italic',
    u: 'underline',
    s: 'strike',
    link: 'link',
    w: 'width',
}

const INLINE_TO_JAMI = {
    bold: 'b',
    italic: 'i',
    underline: 'u',
    strike: 's',
    link: 'link',
    width: 'w',
}

const LIST_STYLES = ['bullet', 'ordered']
const ALIGN_STYLES = ['center', 'right', 'justify']

/** Only what a Jami document is allowed to say about a paragraph. */
function normalizeBlock(attrs) {
    const out = {}
    if (!attrs) return out
    const header = parseInt(attrs.header, 10)
    // The desktop client renders headings through a font size adjustment of
    // +3/+2/+1, so it has exactly three levels. A deeper one would arrive there
    // as plain text.
    if (header >= 1 && header <= 3) out.header = header
    if (LIST_STYLES.includes(attrs.list)) out.list = attrs.list
    // Left is the default and is written as the absence of the attribute.
    if (ALIGN_STYLES.includes(attrs.align)) out.align = attrs.align
    return out
}

/*
 * Values are given a type on the way through, not just a name.
 *
 * The desktop client reads the document as JSON: a width arrives there as
 * QJsonValue::toInt(), which answers zero for a string. Quill stores the same
 * width as an HTML attribute and hands it back as "320". So a width that is
 * merely copied across is a width the other clients silently ignore.
 */
function inlineToQuill(attrs) {
    const out = {}
    if (!attrs) return out
    for (const [jami, quill] of Object.entries(INLINE_TO_QUILL)) {
        const v = attrs[jami]
        if (v === undefined || v === null || v === false) continue
        if (jami === 'w') {
            const width = parseInt(v, 10)
            if (width > 0) out[quill] = width
        } else if (jami === 'link') {
            if (typeof v === 'string' && v !== '') out[quill] = v
        } else {
            out[quill] = true
        }
    }
    return out
}

function inlineToJami(attrs) {
    const out = {}
    if (!attrs) return out
    for (const [quill, jami] of Object.entries(INLINE_TO_JAMI)) {
        const v = attrs[quill]
        if (v === undefined || v === null || v === false) continue
        if (quill === 'width') {
            const width = parseInt(v, 10)
            if (width > 0) out[jami] = width
        } else if (quill === 'link') {
            if (typeof v === 'string' && v !== '') out[jami] = v
        } else {
            out[jami] = true
        }
    }
    return out
}

function blockToQuill(attrs) {
    return normalizeBlock(attrs)
}

function blockToJami(attrs) {
    return normalizeBlock(attrs)
}

/**
 * Shared document -> Quill.
 *
 * @param ops   the delta returned by Y.Text.toDelta()
 * @param Delta Quill's Delta constructor
 */
export function jamiToQuill(ops, Delta) {
    const out = new Delta()
    // Attributes seen on the characters of the line being read. The last
    // character to carry one wins, which is what a well-formed document
    // produces anyway: they are set on the whole line at once.
    let block = {}

    const endLine = (newlineAttrs) => {
        out.insert('\n', { ...newlineAttrs, ...block })
        block = {}
    }

    for (const op of ops || []) {
        const attrs = op.attributes || {}
        const inline = inlineToQuill(attrs)
        if (typeof op.insert !== 'string') {
            Object.assign(block, blockToQuill(attrs))
            out.insert(op.insert, inline)
            continue
        }
        const parts = op.insert.split('\n')
        for (let i = 0; i < parts.length; i++) {
            // Every part but the first is preceded by a newline that belonged
            // to this op, so the op's inline attributes are the newline's.
            if (i > 0) endLine(inline)
            if (parts[i] === '') continue
            Object.assign(block, blockToQuill(attrs))
            out.insert(parts[i], inline)
        }
    }
    // Quill requires a document to end with a newline; the shared one has none.
    endLine({})
    return out
}

/**
 * Quill -> shared document.
 *
 * @param delta the document returned by quill.getContents()
 * @param Delta Quill's Delta constructor
 */
export function quillToJami(delta, Delta) {
    const lines = []
    let runs = []
    for (const op of (delta && delta.ops) || []) {
        const attrs = op.attributes || {}
        if (typeof op.insert !== 'string') {
            runs.push({ insert: op.insert, attributes: attrs })
            continue
        }
        const parts = op.insert.split('\n')
        for (let i = 0; i < parts.length; i++) {
            if (i > 0) {
                lines.push({ runs, newline: attrs })
                runs = []
            }
            if (parts[i] !== '') runs.push({ insert: parts[i], attributes: attrs })
        }
    }
    // Quill guarantees a terminating newline, so this only catches a caller
    // handing us something else.
    if (runs.length) lines.push({ runs, newline: null })

    const out = new Delta()
    lines.forEach((line, index) => {
        const block = blockToJami(line.newline)
        for (const run of line.runs) {
            const attrs = { ...inlineToJami(run.attributes), ...block }
            out.insert(run.insert, attrs)
        }
        // An empty line cannot be a heading here: there is no character to hold
        // the attribute. The desktop client refuses the same edit for the same
        // reason, so the two stay in step.
        const last = index === lines.length - 1
        if (line.newline !== null && !last) out.insert('\n', inlineToJami(line.newline))
    })
    return out
}

export { inlineToJami, inlineToQuill, normalizeBlock }
