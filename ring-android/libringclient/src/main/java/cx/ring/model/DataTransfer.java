/*
 * Copyright (C) 2004-2018 Savoir-faire Linux Inc.
 *
 * Author: Pierre Duchemin <pierre.duchemin@savoirfairelinux.com>
 * Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = DataTransfer.TABLE_NAME)
public class DataTransfer extends ConversationElement {
    public static final String TABLE_NAME = "historydata";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_TIMESTAMP_NAME = "TIMESTAMP";
    public static final String COLUMN_DISPLAY_NAME_NAME = "displayName";
    public static final String COLUMN_IS_OUTGOING_NAME = "isOutgoing";
    public static final String COLUMN_TOTAL_SIZE_NAME = "totalSize";
    public static final String COLUMN_PEER_ID_NAME = "peerId";
    public static final String COLUMN_ACCOUNT_ID_NAME = "accountId";
    public static final String COLUMN_DATA_TRANSFER_EVENT_CODE_NAME = "dataTransferEventCode";

    @DatabaseField(index = true, generatedId = true, columnName = COLUMN_ID_NAME)
    long id;
    @DatabaseField(index = true, columnName = COLUMN_TIMESTAMP_NAME)
    long timestamp;
    @DatabaseField(columnName = COLUMN_DISPLAY_NAME_NAME)
    String displayName;
    @DatabaseField(columnName = COLUMN_IS_OUTGOING_NAME)
    boolean isOutgoing;
    @DatabaseField(columnName = COLUMN_TOTAL_SIZE_NAME)
    long totalSize;
    @DatabaseField(columnName = COLUMN_PEER_ID_NAME)
    String peerId;
    @DatabaseField(columnName = COLUMN_ACCOUNT_ID_NAME)
    String accountId;
    @DatabaseField(columnName = COLUMN_DATA_TRANSFER_EVENT_CODE_NAME)
    String eventCode;

    final long dataTransferId;
    private long bytesProgress;


    /* Needed by ORMLite */
    public DataTransfer() {
        dataTransferId = -1;
    }

    public DataTransfer(Long dataTransferId, String displayName, boolean isOutgoing, long totalSize, long bytesProgress, String peerId, String accountId) {
        this.dataTransferId = dataTransferId;
        this.timestamp = System.currentTimeMillis();
        this.displayName = displayName;
        this.isOutgoing = isOutgoing;
        this.eventCode = DataTransferEventCode.CREATED.name();
        this.totalSize = totalSize;
        this.bytesProgress = bytesProgress;
        this.peerId = peerId;
        this.accountId = accountId;
    }

    @Override
    public CEType getType() {
        return CEType.FILE;
    }

    @Override
    public long getDate() {
        return timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getId() { return id; }
    public void setId(long newId) { id = newId; }

    public Long getDataTransferId() {
        return dataTransferId;
    }

    public boolean isOutgoing() {
        return isOutgoing;
    }

    public DataTransferEventCode getEventCode() {
        return DataTransferEventCode.valueOf(eventCode);
    }

    public void setEventCode(DataTransferEventCode eventCode) {
        this.eventCode = eventCode.name();
    }

    public long getTotalSize() {
        return totalSize;
    }

    public long getBytesProgress() {
        return bytesProgress;
    }

    public void setBytesProgress(long bytesProgress) {
        this.bytesProgress = bytesProgress;
    }

    public String getPeerId() {
        return peerId;
    }

    public String getAccountId() {
        return accountId;
    }
}
