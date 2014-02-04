/*
  Copyright (C) 2006-2007 Werner Dittmann

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/*
 * Authors: Werner Dittmann <Werner.Dittmann@t-online.de>
 */

#include <libzrtpcpp/ZrtpPacketHello.h>


ZrtpPacketHello::ZrtpPacketHello() {
    DEBUGOUT((fprintf(stdout, "Creating Hello packet without data\n")));
}

void ZrtpPacketHello::configureHello(ZrtpConfigure* config) {
    // The NumSupported* data is in ZrtpTextData.h 
    nHash = config->getNumConfiguredAlgos(HashAlgorithm);
    nCipher = config->getNumConfiguredAlgos(CipherAlgorithm);
    nPubkey = config->getNumConfiguredAlgos(PubKeyAlgorithm);
    nSas = config->getNumConfiguredAlgos(SasType);
    nAuth = config->getNumConfiguredAlgos(AuthLength);

    // length is fixed Header plus HMAC size (2*ZRTP_WORD_SIZE)
    int32_t length = sizeof(HelloPacket_t) + (2 * ZRTP_WORD_SIZE);
    length += nHash * ZRTP_WORD_SIZE;
    length += nCipher * ZRTP_WORD_SIZE;
    length += nPubkey * ZRTP_WORD_SIZE;
    length += nSas * ZRTP_WORD_SIZE;
    length += nAuth * ZRTP_WORD_SIZE;

    // Don't change order of this sequence
    oHash = sizeof(Hello_t);
    oCipher = oHash + (nHash * ZRTP_WORD_SIZE);
    oAuth = oCipher + (nCipher * ZRTP_WORD_SIZE);
    oPubkey = oAuth + (nAuth * ZRTP_WORD_SIZE);
    oSas = oPubkey + (nPubkey * ZRTP_WORD_SIZE);
    oHmac = oSas + (nSas * ZRTP_WORD_SIZE);         // offset to HMAC

    void* allocated = &data;
    memset(allocated, 0, sizeof(data));

    zrtpHeader = (zrtpPacketHeader_t *)&((HelloPacket_t *)allocated)->hdr;	// the standard header
    helloHeader = (Hello_t *)&((HelloPacket_t *)allocated)->hello;

    setZrtpId();

    // minus 1 for CRC size 
    setLength(length / ZRTP_WORD_SIZE);
    setMessageType((uint8_t*)HelloMsg);

    setVersion((uint8_t*)zrtpVersion);

    uint32_t lenField = nHash << 16;
    for (int32_t i = 0; i < nHash; i++) {
        AlgorithmEnum& hash = config->getAlgoAt(HashAlgorithm, i);
        setHashType(i, (int8_t*)hash.getName());
    }

    lenField |= nCipher << 12;
    for (int32_t i = 0; i < nCipher; i++) {
        AlgorithmEnum& cipher = config->getAlgoAt(CipherAlgorithm, i);
        setCipherType(i, (int8_t*)cipher.getName());
    }

    lenField |= nAuth << 8;
    for (int32_t i = 0; i < nAuth; i++) {
        AlgorithmEnum& length = config->getAlgoAt(AuthLength, i);
        setAuthLen(i, (int8_t*)length.getName());
    }

    lenField |= nPubkey << 4;
    for (int32_t i = 0; i < nPubkey; i++) {
        AlgorithmEnum& pubKey = config->getAlgoAt(PubKeyAlgorithm, i);
        setPubKeyType(i, (int8_t*)pubKey.getName());
    }

    lenField |= nSas;
    for (int32_t i = 0; i < nSas; i++) {
        AlgorithmEnum& sas = config->getAlgoAt(SasType, i);
        setSasType(i, (int8_t*)sas.getName());
    }
    *((uint32_t*)&helloHeader->flags) = htonl(lenField);
}

ZrtpPacketHello::ZrtpPacketHello(uint8_t *data) {
    DEBUGOUT((fprintf(stdout, "Creating Hello packet from data\n")));

    zrtpHeader = (zrtpPacketHeader_t *)&((HelloPacket_t *)data)->hdr;	// the standard header
    helloHeader = (Hello_t *)&((HelloPacket_t *)data)->hello;

    uint32_t t = *((uint32_t*)&helloHeader->flags);
    uint32_t temp = ntohl(t);

    nHash = (temp & (0xf << 16)) >> 16;
    nCipher = (temp & (0xf << 12)) >> 12;
    nAuth = (temp & (0xf << 8)) >> 8;
    nPubkey = (temp & (0xf << 4)) >> 4;
    nSas = temp & 0xf;

    oHash = sizeof(Hello_t);
    oCipher = oHash + (nHash * ZRTP_WORD_SIZE);
    oAuth = oCipher + (nCipher * ZRTP_WORD_SIZE);
    oPubkey = oAuth + (nAuth * ZRTP_WORD_SIZE);
    oSas = oPubkey + (nPubkey * ZRTP_WORD_SIZE);
    oHmac = oSas + (nSas * ZRTP_WORD_SIZE);         // offset to HMAC
}

ZrtpPacketHello::~ZrtpPacketHello() {
    DEBUGOUT((fprintf(stdout, "Deleting Hello packet: alloc: %x\n", allocated)));
}
