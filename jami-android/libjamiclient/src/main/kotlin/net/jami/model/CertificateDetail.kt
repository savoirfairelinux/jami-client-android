/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.jami.model

enum class CertificateDetailType {
    Date,
    Number,
    String,
    Boolean,
    Binary,
}

enum class CertificateDetail(val key: String, val type: CertificateDetailType = CertificateDetailType.String) {
    ACTIVATION_DATE("ACTIVATION_DATE", CertificateDetailType.Date),
    EXPIRATION_DATE("EXPIRATION_DATE", CertificateDetailType.Date),
    ISSUER("ISSUER"),
    ISSUER_COMMON_NAME("ISSUER_CN"),
    ISSUER_NAME("ISSUER_N"),
    ISSUER_ORGANIZATION("ISSUER_O"),
    ISSUER_UID("ISSUER_UID"),
    PUBLIC_KEY_ID("PUBLIC_KEY_ID", CertificateDetailType.Binary),
    PUBLIC_SIGNATURE("PUBLIC_SIGNATURE", CertificateDetailType.Binary),
    SIGNATURE_ALGORITHM("SIGNATURE_ALGORITHM"),
    SUBJECT_KEY("SUBJECT_KEY", CertificateDetailType.Binary),
    SUBJECT_KEY_ALGORITHM("SUBJECT_KEY_ALGORITHM"),
    UID("UID"),
    COMMON_NAME("CN"),
    NAME("N"),
    ORGANIZATION_NAME("O"),
    IS_CA("IS_CA", CertificateDetailType.Boolean),
}