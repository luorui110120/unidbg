package com.bytedance.frameworks.core.encrypt;

import com.alibaba.fastjson.util.IOUtils;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.Symbol;
import com.github.unidbg.arm.HookStatus;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.arm.context.Arm32RegisterContext;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.debugger.DebuggerType;
import com.github.unidbg.hook.HookContext;
import com.github.unidbg.hook.ReplaceCallback;
import com.github.unidbg.hook.hookzz.Dobby;
import com.github.unidbg.hook.hookzz.HookEntryInfo;
import com.github.unidbg.hook.hookzz.HookZz;
import com.github.unidbg.hook.hookzz.IHookZz;
import com.github.unidbg.hook.hookzz.InstrumentCallback;
import com.github.unidbg.hook.hookzz.WrapCallback;
import com.github.unidbg.hook.xhook.IxHook;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.XHookImpl;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.utils.Inspector;
import com.sun.jna.Pointer;
import com.trace.GlobalData;
import com.trace.KingTrace;

import java.io.File;

public class TTEncrypt {

    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    private final DvmClass TTEncryptUtils;

    private final boolean logging;

    TTEncrypt(boolean logging) {
        this.logging = logging;

        emulator = AndroidEmulatorBuilder.for32Bit()
                .setProcessName("com.qidian.dldl.official")
                .addBackendFactory(new Unicorn2Factory(true))
                .build(); // 创建模拟器实例，要模拟32位或者64位，在这里区分
        final Memory memory = emulator.getMemory(); // 模拟器的内存操作接口
        memory.setLibraryResolver(new AndroidResolver(23)); // 设置系统类库解析

        vm = emulator.createDalvikVM(); // 创建Android虚拟机
        vm.setVerbose(logging); // 设置是否打印Jni调用细节
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/resources/example_binaries/libttEncrypt.so"), false); // 加载libttEncrypt.so到unicorn虚拟内存，加载成功以后会默认调用init_array等函数
        dm.callJNI_OnLoad(emulator); // 手动执行JNI_OnLoad函数
        module = dm.getModule(); // 加载好的libttEncrypt.so对应为一个模块
        TTEncryptUtils = vm.resolveClass("com/bytedance/frameworks/core/encrypt/TTEncryptUtils");

        //过滤掉下面模块的计算时间
//        GlobalData.ignoreModuleList.add("libc.so");
//        GlobalData.ignoreModuleList.add("libc++.so");
//        GlobalData.ignoreModuleList.add("libhookzz.so");
//        GlobalData.ignoreModuleList.add("libxhook.so");
//        ////监控内存 开始地址 和  结束地址;打印输出, 这个是内存地址
//        //GlobalData.watch_address.put(0x401db840, 0);
//        GlobalData.is_dump_ldr=true;
//        GlobalData.is_dump_str=true;
//        KingTrace trace=new KingTrace(emulator);
//        //// 设置监控的其实地址和结束地址, 这些都是内存地址 ea, 所以要加上基地址   module.base
//        trace.initialize(1,0,null);
//        emulator.getBackend().hook_add_new(trace,1,0,emulator);
        ///系统自带的trace 功能;
        //emulator.traceCode();
    }

    void destroy() {
        IOUtils.close(emulator);
        if (logging) {
            System.out.println("destroy");
        }
    }

    public static void main(String[] args) throws Exception {
        TTEncrypt test = new TTEncrypt(true);

        byte[] data = test.ttEncrypt();
        Inspector.inspect(data, "ttEncrypt");

        test.destroy();
    }

