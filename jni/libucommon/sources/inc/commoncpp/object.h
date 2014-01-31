// Copyright (C) 1999-2005 Open Source Telecom Corporation.
// Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// As a special exception, you may use this file as part of a free software
// library without restriction.  Specifically, if other files instantiate
// templates or use macros or inline functions from this file, or you compile
// this file and link it with other files to produce an executable, this
// file does not by itself cause the resulting executable to be covered by
// the GNU General Public License.  This exception does not however
// invalidate any other reasons why the executable file might be covered by
// the GNU General Public License.
//
// This exception applies only to the code released under the name GNU
// Common C++.  If you copy code from other releases into a copy of GNU
// Common C++, as the General Public License permits, the exception does
// not apply to the code that you add in this way.  To avoid misleading
// anyone as to the status of such modified files, you must delete
// this exception notice from them.
//
// If you write modifications of your own for GNU Common C++, it is your choice
// whether to permit this exception to apply to your modifications.
// If you do not wish that, delete this exception notice.
//

/**
 * @file object.h
 * @short Some object manipulation classes for smart pointers, linked lists,
 * etc.
 **/

#ifndef COMMONCPP_OBJECT_H_
#define COMMONCPP_OBJECT_H_

#ifndef COMMONCPP_CONFIG_H_
#include <commoncpp/config.h>
#endif

NAMESPACE_COMMONCPP

class MapObject;
class MapIndex;

/**
 * A reference countable object.  This is used in association with smart
 * pointers (RefPointer).
 *
 * @author David Sugar <dyfet@gnutelephony.org>
 * @short Object managed by smart pointer reference count.
 */
class __EXPORT RefObject
{
protected:
    friend class RefPointer;

    unsigned refCount;

    /**
     * The constructor simply initializes the count.
     */
    inline RefObject()
        {refCount = 0;};

    /**
     * The destructor is called when the reference count returns
     * to zero.  This is done through a virtual destructor.
     */
    virtual ~RefObject();

public:
    /**
     * The actual object being managed can be returned by this
     * method as a void and then recast to the actual type.  This
     * removes the need to dynamic cast from RefObject and the
     * dependence on rtti this implies.
     *
     * @return underlying object being referenced.
     */
    virtual void *getObject(void) = 0;
};

/**
 * Pointer to reference counted objects.  This is a non-template form
 * of a reference count smart pointer, and so uses common code.  This
 * can be subclassed to return explicit object types.
 *
 * @author David Sugar <dyfet@gnutelephony.org>
 * @short Pointer to reference count managed objects.
 */
class __EXPORT RefPointer
{
protected:
    RefObject *ref;

    /**
     * Detach current object, for example, when changing pointer.
     */
    void detach(void);

    /**
     * Patch point for mutex in derived class.  This may often
     * be a single static mutex shared by a managed type.
     */
    virtual void enterLock(void);

    /**
     * Patch point for a mutex in derived class.  This may often
     * be a single static mutex shared by a managed type.
     */
    virtual void leaveLock(void);

public:
    /**
     * Create an unattached pointer.
     */
    inline RefPointer()
        {ref = NULL;};

    /**
     * Create a pointer attached to a reference counted object.
     *
     * Object being referenced.
     */
    RefPointer(RefObject *obj);

    /**
     * A copy constructor.
     *
     * Pointer being copied.
     */
    RefPointer(const RefPointer &ptr);

    virtual ~RefPointer();

    RefPointer& operator=(const RefObject &ref);

    inline void *operator*() const
        {return getObject();};

    inline void *operator->() const
        {return getObject();};

    void *getObject(void) const;

    bool operator!() const;
};

/**
 * Self managed single linked list object chain.  This is used for
 * accumulating lists by using as a base class for a derived subclass.
 *
 * @author David Sugar <dyfet@gnutelephony.org>
 * @short Accumulating single linked list.
 */
class __EXPORT LinkedSingle
{
protected:
    LinkedSingle *nextObject;

    inline LinkedSingle()
        {nextObject = NULL;};

    virtual ~LinkedSingle();

public:
    /**
     * Get first linked object in list.  This may be dynamically
     * recast, and may refer to a master static bookmark pointer
     * in a derived class.  Otherwise it simply returns the current
     * object.  In a "free" list, this may not only return the first
     * object, but also set the first to next.
     *
     * @return pointer to first object in list.
     */
    virtual LinkedSingle *getFirst(void);

    /**
     * Gets the last object in the list.  This normally follows the
     * links to the end.  This is a virtual because derived class
     * may include a static member bookmark for the current end.
     *
     * @return pointer to last object in list.
     */
    virtual LinkedSingle *getLast(void);

