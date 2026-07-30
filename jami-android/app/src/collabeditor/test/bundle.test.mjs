/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

/*
 * Loads the bundle the application ships, in a DOM, and drives it the way the
 * Android side does.
 *
 * The editor lives behind a WebView, where a mistake shows up as a blank page
 * and a line in the log. This runs the same file against the same interface so
 * that a bundle that cannot start is a failed build rather than a blank screen.
 */

import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync, existsSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { JSDOM } from 'jsdom'
import * as Y from 'yjs'
import Delta from 'quill-delta'

import { quillToJami } from '../src/jamiformat.js'

const here = dirname(fileURLToPath(import.meta.url))
const bundle = resolve(here, '../build/collab/editor.js')

/** Stands in for the Kotlin side of the bridge. */
function makeHost() {
    return {
        updates: [],
        awareness: [],
        selections: [],
        logs: [],
        ready: false,
        onReady() { this.ready = true },
        onUpdate(base64) { this.updates.push(base64) },
        onAwareness(state) { this.awareness.push(state) },
        onSelection(json) { this.selections.push(json) },
        onLog(message) { this.logs.push(message) },
    }
}

function launch() {
    const dom = new JSDOM('<!DOCTYPE html><body><div id="editor"></div></body>', {
        pretendToBeVisual: true,
        runScripts: 'outside-only',
    })
    // The editor measures the text to place carets. There is no layout here,
    // so measuring answers zero rather than throwing, and the parts of the
    // editor that are being tested run.
    const zero = () => ({ top: 0, bottom: 0, left: 0, right: 0, width: 0, height: 0 })
    dom.window.Range.prototype.getBoundingClientRect = zero
    dom.window.Range.prototype.getClientRects = () => []
    dom.window.Element.prototype.getBoundingClientRect = zero

    const host = makeHost()
    dom.window.JamiBridge = host
    dom.window.eval(readFileSync(bundle, 'utf8'))
    dom.window.JamiEditor.start({})
    return { dom, host, editor: dom.window.JamiEditor }
}

const built = existsSync(bundle)
const options = built ? {} : { skip: 'run "npm run build" first' }

test('the bundle exposes the interface the application calls', options, () => {
    const { host, editor } = launch()
    assert.equal(host.ready, true, host.logs.join('\n'))
    for (const name of ['applyUpdate', 'applyAwareness', 'removeCursor', 'toggle',
                        'setHeader', 'setList', 'setAlign', 'setLink', 'clearFormat',
                        'undo', 'redo', 'insertImage', 'setImageWidth', 'setEditable',
                        'getHtml', 'getText', 'showVersion', 'leaveVersion']) {
        assert.equal(typeof editor[name], 'function', `missing ${name}`)
    }
    assert.deepEqual(host.logs, [])
})

test('a document sent by a peer is displayed', options, () => {
    const { host, editor } = launch()

    // What another replica would put on the wire.
    const peer = new Y.Doc()
    peer.getText('content').applyDelta(quillToJami(new Delta([
        { insert: 'Title' },
        { insert: '\n', attributes: { header: 1 } },
        { insert: 'A ' },
        { insert: 'strong', attributes: { bold: true } },
        { insert: ' word.\n' },
    ]), Delta).ops)
    editor.applyUpdate(Buffer.from(Y.encodeStateAsUpdate(peer)).toString('base64'))

    assert.equal(editor.getText(), 'Title\nA strong word.\n')
    const html = editor.getHtml()
    assert.match(html, /<h1>Title<\/h1>/)
    assert.match(html, /<strong>strong<\/strong>/)
    assert.deepEqual(host.logs, [])
})

test('a local edit is sent out as an update a peer can apply', options, () => {
    const { host, editor } = launch()

    const peer = new Y.Doc()
    peer.getText('content').applyDelta([{ insert: 'Report' }])
    editor.applyUpdate(Buffer.from(Y.encodeStateAsUpdate(peer)).toString('base64'))
    host.updates.length = 0

    // An edit only the editor can make: it inserts an embed and formats it,
    // which is the whole path from a toolbar tap to bytes on the wire.
    editor.insertImage('att-1', 800, 600)
    editor.setImageWidth(320)

    assert.ok(host.updates.length > 0, 'nothing was sent')
    for (const update of host.updates) {
        Y.applyUpdate(peer, Buffer.from(update, 'base64'))
    }

    // What the peer now holds is what the editor shows.
    assert.deepEqual(peer.getText('content').toDelta(), [
        { insert: { image: { id: 'att-1', width: 800, height: 600 } },
          attributes: { w: 320 } },
        { insert: 'Report' },
    ])
    assert.deepEqual(host.logs, [])
})

test('an awareness state from a peer is accepted and one from nowhere is not',
     options, () => {
    const { host, editor } = launch()
    editor.applyUpdate(Buffer.from(Y.encodeStateAsUpdate(new Y.Doc())).toString('base64'))

    editor.applyAwareness('peer1', 1, '{"p":0,"a":0}', 'Alice', '#e53935')
    editor.applyAwareness('peer1', 1, 'not json at all', 'Alice', '#e53935')
    editor.removeCursor('peer1', 1)

    // A malformed state is another client's problem, and must not become this
    // client's crash.
    assert.deepEqual(host.logs, [])
})
