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
 * The fonts a document may name, in the order they are offered.
 *
 * The id is the value of the document's "font" attribute. Families resolve
 * to locally installed fonts, with a generic fallback on every platform.
 */
export const DOCUMENT_FONTS = [
    { id: 'sans-serif', family: 'Sans Serif', stack: 'sans-serif' },
    { id: 'serif', family: 'Serif', stack: 'serif' },
    { id: 'monospace', family: 'Monospace', stack: 'monospace' },
    { id: 'cursive', family: 'Cursive', stack: 'cursive' },
]

export function fontCss() {
    return DOCUMENT_FONTS.map((font) =>
        `.ql-editor .ql-font-${font.id} { font-family: ${font.stack}; }`).join('\n')
}
