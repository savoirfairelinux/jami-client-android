/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 */

/*
 * These check the one thing that cannot be checked by opening the editor: that
 * what this client writes is what the desktop client reads, down to the key
 * names and to which character carries a paragraph attribute. A document is
 * kept forever in the conversation's history, so a mistake here is not a
 * display bug, it is a document nobody can open properly again.
 *
 * The expected values come from jami-client-qt/src/app/collabrichbinding.cpp:
 * charFormatToAttrs() for the keys, setHeader()/setList()/setAlign() for the
 * range a paragraph attribute is written over.
 */

import test from 'node:test'
import assert from 'node:assert/strict'
import Delta from 'quill-delta'
import * as Y from 'yjs'

import { jamiToQuill, quillToJami } from '../src/jamiformat.js'

const toJami = (ops) => quillToJami(new Delta(ops), Delta).ops
const toQuill = (ops) => jamiToQuill(ops, Delta).ops

test('inline attributes use the short keys the desktop client writes', () => {
    assert.deepEqual(
        toJami([
            { insert: 'a', attributes: { bold: true } },
            { insert: 'b', attributes: { italic: true } },
            { insert: 'c', attributes: { underline: true } },
            { insert: 'd', attributes: { strike: true } },
            { insert: 'e', attributes: { link: 'https://jami.net' } },
            { insert: '\n' },
        ]),
        [
            { insert: 'a', attributes: { b: true } },
            { insert: 'b', attributes: { i: true } },
            { insert: 'c', attributes: { u: true } },
            { insert: 'd', attributes: { s: true } },
            { insert: 'e', attributes: { link: 'https://jami.net' } },
        ],
    )
})

test('a heading is written on the line, not on its newline', () => {
    // What Quill produces for "Title\nbody" with the first line a level 1.
    const quill = [
        { insert: 'Title' },
        { insert: '\n', attributes: { header: 1 } },
        { insert: 'body\n' },
    ]
    assert.deepEqual(toJami(quill), [
        { insert: 'Title', attributes: { header: 1 } },
        // The newline and the next line merge into one op: they carry the same
        // attributes, which is none, and that is the canonical form a delta
        // takes on both sides.
        { insert: '\nbody' },
    ])
    assert.deepEqual(toQuill(toJami(quill)), quill)
})

test('lists and alignment follow the same rule', () => {
    const quill = [
        { insert: 'one' },
        { insert: '\n', attributes: { list: 'bullet' } },
        { insert: 'two' },
        { insert: '\n', attributes: { list: 'bullet', align: 'center' } },
    ]
    assert.deepEqual(toJami(quill), [
        { insert: 'one', attributes: { list: 'bullet' } },
        { insert: '\n' },
        { insert: 'two', attributes: { list: 'bullet', align: 'center' } },
    ])
    assert.deepEqual(toQuill(toJami(quill)), quill)
})

test('the shared document has no trailing newline', () => {
    assert.deepEqual(toJami([{ insert: 'hello\n' }]), [{ insert: 'hello' }])
    assert.deepEqual(toQuill([{ insert: 'hello' }]), [{ insert: 'hello\n' }])
})

test('an empty document is an empty document', () => {
    assert.deepEqual(toJami([{ insert: '\n' }]), [])
    assert.deepEqual(toQuill([]), [{ insert: '\n' }])
})

test('a paragraph attribute is dropped on a line with no characters', () => {
    // There is nowhere to put it: the desktop client refuses the same edit
    // rather than inventing a place for it.
    assert.deepEqual(toJami([{ insert: '\n', attributes: { header: 1 } }]), [])
})

test('a heading deeper than three levels arrives as plain text', () => {
    // The desktop client renders headings through a font size adjustment that
    // has three steps, so a level 4 has no representation there.
    assert.deepEqual(toJami([{ insert: 'x' }, { insert: '\n', attributes: { header: 4 } }]),
        [{ insert: 'x' }])
})

test('left alignment is the absence of the attribute', () => {
    assert.deepEqual(toJami([{ insert: 'x' }, { insert: '\n', attributes: { align: 'left' } }]),
        [{ insert: 'x' }])
})

test('inline and paragraph attributes coexist on the same characters', () => {
    const quill = [
        { insert: 'bold heading', attributes: { bold: true } },
        { insert: '\n', attributes: { header: 2, align: 'right' } },
    ]
    assert.deepEqual(toJami(quill), [
        { insert: 'bold heading', attributes: { b: true, header: 2, align: 'right' } },
    ])
    assert.deepEqual(toQuill(toJami(quill)), quill)
})

