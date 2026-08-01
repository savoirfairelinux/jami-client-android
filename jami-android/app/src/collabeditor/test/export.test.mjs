/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 */

/*
 * What leaves this device has to stand on its own: it is read by something
 * that is not Jami, on a machine that cannot ask the conversation for
 * anything. These check the two ways that fails -- a picture written as a
 * reference nobody can follow, and a format written with markers the reader
 * will take for its own.
 */

import test from 'node:test'
import assert from 'node:assert/strict'
import Delta from 'quill-delta'

import { exportDocument, linesOf, ATTACHMENT_SCHEME } from '../src/export.js'

const write = (ops, format, title) => exportDocument(new Delta(ops), format, title).text
const pictures = (ops, format) => exportDocument(new Delta(ops), format).attachments

test('a format this cannot write is refused rather than half written', () => {
    assert.equal(exportDocument(new Delta([{ insert: 'a\n' }]), 'odt'), null)
    assert.equal(exportDocument(new Delta([{ insert: 'a\n' }]), 'pdf'), null)
})

test('a paragraph attribute is read from the newline that ends the line', () => {
    const lines = linesOf(new Delta([
        { insert: 'Title' },
        { insert: '\n', attributes: { header: 1 } },
        { insert: 'body\n' },
    ]))
    assert.equal(lines.length, 2)
    assert.deepEqual(lines[0].attributes, { header: 1 })
    assert.deepEqual(lines[1].attributes, {})
})

test('an inline attribute on the op that carries the newline stays inline', () => {
    // The two live on the same op when a line is entirely bold.
    const lines = linesOf(new Delta([{ insert: 'all\n', attributes: { bold: true } }]))
    assert.equal(lines.length, 1)
    assert.deepEqual(lines[0].attributes, {})
    assert.deepEqual(lines[0].runs, [{ text: 'all', attributes: { bold: true } }])
})

test('html states its encoding, so accents survive the trip', () => {
    const html = write([{ insert: 'héllo\n' }], 'html', 'Nôtes')
    assert.match(html, /<meta charset="utf-8">/)
    assert.match(html, /<title>Nôtes<\/title>/)
    assert.match(html, /<p>héllo<\/p>/)
})

test('html marks up everything a document can say', () => {
    const html = write([
        { insert: 'b', attributes: { bold: true } },
        { insert: 'i', attributes: { italic: true } },
        { insert: 'u', attributes: { underline: true } },
        { insert: 's', attributes: { strike: true } },
        { insert: 'l', attributes: { link: 'https://jami.net' } },
        { insert: '\n' },
    ], 'html')
    assert.match(html, /<strong>b<\/strong>/)
    assert.match(html, /<em>i<\/em>/)
    assert.match(html, /<u>u<\/u>/)
    assert.match(html, /<s>s<\/s>/)
    assert.match(html, /<a href="https:\/\/jami\.net">l<\/a>/)
})

test('html closes a list before opening the next kind', () => {
    const html = write([
        { insert: 'one' },
        { insert: '\n', attributes: { list: 'bullet' } },
        { insert: 'two' },
        { insert: '\n', attributes: { list: 'ordered' } },
        { insert: 'after\n' },
    ], 'html')
    assert.match(html, /<ul>\n<li>one<\/li>\n<\/ul>\n<ol>\n<li>two<\/li>\n<\/ol>\n<p>after<\/p>/)
})

test('text a reader would take for markup is escaped', () => {
    assert.match(write([{ insert: '<script>\n' }], 'html'), /&lt;script&gt;/)
    assert.match(write([{ insert: 'a*b\n' }], 'md'), /a\\\*b/)
    // A line that opens with one would otherwise become a heading or an item.
    assert.match(write([{ insert: '# not a heading\n' }], 'md'), /\\# not a heading/)
})

test('markdown writes what markdown has, and leaves out what it has not', () => {
    const md = write([
        { insert: 'b', attributes: { bold: true } },
        { insert: 'u', attributes: { underline: true } },
        { insert: '\n' },
    ], 'md')
    assert.match(md, /\*\*b\*\*/)
    // No underline in markdown, and no tag left behind pretending there is.
    assert.match(md, /\*\*b\*\*u/)
})

test('markdown numbers an ordered list and starts over after it', () => {
    const md = write([
        { insert: 'one' },
        { insert: '\n', attributes: { list: 'ordered' } },
        { insert: 'two' },
        { insert: '\n', attributes: { list: 'ordered' } },
        { insert: 'apart\n' },
        { insert: 'again' },
        { insert: '\n', attributes: { list: 'ordered' } },
    ], 'md')
    assert.match(md, /1\. one/)
    assert.match(md, /2\. two/)
    assert.match(md, /1\. again/)
})

test('a picture is named for the application to put in, once per id', () => {
    const ops = [
        { insert: { image: { id: 'aa' } } },
        { insert: { image: { id: 'aa' } } },
        { insert: { image: { id: 'bb' } }, attributes: { width: 320 } },
        { insert: '\n' },
    ]
    assert.deepEqual(pictures(ops, 'html'), ['aa', 'bb'])
    const html = write(ops, 'html')
    assert.match(html, new RegExp(`<img src="${ATTACHMENT_SCHEME}aa">`))
    assert.match(html, new RegExp(`<img src="${ATTACHMENT_SCHEME}bb" width="320">`))
    assert.match(write(ops, 'md'), new RegExp(`!\\[\\]\\(${ATTACHMENT_SCHEME}aa\\)`))
})

test('plain text drops a picture instead of leaving the character it occupies', () => {
    const ops = [
        { insert: 'before' },
        { insert: { image: { id: 'aa' } } },
        { insert: 'after\n' },
    ]
    assert.equal(write(ops, 'txt'), 'beforeafter\n')
    assert.deepEqual(pictures(ops, 'txt'), [])
})

test('a picture the application could not supply is left out, not left dead', () => {
    const ops = [
        { insert: 'a' },
        { insert: { image: { id: 'here' } } },
        { insert: { image: { id: 'gone' } } },
        { insert: '\n' },
    ]
    const out = exportDocument(new Delta(ops), 'html', '', ['gone'])
    assert.deepEqual(out.attachments, ['here'])
    assert.match(out.text, new RegExp(`<img src="${ATTACHMENT_SCHEME}here">`))
    assert.doesNotMatch(out.text, /gone/)
})

test('an empty paragraph is kept, being part of how the document reads', () => {
    assert.match(write([{ insert: 'a\n\nb\n' }], 'html'), /<p>a<\/p>\n<p><br><\/p>\n<p>b<\/p>/)
    assert.equal(write([{ insert: 'a\n\nb\n' }], 'txt'), 'a\n\nb\n')
})
