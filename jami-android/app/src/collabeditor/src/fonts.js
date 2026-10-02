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
 * The id is the value of the document's "font" attribute. The desktop client
 * (DOCUMENT_FONTS in jami-client-qt's collabrichbinding.cpp) ships the same
 * files under the same ids, so the lists have to be kept in step: an id is
 * part of the document format. The files are upstream releases left
 * unmodified, in ../fonts.
 */
export const DOCUMENT_FONTS = [
    { id: 'liberation-sans', family: 'Liberation Sans', file: 'LiberationSans' },
    { id: 'liberation-serif', family: 'Liberation Serif', file: 'LiberationSerif' },
    { id: 'liberation-mono', family: 'Liberation Mono', file: 'LiberationMono' },
    { id: 'carlito', family: 'Carlito', file: 'Carlito' },
    { id: 'caladea', family: 'Caladea', file: 'Caladea' },
    { id: 'gelasio', family: 'Gelasio', file: 'Gelasio' },
    { id: 'eb-garamond', family: 'EB Garamond', file: 'EBGaramond' },
    { id: 'roboto', family: 'Roboto', file: 'Roboto' },
    { id: 'open-sans', family: 'Open Sans', file: 'OpenSans' },
    { id: 'comic-neue', family: 'Comic Neue', file: 'ComicNeue' },
]

/* The four files each font comes as, and the face each of them is. */
export const FONT_STYLES = [
    { suffix: 'Regular', weight: 400, style: 'normal' },
    { suffix: 'Bold', weight: 700, style: 'normal' },
    { suffix: 'Italic', weight: 400, style: 'italic' },
    { suffix: 'BoldItalic', weight: 700, style: 'italic' },
]

/** Where the page finds one of the files, relative to itself. */
export function fontFile(font, style) {
    return `fonts/${font.file}-${style.suffix}.ttf`
}

/**
 * The faces of every font, and the class Quill gives text set in one of them.
 *
 * The browser fetches a face only once text needs it, so a document in one
 * font costs the files of that font and no other. A character the font lacks
 * -- a script it does not cover -- comes from the editor's own font, as it
 * does on the desktop.
 */
export function fontCss() {
    const rules = []
    for (const font of DOCUMENT_FONTS) {
        for (const style of FONT_STYLES) {
            rules.push(`@font-face { font-family: "${font.family}"; src: url("${fontFile(font, style)}")`
                + ` format("truetype"); font-weight: ${style.weight}; font-style: ${style.style}; }`)
        }
        rules.push(`.ql-editor .ql-font-${font.id} { font-family: "${font.family}", var(--jami-font-family); }`)
    }
    return rules.join('\n')
}