test('an image is an attachment reference with its width as an attribute', () => {
    const quill = [
        { insert: { image: { id: 'abc123', width: 800, height: 600 } }, attributes: { width: 320 } },
        { insert: '\n' },
    ]
    assert.deepEqual(toJami(quill), [
        { insert: { image: { id: 'abc123', width: 800, height: 600 } }, attributes: { w: 320 } },
    ])
    assert.deepEqual(toQuill(toJami(quill)), quill)
})

test('unknown attributes do not travel', () => {
    // Quill has formats the document has no key for. Sending them would make
    // the document say things no other client can read back.
    assert.deepEqual(
        toJami([{ insert: 'x', attributes: { background: '#ff0000', font: 'monospace' } },
                { insert: '\n' }]),
        [{ insert: 'x' }],
    )
})

test('a round trip through a real CRDT changes nothing', () => {
    const quill = [
        { insert: 'Report' },
        { insert: '\n', attributes: { header: 1 } },
        { insert: 'A ' },
        { insert: 'strong', attributes: { bold: true } },
        { insert: ' point and a ' },
        { insert: 'link', attributes: { link: 'https://jami.net' } },
        { insert: '.\nfirst' },
        { insert: '\n', attributes: { list: 'ordered' } },
        { insert: 'second' },
        { insert: '\n', attributes: { list: 'ordered' } },
        { insert: 'centred' },
        { insert: '\n', attributes: { align: 'center' } },
    ]

    const doc = new Y.Doc()
    doc.getText('content').applyDelta(toJami(quill))

    // What a second replica receives is what the first stored.
    const other = new Y.Doc()
    Y.applyUpdate(other, Y.encodeStateAsUpdate(doc))

    assert.deepEqual(toQuill(other.getText('content').toDelta()), quill)
})

test('two replicas typing at once converge on the same editor contents', () => {
    const a = new Y.Doc()
    a.getText('content').applyDelta(toJami([{ insert: 'shared\n' }]))
    const b = new Y.Doc()
    Y.applyUpdate(b, Y.encodeStateAsUpdate(a))

    a.getText('content').insert(0, 'A ')
    b.getText('content').insert(6, ' B')

    Y.applyUpdate(a, Y.encodeStateAsUpdate(b))
    Y.applyUpdate(b, Y.encodeStateAsUpdate(a))

    assert.deepEqual(toQuill(a.getText('content').toDelta()),
                     toQuill(b.getText('content').toDelta()))
})

test('a paragraph attribute set by two replicas at once converges', () => {
    // The reason the attribute is on the characters and not on the newline:
    // this is one attribute on shared characters, not two rival newlines.
    const a = new Y.Doc()
    a.getText('content').applyDelta(toJami([{ insert: 'line\n' }]))
    const b = new Y.Doc()
    Y.applyUpdate(b, Y.encodeStateAsUpdate(a))

    a.getText('content').format(0, 4, { header: 1 })
    b.getText('content').format(0, 4, { align: 'center' })

    Y.applyUpdate(a, Y.encodeStateAsUpdate(b))
    Y.applyUpdate(b, Y.encodeStateAsUpdate(a))

    assert.deepEqual(toQuill(a.getText('content').toDelta()), [
        { insert: 'line' },
        { insert: '\n', attributes: { header: 1, align: 'center' } },
    ])
    assert.deepEqual(toQuill(b.getText('content').toDelta()),
                     toQuill(a.getText('content').toDelta()))
})

test('a width Quill wrote as text travels as a number', () => {
    // The desktop client reads it with QJsonValue::toInt(), which answers zero
    // for a string, so a width left as text is a width nobody else applies.
    const jami = toJami([
        { insert: { image: { id: 'x' } }, attributes: { width: '320' } },
        { insert: '\n' },
    ])
    assert.equal(jami[0].attributes.w, 320)
    assert.equal(toQuill(jami)[0].attributes.width, 320)
})

test('a formatting attribute travels as a flag, not as whatever it held', () => {
    const jami = toJami([{ insert: 'x', attributes: { bold: 'true' } }, { insert: '\n' }])
    assert.deepEqual(jami, [{ insert: 'x', attributes: { b: true } }])
})
