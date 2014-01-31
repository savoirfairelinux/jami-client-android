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

/**
 * Realtime timers and timer queues.
 * This offers ucommon support for realtime high-resolution threadsafe
 * timers and timer queues.  Threads may be scheduled by timers and timer
 * queues may be used to inject timer events into callback objects or through
 * virtuals.
 * @file ucommon/timers.h
 */

#ifndef _UCOMMON_TIMERS_H_
#define _UCOMMON_TIMERS_H_

#ifndef _UCOMMON_LINKED_H_
#include <ucommon/linked.h>
#endif

#ifndef _MSWINDOWS_
#include <unistd.h>
#include <sys/time.h>
#endif

#include <time.h>

NAMESPACE_UCOMMON

/**
 * Timer class to use when scheduling realtime events.  The timer generally
 * uses millisecond values but has a microsecond accuracy.  On platforms that
 * support it, the timer uses posix realtime monotonic clock extensions,
 * otherwise lower accuracy timer systems might be used.
 */
class __EXPORT Timer
{
private:
    friend class Conditional;
    friend class Semaphore;
    friend class Event;

#if _POSIX_TIMERS > 0 && defined(POSIX_TIMERS)
    timespec timer;
#else
#undef  POSIX_TIMERS    // make sure not used if no support
    timeval timer;
#endif
    bool updated;

protected:
    /**
     * Check if timer has been updated since last check.
     * @return true if updated.
     */
    bool update(void);

    /**
     * Check if timer active.
     * @return true if active.
     */
    bool is_active(void);

public:
#if _MSC_VER > 1400        // windows broken dll linkage issue...
    static const timeout_t inf = ((timeout_t)(-1));
    static const time_t reset = ((time_t)(0));
#else
    static const timeout_t inf; /**< A value to use for infinite time */
    static const time_t reset;  /**< A value to use when resetting */
#endif

#ifdef  _MSWINDOWS_
    typedef unsigned __int64 tick_t;
#else
    typedef uint64_t tick_t;
#endif

    /**
     * Construct an untriggered timer set to the time of creation.
     */
    Timer();

    /**
     * Construct a triggered timer that expires at specified offset.
     * @param offset to expire in milliseconds.
     */
    Timer(timeout_t offset);

    /**
     * Construct a triggered timer that expires at specified offset.
     * @param offset to expire in seconds.
     */
    Timer(time_t offset);

    /**
     * Construct a timer from a copy of another timer.
     * @param copy of timer to construct from.
     */
    Timer(const Timer& copy);

    /**
     * Set the timer to expire.
     * @param expire time in milliseconds.
     */
    void set(timeout_t expire);

    /**
     * Set the timer to expire.
     * @param expire time in seconds.
     */
    void set(time_t expire);

    /**
     * Set (update) the timer with current time.
     */
    void set(void);

    /**
     * Clear pending timer, has no value.
     */
    void clear(void);

    /**
     * Get remaining time until the timer expires.
     * @return 0 if expired or milliseconds still waiting.
     */
    timeout_t get(void) const;

    /**
     * Get remaining time until timer expires by reference.
     * @return 0 if expired or milliseconds still waiting.
     */
    inline timeout_t operator*() const
        {return get();};

    /**
     * Check if timer has expired.
     * @return true if timer still pending.
     */
    bool operator!() const;

    /**
     * Check if timer expired for is() expression.
     * @return true if timer expired.
     */
    operator bool() const;

    /**
     * Set timer expiration.
     * @param expire timer in specified seconds.
     */
    Timer& operator=(time_t expire);

    /**
     * Set timer expiration.
     * @param expire timer in milliseconds.
     */
    Timer& operator=(timeout_t expire);

    /**
     * Adjust timer expiration.
     * @param expire time to add in seconds.
     */
    Timer& operator+=(time_t expire);

    /**
     * Adjust timer expiration.
     * @param expire time to add in milliseconds.
     */
    Timer& operator+=(timeout_t expire);

    /**
     * Adjust timer expiration.
     * @param expire time to subtract in seconds.
     */
    Timer& operator-=(time_t expire);

    /**
     * Adjust timer expiration.
     * @param expire time to subtract in milliseconds.
     */
    Timer& operator-=(timeout_t expire);

    /**
     * Compute difference between two timers.
     * @param timer to use for difference.
     * @return difference in milliseconds.
     */
    timeout_t operator-(const Timer& timer);

    /**
     * Compare timers if same timeout.
     * @param timer to compare with.
     * @return true if same.
     */
    bool operator==(const Timer& timer) const;

    /**
     * Compare timers if not same timeout.
     * @param timer to compare with.
     * @return true if not same.
     */
    bool operator!=(const Timer& timer) const;

    /**
     * Compare timers if earlier timeout than another timer.
     * @param timer to compare with.
     * @return true if earlier.
     */
    bool operator<(const Timer& timer) const;

