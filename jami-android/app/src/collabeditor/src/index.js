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

import * as Y from 'yjs'
import { toBase64, fromBase64 } from 'lib0/buffer'
import Quill from 'quill'
import QuillCursors from 'quill-cursors'

import 'quill/dist/quill.snow.css'
import './styles.css'

import { JamiQuillBinding } from './binding.js'
import { jamiToQuill } from './jamiformat.js'

const Delta = Quill.import('delta')
const REMOTE_ORIGIN = 'jami-remote'

// Served by the Android side through shouldInterceptRequest. The host is the
// one WebViewAssetLoader reserves, so it never reaches the network even if an
// interception is ever missed.
const ATTACHMENT_BASE = 'https://appassets.androidplatform.net/attachment/'

const ALLOWED_LINK_SCHEMES = ['http', 'https', 'mailto']

/* The bounds the desktop client applies, so a width set here is one it keeps. */
const MIN_IMAGE_WIDTH = 24
const MAX_IMAGE_WIDTH = 4096

/** The Kotlin side, when there is one. Absent when the page is opened in a browser. */
const host = window.JamiBridge || {
    onReady() {},
    onUpdate() {},
    onAwareness() {},
    onSelection() {},
    onLinkRequested() {},
    onImageRequested() {},
    onLog(m) { console.log(m) },
}

/* ------------------------------------------------------------------ images */

const Image = Quill.import('formats/image')

/*
 * An image is an attachment of the conversation, referred to by id. The bytes
 * never enter the CRDT: a document with a dozen photographs would otherwise be
 * replayed in full to every participant on every open.
 *
 * The width the image is *drawn* at is an attribute, not part of the embed,
 * because an embed is immutable in a CRDT: resizing one would mean deleting it
 * and inserting another, and two participants resizing at once would end up
 * with two images where there was one.
 */
class JamiImage extends Image {
    static create(value) {
        const node = super.create(value)
        const ref = typeof value === 'string' ? { id: value } : (value || {})
        node.setAttribute('src', ATTACHMENT_BASE + encodeURIComponent(ref.id || ''))
        node.setAttribute('data-jami-id', ref.id || '')
        if (ref.width > 0) node.setAttribute('data-natural-width', String(ref.width))
        if (ref.height > 0) node.setAttribute('data-natural-height', String(ref.height))
        return node
    }

    static value(node) {
        const ref = { id: node.getAttribute('data-jami-id') || '' }
        const w = parseInt(node.getAttribute('data-natural-width'), 10)
        const h = parseInt(node.getAttribute('data-natural-height'), 10)
        if (w > 0) ref.width = w
        if (h > 0) ref.height = h
        return ref
    }
}
Quill.register(JamiImage, true)
Quill.register('modules/cursors', QuillCursors)

/* ------------------------------------------------------------------ editor */

class Editor {
    constructor() {
        this.ydoc = null
        this.ytext = null
        this.binding = null
        this.quill = null
        this.cursors = null
        this.preview = false
        this.previewContents = null
        this.lastAwareness = null
        this.frame = null
        this.resizing = null
    }

    start(options) {
        const opts = options || {}
        this.quill = new Quill('#editor', {
            theme: 'snow',
            placeholder: opts.placeholder || '',
            readOnly: !!opts.readOnly,
            modules: {
                // The native toolbar drives the editor through format(); Quill's
                // own would be a second one, in a second style, that has to be
                // kept in step with the first.
                toolbar: false,
                cursors: { transformOnTextChange: true, hideDelayMs: 8000 },
                history: { userOnly: true },
                keyboard: { bindings: shortcuts() },
            },
        })
        this.cursors = this.quill.getModule('cursors')

        this.ydoc = new Y.Doc()
        // The branch name the daemon and the desktop client agree on.
        this.ytext = this.ydoc.getText('content')
        this.binding = new JamiQuillBinding(this.ydoc, this.ytext, this.quill, Delta)

        this.ydoc.on('update', (update, origin) => {
            if (origin === REMOTE_ORIGIN) return
            host.onUpdate(toBase64(update))
        })

        this.quill.root.addEventListener('click', (event) => {
            if (!event.target || event.target.tagName !== 'IMG') return
            const blot = Quill.find(event.target)
            if (!blot) return
            this.quill.setSelection(this.quill.getIndex(blot), 1, 'user')
        })

        this.quill.on('selection-change', () => this.reportSelection())
        this.quill.on('editor-change', (name) => {
            if (name === 'text-change') this.reportSelection()
        })

        this.buildResizeHandles()

        host.onReady()
    }

