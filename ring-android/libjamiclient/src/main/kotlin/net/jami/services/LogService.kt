/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package net.jami.services

interface LogService {
    fun e(tag: String, message: String)
    fun d(tag: String, message: String)
    fun w(tag: String, message: String)
    fun i(tag: String, message: String)
    fun e(tag: String, message: String, e: Throwable)
    fun d(tag: String, message: String, e: Throwable)
    fun w(tag: String, message: String, e: Throwable)
    fun i(tag: String, message: String, e: Throwable)
}