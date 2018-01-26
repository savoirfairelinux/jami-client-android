/*
 * Copyright (C) 2004-2018 Savoir-faire Linux Inc.
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

public class HistoryFileTransfer {

    private Long dataTransferId;
    private final long timestamp;
    private String displayName;
    private boolean isOutgoing;
    private DataTransferEventCode dataTransferEventCode;

    public HistoryFileTransfer(String displayName, boolean isOutgoing) {
        this.dataTransferId = 0L;
        this.timestamp = System.currentTimeMillis();
        this.displayName = displayName;
        this.isOutgoing = isOutgoing;
        this.dataTransferEventCode = DataTransferEventCode.CREATED;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDataTransferId(Long dataTransferId) {
        this.dataTransferId = dataTransferId;
    }

    public Long getDataTransferId() {
        return dataTransferId;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isOutgoing() {
        return isOutgoing;
    }

    public void setOutgoing(boolean outgoing) {
        isOutgoing = outgoing;
    }

    public DataTransferEventCode getDataTransferEventCode() {
        return dataTransferEventCode;
    }

    public void setDataTransferEventCode(DataTransferEventCode dataTransferEventCode) {
        this.dataTransferEventCode = dataTransferEventCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HistoryFileTransfer that = (HistoryFileTransfer) o;

        if (timestamp != that.timestamp) return false;
        if (isOutgoing != that.isOutgoing) return false;
        return displayName != null ? displayName.equals(that.displayName) : that.displayName == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (isOutgoing ? 1 : 0);
        return result;
    }
}
