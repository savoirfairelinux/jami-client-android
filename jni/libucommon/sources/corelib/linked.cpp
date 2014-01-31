// Copyright (C) 2006-2010 David Sugar, Tycho Softworks.
//
// This file is part of GNU uCommon C++.
//
// GNU uCommon C++ is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// GNU uCommon C++ is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with GNU uCommon C++.  If not, see <http://www.gnu.org/licenses/>.

#include <ucommon-config.h>
#include <ucommon/export.h>
#include <ucommon/linked.h>
#include <ucommon/string.h>
#include <ucommon/thread.h>

using namespace UCOMMON_NAMESPACE;

// I am not sure if we ever use these...seemed like a good idea at the time
const LinkedObject *LinkedObject::nil = (LinkedObject *)NULL;
const LinkedObject *LinkedObject::inv = (LinkedObject *)-1;

MultiMap::MultiMap(unsigned count) : ReusableObject()
{
    assert(count > 0);

    paths = count;
    links = new link_t[count];
    // just a cheap way to initially set the links to all NULL...
    memset(links, 0, sizeof(link_t) * count);
}

MultiMap::~MultiMap()
{
    unsigned path = 0;

    while(path < paths)
        delist(path++);

    delete[] links;
}

MultiMap *MultiMap::next(unsigned path) const
{
    assert(path < paths);

    return links[path].next;
}

void MultiMap::delist(unsigned path)
{
    assert(path < paths);

    if(!links[path].root)
        return;

    while(links[path].root) {
        if(*links[path].root == this) {
            *links[path].root = next(path);
            break;
        }
        links[path].root = &((*links[path].root)->links[path].next);
    }
    links[path].root = NULL;
    links[path].next = NULL;
}

void MultiMap::enlist(unsigned path, MultiMap **root)
{
    assert(path < paths);
    assert(root != NULL);

    delist(path);
    links[path].next = *root;
    links[path].root = root;
    links[path].key = NULL;
    links[path].keysize = 0;
    *root = this;
}

void MultiMap::enlist(unsigned path, MultiMap **root, caddr_t key, unsigned max, size_t keysize)
{
    assert(path < paths);
    assert(root != NULL);
    assert(key != NULL);
    assert(max > 0);

    unsigned value = 0;

    delist(path);
    while(keysize && !key[0]) {
        ++key;
        --keysize;
    }

    while(keysize--)
        value = (value << 1) ^ (*key++);

    enlist(path, &root[keyindex(key, max, keysize)]);

    if(!keysize)
        keysize = strlen(key);

    links[path].keysize = keysize;
    links[path].key = key;
}

bool MultiMap::equal(unsigned path, caddr_t key, size_t keysize) const
{
    assert(path < paths);
    assert(key != NULL);

    if(!keysize)
        keysize = strlen(key);

    if(links[path].keysize != keysize)
        return false;

    if(memcmp(key, links[path].key, links[path].keysize) == 0)
        return true;

    return false;
}

// this transforms either strings or binary fields (such as addresses)
// into a hash index.
unsigned MultiMap::keyindex(caddr_t key, unsigned max, size_t keysize)
{
    assert(key != NULL);
    assert(max > 0);

    unsigned value = 0;

    // if we are a string, we can just used our generic text hasher
    if(!keysize)
        return NamedObject::keyindex(key, max);

    // strip off lead 0's...just saves time for data that is not changing,
    // especially when little endian 64 bit wide...
    while(keysize && !key[0]) {
        ++key;
        --keysize;
    }

    while(keysize--)
        value = (value << 1) ^ (*key++);

    return value % max;
}

MultiMap *MultiMap::find(unsigned path, MultiMap **root, caddr_t key, unsigned max, size_t keysize)
{
    assert(key != NULL);
    assert(max > 0);

    MultiMap *node = root[keyindex(key, max, keysize)];

    while(node) {
        if(node->equal(path, key, keysize))
            break;
        node = node->next(path);
    }

    return node;
}

LinkedObject::LinkedObject(LinkedObject **root)
{
    assert(root != NULL);
    enlist(root);
}