    /* -------------------------------------------------------- synchronisation */

    applyUpdate(base64) {
        if (!base64) return
        Y.applyUpdate(this.ydoc, fromBase64(base64), REMOTE_ORIGIN)
    }

    /** The state vector, so the daemon can send only what this replica lacks. */
    stateVector() {
        return toBase64(Y.encodeStateVector(this.ydoc))
    }

    /* ------------------------------------------------------------- awareness */

    reportSelection() {
        // Neither call may take the focus. Quill's getFormat() with no argument
        // defaults to getSelection(true), which focuses the editor and scrolls
        // to the caret -- so a peer typing at the other end of the document
        // would raise this user's keyboard and move their view.
        const range = this.quill.getSelection()
        const formats = (range ? this.quill.getFormat(range) : {}) || {}
        const found = this.imageAt(range)
        host.onSelection(JSON.stringify({
            index: range ? range.index : -1,
            length: range ? range.length : 0,
            image: found
                ? { id: found.image.id, width: typeof formats.width === 'number' ? formats.width : 0 }
                : null,
            formats: {
                bold: !!formats.bold,
                italic: !!formats.italic,
                underline: !!formats.underline,
                strike: !!formats.strike,
                link: typeof formats.link === 'string' ? formats.link : '',
                header: typeof formats.header === 'number' ? formats.header : 0,
                list: typeof formats.list === 'string' ? formats.list : '',
                align: typeof formats.align === 'string' ? formats.align : '',
            },
        }))
        this.placeResizeHandles()
        if (!range) return
        // The desktop client sends the caret and the anchor in UTF-16 units,
        // which is what Quill counts in too, and what the CRDT is indexed by.
        const state = JSON.stringify({ p: range.index + range.length, a: range.index })
        if (state === this.lastAwareness) return
        this.lastAwareness = state
        host.onAwareness(state)
    }

    applyAwareness(peerId, clientId, stateJson, name, color) {
        const key = peerId + '/' + clientId
        let state
        try {
            state = JSON.parse(stateJson)
        } catch (e) {
            return
        }
        const caret = Number(state.p)
        const anchor = Number(state.a)
        if (!Number.isFinite(caret)) return
        const from = Number.isFinite(anchor) ? Math.min(caret, anchor) : caret
        const to = Number.isFinite(anchor) ? Math.max(caret, anchor) : caret
        const length = this.quill.getLength()
        this.cursors.createCursor(key, name || peerId, color || '#0056b3')
        this.cursors.moveCursor(key, {
            index: Math.min(from, length - 1),
            length: Math.min(to, length - 1) - Math.min(from, length - 1),
        })
    }

    removeCursor(peerId, clientId) {
        this.cursors.removeCursor(peerId + '/' + clientId)
    }

    /* ------------------------------------------------------------- formatting */

    format(name, value) {
        const range = this.quill.getSelection()
        if (!range) this.quill.focus()
        // A block format with no selection still applies: it belongs to the
        // line the caret is on.
        this.quill.format(name, value === '' ? false : value, 'user')
        this.reportSelection()
    }

    toggle(name) {
        const formats = this.quill.getFormat() || {}
        this.format(name, !formats[name])
    }

    setHeader(level) {
        const formats = this.quill.getFormat() || {}
        this.format('header', formats.header === level ? false : level)
    }

    setList(style) {
        const formats = this.quill.getFormat() || {}
        this.format('list', formats.list === style ? false : style)
    }

