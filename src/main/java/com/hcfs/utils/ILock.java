package com.hcfs.utils;

public interface ILock {
    boolean tryLock(long timeout);

    void unlock();
}
