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

import { jamiToQuill, quillToJami } from './jamiformat.js'

const LOCAL_ORIGIN = 'jami-local'

/**
 * Keeps a Quill editor and the shared Y.Text named "content" equal.
 *
 * Both sides are reconciled by diffing whole documents rather than by
 * translating each edit as it happens. An edit is expressed differently on
 * each side -- making a line a heading is one op in Quill and one per line in
 * the document -- so translating ops means reimplementing that correspondence
 * for every op shape, and being wrong about one of them means a divergence
 * that only shows up once two people type at once. Diffing states cannot
 * diverge: whatever the ops were, the two documents end up equal. The desktop
 * client keeps a shadow copy and diffs it for the same reason.
 *
 * The cost is one conversion of the document per edit, which for a text people
 * write together is nothing next to what it buys.
 */
export class JamiQuillBinding {
    constructor(ydoc, ytext, quill, Delta) {
        this.ydoc = ydoc
        this.ytext = ytext
        this.quill = quill
        this.Delta = Delta
        this.applyingRemote = false
        // Set while the editor shows something other than the document -- a
        // past version. The document keeps taking everyone's changes; it is
        // only the painting of them that waits.
        this.paused = false

        this._onYChange = (event, transaction) => {
            if (transaction.origin === LOCAL_ORIGIN) return
            if (this.paused) return
            this.pullFromDocument()
        }
        this._onQuillChange = (delta, oldDelta, source) => {
            if (this.applyingRemote || source === 'silent') return
            this.pushToDocument()
        }

        this.ytext.observe(this._onYChange)
        this.quill.on('text-change', this._onQuillChange)
        this.pullFromDocument()
    }

    destroy() {
        this.ytext.unobserve(this._onYChange)
        this.quill.off('text-change', this._onQuillChange)
    }

    /** Make the editor say what the shared document says. */
    pullFromDocument() {
        const target = jamiToQuill(this.ytext.toDelta(), this.Delta)
        const diff = this.quill.getContents().diff(target)
        if (diff.ops.length === 0) return
        this.applyingRemote = true
        try {
            // 'silent' keeps a peer's typing out of the local undo stack: undo
            // should take back what this user did, not what someone else did.
            // The selection is transformed through the change either way.
            this.quill.updateContents(diff, 'silent')
        } finally {
            this.applyingRemote = false
        }
    }

    /** Make the shared document say what the editor says. */
    pushToDocument() {
        const current = new this.Delta(this.ytext.toDelta())
        const target = quillToJami(this.quill.getContents(), this.Delta)
        const diff = current.diff(target)
        if (diff.ops.length === 0) return
        this.ydoc.transact(() => this.ytext.applyDelta(diff.ops), LOCAL_ORIGIN)
    }
}

export { LOCAL_ORIGIN }