    setAlign(style) {
        // The four alignments exclude one another, so this is not a toggle.
        // Left is the default and clears the attribute rather than storing itself.
        this.format('align', style === 'left' ? false : style)
    }

    /*
     * A link reaches the document from a user typing it or from a peer's
     * update, and ends up in a text that can be copied anywhere. Only schemes
     * that address a document are kept: "javascript:" runs, and "file:" and
     * "data:" address the machine the document happens to be opened on. This
     * is the rule the desktop client applies, so that neither client stores a
     * link the other refuses.
     */
    setLink(href) {
        const trimmed = (href || '').trim()
        if (trimmed === '') {
            this.format('link', false)
            return
        }
        const withScheme = /^[a-zA-Z][a-zA-Z0-9+.-]*:/.test(trimmed)
            ? trimmed
            : 'https://' + trimmed
        let scheme
        try {
            scheme = new URL(withScheme).protocol.replace(':', '').toLowerCase()
        } catch (e) {
            return
        }
        if (!ALLOWED_LINK_SCHEMES.includes(scheme)) return
        this.format('link', withScheme)
    }

    clearFormat() {
        const range = this.quill.getSelection()
        if (!range) return
        this.quill.removeFormat(range.index, range.length, 'user')
        this.reportSelection()
    }

    undo() { this.quill.history.undo() }
    redo() { this.quill.history.redo() }

    insertImage(id, width, height) {
        const range = this.quill.getSelection(true)
        const at = range ? range.index : this.quill.getLength() - 1
        const ref = { id }
        if (width > 0) ref.width = width
        if (height > 0) ref.height = height
        this.quill.insertEmbed(at, 'image', ref, 'user')
        this.quill.setSelection(at + 1, 0, 'user')
    }

    /**
     * The image the given selection means: the selected one, or the one the
     * caret sits just after, which is where it is left after an insertion.
     */
    imageAt(range) {
        if (!range) return null
        const embedAt = (index) => {
            if (index < 0) return null
            const ops = this.quill.getContents(index, 1).ops
            const value = ops.length === 1 ? ops[0].insert : null
            return value && value.image !== undefined ? value.image : null
        }
        if (range.length === 1) {
            const image = embedAt(range.index)
            if (image) return { index: range.index, image }
        }
        if (range.length === 0) {
            const image = embedAt(range.index - 1)
            if (image) return { index: range.index - 1, image }
        }
        return null
    }

    setImageWidth(width) {
        const found = this.imageAt(this.quill.getSelection())
        if (!found) return
        const w = this.boundWidth(width)
        this.quill.formatText(found.index, 1, 'width', w > 0 ? w : false, 'user')
        this.reportSelection()
    }

    /** No wider than the page it is on, and never too small to grab again. */
    boundWidth(width) {
        const w = Math.round(width)
        if (!(w > 0)) return 0
        const available = this.textWidth()
        const max = available > MIN_IMAGE_WIDTH
            ? Math.min(available, MAX_IMAGE_WIDTH)
            : MAX_IMAGE_WIDTH
        return Math.max(MIN_IMAGE_WIDTH, Math.min(w, max))
    }

    /** The width of the text column, which is as wide as an image may be. */
    textWidth() {
        const root = this.quill.root
        const style = window.getComputedStyle ? window.getComputedStyle(root) : null
        const padding = style
            ? (parseFloat(style.paddingLeft) || 0) + (parseFloat(style.paddingRight) || 0)
            : 0
        return root.clientWidth - padding
    }

    /* ---------------------------------------------------------- image sizing */

