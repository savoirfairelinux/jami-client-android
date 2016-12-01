package cx.ring.utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import cx.ring.model.DaemonEvent;

public class Observable {

    private static final String TAG = Observable.class.getSimpleName();

    private List<WeakReference<Observer>> mObservers;
    private boolean mIsChanged;

    public synchronized void addObserver(Observer observer) {
        if (mObservers == null) {
            mObservers = new ArrayList<>();
        }

        mObservers.add(new WeakReference<Observer>(observer));
    }

    public synchronized void setChanged() {
        mIsChanged = true;
    }

    public synchronized void clearChanged() {
        mIsChanged = false;
    }

    public void notifyObservers() {
        notifyObservers(null);
    }

    public void notifyObservers(Object argument) {

        if (!mIsChanged) {
            return;
        }

        if (argument != null) {
            Log.d(TAG, "-----------------------> WILL NOTIFY "+countObservers()+" OBSERVERS WITH EVENT " + ((DaemonEvent) argument).getEventType());
        } else {
            Log.d(TAG, "-----------------------> WILL NOTIFY WITHOUT EVENT");
        }
        Log.d(TAG, "-----------------------> IN THREAD "+Thread.currentThread().getName()+" "+Thread.currentThread().getId());

        List<WeakReference<Observer>> notifyObservers = new ArrayList<>(mObservers);
        for (WeakReference<Observer> weakObserver : notifyObservers) {
            Observer realObserver = weakObserver.get();
            if (realObserver != null) {
                Log.d(TAG, "-----------------------> NOTIFY " + realObserver);

                realObserver.update(this, argument);
            }

        }

        Log.d(TAG, "-----------------------> FINISH NOTIFY");

        clearChanged();
    }

    public synchronized void deleteObserver(Observer observer) {
        mObservers.remove(observer);
    }

    public synchronized boolean hasChanged() {
        return mIsChanged;
    }

    public synchronized int countObservers() {
        return mObservers.size();
    }

}
