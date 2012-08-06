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

#include "sdes_negotiator.h"
#include "pattern.h"

#include <cstdio>
#include <tr1/memory>
#include <iostream>
#include <sstream>
#include <algorithm>
#include <stdexcept>

using namespace sfl;

SdesNegotiator::SdesNegotiator(const std::vector<CryptoSuiteDefinition>& localCapabilites,
                               const std::vector<std::string>& remoteAttribute) :
    remoteAttribute_(remoteAttribute),
    localCapabilities_(localCapabilites),
    cryptoSuite_(),
    srtpKeyMethod_(),
    srtpKeyInfo_(),
    lifetime_(),
    mkiValue_(),
    mkiLength_(),
    authTagLength_()
{}

std::vector<CryptoAttribute *> SdesNegotiator::parse()
{
    // The patterns below try to follow
    // the ABNF grammar rules described in
    // RFC4568 section 9.2 with the general
    // syntax :
    //a=crypto:tag 1*WSP crypto-suite 1*WSP key-params *(1*WSP session-param)

    std::tr1::shared_ptr<Pattern> generalSyntaxPattern, tagPattern, cryptoSuitePattern,
        keyParamsPattern;

    try {
        // used to match white space (which are used as separator)
        generalSyntaxPattern.reset(new Pattern("[\x20\x09]+", "g"));

        tagPattern.reset(new Pattern("^a=crypto:(?P<tag>[0-9]{1,9})"));

        cryptoSuitePattern.reset(new Pattern(
            "(?P<cryptoSuite>AES_CM_128_HMAC_SHA1_80|" \
            "AES_CM_128_HMAC_SHA1_32|" \
            "F8_128_HMAC_SHA1_80|" \
            "[A-Za-z0-9_]+)")); // srtp-crypto-suite-ext

        keyParamsPattern.reset(new Pattern(
            "(?P<srtpKeyMethod>inline|[A-Za-z0-9_]+)\\:" \
            "(?P<srtpKeyInfo>[A-Za-z0-9\x2B\x2F\x3D]+)"	 \
            "(\\|2\\^(?P<lifetime>[0-9]+)\\|"		 \
            "(?P<mkiValue>[0-9]+)\\:"			 \
            "(?P<mkiLength>[0-9]{1,3})\\;?)?", "g"));

    } catch (const CompileError& exception) {
        throw ParseError("A compile exception occured on a pattern.");
    }


    // Take each line from the vector
    // and parse its content

    std::vector<CryptoAttribute *> cryptoAttributeVector;

    for (std::vector<std::string>::iterator iter = remoteAttribute_.begin();
            iter != remoteAttribute_.end(); ++iter) {

        // Split the line into its component
        // that we will analyze further down.
        std::vector<std::string> sdesLine;

        *generalSyntaxPattern << (*iter);

        try {
            sdesLine = generalSyntaxPattern->split();

            if (sdesLine.size() < 3)
                throw ParseError("Missing components in SDES line");
        } catch (const MatchError& exception) {
            throw ParseError("Error while analyzing the SDES line.");
        }

        // Check if the attribute starts with a=crypto
        // and get the tag for this line
        *tagPattern << sdesLine.at(0);

        std::string tag;

        if (tagPattern->matches()) {
            try {
                tag = tagPattern->group("tag");
            } catch (const MatchError& exception) {
                throw ParseError("Error while parsing the tag field");
            }
        } else
            return cryptoAttributeVector;

        // Check if the crypto suite is valid and retreive
        // its value.
        *cryptoSuitePattern << sdesLine.at(1);

        std::string cryptoSuite;

        if (cryptoSuitePattern->matches()) {
            try {
                cryptoSuite = cryptoSuitePattern->group("cryptoSuite");
            } catch (const MatchError& exception) {
                throw ParseError("Error while parsing the crypto-suite field");
            }
        } else
            return cryptoAttributeVector;

        // Parse one or more key-params field.
        *keyParamsPattern << sdesLine.at(2);

        std::string srtpKeyInfo;
        std::string srtpKeyMethod;
        std::string lifetime;
        std::string mkiLength;
        std::string mkiValue;

        try {
            while (keyParamsPattern->matches()) {
                srtpKeyMethod = keyParamsPattern->group("srtpKeyMethod");
                srtpKeyInfo = keyParamsPattern->group("srtpKeyInfo");
                lifetime = keyParamsPattern->group("lifetime");
                mkiValue = keyParamsPattern->group("mkiValue");
                mkiLength = keyParamsPattern->group("mkiLength");
            }
        } catch (const MatchError& exception) {
            throw ParseError("Error while parsing the key-params field");
        }

        // Add the new CryptoAttribute to the vector

        CryptoAttribute * cryptoAttribute = new CryptoAttribute(tag, cryptoSuite, srtpKeyMethod, srtpKeyInfo, lifetime, mkiValue, mkiLength);
        cryptoAttributeVector.push_back(cryptoAttribute);
    }

    return cryptoAttributeVector;
}

bool SdesNegotiator::negotiate()
{
    std::vector<CryptoAttribute *> cryptoAttributeVector(parse());
    std::vector<CryptoAttribute *>::iterator iter_offer = cryptoAttributeVector.begin();

    std::vector<CryptoSuiteDefinition>::const_iterator iter_local = localCapabilities_.begin();

    bool negotiationSuccess = false;

    try {
        while (!negotiationSuccess && (iter_offer != cryptoAttributeVector.end())) {
            iter_local = localCapabilities_.begin();

            while (!negotiationSuccess && (iter_local != localCapabilities_.end())) {

                if ((*iter_offer)->getCryptoSuite().compare((*iter_local).name)) {
                    negotiationSuccess = true;

                    cryptoSuite_ = (*iter_offer)->getCryptoSuite();
                    srtpKeyMethod_ = (*iter_offer)->getSrtpKeyMethod();
                    srtpKeyInfo_ = (*iter_offer)->getSrtpKeyInfo();
                    authTagLength_ = cryptoSuite_.substr(cryptoSuite_.size() - 2, 2);
                }

                ++iter_local;
            }
            delete *iter_offer;
            *iter_offer = 0;
            ++iter_offer;
        }

    } catch (const ParseError& exception) {
        return false;
    } catch (const MatchError& exception) {
        return false;
    }

    return negotiationSuccess;
}