    /**
     * Handles for resizing the selected image by dragging.
     *
     * They sit in Quill's container rather than in the text, so that they are
     * not part of the document: anything inside the editable area would be
     * content, and would travel to everyone else.
     *
     * A drag changes what this device draws and nothing more; only the release
     * writes to the document. A width per pixel moved would be a swarm message
     * per pixel moved.
     */
    buildResizeHandles() {
        const frame = document.createElement('div')
        frame.className = 'jami-image-frame'
        frame.setAttribute('aria-hidden', 'true')
        for (const side of ['left', 'right']) {
            const handle = document.createElement('div')
            handle.className = 'jami-image-handle jami-image-handle-' + side
            handle.addEventListener('pointerdown', (e) => this.startResize(e, side))
            frame.appendChild(handle)
        }
        this.quill.container.appendChild(frame)
        this.frame = frame

        const follow = () => this.placeResizeHandles()
        this.quill.root.addEventListener('scroll', follow)
        window.addEventListener('resize', follow)
        // An image that has just finished loading is a different size than the
        // frame that was drawn around it.
        this.quill.root.addEventListener('load', follow, true)
    }

    /** Puts the frame over the selected image, or takes it away. */
    placeResizeHandles() {
        if (!this.frame) return
        const found = this.resizing || this.imageAt(this.quill.getSelection())
        const node = found ? this.imageNode(found.index) : null
        if (!node || this.preview) {
            this.frame.classList.remove('is-shown')
            return
        }
        const box = node.getBoundingClientRect()
        const origin = this.quill.container.getBoundingClientRect()
        this.frame.style.left = (box.left - origin.left) + 'px'
        this.frame.style.top = (box.top - origin.top) + 'px'
        this.frame.style.width = box.width + 'px'
        this.frame.style.height = box.height + 'px'
        this.frame.classList.add('is-shown')
    }

    imageNode(index) {
        const [blot] = this.quill.getLeaf(index + 1)
        const node = blot && blot.domNode
        return node && node.tagName === 'IMG' ? node : null
    }

    startResize(event, side) {
        const found = this.imageAt(this.quill.getSelection())
        const node = found ? this.imageNode(found.index) : null
        if (!node) return
        event.preventDefault()
        if (event.target && event.target.setPointerCapture && event.pointerId !== undefined) {
            try { event.target.setPointerCapture(event.pointerId) } catch (e) { /* not held */ }
        }
        this.resizing = {
            index: found.index,
            side,
            node,
            fromX: event.clientX,
            fromWidth: node.getBoundingClientRect().width,
            // What to put back if the drag never ends: a width only this replica
            // knows about is silent divergence.
            was: node.getAttribute('width'),
            width: 0,
        }
        const move = (e) => this.duringResize(e)
        const end = (e) => this.endResize(e, false)
        const cancel = (e) => this.endResize(e, true)
        this.resizing.release = () => {
            document.removeEventListener('pointermove', move)
            document.removeEventListener('pointerup', end)
            document.removeEventListener('pointercancel', cancel)
        }
        document.addEventListener('pointermove', move)
        document.addEventListener('pointerup', end)
        document.addEventListener('pointercancel', cancel)
    }

    duringResize(event) {
        const drag = this.resizing
        if (!drag) return
        const moved = event.clientX - drag.fromX
        // Dragging a left handle outwards means leftwards.
        const wanted = drag.fromWidth + (drag.side === 'right' ? moved : -moved)
        drag.width = this.boundWidth(wanted)
        drag.node.setAttribute('width', String(drag.width))
        this.placeResizeHandles()
    }

    endResize(event, cancelled) {
        const drag = this.resizing
        if (!drag) return
        drag.release()
        this.resizing = null
        if (cancelled || !(drag.width > 0)) {
            if (drag.was) drag.node.setAttribute('width', drag.was)
            else drag.node.removeAttribute('width')
        } else {
            this.quill.formatText(drag.index, 1, 'width', drag.width, 'user')
        }
        this.placeResizeHandles()
        this.reportSelection()
    }

    setEditable(editable) {
        this.quill.enable(!!editable)
    }

    /* ---------------------------------------------------------------- reading */

    getHtml() {
        return this.quill.getSemanticHTML()
    }

    getText() {
        return this.quill.getText()
    }

