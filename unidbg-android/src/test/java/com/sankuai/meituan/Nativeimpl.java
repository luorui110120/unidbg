package com.sankuai.meituan;

import com.github.unidbg.Emulator;
import com.github.unidbg.debugger.DebuggerType;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.android.dvm.wrapper.DvmInteger;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.linux.file.SimpleFileIO;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class Nativeimpl extends  BaseNative{


    public Nativeimpl(){
        super();

    }

    @Override
    public Object[]  main(int i, ArrayObject objArr, IOResolver<AndroidFileIO> io, AbstractJni jni) {
        if(null != io){
            emulator.getSyscallHandler().addIOResolver(io);
        }
        if(null != jni){
            vm.setJni(jni);
        }
        //emulator.attach(DebuggerType.ANDROID_SERVER_V8);
        Object ret = nbridge.callStaticJniMethodObject(emulator, "main(I[Ljava/lang/Object;)[Ljava/lang/Object;", i, objArr);
        Object[] objret = (Object[])((DvmObject)ret).getValue();
        return objret;

        //// 另一种更简单的调用方法
//        List<Object> list = new ArrayList<>(10);
//        list.add(vm.getJNIEnv()); // 第一个参数是env
//        list.add(0);   //this对象
//        list.add(i);
//        list.add(objArr);
//        /// 这里 0x5a38d  就是os RVA地址
//        Object ret = module.callFunction(emulator, 0x5a38d, list.toArray());
//        return (Object[])((DvmObject)ret).getValue();
    }
    ///hook_all(Memory.readPointer(ptr(0xDF570E50)))
    public int main111(){
        DvmObject<?> obj = vm.resolveClass("java/lang/object").newObject(null);
        vm.addGlobalObject(obj);
        ArrayObject arrobj = new ArrayObject(obj);
        vm.addGlobalObject(arrobj);
        IOResolver ior = new IOResolver(){
            // 3
            @Override
            public FileResult resolve(Emulator emulator, String pathname, int oflags) {
                switch (pathname){
                    case "/data/app/com.sankuai.meituan-TEfTAIBttUmUzuVbwRK1DQ==/base.apk": {
                        ///返回 文件句柄, 程序还有 lseek 等函数操作
                        return FileResult.success(new SimpleFileIO(oflags, new File(apkPath), pathname));

                    }
                }
                // //一般对于 文件操作
                if (pathname.equals(String.format("/proc/%d/cmdline",emulator.getPid()))) {
                    return FileResult.<AndroidFileIO>success(new ByteArrayFileIO(oflags, pathname, "com.sankuai.meituan".getBytes()));
                }
                return null;
            }
        };
        /// 给每一个 main 创建 AbstractJni 对象 补环境也更加有针对性, 方便我们在一些场景下通过中间过程 callObjectMethodV 等函数拿到结果
        Object[] objs = main(111, arrobj, ior, new AbstractJni() {

            @Override
            public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
                switch (signature) {
                    case "com/meituan/android/common/mtguard/NBridge->getPicName()Ljava/lang/String;":{
                        return new StringObject(vm, "ms_com.sankuai.meituan");
                    }
                    case "com/meituan/android/common/mtguard/NBridge->getSecName()Ljava/lang/String;":{
                        return new StringObject(vm, "ppd_com.sankuai.meituan.xbt");
                    }
                    case "com/meituan/android/common/mtguard/NBridge->getAppContext()Landroid/content/Context;":{
                        return vm.resolveClass("android/content/Context").newObject(null);
                    }
                    case "com/meituan/android/common/mtguard/NBridge->getMtgVN()Ljava/lang/String;": {
                        return new StringObject(vm, "4.4.7.3");
                    }
                    ////这里随便补了一个值
                    case "com/meituan/android/common/mtguard/NBridge->getDfpId()Ljava/lang/String;":{
                        return new StringObject(vm, String.valueOf(emulator.getPid()));
                    }
                }
                return super.callStaticObjectMethodV(vm, dvmClass, signature,vaList);
            }

            @Override
            public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
                switch (signature) {
                    case "android/content/Context->getPackageCodePath()Ljava/lang/String;":{
                        return new StringObject(vm, "/data/app/com.sankuai.meituan-TEfTAIBttUmUzuVbwRK1DQ==/base.apk");
                    }
                }
                return super.callObjectMethodV(vm, dvmObject, signature, vaList);
            }

            @Override
            public int getStaticIntField(BaseVM vm, DvmClass dvmClass, String signature) {
                switch (signature){
                    case "android/content/pm/PackageManager->GET_SIGNATURES:I":{
                        return 64;
                    }
                }
                return super.getStaticIntField(vm, dvmClass, signature);
            }

            @Override
            public int getIntField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
                switch (signature){
                    case "android/content/pm/PackageInfo->versionCode:I":{
                        return 1100090405;
                    }
                };
                return super.getIntField(vm, dvmObject, signature);
            }

            @Override
            public DvmObject<?> newObjectV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
                switch (signature) {
                    case "java/lang/Integer-><init>(I)V":
                        int input = vaList.getIntArg(0);
                        return DvmInteger.valueOf(vm, input);
                        //return vm.resolveClass("java/lang/Integer").newObject(new Integer(0));
                }
                return super.newObjectV(vm, dvmClass, signature, vaList);
            }
        });
        //System.out.println(objs[0].toString());
        return 0;
    }

    public String main203(String key, String getBuf, int mode){


        StringObject vmStrKey = new StringObject(vm, key);
        ByteArray vmBytesBuf = new ByteArray(vm, getBuf.getBytes());
        DvmInteger vmMode = DvmInteger.valueOf(vm, mode);
        vm.addGlobalObject(vmStrKey);
        vm.addGlobalObject(vmBytesBuf);
        vm.addGlobalObject(vmMode);
        ArrayObject arrobj = new ArrayObject(vmStrKey, vmBytesBuf, vmMode);
        vm.addGlobalObject(arrobj);

        Object[] objs = main(203, arrobj, null, new AbstractJni(){

        });
        //System.out.println(objs[0].toString());
        return objs[0].toString();
    }

    public void main111_old(){
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv()); // 第一个参数是env
        list.add(0); // 第二个参数，实例方法是jobject，静态方法是jclazz，直接填0，一般用不到。
        list.add(111);
        DvmObject<?> obj = vm.resolveClass("java/lang/object").newObject(null);
        vm.addLocalObject(obj);
        ArrayObject myobject = new ArrayObject(obj);
        vm.addLocalObject(myobject);
        list.add(vm.addLocalObject(myobject));
        System.out.println("start 111");
        module.callFunction(emulator, 0x5a38d, list.toArray());
        System.out.println("end 111");
    };

    public String main203_old(){
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv()); // 第一个参数是env
        list.add(0); // 第二个参数，实例方法是jobject，静态方法是jclazz，直接填0，一般用不到。
        list.add(203);
        StringObject input2_1 = new StringObject(vm, "9b69f861-e054-4bc4-9daf-d36ae205ed3e");
        ByteArray input2_2 = new ByteArray(vm, "GET /aggroup/homepage/display __r0ysue".getBytes(StandardCharsets.UTF_8));
        DvmInteger input2_3 = DvmInteger.valueOf(vm, 2);
        vm.addLocalObject(input2_1);
        vm.addLocalObject(input2_2);
        vm.addLocalObject(input2_3);
        // 完整的参数2
        list.add(vm.addLocalObject(new ArrayObject(input2_1, input2_2, input2_3)));
        Number number = module.callFunction(emulator, 0x5a38d, list.toArray());
        return vm.getObject(number.intValue()).getValue().toString();
    };




}
