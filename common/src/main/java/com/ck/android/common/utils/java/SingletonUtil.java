package com.ck.android.common.utils.java;

/**
 * Singleton helper class for lazily initialization.
 * 
 * @author <a href="http://www.trinea.cn/" target="_blank">Trinea</a>
 * 
 * @param <T>
 */
public abstract class SingletonUtil<T> {

    private T instance;

    protected abstract T newInstance();

    public final T getInstance() {
        if (instance == null) {
            synchronized (SingletonUtil.class) {
                if (instance == null) {
                    instance = newInstance();
                }
            }
        }
        return instance;
    }
}