    /**
     * Show a past state of the document without joining it: a checkpoint is a
     * thing to read, and typing into one would have to go somewhere.
     */
    showVersion(base64) {
        const past = new Y.Doc()
        Y.applyUpdate(past, fromBase64(base64))
        this.preview = true
        // Everyone else carries on typing while this user reads. Their changes
        // must reach the document, and must not be painted over the version
        // being read, which would take it away mid-sentence.
        this.binding.paused = true
        this.binding.applyingRemote = true
        try {
            this.previewContents = jamiToQuill(past.getText('content').toDelta(), Delta)
            this.quill.setContents(this.previewContents, 'silent')
            this.quill.enable(false)
        } finally {
            this.binding.applyingRemote = false
        }
        past.destroy()
    }

    leaveVersion() {
        if (!this.preview) return
        this.preview = false
        this.previewContents = null
        this.binding.paused = false
        this.quill.enable(true)
        this.binding.pullFromDocument()
        this.placeResizeHandles()
    }

    /**
     * Make the document say again what it said at the version being read.
     *
     * An edit like any other, as on the desktop: it travels the usual way, and
     * can itself be taken back by restoring a later version. Rewinding the
     * document instead would be a state only this replica agreed to.
     */
    restoreVersion() {
        if (!this.preview || !this.previewContents) return
        const target = this.previewContents
        this.leaveVersion()
        const diff = this.quill.getContents().diff(target)
        if (diff.ops.length === 0) return
        this.quill.updateContents(diff, 'user')
    }
}

/* --------------------------------------------------------------- shortcuts */

function shortcuts() {
    const bind = (key, handler) => ({ key, shortKey: true, handler })
    return {
        // Quill binds bold/italic/underline itself; the rest are ours.
        strike: { ...bind('S', function () { editor.toggle('strike'); return false }), shiftKey: true },
        clean: bind('\\', function () { editor.clearFormat(); return false }),
    }
}

/* ------------------------------------------------------------------ exports */

const editor = new Editor()

// Called from Kotlin through evaluateJavascript. Everything is wrapped so that
// a failure is reported rather than swallowed by the WebView.
function guard(fn) {
    return function (...args) {
        try {
            return fn.apply(editor, args)
        } catch (e) {
            host.onLog('editor: ' + (e && e.stack ? e.stack : e))
            return null
        }
    }
}

window.JamiEditor = {
    start: guard(Editor.prototype.start),
    applyUpdate: guard(Editor.prototype.applyUpdate),
    stateVector: guard(Editor.prototype.stateVector),
    applyAwareness: guard(Editor.prototype.applyAwareness),
    removeCursor: guard(Editor.prototype.removeCursor),
    toggle: guard(Editor.prototype.toggle),
    setHeader: guard(Editor.prototype.setHeader),
    setList: guard(Editor.prototype.setList),
    setAlign: guard(Editor.prototype.setAlign),
    setLink: guard(Editor.prototype.setLink),
    clearFormat: guard(Editor.prototype.clearFormat),
    undo: guard(Editor.prototype.undo),
    redo: guard(Editor.prototype.redo),
    insertImage: guard(Editor.prototype.insertImage),
    setImageWidth: guard(Editor.prototype.setImageWidth),
    setEditable: guard(Editor.prototype.setEditable),
    getHtml: guard(Editor.prototype.getHtml),
    getText: guard(Editor.prototype.getText),
    showVersion: guard(Editor.prototype.showVersion),
    leaveVersion: guard(Editor.prototype.leaveVersion),
    restoreVersion: guard(Editor.prototype.restoreVersion),
}

/*
 * The page starts itself.
 *
 * Building the editor is the page's own business, and the application has no
 * say in when it happens: it is waiting to be told the page is ready, which is
 * the last thing start() does. Leaving the call to the application would have
 * each side waiting for the other.
 */
function boot() {
    try {
        editor.start({ placeholder: '' })
    } catch (e) {
        host.onLog('editor: start failed: ' + (e && e.stack ? e.stack : e))
    }
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot)
} else {
    boot()
}
