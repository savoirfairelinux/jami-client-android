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
const page = resolve(here, '../build/collab/editor.html')

/** Stands in for the Kotlin side of the bridge. */
function makeHost() {
    let announce
    const host = {
        updates: [],
        awareness: [],
        selections: [],
        logs: [],
        ready: false,
        // Resolves when the page says it is ready, which is what the
        // application waits for before it asks the daemon for the document.
        started: new Promise((resolve) => { announce = resolve }),
        onReady() { this.ready = true; announce() },
        onUpdate(base64) { this.updates.push(base64) },
        onAwareness(state) { this.awareness.push(state) },
        onSelection(json) { this.selections.push(json) },
        onLog(message) { this.logs.push(message) },
    }
    return host
}

function launch() {
    // The page the application loads, not a stand-in for it: an editor that
    // builds itself into an element the shipped HTML does not have is exactly
    // the kind of blank screen this is here to catch.
    const dom = new JSDOM(readFileSync(page, 'utf8'), {
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
    // Nothing here starts the editor: the page does that itself, and an editor
    // waiting to be started is an editor the application waits for for ever.
    dom.window.eval(readFileSync(bundle, 'utf8'))
    // The page boots when the document is parsed, so this waits for the same
    // signal the application waits for rather than assuming it has happened.
    return withTimeout(
        host.started.then(() => ({ dom, host, editor: dom.window.JamiEditor })), host)
}

/** Fails with the page's own log rather than a bare timeout. */
function withTimeout(promise, host) {
    let timer
    const expiry = new Promise((_, reject) => {
        timer = setTimeout(
            () => reject(new Error('the editor never reported ready: '
                + (host.logs.join('\n') || 'it said nothing'))), 5000)
    })
    return Promise.race([promise, expiry]).finally(() => clearTimeout(timer))
}

const built = existsSync(bundle) && existsSync(page)
const options = built ? {} : { skip: 'run "npm run build" first' }

test('the editor starts itself and says so', options, async () => {
    // The application shows a spinner until this arrives, and asks the daemon
    // for the document only then. Were the page waiting to be started instead,
    // each side would be waiting for the other.
    const { host } = await launch()
    assert.equal(host.ready, true, host.logs.join('\n'))
    assert.deepEqual(host.logs, [])
})

test('the bundle exposes the interface the application calls', options, async () => {
    const { host, editor } = await launch()
    assert.equal(host.ready, true, host.logs.join('\n'))
    for (const name of ['applyUpdate', 'applyAwareness', 'removeCursor', 'toggle',
                        'setHeader', 'setList', 'setAlign', 'setLink', 'clearFormat',
                        'undo', 'redo', 'insertImage', 'setImageWidth', 'setEditable',
                        'getHtml', 'getText', 'showVersion', 'leaveVersion']) {
        assert.equal(typeof editor[name], 'function', `missing ${name}`)
    }
    assert.deepEqual(host.logs, [])
})

test('a document sent by a peer is displayed', options, async () => {
    const { host, editor } = await launch()

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

test('a local edit is sent out as an update a peer can apply', options, async () => {
    const { host, editor } = await launch()

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
     options, async () => {
    const { host, editor } = await launch()
    editor.applyUpdate(Buffer.from(Y.encodeStateAsUpdate(new Y.Doc())).toString('base64'))

    editor.applyAwareness('peer1', 1, '{"p":0,"a":0}', 'Alice', '#e53935')
    editor.applyAwareness('peer1', 1, 'not json at all', 'Alice', '#e53935')
    editor.removeCursor('peer1', 1)

    // A malformed state is another client's problem, and must not become this
    // client's crash.
    assert.deepEqual(host.logs, [])
})

/* ------------------------------------------------------- resizing an image */

const decode = (base64) => new Uint8Array(Buffer.from(base64, 'base64'))

const rect = (left, width) => ({
    left, width, right: left + width, top: 0, bottom: 120, height: 120,
})

/** A pointer event carrying the one thing the editor reads from it. */
function fire(dom, target, type, clientX) {
    const event = new dom.window.Event(type, { bubbles: true, cancelable: true })
    event.clientX = clientX
    target.dispatchEvent(event)
}

/** A two-finger gesture, spread apart along the x axis. */
function pinch(dom, target, type, gap) {
    const event = new dom.window.Event(type, { bubbles: true, cancelable: true })
    event.touches = [
        { clientX: 100 - gap / 2, clientY: 60 },
        { clientX: 100 + gap / 2, clientY: 60 },
    ]
    target.dispatchEvent(event)
}

function drag(dom, handle, from, to) {
    fire(dom, handle, 'pointerdown', from)
    fire(dom, dom.window.document, 'pointermove', to)
    fire(dom, dom.window.document, 'pointerup', to)
}

test('dragging an image handle resizes it for everyone', options, async () => {
    const { dom, host, editor } = await launch()

    const peer = new Y.Doc()
    editor.insertImage('att-1', 800, 600)
    for (const update of host.updates) Y.applyUpdate(peer, decode(update))
    host.updates.length = 0

    const image = dom.window.document.querySelector('img')
    assert.ok(image, 'no image was inserted')
    // There is no layout here, so the image is told how wide it starts.
    image.getBoundingClientRect = () => rect(0, 200)

    const handle = dom.window.document.querySelector('.jami-image-handle-br')
    assert.ok(handle, 'no resize handle')

    drag(dom, handle, 100, 160)

    // Moved 60 to the right, so 60 wider -- and said once, on release, not
    // once per pixel.
    assert.equal(image.getAttribute('width'), '260')
    assert.equal(host.updates.length, 1, 'a message per pixel moved')

    for (const update of host.updates) Y.applyUpdate(peer, decode(update))
    assert.deepEqual(peer.getText('content').toDelta(), [
        { insert: { image: { id: 'att-1', width: 800, height: 600 } },
          attributes: { w: 260 } },
    ])
    assert.deepEqual(host.logs, [])
})

test('an abandoned drag leaves the document as it was', options, async () => {
    const { dom, host, editor } = await launch()
    editor.insertImage('att-1', 800, 600)
    editor.setImageWidth(200)
    host.updates.length = 0

    const image = dom.window.document.querySelector('img')
    image.getBoundingClientRect = () => rect(0, 200)
    const handle = dom.window.document.querySelector('.jami-image-handle-br')

    fire(dom, handle, 'pointerdown', 100)
    fire(dom, dom.window.document, 'pointermove', 300)
    // The grab is taken away: no release will ever come.
    fire(dom, dom.window.document, 'pointercancel', 300)

    // A width only this replica knows about is silent divergence.
    assert.equal(image.getAttribute('width'), '200')
    assert.deepEqual(host.updates, [])
    assert.deepEqual(host.logs, [])
})

/* ------------------------------------------------------ looking at the past */

test('a past checkpoint is shown, and leaving it comes back', options, async () => {
    const { dom, host, editor } = await launch()
    const text = () => dom.window.document.querySelector('.ql-editor').textContent.trim()

    // What the daemon answers for an old checkpoint: the whole document as it
    // stood then, replayed into a throwaway replica.
    const past = new Y.Doc()
    past.getText('content').insert(0, 'written by a peer')
    const atCheckpoint = Buffer.from(Y.encodeStateAsUpdate(past)).toString('base64')

    // Meanwhile the live document has moved on.
    const live = new Y.Doc()
    live.getText('content').insert(0, 'written by a peer, and then some more')
    editor.applyUpdate(Buffer.from(Y.encodeStateAsUpdate(live)).toString('base64'))
    assert.equal(text(), 'written by a peer, and then some more')

    host.updates.length = 0
    editor.showVersion(atCheckpoint)
    assert.equal(text(), 'written by a peer',
        'the past checkpoint is not what is shown')
    // Looking at the past says nothing to anyone.
    assert.deepEqual(host.updates, [])

    editor.leaveVersion()
    assert.equal(text(), 'written by a peer, and then some more',
        'leaving the past does not come back to the present')
    assert.deepEqual(host.updates, [])
    assert.deepEqual(host.logs, [])
})

test('a peer typing does not take away the version being read', options, async () => {
    const { dom, host, editor } = await launch()
    const text = () => dom.window.document.querySelector('.ql-editor').textContent.trim()

    const live = new Y.Doc()
    live.getText('content').insert(0, 'first')
    editor.applyUpdate(Buffer.from(Y.encodeStateAsUpdate(live)).toString('base64'))

    const past = new Y.Doc()
    past.getText('content').insert(0, 'as it was')
    editor.showVersion(Buffer.from(Y.encodeStateAsUpdate(past)).toString('base64'))
    assert.equal(text(), 'as it was')

    // Someone else is still typing while this user reads the past.
    live.getText('content').insert(5, ' and more')
    editor.applyUpdate(Buffer.from(Y.encodeStateAsUpdate(live)).toString('base64'))

    assert.equal(text(), 'as it was', 'the version being read was taken away')

    // And the document kept up all along.
    editor.leaveVersion()
    assert.equal(text(), 'first and more')
    assert.deepEqual(host.logs, [])
})

test('a version can be restored, and everyone gets it', options, async () => {
    const { dom, host, editor } = await launch()
    const text = () => dom.window.document.querySelector('.ql-editor').textContent.trim()

    const peer = new Y.Doc()
    peer.getText('content').applyDelta([{ insert: 'a mistake' }])
    editor.applyUpdate(Buffer.from(Y.encodeStateAsUpdate(peer)).toString('base64'))

    const past = new Y.Doc()
    past.getText('content').applyDelta([
        { insert: 'what it said', attributes: { b: true } },
    ])
    editor.showVersion(Buffer.from(Y.encodeStateAsUpdate(past)).toString('base64'))
    host.updates.length = 0

    editor.restoreVersion()

    assert.equal(text(), 'what it said', 'the document was not restored')
    assert.ok(host.updates.length > 0, 'the restore was never sent')
    for (const update of host.updates) Y.applyUpdate(peer, decode(update))
    // Restoring is an edit like any other: it reaches the others, formatting
    // and all, and can itself be taken back by restoring a later version.
    assert.deepEqual(peer.getText('content').toDelta(), [
        { insert: 'what it said', attributes: { b: true } },
    ])
    assert.deepEqual(host.logs, [])
})

test('an image can be resized by pinching it', options, async () => {
    const { dom, host, editor } = await launch()

    const peer = new Y.Doc()
    editor.insertImage('att-1', 800, 600)
    for (const update of host.updates) Y.applyUpdate(peer, decode(update))
    host.updates.length = 0

    const image = dom.window.document.querySelector('img')
    // There is no layout here, so the picture is told where it is.
    image.getBoundingClientRect = () => rect(40, 120)

    const root = dom.window.document.querySelector('.ql-editor')
    pinch(dom, root, 'touchstart', 60)
    pinch(dom, root, 'touchmove', 120)
    pinch(dom, root, 'touchend', 120)

    // Fingers twice as far apart, picture twice as wide -- said once, at the
    // end, not once per finger movement.
    assert.equal(image.getAttribute('width'), '240')
    assert.equal(host.updates.length, 1, 'a message per finger movement')

    for (const update of host.updates) Y.applyUpdate(peer, decode(update))
    assert.deepEqual(peer.getText('content').toDelta(), [
        { insert: { image: { id: 'att-1', width: 800, height: 600 } },
          attributes: { w: 240 } },
    ])
    assert.deepEqual(host.logs, [])
})

test('a pinch away from any image is left alone', options, async () => {
    const { dom, host, editor } = await launch()
    editor.insertImage('att-1', 800, 600)
    host.updates.length = 0

    const image = dom.window.document.querySelector('img')
    image.getBoundingClientRect = () => rect(400, 120)

    const root = dom.window.document.querySelector('.ql-editor')
    pinch(dom, root, 'touchstart', 60)
    pinch(dom, root, 'touchmove', 200)
    pinch(dom, root, 'touchend', 200)

    // Pinching the page is not pinching the picture.
    assert.equal(image.getAttribute('width'), null)
    assert.deepEqual(host.updates, [])
    assert.deepEqual(host.logs, [])
})
