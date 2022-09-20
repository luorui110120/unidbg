package com.kuaishou;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.Symbol;
import com.github.unidbg.arm.backend.*;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.linux.android.dvm.wrapper.DvmBoolean;
import com.github.unidbg.linux.android.dvm.wrapper.DvmInteger;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.linux.file.SimpleFileIO;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.utils.KdUtils;
import com.github.unidbg.utils.SnapShotUtils;
import com.utils.SmileUtils;
import unicorn.ArmConst;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

public class JNICLibrary32 extends AbstractJni implements IOResolver {
    public final AndroidEmulator emulator;
    public final VM vm;
    public DalvikModule dm;
    public Module libc;
    private Map<Long, String> libcsym = new HashMap<Long, String>();
    public JNICLibrary32(){
        emulator = AndroidEmulatorBuilder.for32Bit()
                .setProcessName("com.smile.gifmaker")
                ///使用 DynarmicFactory 在需要发布的时候使用,虚拟机的效率比 Unicorn2Factory 高, 同时也要删除对pc寄存器的赋值,否则也出错;
                //.addBackendFactory(new DynarmicFactory(true))
                .addBackendFactory(new Unicorn2Factory(true))
                .build(); // 创建模拟器实例，要模拟32位或者64位，在这里区分
        final Memory memory = emulator.getMemory(); // 模拟器的内存操作接口
        emulator.getSyscallHandler().setEnableThreadDispatcher(false);
        memory.setLibraryResolver(new AndroidResolver(23)); // 设置系统类库解析

        vm = emulator.createDalvikVM(); // 创建Android虚拟机
        vm.setJni(this);
        emulator.getSyscallHandler().addIOResolver(this);
        vm.setVerbose(false); // 设置是否打印Jni调用细节
//        vm.loadLibrary(new File(KdUtils.getJarDirPath(this.getClass()) + "/src/main/resources/android/sdk23/lib/libc.so"), false);
//        libc = emulator.getMemory().findModule("libc.so");

        /// 设置指令跟踪
        // 填入自己的 traceFile 用来保存trace 日志的文件,只要在 trace*系列的函数后面添加 setRedirect 函数设置
        // 比如 下面的 emulator.traceRead().setRedirect(traceStream);
        String traceFile = "trace_unidbg.txt";
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
    private boolean isAddrEffective(long addr){
        try{
            byte[] tmp = emulator.getBackend().mem_read(addr, 1);
        }
        catch (Exception e){
            return false;
        }
        return true;
    }

    @Override
    public boolean callBooleanMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature){
            case "java/lang/Boolean->booleanValue()Z":{
                return (Boolean) dvmObject.getValue();
                //return false;
            }
        }
        return super.callBooleanMethodV(vm, dvmObject, signature, vaList);
    }
    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        //System.out.println("----" + pathname);
        ///
        return FileResult.<AndroidFileIO>success(new ByteArrayFileIO(oflags, pathname, "com.smile.gifmaker".getBytes()));

//        if(pathname.equals("test.png"))
//        {
//            return FileResult.success(new SimpleFileIO(oflags,new File("/Users/smali/tmp/t103/new/unidbg/test.sh"),pathname));
//        }
        //return null;
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
    private String doCommandNative(String strSign) {

        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv()); // 第一个参数是env
        DvmObject<?> thiz = vm.resolveClass("com/kuaishou/android/security/internal/dispatch/JNICLibrary").newObject(null);
        list.add(vm.addLocalObject(thiz)); // 第二个参数，实例方法是jobject，静态方法是jclass，直接填0，一般用不到。
        DvmObject<?> context = vm.resolveClass("com/yxcorp/gifshow/App", vm.resolveClass("android/app/Application")).newObject(null); // context
        vm.addLocalObject(context);
        list.add(10418); //参数1
        StringObject urlObj = new StringObject(vm, strSign);
        vm.addLocalObject(urlObj);
        ArrayObject arrayObject = new ArrayObject(urlObj);
        vm.addLocalObject(arrayObject);
        StringObject appkey = new StringObject(vm,"d7b7d042-d4f2-4012-be60-d97ff2429c17");
        vm.addLocalObject(appkey);
        DvmInteger intergetobj = DvmInteger.valueOf(vm, -1);
        vm.addLocalObject(intergetobj);
        DvmBoolean boolobj = DvmBoolean.valueOf(vm, false);
        vm.addLocalObject(boolobj);
        StringObject whitestr = new StringObject(vm,"");
        vm.addLocalObject(whitestr);
        ArrayObject objlist = new ArrayObject(arrayObject, appkey, intergetobj, boolobj, context, null, boolobj, whitestr);
        list.add(vm.addLocalObject(objlist));

//        这里获取 dump 时的 pc 地址作为模拟执行起始地址
        //long ctx_addr = emulator.getBackend().reg_read(ArmConst.UC_ARM_REG_PC).longValue();
        Module kwsModule = findModule("libkwsgmain.so");
        long ctx_addr = kwsModule.base + 0x4b918;
//        开始模拟执行
        Number result = Module.emulateFunction(emulator, ctx_addr + 1, list.toArray());
        String sign_str = (String) vm.getObject(result.intValue()).getValue();
        //System.out.println("sign_str=" + sign_str);
        return sign_str;

    }
    public static void main(String[] args) throws Exception {

        String instr = "/rest/n/search/selection/hotwordsd4b8f0766814284f3903ae5de3dac315";
        if(args.length > 0){
            instr = args[0];
        }
        else{
            System.out.println("输入要签名的字符串, 例子: java -jar getsig3.jar " + instr);
        }
        //System.out.println("instr:" + instr);
        JNICLibrary32 mNativeApi = new JNICLibrary32();
        String contextPath = KdUtils.getJarDirPath(mNativeApi.getClass()) + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator + "example_binaries" +
                File.separator + "MemorySnapShot" + File.separator + "ks_32_system_32.zip";
        if(SmileUtils.isStartupFromJar()){
            contextPath = KdUtils.getJarDirPath(mNativeApi.getClass()) + File.separator + "MemorySnapShot" + File.separator + "ks_32_system_32.zip";
        }
        List<String> white_list = Arrays.asList(new String[]{"libkwsgmain.so", "libc.so", "libc++_shared.so", "[anon:.bss]", "[anon:libc_malloc]"});
        List<Long> addr_list = Arrays.asList(new Long[]{0xc350aab4L, 0xff7f82c4L,0xbee76b74L});
        SnapShotUtils.loadSnapShot(contextPath, mNativeApi.emulator, null, null);
        ///在 64位手机上dump 32位 需要执行下面的函数修复 libc 函数的代用;
        //mNativeApi.rellibcsym(path);
        String sig3str = mNativeApi.doCommandNative(instr);
        System.out.println(sig3str);
    }


}