LinkedObject::~LinkedObject()
{
}

void LinkedObject::purge(LinkedObject *root)
{
    LinkedObject *after;

    assert(root != NULL);

    while(root) {
        after = root->Next;
        root->release();
        root = after;
    }
}

bool LinkedObject::is_member(LinkedObject *list) const
{
    assert(list != NULL);

    while(list) {
        if(list == this)
            return true;
        list = list->Next;
    }
    return false;
}

void LinkedObject::enlist(LinkedObject **root)
{
    assert(root != NULL);

    Next = *root;
    *root = this;
}

void LinkedObject::delist(LinkedObject **root)
{
    assert(root != NULL);

    LinkedObject *prior = NULL, *node = *root;

    while(node && node != this) {
        prior = node;
        node = node->Next;
    }

    if(!node)
        return;

    if(!prior)
        *root = Next;
    else
        prior->Next = Next;
}

void ReusableObject::release(void)
{
    Next = (LinkedObject *)nil;
}

NamedObject::NamedObject() :
OrderedObject()
{
    Id = NULL;
}

NamedObject::NamedObject(OrderedIndex *root, char *nid) :
OrderedObject()
{
    assert(root != NULL);
    assert(nid != NULL && *nid != 0);

    NamedObject *node = static_cast<NamedObject*>(root->head), *prior = NULL;

    while(node) {
        if(node->equal(nid)) {
            if(prior)
                prior->Next = node->getNext();
            else
                root->head = node->getNext();
            node->release();
            break;
        }
        prior = node;
        node = node->getNext();
    }
    Next = NULL;
    Id = nid;
    if(!root->head)
        root->head = this;
    if(!root->tail)
        root->tail = this;
    else
        root->tail->Next = this;
}

// One thing to watch out for is that the id is freed in the destructor.
// This means that you should use a dup'd string for your nid.  Otherwise
// you will need to set it to NULL before destroying the object.

NamedObject::NamedObject(NamedObject **root, char *nid, unsigned max) :
OrderedObject()
{
    assert(root != NULL);
    assert(nid != NULL && *nid != 0);
    assert(max > 0);

    Id = NULL;
    add(root, nid, max);
}

void NamedObject::add(NamedObject **root, char *nid, unsigned max)
{
    assert(root != NULL);
    assert(nid != NULL && *nid != 0);
    assert(max > 0);

    clearId();

    NamedObject *node, *prior = NULL;

    if(max < 2)
        max = 0;
    else
        max = keyindex(nid, max);

    node = root[max];
    while(node) {
        if(node && node->equal(nid)) {
            if(prior) {
                prior->Next = this;
                Next = node->Next;
            }
            else
                root[max] = node->getNext();
            node->release();
            break;
        }
        prior = node;
        node = node->getNext();
    }

    if(!node) {
        Next = root[max];
        root[max] = this;
    }
    Id = nid;
}

void NamedObject::clearId(void)
{
    if(Id) {
        free(Id);
        Id = NULL;
    }
}

NamedObject::~NamedObject()
{
    // this assumes the id is a malloc'd or strdup'd string.
    // maybe overridden if virtual...

    clearId();
}

// Linked objects are assumed to be freeable if they are released.  The retain
// simply marks it into a self reference state which can never otherwise happen
// naturally.  This is used to mark avoid freeing during release.

void LinkedObject::retain(void)
{
    Next = this;
}


void LinkedObject::release(void)
{
    if(Next != this) {
        Next = this;
        delete this;
    }
}

LinkedObject *LinkedObject::getIndexed(LinkedObject *root, unsigned index)
{
    while(index-- && root != NULL)
        root = root->Next;

    return root;
}

unsigned LinkedObject::count(const LinkedObject *root)
{
    assert(root != NULL);

    unsigned c = 0;
    while(root) {
        ++c;
        root = root->Next;
    }
    return c;
}

