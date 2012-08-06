/*
 *  Copyright (C) 2004, 2005, 2006, 2008, 2009, 2010, 2011 Savoir-Faire Linux Inc.
 *  Author: Pierre-Luc Bacon <pierre-luc.bacon@savoirfairelinux.com>
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */
#ifndef __SDES_NEGOTIATOR_H__
#define __SDES_NEGOTIATOR_H__

#include <stdexcept>
#include <string>
#include <vector>

namespace sfl {

/**
 * General exception object that is thrown when
 * an error occured with a regular expression
 * operation.
 */
class ParseError : public std::invalid_argument {
    public:
        explicit ParseError(const std::string& error) :
            std::invalid_argument(error) {}
};

enum CipherMode {
    AESCounterMode,
    AESF8Mode
};

enum MACMode {
    HMACSHA1
};

enum KeyMethod {
    Inline
    // url, maybe at some point
};

struct CryptoSuiteDefinition {
    const char *name;
    int masterKeyLength;
    int masterSaltLength;
    int srtpLifetime;
    int srtcpLifetime;
    CipherMode cipher;
    int encryptionKeyLength;
    MACMode mac;
    int srtpAuthTagLength;
    int srtcpAuthTagLength;
    int srtpAuthKeyLength;
    int srtcpAuthKeyLen;
};

/**
* List of accepted Crypto-Suites
* as defined in RFC4568 (6.2)
*/
static const CryptoSuiteDefinition CryptoSuites[] = {
    { "AES_CM_128_HMAC_SHA1_80", 128, 112, 48, 31, AESCounterMode, 128, HMACSHA1, 80, 80, 160, 160 },
    { "AES_CM_128_HMAC_SHA1_32", 128, 112, 48, 31, AESCounterMode, 128, HMACSHA1, 32, 80, 160, 160 },
    { "F8_128_HMAC_SHA1_80", 128, 112, 48, 31, AESF8Mode, 128, HMACSHA1, 80, 80, 160, 160 }
};


class CryptoAttribute {

    public:
        CryptoAttribute(const std::string &tag,
                        const std::string &cryptoSuite,
                        const std::string &srtpKeyMethod,
                        const std::string &srtpKeyInfo,
                        const std::string &lifetime,
                        const std::string &mkiValue,
                        const std::string &mkiLength) :
            tag_(tag),
            cryptoSuite_(cryptoSuite),
            srtpKeyMethod_(srtpKeyMethod),
            srtpKeyInfo_(srtpKeyInfo),
            lifetime_(lifetime),
            mkiValue_(mkiValue),
            mkiLength_(mkiLength) {}


        std::string getTag() const {
            return tag_;
        }
        std::string getCryptoSuite() const {
            return cryptoSuite_;
        }
        std::string getSrtpKeyMethod() const {
            return srtpKeyMethod_;
        }
        std::string getSrtpKeyInfo() const {
            return srtpKeyInfo_;
        }
        std::string getLifetime() const {
            return lifetime_;
        }
        std::string getMkiValue() const {
            return mkiValue_;
        }
        std::string getMkiLength() const {
            return mkiLength_;
        }

    private:
        std::string tag_;
        std::string cryptoSuite_;
        std::string srtpKeyMethod_;
        std::string srtpKeyInfo_;
        std::string lifetime_;
        std::string mkiValue_;
        std::string mkiLength_;
};

class SdesNegotiator {
        /**
         * Constructor for an SDES crypto attributes
         * negotiator.
         *
         * @param attribute
         *       A vector of crypto attributes as defined in
         *       RFC4568. This string will be parsed
         *       and a crypto context will be created
         *       from it.
         */

    public:
        SdesNegotiator(const std::vector<CryptoSuiteDefinition>& localCapabilites, const std::vector<std::string>& remoteAttribute);

        bool negotiate();

        /**
         * Return crypto suite after negotiation
         */
        std::string getCryptoSuite() const {
            return cryptoSuite_;
        }

        /**
         * Return key method after negotiation (most likely inline:)
         */
        std::string getKeyMethod() const {
            return srtpKeyMethod_;
        }

        /**
         * Return crypto suite after negotiation
         */
        std::string getKeyInfo() const {
            return srtpKeyInfo_;
        }

        /**
         * Return key lifetime after negotiation
         */
        std::string getLifeTime() const {
            return lifetime_;
        }

        /**
         * Return mki value after negotiation
         */
        std::string getMkiValue() const {
            return mkiValue_;
        }

        /**
         * Return mki length after negotiation
         */
        std::string getMkiLength() const {
            return mkiLength_;
        }

        /**
        * Authentication tag lenth
        */
        std::string getAuthTagLength() const {
            return authTagLength_;
        }


    private:
        /**
         * A vector list containing the remote attributes.
         * Multiple crypto lines can be sent, and the
         * prefered method is then chosen from that list.
         */
        std::vector<std::string> remoteAttribute_;

        std::vector<CryptoSuiteDefinition> localCapabilities_;

        /**
         * Selected crypto suite after negotiation
         */
        std::string cryptoSuite_;

        /**
         * Selected key method after negotiation (most likely inline:)
         */
        std::string srtpKeyMethod_;

        /**
         * Selected crypto suite after negotiation
         */
        std::string srtpKeyInfo_;

        /**
         * Selected key lifetime after negotiation
         */
        std::string lifetime_;

        /**
         * Selected mki value after negotiation
         */
        std::string mkiValue_;

        /**
         * Selected mki length after negotiation
         */
        std::string mkiLength_;

        /**
         * Authenticvation tag length in byte
         */
        std::string authTagLength_;

        std::vector<CryptoAttribute *> parse();
};
}
#endif // __SDES_NEGOTIATOR_H__
