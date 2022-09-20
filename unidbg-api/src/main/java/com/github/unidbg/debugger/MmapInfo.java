package com.github.unidbg.debugger;

public interface MmapInfo {

    public long getAddr();
    public long getSize();
    public int getPerms();

}
