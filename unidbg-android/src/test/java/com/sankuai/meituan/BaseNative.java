package com.sankuai.meituan;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.Symbol;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.linux.android.dvm.wrapper.DvmInteger;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.linux.file.SimpleFileIO;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.Inspector;
import com.sun.jna.Pointer;

import java.io.File;
import java.util.Arrays;

import keystone.Keystone;
import keystone.KeystoneArchitecture;
import keystone.KeystoneEncoded;
import keystone.KeystoneMode;
import com.trace.GlobalData;
import com.trace.KingTrace;


public abstract class BaseNative {
    public AndroidEmulator emulator;
    public VM vm;
    public Module module;
    public DalvikModule dm;
    public DvmClass nbridge;
    public final String apkPath = "unidbg-android/src/test/resources/example_binaries/mt/base.apk";
    public final String osPath =  "unidbg-android/src/test/resources/example_binaries/mt/libmtguard.so";

    public BaseNative() {

        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.sankuai.meituan").build(); // 创建模拟器实例，要模拟32位或者64位，在这里区分
        final Memory memory = emulator.getMemory(); // 模拟器的内存操作接口
        memory.setLibraryResolver(new AndroidResolver(23)); // 设置系统类库解析
        ///加载apk
        vm = emulator.createDalvikVM(new File(apkPath)); // 创建Android虚拟机
        vm.setVerbose(true); // 设置是否打印Jni调用细节
        // 加载动态库
        dm = vm.loadLibrary(new File(osPath), true); // 加载libttEncrypt.so到unicorn虚拟内存，加载成功以后会默认调用init_array等函数

        module = dm.getModule(); // 加载好的libttEncrypt.so对应为一个模块
        /// patch 代码过掉校验;
        //emulator.getBackend().mem_write(0x1cd36 + module.base, new byte[]{1});

        //vm.setJni(this);
        //emulator.getSyscallHandler().addIOResolver(this);

        ///获取类对象
        dm.callJNI_OnLoad(emulator); // 手动执行JNI_OnLoad函数
        nbridge = vm.resolveClass("com/meituan/android/common/mtguard/NBridge");


        //// 打印日志
        GlobalData.ignoreModuleList.add("libc.so");
        GlobalData.ignoreModuleList.add("libhookzz.so");
        ////监控内存 开始地址 和  结束地址;打印输出, 这个是内存地址
        //GlobalData.watch_address.put(0x401db840, 0);
        //GlobalData.is_dump_ldr=true;
        //GlobalData.is_dump_str=true;
        KingTrace trace=new KingTrace(emulator);

        //// 设置监控的其实地址和结束地址, 这些都是内存地址 ea ,所以要加上基地址   dm.base
        trace.initialize(1,0,null);
        ////
        //emulator.getBackend().hook_add_new(trace,1,0,emulator);

    }

    public  abstract Object[] main(int i, ArrayObject objArr, IOResolver<AndroidFileIO> io, AbstractJni jni);

    /// 一个patch 的函数,写的比较整体
    public void patchVerify1(){
        ////获取要 patch 的地址
        Pointer pointer = UnidbgPointer.pointer(emulator, module.base + 0x1E86);
        assert pointer != null;
        /// 读取当前内存中的字节数据
        byte[] code = pointer.getByteArray(0, 4);
        /// 进行比较是否相等
        if (!Arrays.equals(code, new byte[]{ (byte)0xFF, (byte) 0xF7, (byte) 0xEB, (byte) 0xFE })) { // BL sub_1C60
            throw new IllegalStateException(Inspector.inspectString(code, "patch32 code=" + Arrays.toString(code)));
        }
        /// 使用 Keystone 库 将汇编转成16进制
        try (Keystone keystone = new Keystone(KeystoneArchitecture.Arm, KeystoneMode.ArmThumb)) {
            KeystoneEncoded encoded = keystone.assemble("mov r0,1");
            byte[] patch = encoded.getMachineCode();
            if (patch.length != code.length) {
                throw new IllegalStateException(Inspector.inspectString(patch, "patch32 length=" + patch.length));
            }
            /// 写入内存进行patch
            pointer.write(0, patch, 0, patch.length);
        }
    }
}
