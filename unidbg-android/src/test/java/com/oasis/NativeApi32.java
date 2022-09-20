package com.oasis;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.Symbol;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.arm.backend.CodeHook;
import com.github.unidbg.arm.backend.UnHook;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.debugger.DebuggerType;
import com.github.unidbg.debugger.ida.AndroidServer;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.KdUtils;
import com.github.unidbg.utils.SnapShotUtils;
import com.utils.SmileUtils;
import keystone.Keystone;
import keystone.KeystoneArchitecture;
import keystone.KeystoneEncoded;
import keystone.KeystoneMode;
import unicorn.Arm64Const;
import unicorn.ArmConst;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class NativeApi32 extends AbstractJni {

    public final AndroidEmulator emulator;
    public final VM vm;
    public DalvikModule dm;
    public Module module;
    public Module libc;
    public Map<String, UnidbgPointer> midamem = new IdentityHashMap<String, UnidbgPointer>();

    NativeApi32(boolean loadlic) {
        emulator = AndroidEmulatorBuilder.for32Bit()
                .setProcessName("com.sina.oasis")
                .addBackendFactory(new Unicorn2Factory(true))
                .build(); // 创建模拟器实例，要模拟32位或者64位，在这里区分
        final Memory memory = emulator.getMemory(); // 模拟器的内存操作接口
        emulator.getSyscallHandler().setEnableThreadDispatcher(false);
        memory.setLibraryResolver(new AndroidResolver(23)); // 设置系统类库解析

        AndroidServer.setSnapShotStat(true);   ///开启ida 调试快照;

        vm = emulator.createDalvikVM(); // 创建Android虚拟机
        vm.setJni(this);
        vm.setVerbose(true); // 设置是否打印Jni调用细节
        if(loadlic){
            vm.loadLibrary(new File(KdUtils.getJarDirPath(this.getClass()) + "/src/main/resources/android/sdk23/lib/libc.so"), false);
            libc = emulator.getMemory().findModule("libc.so");
        }

        //dm = vm.loadLibrary(new File("unidbg-android/src/test/resources/example_binaries/lz/liboasiscore.so"), true); // 加载libttEncrypt.so到unicorn虚拟内存，加载成功以后会默认调用init_array等函数
        //module = dm.getModule(); // 加载好的libttEncrypt.so对应为一个模块
        /// 设置指令跟踪
        // 填入自己的 traceFile 用来保存trace 日志的文件,只要在 trace*系列的函数后面添加 setRedirect 函数设置
        // 比如 下面的 emulator.traceRead().setRedirect(traceStream);
        String traceFile = "/Users/smali/tmp/t103/trace_unidbg.txt";
        try {
            PrintStream traceStream = new PrintStream(new FileOutputStream(traceFile), true);
//            emulator.traceRead().setRedirect(traceStream);
//            emulator.traceWrite();
            // trace 将数据保存到文件中;
            //emulator.traceCode();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }


    private Map<Long, String> libcsym = new HashMap<Long, String>();
    private boolean isAddrEffective(long addr){
        try{
            byte[] tmp = emulator.getBackend().mem_read(addr, 1);
        }
        catch (Exception e){
            return false;
        }
        return true;
    }
    private void rellibcsym(String path){
        String libcsymStr = SnapShotUtils.readDumpjson(path, "libc.so_sym.json");
        String libcsopath = KdUtils.getJarDirPath(this.getClass());
        if(emulator.is64Bit()){
            libcsopath += File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "android" +
                    File.separator + "sdk23" + File.separator + "lib64" + File.separator + "libc.so";
        }
        else{
            libcsopath += File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "android" +
                    File.separator + "sdk23" + File.separator + "lib" + File.separator + "libc.so";
        }
//        vm.loadLibrary(new File(KdUtils.getJarDirPath() + "/src/main/resources/android/sdk23/lib/libc.so"), true);
//        libc = emulator.getMemory().findModule("libc.so");
        /////保存到 SnapShotUtils 方便后续dump快照使用;
        SnapShotUtils.saveLibcSym(libcsymStr);
        JSONArray libcSymJson = JSONArray.parseArray(libcsymStr);

        for (int i = 0; i < libcSymJson.size(); i++) {
            JSONObject jsonObject = libcSymJson.getJSONObject(i);
            long addr = jsonObject.getLong("addr");
            String name = jsonObject.getString("name");

            if(isAddrEffective(addr) && (libc.findSymbolByName(name)!= null)) {
                libcsym.put(addr, name);
                ///malloc 函数涉及到 mmap 的问题,为让ida 方便调试,所以自己实现了, 也可以不处理,直接走libc.so 也行;
                if ("malloc".equals(name)) {
                    emulator.getBackend().hook_add_new(new CodeHook() {
                        @Override
                        public void hook(Backend backend, long address, int size, Object user) {

                            int msize = backend.reg_read(ArmConst.UC_ARM_REG_R0).intValue();

                            MemoryBlock block = emulator.getMemory().malloc(msize, true);
                            System.out.println("my malloc size:" + Integer.toHexString(size));
                            backend.reg_write(ArmConst.UC_ARM_REG_R0, block.getPointer().toUIntPeer());
                            backend.reg_write(ArmConst.UC_ARM_REG_PC, backend.reg_read(ArmConst.UC_ARM_REG_LR).longValue());
                            //backend.reg_write(ArmConst.UC_ARM_REG_PC, libc.findSymbolByName("malloc").getAddress());
                        }

                        @Override
                        public void onAttach(UnHook unHook) {

                        }

                        @Override
                        public void detach() {
                        }
                    }, addr, addr, null);
                }else if("free".equals(name)){     //因为上面处理的 malloc 所以 free 也一并要处理;
                    emulator.getBackend().hook_add_new(new CodeHook() {
                        @Override
                        public void hook(Backend backend, long address, int size, Object user) {
                            backend.reg_write(ArmConst.UC_ARM_REG_PC, backend.reg_read(ArmConst.UC_ARM_REG_LR).longValue());
                        }

                        @Override
                        public void onAttach(UnHook unHook) {
                        }

                        @Override
                        public void detach() {
                        }
                    }, addr, addr, null);
                }
                else {
                    //// 调用 libc.so 的函数;
                    emulator.getBackend().hook_add_new(new CodeHook() {

                        @Override
                        public void onAttach(UnHook unHook) {

                        }

                        @Override
                        public void detach() {

                        }

                        @Override
                        public void hook(Backend backend, long address, int size, Object user) {
                            long pc_value = address;
                            if (libcsym.containsKey(pc_value)) {
                                String symName = libcsym.get(pc_value);
                                Symbol symbolByName = libc.findSymbolByName(symName);
                                if (symbolByName != null) {
                                    emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_PC, symbolByName.getAddress());
                                    System.out.println("call " + symName);
                                }

                            }
                        }
                    }, addr, addr, null);
                }
            }
        }
    }


    private void hook_libc() {
        long liboasiscoreBase = findModule("liboasiscore.so").base;
        long libc_malloc_addr = liboasiscoreBase + 0x5BD4;

        emulator.getBackend().hook_add_new(new CodeHook() {
            @Override
            public void hook(Backend backend, long address, int size, Object user) {

                int msize = backend.reg_read(ArmConst.UC_ARM_REG_R0).intValue();

                MemoryBlock block = emulator.getMemory().malloc(msize, true);
                System.out.println("malloc size:" + Integer.toHexString(size));
                backend.reg_write(ArmConst.UC_ARM_REG_R0, block.getPointer().toUIntPeer());
                backend.reg_write(ArmConst.UC_ARM_REG_PC, backend.reg_read(ArmConst.UC_ARM_REG_LR).longValue());
            }

            @Override
            public void onAttach(UnHook unHook) {

            }

            @Override
            public void detach() {
            }
        }, libc_malloc_addr, libc_malloc_addr, null);
        long libc_new_addr = liboasiscoreBase +  0x5CB8;
//        emulator.getBackend().hook_add_new(new CodeHook() {
//            @Override
//            public void hook(Backend backend, long address, int size, Object user) {
//                int msize = backend.reg_read(ArmConst.UC_ARM_REG_R0).intValue();
//                MemoryBlock block = emulator.getMemory().malloc(msize, true);
//                backend.reg_write(ArmConst.UC_ARM_REG_R0, block.getPointer().toUIntPeer());
//                backend.reg_write(ArmConst.UC_ARM_REG_PC, backend.reg_read(ArmConst.UC_ARM_REG_LR).longValue());
//            }
//
//            @Override
//            public void onAttach(UnHook unHook) {
//
//            }
//
//            @Override
//            public void detach() {
//            }
//        }, libc_new_addr, libc_new_addr, null);

        long libc_free_addr = liboasiscoreBase + 0x5C04;
        emulator.getBackend().hook_add_new(new CodeHook() {
            @Override
            public void hook(Backend backend, long address, int size, Object user) {
                backend.reg_write(ArmConst.UC_ARM_REG_PC, backend.reg_read(ArmConst.UC_ARM_REG_LR).longValue());
            }

            @Override
            public void onAttach(UnHook unHook) {
            }

            @Override
            public void detach() {
            }
        }, libc_free_addr, libc_free_addr, null);

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
        System.out.println("jnienv:" + (vm.getJNIEnv().toString()));

//        参数二 jobject thiz
        DvmClass NativeApiobj = vm.resolveClass("com/weibo/xvideo/NativeApi");
        list.add(NativeApiobj.hashCode());

//        参数三 java 层的参数一
        //String data = "aid=01AxlUJKR0Ty44wiNo-ebcin69clFdov931m6rKA-DoQZ7Pkk.&cfrom=28C7295010&cuid=0&noncestr=g8g1N6V3t49z943Hx80395kb63f42A&platform=ANDROID&timestamp=1659164634618&ua=Google-Pixel4__oasis__4.5.6__Android__Android11&version=4.5.6&vid=2007759688214&wm=2468_90123";
        String data = instr;
        ByteArray input_array = new ByteArray(vm, data.getBytes(StandardCharsets.UTF_8));
        vm.addLocalObject(input_array);
        list.add(input_array.hashCode());

//        参数四 java 层的参数二
        boolean flag = false;
        list.add((Boolean) flag ? VM.JNI_TRUE : VM.JNI_FALSE);

//        这里获取 dump 时的 pc 地址作为模拟执行起始地址
        int pcreg = 0;
        if(emulator.is64Bit()){
            pcreg = Arm64Const.UC_ARM64_REG_PC;
        }
        else{
            pcreg = ArmConst.UC_ARM_REG_PC;
        }
        ///通过 pc 寄存器 设置调用起始地址;
        long ctx_addr = emulator.getBackend().reg_read(pcreg).longValue();

        ctx_addr = findModule("liboasiscore.so").base + 0xc365;
        //ctx_addr = SetRegPC.setArmRegPcValue(emulator, ctx_addr);
        System.out.println("pc:0x" + Long.toHexString(ctx_addr));

        //// 添加ida 调试
        emulator.attach(DebuggerType.ANDROID_SERVER_V8);
//        开始模拟执行
        Number result = Module.emulateFunction(emulator, ctx_addr, list.toArray());


//        获取返回结果
        String sign_str = (String) vm.getObject(result.intValue()).getValue();
        System.out.println("sign_str=" + sign_str);
    }
    private void recovery(){
        int pcreg = 0;
        if(emulator.is64Bit()){
            pcreg = Arm64Const.UC_ARM64_REG_PC;
        }
        else{
            pcreg = ArmConst.UC_ARM_REG_PC;
        }
        long ctx_addr = emulator.getBackend().reg_read(pcreg).longValue() + 1;
//        if(!emulator.is64Bit() && ((ctx_addr & 1) == 0)){
//            ctx_addr += 1;
//        }
        //// 添加ida 调试
        //emulator.attach(DebuggerType.ANDROID_SERVER_V8);

        Number result = Module.emulateFunction(emulator, ctx_addr);
        String sign_str = (String) vm.getObject(result.intValue()).getValue();
        System.out.println("sign_str=" + sign_str);
    }

    public static void main(String[] args) throws Exception {
        ///// dump 应用   "绿洲 v4.5.6"
        System.out.println("path dir:" + KdUtils.getJarDirPath() + SmileUtils.isStartupFromJar());
        ///  模块名
        List<String> white_list = Arrays.asList(new String[]{"liboasiscore.so", "libc.so",  "[anon:thread stack guard page]", "[anon:.bss]", "[anon:libc_malloc]", "[stack]"});
        ///  通过地址获取某个段;
        List<Long> addr_list = Arrays.asList(new Long[]{0xff7f96b4L});   ///添加后

        //// 在 32位的系统dump 32 位的程序 不需要修复libc.so 的函数;
        NativeApi32 mNativeApi = new NativeApi32(false);
        String contextPath = KdUtils.getJarDirPath(mNativeApi.getClass()) + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator + "example_binaries" +
                File.separator + "MemorySnapShot" + File.separator + "lz_32_system_32.zip";
        SnapShotUtils.loadSnapShot(contextPath, mNativeApi.emulator, white_list, addr_list);
        mNativeApi.s("1234567890");

        //////在 64位的系统dump 32 位的程序 需要修复libc.so 的函数;
//        NativeApi32 mNativeApi = new NativeApi32(true);   ///需要先加载 libc.so
//
//        String contextPath = KdUtils.getJarDirPath(mNativeApi.getClass()) + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator + "example_binaries" +
//                File.separator + "MemorySnapShot" + File.separator + "lz_32_system_64.zip";
//        SnapShotUtils.loadSnapShot(contextPath, mNativeApi.emulator, white_list, addr_list);
//        //// 下面两种修复 libc 调用的方法, 默认可以使用  rellibcsym 修复, 最为简单;
//        mNativeApi.rellibcsym(contextPath);
//        //// 通过运行调试来修补;
//        //mNativeApi.hook_libc();
//        mNativeApi.s("1234567890");


        ///// revoery  通过快照恢复调试,  快照恢复调试还不完美, 一些jni 函数的类对象还无法恢复, 对 要修复 libc.so 库的快照目录也会出问题;
//        NativeApi32 mNativeApi = new NativeApi32(false);
//        String contextPath = "/Users/smali/tmp/t103/idaDebugSnapShot.zip";
//        SnapShotUtils.loadSnapShot(contextPath, mNativeApi.emulator, null, null);
//        //mNativeApi.rellibcsym(contextPath);
//        mNativeApi.recovery();


    }

}