    /**
     * Compare timers if earlier than or equal to another timer.
     * @param timer to compare with.
     * @return true if earlier or same.
     */
    bool operator<=(const Timer& timer) const;

    /**
     * Compare timers if later timeout than another timer.
     * @param timer to compare with.
     * @return true if later.
     */
    bool operator>(const Timer& timer) const;

    /**
     * Compare timers if later than or equal to another timer.
     * @param timer to compare with.
     * @return true if later or same.
     */
    bool operator>=(const Timer& timer) const;

    /**
     * Sleep current thread until the specified timer expires.
     * @param timer to reference for sleep.
     */
    static void sync(Timer &timer);

    /**
     * Get timer ticks since uuid epoch.
     * @return timer ticks in 100ns resolution.
     */
    static tick_t ticks(void);
};

/**
 * A timer queue for timer events.  The timer queue is used to hold a
 * linked list of timers that must be processed together.  The timer
 * queue processes the timer event list and calls an expired function
 * on events that have expired.  The timer queue also determines the
 * wait time until the next timer will expire.  When timer events are
 * modified, they can retrigger the queue to re-examine the list to
 * find when the next timer will now expire.
 * @author David Sugar <dyfet@gnutelephony.org>
 */
class __EXPORT TimerQueue : public OrderedIndex
{
public:
    /**
     * A timer event object that lives on a timer queue.  Timer events are
     * triggered through the timer queue's expire method.  Timer events
     * also modify the queue when they are changed, particularly to force
     * re-evaluation of the expiration period.  This class is not used by
     * itself but rather as a base class for a timer event object.
     * @author David Sugar <dyfet@gnutelephony.org>
     */
    class __EXPORT event : protected Timer, public LinkedList
    {
    protected:
        friend class TimerQueue;

        /**
         * Construct a timer event object and initially arm.
         * @param expire timer in specified milliseconds.
         */
        event(timeout_t expire);

        /**
         * Construct an armed timer event object and attach to queue.
         * @param queue to add event to.
         * @param expire timer in specified milliseconds.
         */
        event(TimerQueue *queue, timeout_t expire);

        /**
         * Event method to call in derived class when timer expires.
         */
        virtual void expired(void) = 0;

        /**
         * Expected next timeout for the timer.  This may be overriden
         * for strategy purposes when evaluted by timer queue's expire.
         * @return milliseconds until timer next triggers.
         */
        virtual timeout_t timeout(void);

    public:
        /**
         * Detaches from queue when destroyed.
         */
        virtual ~event();

        /**
         * Attach event to a timer queue.  Detaches from previous list if
         * already attached elsewhere.
         * @param queue to attach to.
         */
        void attach(TimerQueue *queue);

        /**
         * Detach event from a timer queue.
         */
        void detach(void);

        /**
         * Arm event to trigger at specified timeout.
         * @param timeout to expire and trigger.
         */
        void arm(timeout_t timeout);

        /**
         * Disarm event.
         */
        void disarm(void);

        /**
         * Time remaining until expired.
         * @return milliseconds until timer expires.
         */
        inline timeout_t get(void) const
            {return Timer::get();};

        /**
         * Notify timer queue that the timer has been updated.
         */
        void update(void);

        /**
         * Get the timer queue we are attached to.
         * @return timer queue or NULL if not attached.
         */
        inline TimerQueue *list(void)
            {return static_cast<TimerQueue*>(Root);};
    };

protected:
    friend class event;

    /**
     * Called in derived class when the queue is being modified.
     * This is often used to lock the list.
     */
    virtual void modify(void) = 0;

    /**
     * Called in derived class after the queue has been modified.  This often
     * releases a lock that modify set and to wakeup a timer thread to
     * evaluate when the next timer will now expire.
     */
    virtual void update(void) = 0;

public:
    /**
     * Create an empty timer queue.
     */
    TimerQueue();

    /**
     * Destroy queue, does not remove event objects.
     */
    virtual ~TimerQueue();

    /**
     * Add a timer event to the timer queue.
     * @param timer event to add.
     */
    void operator+=(event &timer);

    /**
     * Remove a timer event from the timer queue.
     * @param timer event to remove.
     */
    void operator-=(event &timer);

    /**
     * Process timer queue and find when next event triggers.  This function
     * will call the expired methods on expired timers.  Normally this function
     * will be called in the context of a timer thread which sleeps for the
     * timeout returned unless it is awoken on an update event.
     * @return timeout until next timer expires in milliseconds.
     */
    timeout_t expire();
};

/**
 * A convenience type for timer queue timer events.
 */
typedef TimerQueue::event TQEvent;

/**
 * A convenience type for timers.
 */
typedef Timer timer_t;

END_NAMESPACE

extern "C" {
#if defined(WIN32)
    __EXPORT int gettimeofday(struct timeval *tv, void *tz);
#endif
}

#endif