    /**
     * Get next object, for convenience.  Derived class may use
     * this with a dynamic cast.
     *
     * @return next object in list.
     */
    inline LinkedSingle *getNext(void)
        {return nextObject;};

    /**
     * Insert object into chain.  This is a virtual because
     * derived class may choose instead to perform an insert
     * at head or tail, may manage bookmarks, and may add mutex lock.
     *
     * @param object being inserted.
     */
    virtual void insert(LinkedSingle& obj);

    LinkedSingle &operator+=(LinkedSingle &obj);
};

/**
 * Self managed double linked list object chain.  This is used for
 * accumulating lists by using as a base class for a derived subclass.
 *
 * @author David Sugar <dyfet@gnutelephony.org>
 * @short Accumulating double linked list.
 */
class __EXPORT LinkedDouble
{
protected:
    LinkedDouble *nextObject, *prevObject;

    inline LinkedDouble()
        {nextObject = prevObject = NULL;};

    virtual ~LinkedDouble();

    virtual void enterLock(void);

    virtual void leaveLock(void);

    virtual LinkedDouble *firstObject();

    virtual LinkedDouble *lastObject();

public:

  /**
   * Requested in overloaded insert() method to indicate how to insert
   * data into list
   */
  enum InsertMode
  {
    modeAtFirst,  /**< insert at first position in list pointed by current object */
    modeAtLast,   /**< insert at last position in list pointed by current object */
    modeBefore,   /**< insert in list before current object */
    modeAfter     /**< insert in list after current object */
  };

    /**
     * Get first linked object in list.  This may be dynamically
     * recast, and may refer to a master static bookmark pointer
     * in a derived class.  Otherwise it follows list to front.
     *
     * @return pointer to first object in list.
     */
    virtual LinkedDouble *getFirst(void);

    /**
     * Gets the last object in the list.  This normally follows the
     * links to the end.  This is a virtual because derived class
     * may include a static member bookmark for the current end.
     *
     * @return pointer to last object in list.
     */
    virtual LinkedDouble *getLast(void);

    /**
     * Virtual to get the insert point to use when adding new members.  This
     * may be current, or always head or always tail.  As a virtual, this allows
     * derived class to establish "policy".
     *
     * @return pointer to insertion point in list.
     */
    virtual LinkedDouble *getInsert(void);

    /**
     * Get next object, for convenience.  Derived class may use
     * this with a dynamic cast.
     *
     * @return next object in list.
     */
    inline LinkedDouble *getNext(void)
        {return nextObject;};

    /**
     * Get prev object in the list.
     *
     * @return pointer to previous object.
     */
    inline LinkedDouble *getPrev(void)
        {return prevObject;};

  /**
   * Insert object into chain at given position, as indicated by \ref InsertMode;
   * If no position is given, it defaults to \ref modeAtLast, inserting element
   * at list's end.
   *
   * @param object being inserted.
   * @param position where object is inserted.
   */
  virtual void insert(LinkedDouble& obj, InsertMode position = modeAtLast);

    /**
     * Remove object from chain.
     */
    virtual void detach(void);

    LinkedDouble &operator+=(LinkedDouble &obj);

    LinkedDouble &operator--();
};

/**
 * A map table allows for entities to be mapped (hash index) onto it.
 * Unlike with Assoc, This form of map table also allows objects to be
 * removed from the table.  This table also includes a mutex lock for
 * thread safety.  A free list is also optionally maintained for reusable
 * maps.
 *
 * @author David Sugar <dyfet@gnutelephony.org>
 * @short Table to hold hash indexed objects.
 */
class __EXPORT MapTable : public Mutex
{
protected:
    friend class MapObject;
    friend class MapIndex;
    unsigned range;
  unsigned count;
    MapObject **map;

    void cleanup(void);

public:
    /**
     * Create a map table with a specified number of slots.
     *
     * @param number of slots.
     */
    MapTable(unsigned size);

    /**
     * Destroy the table, calls cleanup.
     */
    virtual ~MapTable();

    /**
     * Get index value from id string.  This function can be changed
     * as needed to provide better collision avoidence for specific
     * tables.
     *
     * @param id string
     * @return index slot in table.
     */
    virtual unsigned getIndex(const char *id);

    /**
     * Return range of this table.
     *
     * @return table range.
     */
    inline unsigned getRange(void)
        {return range;};

    /**
     * Return the number of object stored in this table.
     *
     * @return table size.
     */
    inline unsigned getSize(void)
        {return count;};

