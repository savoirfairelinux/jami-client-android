package cx.ring.utils;

public interface Observer<T> {

    void update(Observable observable, T argument);

}