unsigned NamedObject::keyindex(const char *id, unsigned max)
{
    assert(id != NULL && *id != 0);
    assert(max > 1);

    unsigned val = 0;

    while(*id)
        val = (val << 1) ^ (*(id++) & 0x1f);

    return val % max;
}

int NamedObject::compare(const char *cid) const
{
    assert(cid != NULL && *cid != 0);

#ifdef  HAVE_STRCOLL
    return strcoll(Id, cid);
#else
    return strcmp(Id, cid);
#endif
}

extern "C" {

    static int ncompare(const void *o1, const void *o2)
    {
        assert(o1 != NULL);
        assert(o2 != NULL);
        const NamedObject * const *n1 = static_cast<const NamedObject * const*>(o1);
        const NamedObject * const*n2 = static_cast<const NamedObject * const*>(o2);
        return ((*n1)->compare((*n2)->getId()));
    }
}

NamedObject **NamedObject::sort(NamedObject **list, size_t size)
{
    assert(list != NULL);

    if(!size) {
        while(list[size])
            ++size;
    }

    qsort(static_cast<void *>(list), size, sizeof(NamedObject *), &ncompare);
    return list;
}

NamedObject **NamedObject::index(NamedObject **idx, unsigned max)
{
    assert(idx != NULL);
    assert(max > 0);
    NamedObject **op = new NamedObject *[count(idx, max) + 1];
    unsigned pos = 0;
    NamedObject *node = skip(idx, NULL, max);

    while(node) {
        op[pos++] = node;
        node = skip(idx, node, max);
    }
    op[pos] = NULL;
    return op;
}

NamedObject *NamedObject::skip(NamedObject **idx, NamedObject *rec, unsigned max)
{
    assert(idx != NULL);
    assert(max > 0);

    unsigned key = 0;
    if(rec && !rec->Next)
        key = keyindex(rec->Id, max) + 1;

    if(!rec || !rec->Next) {
        while(key < max && !idx[key])
            ++key;
        if(key >= max)
            return NULL;
        return idx[key];
    }

    return rec->getNext();
}

void NamedObject::purge(NamedObject **idx, unsigned max)
{
    assert(idx != NULL);
    assert(max > 0);

    LinkedObject *root;

    if(max < 2)
        max = 0;

    while(max--) {
        root = idx[max];
        LinkedObject::purge(root);
    }
}

unsigned NamedObject::count(NamedObject **idx, unsigned max)
{
    assert(idx != NULL);
    assert(max > 0);

    unsigned count = 0;
    LinkedObject *node;

    if(max < 2)
        max = 1;

    while(max--) {
        node = idx[max];
        while(node) {
            ++count;
            node = node->Next;
        }
    }
    return count;
}

NamedObject *NamedObject::remove(NamedObject **idx, const char *id, unsigned max)
{
    assert(idx != NULL);
    assert(id != NULL && *id != 0);
    assert(max > 0);

    if(max < 2)
        return remove(idx, id);

    return remove(&idx[keyindex(id, max)], id);
}

NamedObject *NamedObject::map(NamedObject **idx, const char *id, unsigned max)
{
    assert(idx != NULL);
    assert(id != NULL && *id != 0);
    assert(max > 0);

    if(max < 2)
        return find(*idx, id);

    return find(idx[keyindex(id, max)], id);
}

NamedObject *NamedObject::find(NamedObject *root, const char *id)
{
    assert(id != NULL && *id != 0);

    while(root) {
        if(root->equal(id))
            break;
        root = root->getNext();
    }
    return root;
}

NamedObject *NamedObject::remove(NamedObject **root, const char *id)
{
    assert(id != NULL && *id != 0);
    assert(root != NULL);

    NamedObject *prior = NULL;
    NamedObject *node = *root;

    while(node) {
        if(node->equal(id))
            break;
        prior = node;
        node = node->getNext();
    }

    if(!node)
        return NULL;

    if(prior == NULL)
        *root = node->getNext();
    else
        prior->Next = node->getNext();

    return node;
}

// Like in NamedObject, the nid that is used will be deleted by the
// destructor through calling purge.  Hence it should be passed from
// a malloc'd or strdup'd string.

