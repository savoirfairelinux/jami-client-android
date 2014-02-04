/*
  Copyright (C) 2006-2013 Werner Dittmann

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

#include <string>

#include "ZIDRecord.h"

#ifndef _ZIDCACHE_H_
#define _ZIDCACHE_H_

/**
 * @file ZIDCache.h
 * @brief ZID cache management
 *
 * A ZID file stores (caches) some data that helps ZRTP to achives its
 * key continuity feature. See @c ZIDRecord for further info which data
 * the ZID file contains.
 *
 * @ingroup GNU_ZRTP
 * @{
 */

/**
 * Interface for classes that implements a ZID (ZRTP Identifiers) file.
 *
 * The ZID file holds information about peers.
 *
 * @author: Werner Dittmann <Werner.Dittmann@t-online.de>
 */

class ZIDCache;

__EXPORT ZIDCache* getZidCacheInstance();


class __EXPORT ZIDCache {

public:

    /**
     * @brief Destructor.
     * Define a virtual destructor to enable cleanup in derived classes.
     */
    virtual ~ZIDCache() {};

    /**
     * @brief Open the named ZID file and return a ZID file class.
     *
     * This static function either opens an existing ZID file or
     * creates a new ZID file with the given name. The ZIDCache is a
     * singleton, thus only <em>one</em> ZID file can be open at one
     * time.
     *
     * To open another ZID file you must close the active ZID file
     * first.
     *
     * @param name
     *    The name of the ZID file to open or create
     * @return
     *    1 if file could be opened/created, 0 if the ZID instance
     *    already has an open file, -1 if open/creation of file failed.
     */
    virtual int open(char *name) =0;

    /**
     * @brief Check if ZIDCache has an active (open) file.
     *
     * @return
     *    True if ZIDCache has an active file, false otherwise
     */
    virtual bool isOpen() =0;

     /**
      * @brief Close the ZID file.
      *
      * Closes the ZID file, and prepares to open a new ZID file.
      */
    virtual void close() =0;

    /**
     * @brief Get a ZID record from ZID cache or create a new record.
     *
     * The method tries to read a ZRTP cache record for the ZID.
     * If no matching record exists in the cache the method creates
     * it and fills it with default values.
     *
     * @param zid is the ZRTP id of the peer
     * @return pointer to the ZID record. The call must @c delete the
     *         record if it is not longer used.
     */
    virtual ZIDRecord *getRecord(unsigned char *zid) =0;

    /**
     * @brief Save a ZID record into the active ZID file.
     *
     * This method saves the content of a ZID record into the ZID file. Before
     * you can save the ZID record you must have performed a getRecord()
     * first.
     *
     * @param zidRecord
     *    The ZID record to save.
     * @return
     *    1 on success
     */
    virtual unsigned int saveRecord(ZIDRecord *zidRecord) =0;

    /**
     * @brief Get the ZID associated with this ZID file.
     *
     * @return
     *    Pointer to the ZID
     */
    virtual const unsigned char* getZid() =0;

    /**
     * @brief Get peer name from database
     *
     * This is an optional function.
     *
     * A client may use this function to retrieve a name that was assigned
     * to the peer's ZID.
     *
     * @param peerZid the peer's ZID
     *
     * @param name string that will get the peer's name. The returned name will
     *             be truncated to 200 bytes
     *
     * @return length og the name read or 0 if no name was previously stored.
     */
    virtual int32_t getPeerName(const uint8_t *peerZid, std::string *name) =0;

    /**
     * @brief Write peer name to database
     *
     * This is an optional function.
     *
     * A client may use this function to write a name in the ZRTP cache database and
     * asign it to the peer's ZID.
     *
     * @param peerZid the peer's ZID
     *
     * @param name the name string
     *
     */
    virtual void putPeerName(const uint8_t *peerZid, const std::string name) =0;

    /**
     * @brief Clean the cache.
     *
     * The function drops and re-creates all tables in the database. This removes all stored
     * data. The application must not call this while a ZRTP call is active. Also the application
     * <b>must</b> get the local ZID again.
     *
     */
    virtual void cleanup() =0;
};

/**
 * @}
 */
#endif
