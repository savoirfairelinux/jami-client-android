package cx.ring.utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Observable {

    private List<WeakReference<Observer>> mObservers;
    private boolean mIsChanged;

    public synchronized void addObserver(Observer observer) {
        if (mObservers == null) {
            mObservers = new ArrayList<>();
        }

        mObservers.add(new WeakReference<>(observer));
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

        List<WeakReference<Observer>> notifyObservers = new ArrayList<>(mObservers);
        for (WeakReference<Observer> weakObserver : notifyObservers) {
            final Observer realObserver = weakObserver.get();
            if (realObserver != null) {
                realObserver.update(this, argument);
            }

        }

        clearChanged();
    }

    public synchronized void removeObserver(Observer observerToRemove) {

        if (observerToRemove == null) {
            return;
        }

        Iterator<WeakReference<Observer>> observerIterator = mObservers.iterator();
        while (observerIterator.hasNext()) {
            WeakReference<Observer> weakObserver = observerIterator.next();
            Observer observer = weakObserver.get();
            if (observerToRemove == observer) {
                observerIterator.remove();
            }
        }
    }

    public synchronized boolean hasChanged() {
        return mIsChanged;
    }

    public synchronized int countObservers() {
        return mObservers.size();
    }

}