NamedTree::NamedTree(char *nid) :
NamedObject(), Child()
{
    Id = nid;
    Parent = NULL;
}

NamedTree::NamedTree(const NamedTree& source)
{
    Id = source.Id;
    Parent = NULL;
    Child = source.Child;
}

NamedTree::NamedTree(NamedTree *p, char *nid) :
NamedObject(), Child()
{
    assert(p != NULL);
    assert(nid != NULL && *nid != 0);

    enlistTail(&p->Child);
    Id = nid;
    Parent = p;
}

NamedTree::~NamedTree()
{
    Id = NULL;
    purge();
}

NamedTree *NamedTree::getChild(const char *tid) const
{
    assert(tid != NULL && *tid != 0);

    linked_pointer<NamedTree> node = Child.begin();

    while(node) {
        if(eq(node->Id, tid))
            return *node;
        node.next();
    }
    return NULL;
}

void NamedTree::relistTail(NamedTree *trunk)
{
    // if moving to same place, just return...
    if(Parent == trunk)
        return;

    if(Parent)
        delist(&Parent->Child);
    Parent = trunk;
    if(Parent)
        enlistTail(&Parent->Child);
}

void NamedTree::relistHead(NamedTree *trunk)
{
    if(Parent == trunk)
        return;

    if(Parent)
        delist(&Parent->Child);
    Parent = trunk;
    if(Parent)
        enlistHead(&Parent->Child);
}

NamedTree *NamedTree::path(const char *tid) const
{
    assert(tid != NULL && *tid != 0);

    const char *np;
    char buf[65];
    char *ep;
    NamedTree *node = const_cast<NamedTree*>(this);

    if(!tid || !*tid)
        return const_cast<NamedTree*>(this);

    while(*tid == '.') {
        if(!node->Parent)
            return NULL;
        node = node->Parent;

        ++tid;
    }

    while(tid && *tid && node) {
        String::set(buf, sizeof(buf), tid);
        ep = strchr(buf, '.');
        if(ep)
            *ep = 0;
        np = strchr(tid, '.');
        if(np)
            tid = ++np;
        else
            tid = NULL;
        node = node->getChild(buf);
    }
    return node;
}

NamedTree *NamedTree::getLeaf(const char *tid) const
{
    assert(tid != NULL && *tid != 0);

    linked_pointer<NamedTree> node = Child.begin();

    while(node) {
        if(node->is_leaf() && eq(node->Id, tid))
            return *node;
        node.next();
    }
    return NULL;
}

NamedTree *NamedTree::leaf(const char *tid) const
{
    assert(tid != NULL && *tid != 0);

    linked_pointer<NamedTree> node = Child.begin();
    NamedTree *obj;

    while(node) {
        if(node->is_leaf() && eq(node->Id, tid))
            return *node;
        obj = NULL;
        if(!node->is_leaf())
            obj = node->leaf(tid);
        if(obj)
            return obj;
        node.next();
    }
    return NULL;
}

NamedTree *NamedTree::find(const char *tid) const
{
    assert(tid != NULL && *tid != 0);

    linked_pointer<NamedTree> node = Child.begin();
    NamedTree *obj;

    while(node) {
        if(!node->is_leaf()) {
            if(eq(node->Id, tid))
                return *node;
            obj = node->find(tid);
            if(obj)
                return obj;
        }
        node.next();
    }
    return NULL;
}

void NamedTree::setId(char *nid)
{
    assert(nid != NULL && *nid != 0);

    Id = nid;
}

// If you remove the tree node, the id is NULL'd also.  This keeps the
// destructor from freeing it.

void NamedTree::remove(void)
{
    if(Parent)
        delist(&Parent->Child);

    Id = NULL;
}

void NamedTree::purge(void)
{
    linked_pointer<NamedTree> node = Child.begin();
    NamedTree *obj;

    if(Parent)
        delist(&Parent->Child);

    while(node) {
        obj = *node;
        obj->Parent = NULL; // save processing
        node = obj->getNext();
        delete obj;
    }

    // this assumes the object id is a malloc'd/strdup string.
    // may be overridden if virtual...
    clearId();
}

