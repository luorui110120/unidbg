package com.oasis;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.debugger.DebuggerType;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.android.dvm.jni.ProxyClassFactory;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.KdUtils;
import com.github.unidbg.utils.SnapShotUtils;
import unicorn.Arm64Const;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class NativeApi64 extends AbstractJni {

    public final AndroidEmulator emulator;
    public final VM vm;
    public DalvikModule dm;
    public Module module;
    public Map<String, UnidbgPointer> midamem = new IdentityHashMap<String, UnidbgPointer>();

    NativeApi64() {
        emulator = AndroidEmulatorBuilder.for64Bit()
                .setProcessName("com.sina.oasis")
                .addBackendFactory(new Unicorn2Factory(true))
                .build(); // 创建模拟器实例，要模拟32位或者64位，在这里区分
        final Memory memory = emulator.getMemory(); // 模拟器的内存操作接口
        memory.setLibraryResolver(new AndroidResolver(23)); // 设置系统类库解析

        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/example_binaries/lz/com.sina.oasis.apk")); // 创建Android虚拟机
        vm.setDvmClassFactory(new ProxyClassFactory());
        vm.setJni(this);
        vm.setVerbose(true); // 设置是否打印Jni调用细节
        /// 设置指令跟踪
        // 填入自己的 traceFile 用来保存trace 日志的文件,只要在 trace*系列的函数后面添加 setRedirect 函数设置
        // 比如 下面的 emulator.traceRead().setRedirect(traceStream);
        String traceFile = "/Users/smali/tmp/t103/trace_unidbg.txt";
        try {
            PrintStream traceStream = new PrintStream(new FileOutputStream(traceFile), true);
            //emulator.traceRead().setRedirect(traceStream);
            //emulator.traceWrite();
            // trace 将数据保存到文件中;
            //emulator.traceCode();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


    }
    public Module findModule(String name){
        //    return emulator.getMemory().findModule(name);

        for( Module module : emulator.getMemory().getLoadedModules()){
            String[] paths = module.name.split("/");
            String module_name = paths[paths.length - 1];
            if(module_name.equals(name)){
                return  module;
            }
        }
        return null;
    }
    private void s(String instr) {
        List<Object> list = new ArrayList<>(4);

//        参数一 JNIEnv* env
        list.add(vm.getJNIEnv());

//        参数二 jobject thiz
        DvmClass NativeApiobj = vm.resolveClass("com/weibo/xvideo/NativeApi");
        list.add(NativeApiobj.hashCode());

//        参数三 java 层的参数一
        //String data = "aid=01AxlUJKR0Ty44wiNo-ebcin69clFdov931m6rKA-DoQZ7Pkk.&cfrom=28C7295010&cuid=0&noncestr=g8g1N6V3t49z943Hx80395kb63f42A&platform=ANDROID&timestamp=1659164634618&ua=Google-Pixel4__oasis__4.5.6__Android__Android11&version=4.5.6&vid=2007759688214&wm=2468_90123";
        String data= instr;
        ByteArray input_array = new ByteArray(vm, data.getBytes(StandardCharsets.UTF_8));
        vm.addLocalObject(input_array);
        list.add(input_array.hashCode());

//        参数四 java 层的参数二
        boolean flag = false;
        list.add((Boolean) flag ? VM.JNI_TRUE : VM.JNI_FALSE);

//        这里获取 dump 时的 pc 地址作为模拟执行起始地址
        long ctx_addr = emulator.getBackend().reg_read(Arm64Const.UC_ARM64_REG_PC).longValue();
        ctx_addr = findModule("liboasiscore.so").base + 0x116CC;
        System.out.println("pc:0x" + Long.toHexString(ctx_addr));
        //// 手动patch
        //Module oasisModule = findModule("/data/app/com.sina.oasis-y0HZb42Ctr1q1LKSKJ8E5Q==/lib/arm64/liboasiscore.so");
        //emulator.getBackend().mem_write(oasisModule.base + 0x116D0, new byte[]{(byte)0x1f,(byte)0x20,(byte)0x3,(byte)0xd5});
        //
        //// 添加ida 调试
        emulator.attach(DebuggerType.ANDROID_SERVER_V8);
//        开始模拟执行
        Number result = Module.emulateFunction(emulator, ctx_addr, list.toArray());
//        获取返回结果
        String sign_str = (String) vm.getObject(result.intValue()).getValue();
        System.out.println("sign_str=" + sign_str);
    }
    public static void main(String[] args) throws Exception {
        NativeApi64 mNativeApi = new NativeApi64();
        System.out.println("path dir:" + KdUtils.getJarDirPath(mNativeApi.getClass()));
        ///  模块名
        List<String> white_list = Arrays.asList(new String[]{"liboasiscore.so", "libc.so",  "[anon:thread stack guard page]", "[anon:.bss]", "[anon:libc_malloc]"});
        ///  通过地址获取某个段;
        List<Long> addr_list = Arrays.asList(new Long[]{0x7eeb6ee338L});
        /// 将不需要的段和数据清除只留下关键的, 所以体积会小很多;
        String contextPath = KdUtils.getJarDirPath(mNativeApi.getClass()) + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator + "example_binaries" +
                File.separator + "MemorySnapShot" + File.separator + "lz_64_system_64.zip";
        SnapShotUtils.loadSnapShot(contextPath, mNativeApi.emulator, white_list, addr_list);

        //// release 包
//        String contextPath = KdUtils.getJarDirPath(mNativeApi.getClass()) + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator + "example_binaries" +
//                File.separator + "MemorySnapShot" + File.separator + "lz_64_system_64_realse.zip";
//        SnapShotUtils.loadSnapShot(contextPath, mNativeApi.emulator, null, null);

        mNativeApi.s("1234567890");
    }

}