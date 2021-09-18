package com.anjuke.mobile.sign;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.StringObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.android.dvm.jni.ProxyClassFactory;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;
import com.trace.GlobalData;
import com.trace.KingTrace;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class SignUtil {

    private final AndroidEmulator emulator;

    private final DvmClass cSignUtil;
    private final VM vm;
    private final Module module;

    public SignUtil() {
        emulator = AndroidEmulatorBuilder.for32Bit()
                .setProcessName("com.anjuke.android.app")
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM();
        vm.setDvmClassFactory(new ProxyClassFactory());
        vm.setVerbose(false);
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/resources/example_binaries/armeabi-v7a/libsignutil.so"), false);
        cSignUtil = vm.resolveClass("com/anjuke/mobile/sign/SignUtil");
        dm.callJNI_OnLoad(emulator);
        module = dm.getModule();
        //// 抖音打印日志会出现异常, 估计是有时间计算函数;
        GlobalData.ignoreModuleList.add("libc.so");
        GlobalData.ignoreModuleList.add("libhookzz.so");
        ////监控内存 开始地址 和  结束地址;打印输出, 这个是内存地址
        //GlobalData.watch_address.put(0x401db840, 0);
        //GlobalData.is_dump_ldr=true;
        //GlobalData.is_dump_str=true;
        KingTrace trace=new KingTrace(emulator);

        //// 设置监控的其实地址和结束地址, 这些都是内存地址 ea ,所以要加上基地址   dm.base
        trace.initialize(1,0,null);
        emulator.getBackend().hook_add_new(trace,1,0,emulator);
    }

    public void destroy() throws IOException {
        emulator.close();
    }

    public String getSign0(String p1, String p2, Map<String, byte[]> map, String p3, int i) {
        String methodSign = "getSign0(Ljava/lang/String;Ljava/lang/String;Ljava/util/Map;Ljava/lang/String;I)Ljava/lang/String;";
        StringObject obj = cSignUtil.callStaticJniMethodObject(emulator, methodSign, p1, p2, ProxyDvmObject.createObject(vm, map), p3, i);
        return obj.getValue();
    }

    private synchronized String sign(String p1, String p2, Map<String, String> paramMap, String p3, int i) {
        Map<String, byte[]> map = new HashMap<>();
        for (String key : paramMap.keySet()) {
            map.put(key, paramMap.get(key).getBytes(StandardCharsets.UTF_8));
        }
        return getSign0(p1, p2, map, p3, i);
    }


    public static void main(String[] args) throws Exception {
        Map<String, String> paramMap = new HashMap<String, String>() {{
            put("a", "b");
            put("b", "b");
        }};
        String p1 = "aa";
        String p2 = "bb";
        String p3 = "cc";
        int i = 10;

        SignUtil signUtil = new SignUtil();
        String sign = signUtil.sign(p1, p2, paramMap, p3, i);
        System.out.println("sign=" + sign);
        signUtil.destroy();
    }
    public static String getAnjukeSig(String p1, String p2, String p3){
        SignUtil signUtil = new SignUtil();
        Map<String, String> paramMap = new HashMap<String, String>() {{
            put("a", "b");
            put("b", "b");
        }};
        String sign = signUtil.sign(p1, p2, paramMap, p3, 10);
        System.out.println("sign=" + sign);
        try {
            signUtil.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sign;
    }

}
