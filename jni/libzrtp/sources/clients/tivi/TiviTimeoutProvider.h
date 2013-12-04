/*
 *  Copyright (C) 2006, 2005, 2004 Erik Eliasson, Johan Bilien, Werner Dittmann
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */


#ifndef _TIMEOUTPROVIDER_H_
#define _TIMEOUTPROVIDER_H_

/**
 * Provides a way to request timeouts after a number of milli seconds.
 *
 * A command is associated to each timeout.
 *
 * Modified to use the common c++ library functions and the STL
 * list by Werner Dittmann.
 *
 * @author Erik Eliasson, eliasson@it.kth.se, 2003
 * @author Werner Dittmann <Werner.Dittmann@t-online.de>
 */

#include <list>
#include <stdint.h>

#include <common/Thread.h>
#include <common/osSpecifics.h>

/**
 * Represents a request of a "timeout" (delivery of a command to a
 * "timeout receiver" after at least a specified time period).
 *
 * Slightly modified to use gettimeofday directly.
 *
 * @author Werner Dittmann
 */
template <class TOCommand, class TOSubscriber>
class TPRequest
{

public:

    TPRequest( TOSubscriber tsi, int timeoutMs, const TOCommand &command):
        subscriber(tsi)
    {
        when_ms = zrtpGetTickCount();

        when_ms += timeoutMs;
        this->command = command;
    }

    /**
     * @param t  ms since Epoch
     */
    bool happensBefore(uint64_t t)
    {
        if (when_ms < t) {
            return true;
        }
        if (when_ms > t) {
            return false;
        }
        return false; // if equal it does not "happens_before"

    }

    bool happensBefore(const TPRequest *req){
        return happensBefore(req->when_ms);
    }

    /**
     * Number of milli seconds until timeout from when this method is
     * called
     */
    int getMsToTimeout ()
    {
        uint64_t now = zrtpGetTickCount();

        if (happensBefore(now)) {
            return 0;
        }
        else {
            return (int)(when_ms - now);
        }
    }

    TOCommand getCommand()
    {
        return command;
    }

    TOSubscriber getSubscriber()
    {
        return subscriber;
    }

    /**
     * Two timeout requests are considered equeal if they have
     * the same subscriber AND command AND time when they
     * occur. If one of the time is zero then this is a
     * wildcard and matches always.
     */
    bool operator==(const TPRequest<TOCommand, TOSubscriber> &req)
    {
        if (req.subscriber == subscriber &&
            req.command == command &&
            req.when_ms == when_ms) {
            return true;
        }
        return false;
    }

private:
    TOSubscriber subscriber;
    uint64_t when_ms;     // Time since Epoch in ms when the timeout
    // will happen

    TOCommand command;      // Command that will be delivered to the
    // receiver (subscriber) of the timeout.
};

/**
 * Class to generate objects giving timeout functionality.
 *
 * @author Erik Eliasson
 * @author Werner Dittmann
 */
template<class TOCommand, class TOSubscriber>
class TimeoutProvider : public CThread {

private:

    // The timeouts are ordered in the order of which they
    // will expire. Nearest in future is first in list.
    std::list<TPRequest<TOCommand, TOSubscriber> *> requests;

    CMutexClass synchLock;
    CEventClass timeEvent;

    bool stop;      // Flag to tell the worker thread
    // to terminate. Set to true and
    // wake the worker thread to
    // terminate it.

public:

    /**
     * Timeout Provide Constructor
     */
    TimeoutProvider(): requests(), stop(false)  { }

    /**
     * Destructor also terminates the Timeout thread.
     */
    ~TimeoutProvider() {
        stop = true;
        timeEvent.Set();
    }

    /**
     * Terminates the Timeout provider thread.
     */
    void stopThread(){
        stop = true;
        timeEvent.Set();
    }

    /**
     * Request a timeout trigger.
     *
     * @param time_ms   Number of milli-seconds until the timeout is
     *          wanted. Note that a small additional period of time is
     *          added that depends on execution speed.
     * @param subscriber The receiver of the callback when the command has timed
     *          out. This argument must not be NULL.
     * @param command   Specifies the String command to be passed back in the
     *          callback.
     */
    void requestTimeout(int32_t time_ms, TOSubscriber subscriber, const TOCommand &command)
    {
        TPRequest<TOCommand, TOSubscriber>* request =
            new TPRequest<TOCommand, TOSubscriber>(subscriber, time_ms, command);

        synchLock.Lock();

        if (requests.size()==0) {
            requests.push_front(request);
            timeEvent.Set();
            synchLock.Unlock();
            return;
        }
        if (request->happensBefore(requests.front())) {
            requests.push_front(request);
            timeEvent.Set();
            synchLock.Unlock();
            return;
        }
        if (requests.back()->happensBefore(request)){
            requests.push_back(request);
            timeEvent.Set();
            synchLock.Unlock();
            return;
        }

        typename std::list<TPRequest<TOCommand, TOSubscriber>* >::iterator i;
        for(i = requests.begin(); i != requests.end(); i++ ) {
            if( request->happensBefore(*i)) {
                requests.insert(i, request);
                break;
            }
        }
        timeEvent.Set();
        synchLock.Unlock();
    }

    /**
     * Removes timeout requests that belong to a subscriber and command.
     *
     * @see requestTimeout
     */
    void cancelRequest(TOSubscriber subscriber, const TOCommand &command)
    {
        synchLock.Lock();
        typename std::list<TPRequest<TOCommand, TOSubscriber>* >::iterator i;
        for(i = requests.begin(); i != requests.end(); ) {
            if( (*i)->getCommand() == command &&
                (*i)->getSubscriber() == subscriber) {
                i = requests.erase(i);
                continue;
            }
            i++;
        }
        synchLock.Unlock();
    }

    virtual BOOL OnTask(LPVOID lpv)
    {
        do {
            synchLock.Lock();
            int32_t time = 3600000;
            int32_t size = 0;

            if ((size = requests.size()) > 0) {
                time = requests.front()->getMsToTimeout();
            }
            if (time == 0 && size > 0) {
                TPRequest<TOCommand, TOSubscriber>* req = requests.front();
                TOSubscriber subs = req->getSubscriber();
                TOCommand command = req->getCommand();

                requests.pop_front();
                if (stop) {         // This must be checked so that we will
                    synchLock.Unlock();
                    return FALSE;
                }
                synchLock.Unlock(); // call the command with free Mutex

                subs->handleTimeout(command);
                continue;
            }
            else 
                synchLock.Unlock();
            if (stop) {     // If we are told to exit while executing cmd
                return FALSE;
            }
            timeEvent.Wait(time);
            timeEvent.Reset();
            if (stop) {     // If we are told to exit while waiting we will exit
                return FALSE;
            }

        } while (true);
    }

};

#endif