LinkedObject::LinkedObject()
{
    Next = 0;
}

OrderedObject::OrderedObject() : LinkedObject()
{
}

OrderedObject::OrderedObject(OrderedIndex *root) :
LinkedObject()
{
    assert(root != NULL);
    Next = NULL;
    enlistTail(root);
}

void OrderedObject::delist(OrderedIndex *root)
{
    assert(root != NULL);

    OrderedObject *prior = NULL, *node;

    node = root->head;

    while(node && node != this) {
        prior = node;
        node = node->getNext();
    }

    if(!node)
        return;

    if(!prior)
        root->head = getNext();
    else
        prior->Next = Next;

    if(this == root->tail)
        root->tail = prior;
}

void OrderedObject::enlist(OrderedIndex *root)
{
    assert(root != NULL);

    Next = NULL;
    enlistTail(root);
}

void OrderedObject::enlistTail(OrderedIndex *root)
{
    assert(root != NULL);

    if(root->head == NULL)
        root->head = this;
    else if(root->tail)
        root->tail->Next = this;

    root->tail = this;
}

void OrderedObject::enlistHead(OrderedIndex *root)
{
    assert(root != NULL);

    Next = NULL;
    if(root->tail == NULL)
        root->tail = this;
    else if(root->head)
        Next = root->head;

    root->head = this;
}

LinkedList::LinkedList()
{
    Root = 0;
    Prev = 0;
    Next = 0;
}

LinkedList::LinkedList(OrderedIndex *r)
{
    Root = NULL;
    Next = Prev = 0;
    if(r)
        enlist(r);
}

void LinkedList::enlist(OrderedIndex *r)
{
    assert(r != NULL);

    enlistTail(r);
}

void LinkedList::insert(LinkedList *o)
{
    assert(o != NULL);

    insertTail(o);
}

void LinkedList::insertHead(LinkedList *o)
{
    assert(o != NULL);

    if(o->Root)
        o->delist();

    if(Prev) {
        o->Next = this;
        o->Prev = Prev;
    }
    else {
        Root->head = o;
        o->Prev = NULL;
    }
    o->Root = Root;
    o->Next = this;
    Prev = o;
}

void LinkedList::insertTail(LinkedList *o)
{
    assert(o != NULL);

    if(o->Root)
        o->delist();

    if(Next) {
        o->Prev = this;
        o->Next = Next;
    }
    else {
        Root->tail = o;
        o->Next = NULL;
    }
    o->Root = Root;
    o->Prev = this;
    Next = o;
}

void LinkedList::enlistHead(OrderedIndex *r)
{
    assert(r != NULL);

    if(Root)
        delist();

    Root = r;
    Prev = 0;
    Next = 0;

    if(!Root->tail) {
        Root->tail = Root->head = static_cast<OrderedObject *>(this);
        return;
    }

    Next = static_cast<LinkedList *>(Root->head);
    ((LinkedList*)Next)->Prev = this;
    Root->head = static_cast<OrderedObject *>(this);
}


void LinkedList::enlistTail(OrderedIndex *r)
{
    assert(r != NULL);

    if(Root)
        delist();

    Root = r;
    Prev = 0;
    Next = 0;

    if(!Root->head) {
        Root->head = Root->tail = static_cast<OrderedObject *>(this);
        return;
    }

    Prev = static_cast<LinkedList *>(Root->tail);
    Prev->Next = this;
    Root->tail = static_cast<OrderedObject *>(this);
}

void LinkedList::delist(void)
{
    if(!Root)
        return;

    if(Prev)
        Prev->Next = Next;
    else if(Root->head == static_cast<OrderedObject *>(this))
        Root->head = static_cast<OrderedObject *>(Next);

    if(Next)
        (static_cast<LinkedList *>(Next))->Prev = Prev;
    else if(Root->tail == static_cast<OrderedObject *>(this))
        Root->tail = static_cast<OrderedObject *>(Prev);

    Root = 0;
    Next = Prev = 0;
}

