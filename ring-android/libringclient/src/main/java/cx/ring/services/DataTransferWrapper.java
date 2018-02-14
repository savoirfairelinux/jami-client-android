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

package cx.ring.services;

import cx.ring.daemon.DataTransferInfo;
import cx.ring.model.DataTransferError;

public class DataTransferWrapper {

        private DataTransferInfo dataTransferInfo;
        private DataTransferError dataTransferError;

        public DataTransferWrapper(DataTransferInfo dataTransferInfo, DataTransferError dataTransferError) {
            this.dataTransferInfo = dataTransferInfo;
            this.dataTransferError = dataTransferError;
        }

        public DataTransferInfo getDataTransferInfo() {
            return dataTransferInfo;
        }

        public DataTransferError getDataTransferError() {
            return dataTransferError;
        }

        public boolean isOutgoing() {
            return this.dataTransferInfo.getFlags() == 0;
        }
    }