    /**
     * Lookup an object by id key.  It is returned as void * for
     * easy re-cast.
     *
     * @param key to find.
     * @return pointer to found object or NULL.
     */
    void *getObject(const char *id);

    /**
     * Map an object to our table.  If it is in another table
     * already, it is removed there first.
     *
     * @param object to map.
     */
    void addObject(MapObject &obj);
    /**
     * Get the first element into table, it is returned as void * for
     * easy re-cast.
     *
     * @return pointer to found object or NULL.
     */
    void *getFirst();

    /**
     * Get the last element into table, it is returned as void * for
     * easy re-cast.
     *
     * @return pointer to found object or NULL.
     */
    void *getLast();

    /**
     * Get table's end, useful for cycle control; it is returned as void * for
     * easy re-cast.
     *
     * @return pointer to found object or NULL.
     */
    void *getEnd()
        {   return NULL;    };

    /**
     * Get next object from managed free list.  This returns as a
     * void so it can be recast into the actual type being used in
     * derived MapObject's.  A derived version of MapTable may well
     * offer an explicit type version of this.  Some derived
     * MapObject's may override new to use managed list.
     *
     * @return next object on free list.
     */
    void *getFree(void);

    /**
     * Add an object to the managed free list.  Some MapObject's
     * may override delete operator to detach and do this.
     *
     * @param object to add.
     */
    void addFree(MapObject *obj);

    /**
     * An operator to map an object to the table.
     *
     * @return table being used.
     * @param object being mapped.
     */
    MapTable &operator+=(MapObject &obj);

    /**
     * This operator is virtual in case it must also add the object to a
     * managed free list.
     *
     * @return current table.
     * @param object entity to remove.
     */
    virtual MapTable &operator-=(MapObject &obj);
};

/**
 * The MapIndex allows linear access into a MapTable, that otherwise could have
 * its elements being retrieved only by key.
 * It can be increased, checked and dereferenced like a pointer, by means of
 * suitable operators.
 *
 * @author Sergio Repetto <s.repetto@pentaengineering.it>
 * @short Index object to access MapTable elements
 */
class __EXPORT MapIndex
{
    MapObject*  thisObject;

public :

    /**
     * Creates an empty map index (pointing to nothing).
     */
    MapIndex() : thisObject(NULL)
    {};

    /**
     * Creates a map index pointing to a specific map object
     *
     * @param the indexed object
     */
    MapIndex(MapObject* theObject) : thisObject(theObject)
    {};

    /**
     * Creates a copy of a given map index
     *
     * @param the source index object
     */
    MapIndex(const MapIndex& theIndex) : thisObject(theIndex.thisObject)
    {};

    /**
     * Dereference operator: the pointed object it is returned as void * for
     * easy re-cast.
     *
     * @return pointer to indexed object.
     */
  void* operator*() const
  { return (void*)thisObject;   }

    /**
     * Assignment operator to avoid implicit cast.
     *
     * @return the object itself, as changed.
     */
    MapIndex& operator=(MapObject *theObject);

    /**
     * Prefix increment operator, to be used in loops and such.
     *
     * @return the object itself, as changed.
     */
  MapIndex& operator++();           // prefix

    /**
     * Postfix increment operator, to be used in loops and such.
     *
     * @return the object itself, as changed.
     */
  MapIndex  operator++(int)     // postfix
    {   return this->operator++();  }

    /**
     * Comparison operator, between two MapIndex's.
     *
     * @return the object itself, as changed.
     */
    bool operator==(const MapIndex& theIndex) const
    {   return thisObject == theIndex.thisObject;   };

    bool operator!=(const MapIndex& theIndex) const
    {   return !(*this == theIndex);    };

    /**
     * Comparison operator, between the MapIndex and a MapObject, useful to avoid
   * casts for sake of clearness.
     *
     * @return the object itself, as changed.
     */
    bool operator==(const MapObject* theObject) const
    {   return thisObject == theObject; };

    bool operator!=(const MapObject* theObject) const
    {   return !(*this == theObject);   };
};

/**
 * The MapObject is a base class which can be used to make a derived
 * class operate on a MapTable.  Derived classes may override new and
 * delete operators to use managed free list from a MapTable.
 *
 * @author David Sugar <dyfet@gnutelephony.org>
 * @short Mappable object.
 */
class __EXPORT MapObject
{
protected:
    friend class MapTable;
    friend class MapIndex;
    MapObject *nextObject;
    const char *idObject;
    MapTable *table;

public:

    /**
     * Remove the object from it's current table.
     */
    void detach(void);

    /**
     * Save id, mark as not using any table.
     *
     * @param id string for this object.
     */
    MapObject(const char *id);
};

END_NAMESPACE

#endif
