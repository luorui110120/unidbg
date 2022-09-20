package com.github.unidbg.arm.backend.unicorn;

import com.github.unidbg.debugger.MmapInfo;

public class MmapInfoImplBack implements MmapInfo {
    public final long address;
    public final long size;
    public int perms;
    public MmapInfoImplBack(long address, long size, int perms){
        this.address = address;
        this.size = size;
        this.perms = perms;
    }

    @Override
    public long getAddr() {
        return address;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public int getPerms() {
        return perms;
    }
}