LinkedList::~LinkedList()
{
    delist();
}

OrderedIndex::OrderedIndex()
{
    head = tail = NULL;
}

OrderedIndex::~OrderedIndex()
{
    head = tail = NULL;
}

void OrderedIndex::copy(const OrderedIndex& source)
{
    head = source.head;
    tail = source.tail;
}

void OrderedIndex::operator*=(OrderedObject *object)
{
    assert(object != NULL);

    object->enlist(this);
}

void OrderedIndex::add(OrderedObject *object)
{
    assert(object != NULL);

    object->enlist(this);
}

LinkedObject *OrderedIndex::get(void)
{
    LinkedObject *node;

    if(!head)
        return NULL;

    node = head;
    head = static_cast<OrderedObject *>(node->getNext());
    if(!head)
        tail = NULL;

    return static_cast<LinkedObject *>(node);
}

void OrderedIndex::purge(void)
{
    if(head) {
        LinkedObject::purge((LinkedObject *)head);
        head = tail = NULL;
    }
}

void OrderedIndex::reset(void)
{
    head = tail = NULL;
}

void OrderedIndex::lock_index(void)
{
}

void OrderedIndex::unlock_index(void)
{
}

DLinkedObject::DLinkedObject() : OrderedObject()
{
    Prev = NULL;
}

void DLinkedObject::delist(void)
{
    if(Prev)
        Prev->Next = Next;

    if(Next)
        ((DLinkedObject *)Next)->Prev = Prev;

    Next = Prev = NULL;
}

ObjectQueue::ObjectQueue() :
OrderedIndex() {}

void ObjectQueue::add(DLinkedObject *object)
{
    assert(object);

    if(tail) {
        ((DLinkedObject *)tail)->Next = object;
        object->Prev = (DLinkedObject *)tail;
    }

    object->Next = NULL;
    tail = object;
    if(!head)
        head = tail;
}

void ObjectQueue::push(DLinkedObject *object)
{
    assert(object);

    if(head) {
        ((DLinkedObject *)head)->Prev = object;
        object->Next = (DLinkedObject *)head;
    }
    object->Prev = NULL;
    head = object;
    if(!tail)
        tail = head;
}

DLinkedObject *ObjectQueue::pull(void)
{
    DLinkedObject *obj = (DLinkedObject *)head;

    if(!obj)
        return NULL;

    head = (OrderedObject *)(obj->Next);
    if(!head)
        tail = NULL;
    obj->delist();
    return obj;
}

DLinkedObject *ObjectQueue::pop(void)
{
    DLinkedObject *obj = (DLinkedObject *)tail;

    if(!obj)
        return NULL;

    tail = (OrderedObject *)(obj->Prev);
    if(!tail)
        head = NULL;
    obj->delist();
    return obj;
}

LinkedObject **OrderedIndex::index(void) const
{
    LinkedObject **op = new LinkedObject *[count() + 1];
    LinkedObject *node;
    unsigned idx = 0;

    node = head;
    while(node) {
        op[idx++] = node;
        node = node->Next;
    }
    op[idx] = NULL;
    return op;
}

LinkedObject *OrderedIndex::find(unsigned index) const
{
    unsigned count = 0;
    LinkedObject *node;

    node = head;

    while(node && ++count < index)
        node = node->Next;

    return node;
}

unsigned OrderedIndex::count(void) const
{
    unsigned count = 0;
    LinkedObject *node;

    node = head;

    while(node) {
        node = node->Next;
        ++count;
    }
    return count;
}

ObjectStack::ObjectStack()
{
    root = NULL;
}

ObjectStack::ObjectStack(LinkedObject *list)
{
    root = list;
}

void ObjectStack::push(LinkedObject *list)
{
    assert(list != NULL);

    list->Next = root;
    root = list;
}

LinkedObject *ObjectStack::pull(void)
{
    LinkedObject *obj;

    obj = root;

    if(obj) {
        root = obj->Next;
        obj->Next = NULL;
    }

    return obj;
}