    byte[] ttEncrypt() {
        if (logging) {
            Symbol sbox0 = module.findSymbolByName("sbox0"); // 在libttEncrypt.so模块中查找sbox0导出符号
            Symbol sbox1 = module.findSymbolByName("sbox1");
            Inspector.inspect(sbox0.createPointer(emulator).getByteArray(0, 256), "sbox0"); // 打印sbox0导出符号在unicorn中的内存数据
            Inspector.inspect(sbox1.createPointer(emulator).getByteArray(0, 256), "sbox1");

            IHookZz hookZz = HookZz.getInstance(emulator); // 加载HookZz，支持inline hook，文档看https://github.com/jmpews/HookZz
            hookZz.enable_arm_arm64_b_branch(); // 测试enable_arm_arm64_b_branch，可有可无
            hookZz.wrap(module.findSymbolByName("ss_encrypt"), new WrapCallback<RegisterContext>() { // inline wrap导出函数
                @Override
                public void preCall(Emulator<?> emulator, RegisterContext ctx, HookEntryInfo info) {
                    Pointer pointer = ctx.getPointerArg(2);
                    int length = ctx.getIntArg(3);
                    byte[] key = pointer.getByteArray(0, length);
                    Inspector.inspect(key, "ss_encrypt key");
                }
                @Override
                public void postCall(Emulator<?> emulator, RegisterContext ctx, HookEntryInfo info) {
                    System.out.println("ss_encrypt.postCall R0=" + ctx.getLongArg(0));
                }
            });
            hookZz.disable_arm_arm64_b_branch();
            hookZz.instrument(module.base + 0x00000F5C + 1, new InstrumentCallback<Arm32RegisterContext>() {
                @Override
                public void dbiCall(Emulator<?> emulator, Arm32RegisterContext ctx, HookEntryInfo info) { // 通过base+offset inline wrap内部函数，在IDA看到为sub_xxx那些
                    System.out.println("R3=" + ctx.getLongArg(3) + ", R10=0x" + Long.toHexString(ctx.getR10Long()));
                }
            });

            Dobby dobby = Dobby.getInstance(emulator);
            dobby.replace(module.findSymbolByName("ss_encrypted_size"), new ReplaceCallback() { // 使用Dobby inline hook导出函数
                @Override
                public HookStatus onCall(Emulator<?> emulator, HookContext context, long originFunction) {
                    System.out.println("ss_encrypted_size.onCall arg0=" + context.getIntArg(0) + ", originFunction=0x" + Long.toHexString(originFunction));
                    return HookStatus.RET(emulator, originFunction);
                }
                @Override
                public void postCall(Emulator<?> emulator, HookContext context) {
                    System.out.println("ss_encrypted_size.postCall ret=" + context.getIntArg(0));
                }
            }, true);

            IxHook xHook = XHookImpl.getInstance(emulator); // 加载xHook，支持Import hook，文档看https://github.com/iqiyi/xHook
            xHook.register("libttEncrypt.so", "strlen", new ReplaceCallback() { // hook libttEncrypt.so的导入函数strlen
                @Override
                public HookStatus onCall(Emulator<?> emulator, HookContext context, long originFunction) {
                    Pointer pointer = context.getPointerArg(0);
                    String str = pointer.getString(0);
                    System.out.println("strlen=" + str);
                    context.push(str);
                    return HookStatus.RET(emulator, originFunction);
                }
                @Override
                public void postCall(Emulator<?> emulator, HookContext context) {
                    System.out.println("strlen=" + context.pop() + ", ret=" + context.getIntArg(0));
                }
            }, true);
            xHook.register("libttEncrypt.so", "memmove", new ReplaceCallback() {
                @Override
                public HookStatus onCall(Emulator<?> emulator, long originFunction) {
                    RegisterContext context = emulator.getContext();
                    Pointer dest = context.getPointerArg(0);
                    Pointer src = context.getPointerArg(1);
                    int length = context.getIntArg(2);
                    Inspector.inspect(src.getByteArray(0, length), "memmove dest=" + dest);
                    return HookStatus.RET(emulator, originFunction);
                }
            });
            xHook.register("libttEncrypt.so", "memcpy", new ReplaceCallback() {
                @Override
                public HookStatus onCall(Emulator<?> emulator, long originFunction) {
                    RegisterContext context = emulator.getContext();
                    Pointer dest = context.getPointerArg(0);
                    Pointer src = context.getPointerArg(1);
                    int length = context.getIntArg(2);
                    Inspector.inspect(src.getByteArray(0, length), "memcpy dest=" + dest);
                    return HookStatus.RET(emulator, originFunction);
                }
            });
            xHook.refresh(); // 使Import hook生效
        }

        if (logging) {
            emulator.attach(DebuggerType.ANDROID_SERVER_V8); // 附加IDA android_server，可输入c命令取消附加继续运行
        }
        byte[] data = new byte[16];
        ByteArray array = TTEncryptUtils.callStaticJniMethodObject(emulator, "ttEncrypt([BI)[B", new ByteArray(vm, data), data.length); // 执行Jni方法
        return array.getValue();
    }

}
