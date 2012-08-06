/*
 *  Copyright (C) 2004, 2005, 2006, 2008, 2009, 2010, 2011 Savoir-Faire Linux Inc.
 *  Author: Pierre-Luc Bacon <pierre-luc.bacon@savoirfairelinux.com>
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

#include "pattern.h"
#include <sstream>
#include <cstdio>

namespace sfl {

Pattern::Pattern(const std::string& pattern, const std::string& options) :
    pattern_(pattern),
    subject_(),
    re_(NULL),
    ovector_(),
    count_(0),
    options_(0),
    optionsDescription_(options)
{
    // Set offsets
    offset_[0] = offset_[1] = 0;

    for (unsigned int i = 0; i < options.length(); i++) {
        switch (options.at(i)) {
            case 'i':
                options_ |= PCRE_CASELESS;
                break;

            case 'm':
                options_ |= PCRE_MULTILINE;
                break;

            case 's':
                options_ |= PCRE_DOTALL;
                break;

            case 'x':
                options_ |= PCRE_EXTENDED;
                break;
        }
    }

    // Compile the pattern.
    compile();
}

Pattern::~Pattern()
{
    if (re_ != NULL)
        pcre_free(re_);
}

void Pattern::compile()
{
    // Compile the pattern
    int offset;
    const char * error;

    re_ = pcre_compile(pattern_.c_str(), 0, &error, &offset, NULL);

    if (re_ == NULL) {
        std::string offsetStr;
        std::stringstream ss;
        ss << offset;
        offsetStr = ss.str();

        std::string msg("PCRE compiling failed at offset " + offsetStr);

        throw CompileError(msg);
    }

    // Allocate an appropriate amount
    // of memory for the output vector.
    int captureCount;
    pcre_fullinfo(re_, NULL, PCRE_INFO_CAPTURECOUNT, &captureCount);

    ovector_.clear();
    ovector_.resize((captureCount + 1) * 3);
}

unsigned int Pattern::getCaptureGroupCount()
{
    int captureCount = 0;
    pcre_fullinfo(re_, NULL, PCRE_INFO_CAPTURECOUNT, &captureCount);
    return captureCount;
}

std::vector<std::string> Pattern::groups()
{
    const char ** stringList;

    pcre_get_substring_list(subject_.c_str(), &ovector_[0], count_, &stringList);
    std::vector<std::string> matchedSubstrings;
    for (int i = 1; stringList[i] != NULL; i++)
        matchedSubstrings.push_back(stringList[i]);

    pcre_free_substring_list(stringList);

    return matchedSubstrings;
}

std::string Pattern::group(int groupNumber)
{
    const char * stringPtr;
    int rc = pcre_get_substring(subject_.substr(offset_[0]).c_str(), &ovector_[0],
                                count_, groupNumber, &stringPtr);

    if (rc < 0) {
        switch (rc) {
            case PCRE_ERROR_NOSUBSTRING:
                throw std::out_of_range("Invalid group reference.");

            case PCRE_ERROR_NOMEMORY:
                throw MatchError("Memory exhausted.");

            default:
                throw MatchError("Failed to get named substring.");
        }
    }
    std::string matchedStr(stringPtr);

    pcre_free_substring(stringPtr);

    return matchedStr;
}

std::string Pattern::group(const std::string& groupName)
{
    const char * stringPtr = NULL;
    int rc = pcre_get_named_substring(re_, subject_.substr(offset_[0]).c_str(),
                                      &ovector_[0], count_, groupName.c_str(),
                                      &stringPtr);

    if (rc < 0) {
        switch (rc) {
            case PCRE_ERROR_NOSUBSTRING:
                break;

            case PCRE_ERROR_NOMEMORY:
                throw MatchError("Memory exhausted.");

            default:
                throw MatchError("Failed to get named substring.");
        }
    }
    std::string matchedStr;
    if (stringPtr) {
        matchedStr = stringPtr;
        pcre_free_substring(stringPtr);
    }
    return matchedStr;
}

void Pattern::start(const std::string& groupName) const
{
    int index = pcre_get_stringnumber(re_, groupName.c_str());
    start(index);
}

size_t Pattern::start(unsigned int groupNumber) const
{
    if (groupNumber <= (unsigned int) count_)
        return ovector_[(groupNumber + 1) * 2];
    else
        throw std::out_of_range("Invalid group reference.");
    return 0;
}

size_t Pattern::start() const
{
    return ovector_[0] + offset_[0];
}

void Pattern::end(const std::string& groupName) const
{
    int index = pcre_get_stringnumber(re_, groupName.c_str());
    end(index);
}

size_t Pattern::end(unsigned int groupNumber) const
{
    if (groupNumber <= (unsigned int) count_)
        return ovector_[((groupNumber + 1) * 2) + 1 ] - 1;
    else
        throw std::out_of_range("Invalid group reference.");
    return 0;
}

size_t Pattern::end() const
{
    return (ovector_[1] - 1) + offset_[0];
}

bool Pattern::matches()
{
    return matches(subject_);
    return true;
}

bool Pattern::matches(const std::string& subject)
{
    // Try to find a match for this pattern
    int rc = pcre_exec(re_, NULL, subject.substr(offset_[1]).c_str(),
                       subject.length() - offset_[1], 0, options_, &ovector_[0],
                       ovector_.size());

    // Matching failed.
    if (rc < 0) {
        offset_[0] = offset_[1] = 0;
        return false;
    }

    // Handle the case if matching should be done globally
    if (optionsDescription_.find("g") != std::string::npos) {
        offset_[0] = offset_[1];
        // New offset is old offset + end of relative offset
        offset_[1] =  ovector_[1] + offset_[0];
    }

    // Matching succeded but not enough space.
    // @TODO figure out something more clever to do in this case.
    if (rc == 0)
        throw MatchError("No space to store all substrings.");

    // Matching succeeded. Keep the number of substrings for
    // subsequent calls to group().
    count_ = rc;
    return true;
}

std::vector<std::string> Pattern::split()
{
    size_t tokenEnd = -1;
    size_t tokenStart = 0;
    std::vector<std::string> substringSplitted;
    while (matches()) {
        tokenStart = start();
        substringSplitted.push_back(subject_.substr(tokenEnd + 1,
                                    tokenStart - tokenEnd - 1));
        tokenEnd = end();
    }

    substringSplitted.push_back(subject_.substr(tokenEnd + 1,
                                                tokenStart - tokenEnd - 1));
    return substringSplitted;
}
}
