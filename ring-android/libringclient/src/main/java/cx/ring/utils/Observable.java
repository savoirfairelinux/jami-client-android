package cx.ring.utils;

import java.util.ArrayList;
import java.util.List;

public class Observable {

    private List<Observer> mObservers;
    private boolean mIsChanged;

    public synchronized void addObserver (Observer observer) {
        if (mObservers == null) {
            mObservers = new ArrayList<>();
        }

        mObservers.add(observer);
    }

    public synchronized void setChanged () {
        mIsChanged = true;
    }

    public synchronized void clearChanged () {
        mIsChanged = false;
    }

    public void notifyObservers () {
        notifyObservers(null);
    }

    public synchronized void notifyObservers (Object argument) {

        if (!mIsChanged) {
            return;
        }

        List<Observer> notifyObservers = new ArrayList<>(mObservers);
        for (Observer observer: notifyObservers) {
            observer.update(this, argument);
        }

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
