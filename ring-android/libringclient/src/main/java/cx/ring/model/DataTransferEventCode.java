/*
 * Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 * Author: Pierre Duchemin <pierre.duchemin@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.model;

public enum DataTransferEventCode {
    INVALID,
    CREATED,
    UNSUPPORTED(true),
    WAIT_PEER_ACCEPTANCE,
    WAIT_HOST_ACCEPTANCE,
    ONGOING,
    FINISHED,
    CLOSED_BY_HOST(true),
    CLOSED_BY_PEER(true),
    INVALID_PATHNAME(true),
    UNJOINABLE_PEER(true);

    private boolean isError;

    DataTransferEventCode() {
        isError = false;
    }

    DataTransferEventCode(boolean isError) {
        this.isError = isError;
    }

    public boolean isError() {
        return isError;
    }

    public boolean isOver() {
        return isError() || this == FINISHED;
    }
}