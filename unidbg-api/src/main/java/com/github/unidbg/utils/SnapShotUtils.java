package com.github.unidbg.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.debugger.MmapInfo;
import com.github.unidbg.memory.MemRegion;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.sun.org.apache.bcel.internal.generic.ACONST_NULL;
import org.apache.commons.io.IOUtils;
import unicorn.UnicornConst;
import com.github.unidbg.Emulator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


public class SnapShotUtils {

    private static String s_SaveLicSym = null;

    public static void saveLibcSym(String instr){
        s_SaveLicSym = instr;
    }

    ////将  white_list 和  addr_list  设为 null, 将加载所有模块;
    public static void loadSnapShot(String dump_dir, Emulator emulator, List<String> white_list, List<Long> addr_list) {

        Backend backend = emulator.getBackend();
        Memory memory = emulator.getMemory();
        Map<String, UnidbgPointer> midamem = new IdentityHashMap<String, UnidbgPointer>();

        JSONObject regs = JSONObject.parseObject(readDumpjson(dump_dir, "regs.json"));


        Map<String, Integer> mapregs = null;
        if(emulator.is64Bit()){
            mapregs = RegsConst.arm64Regs;
        }
        else{
            mapregs = RegsConst.arm32Regs;
        }
        for(String strkey : mapregs.keySet()){
            if(null == regs.getString(strkey)){
                continue;
            }
            backend.reg_write(mapregs.get(strkey), KdUtils.StringHex2Long(regs.getString(strkey)));
        }


//        好像不设置这个也不会有什么影响
        memory.setStackPoint(Long.decode(regs.getString("sp")));

        JSONArray segments = JSONArray.parseArray(readDumpjson(dump_dir, "segments.json"));

        for (int i = 0; i < segments.size(); i++) {
            JSONObject segment = segments.getJSONObject(i);
            String path = segment.getString("name");
            long start = segment.getLong("start");
            long end = segment.getLong("end");
            String content_file = segment.getString("content_file");
            JSONObject permissions = segment.getJSONObject("permissions");
            int perms = 0;
            if (permissions.getBoolean("r")){
                perms |= UnicornConst.UC_PROT_READ;
            }
            if (permissions.getBoolean("w")){
                perms |= UnicornConst.UC_PROT_WRITE;
            }
            if (permissions.getBoolean("x")){
                perms |= UnicornConst.UC_PROT_EXEC;
            }

            String[] paths = path.split("/");
            String module_name = paths[paths.length - 1];

            if (white_list == null || white_list.contains(module_name) || isAddrRange(start, end, addr_list)){

                int size = (int)(end - start);

//                if(0xbff00000L == start ||
//                        0xFFFE0000L == start ||
//                        0x7FFFF0000L == start
//                ){
//                    continue;
//                }

                //String content_file_path = dump_dir + File.separator + content_file;
                boolean bmmaps = map_segment(start, size, perms, path, midamem, emulator);
//                if(!bmmaps){
//                    continue;
//                }
                byte[] content_file_buf = readDumpBin(dump_dir, content_file);
                if (null != content_file_buf){
                    byte[] result = KdUtils.zlibBytes2Bytes(content_file_buf, size);

                    backend.mem_write(start, result);
                }
                else {
                    System.out.println("not exists path=" + path);
                    byte[] fill_mem = new byte[size];
                    Arrays.fill( fill_mem, (byte) 0 );
                    backend.mem_write(start, fill_mem);
                }

            }
        }

        ///// 给段添加模块;
        Map<String, Map<String, UnidbgPointer>> tmpmem = new HashMap<String, Map<String, UnidbgPointer>>();
        for(Map.Entry<String, UnidbgPointer> vo : midamem.entrySet()){
            if( !tmpmem.containsKey(vo.getKey())){
                //List <Map<String, UnidbgPointer>>  tmplist= new ArrayList<Map<String, UnidbgPointer>>();
                Map<String, UnidbgPointer> tmp = new IdentityHashMap<String, UnidbgPointer> ();
                tmp.put(vo.getKey(), vo.getValue());
                tmpmem.put(vo.getKey(), tmp);
            }
            else{
                tmpmem.get(vo.getKey()).put(vo.getKey(), vo.getValue());
            }
        }

        for(String key : tmpmem.keySet()){
            memory.loadIdaModule(key, tmpmem.get(key));
        }

        ////将上面所有的段添加到一个模块中;
        //memory.loadIdaModule("dumpmem", midamem);

        //// 保存镜像;
        //save_context("/Users/smali/tmp/t103/savedump.zip");

    }

    public static boolean saveSnapShot(String zippath, Emulator emulator){
        String dirpath = null;
        File tmpdirFile = null;
        try {
            tmpdirFile = KdUtils.createTempDir();
        } catch (IOException e) {
            e.printStackTrace();
        }
        dirpath = tmpdirFile.getAbsolutePath();

        Memory memory = emulator.getMemory();
        Backend backend = emulator.getBackend();

        /////保存段信息;
        Collection<Module> modules = memory.getLoadedModules();
        List<MemRegion> list = new ArrayList<>(modules.size());
        for (Module module : modules) {
            ///不保存 unidbg段;
            if(!module.name.equals("unidbg")){
                list.addAll(module.getRegions());
            }
        }

        ///保存新添加的段
        List<MmapInfo> mmaplist = backend.mem_maplist();
        for(MmapInfo mmapvalue : mmaplist){
            ///过滤unidbg自己创建的段
//            if(0xbff00000L == mmapvalue.getAddr() ||
//                    0xFFFE0000L == mmapvalue.getAddr() ||
//                    0x7FFFF0000L == mmapvalue.getAddr()
//            ){
//                continue;
//            }
            boolean flags = true;
            for(MemRegion memreg : list){
                if(mmapvalue.getAddr() == memreg.begin){
                    flags = false;
                    break;
                }
            }
            if(flags){
                list.add(MemRegion.create(mmapvalue.getAddr(), (int)mmapvalue.getSize(), mmapvalue.getPerms(), "newmmap"));
            }
        }

        Collections.sort(list);
        JSONArray segments = new JSONArray();
        for (MemRegion mem : list){
            JSONObject segment = new JSONObject(true);
            segment.put("start", mem.begin);
            segment.put("end", mem.end);
            segment.put("name", mem.getName());
            JSONObject permissions = new JSONObject(true);
            if((mem.perms & UnicornConst.UC_PROT_READ) > 0){
                permissions.put("r", true);
            }
            else{
                permissions.put("r", false);
            }

            if((mem.perms & UnicornConst.UC_PROT_WRITE) > 0){
                permissions.put("w", true);
            }
            else{
                permissions.put("w", false);
            }
            if((mem.perms & UnicornConst.UC_PROT_EXEC) > 0){
                permissions.put("x", true);
            }
            else{
                permissions.put("x", false);
            }
            segment.put("permissions", permissions);
            byte[] data = backend.mem_read(mem.begin, mem.end - mem.begin);
            byte[] zlibdata = KdUtils.bytes2ZlibBytes(data);
            String zlibdataFileName = KdUtils.geetMD5(zlibdata) +  ".bin";
            segment.put("content_file", zlibdataFileName);

            KdUtils.bytes2File(zlibdata, dirpath + File.separator + zlibdataFileName);
            segments.add(segment);
        }
        KdUtils.string2File(segments.toJSONString(), dirpath + File.separator + "segments.json");

        //// 保存寄存器
        JSONObject regs = new JSONObject(true);

        Map<String, Integer> mapregs = null;
        int bit = 32;
        if(emulator.is64Bit()){
            mapregs = RegsConst.arm64Regs;
            bit = 64;
        }
        else{
            mapregs = RegsConst.arm32Regs;
        }
        for(String strkey : mapregs.keySet()){
            long tmpLong = backend.reg_read(mapregs.get(strkey)).longValue();
            regs.put(strkey, KdUtils.long2StringHex(tmpLong, bit));
            //    backend.reg_write(arm64Regs.get(strkey), KdUtils.StringHex2Long(regs.getString(strkey)));
        }
        KdUtils.string2File(regs.toJSONString(), dirpath + File.separator + "regs.json");

        //// 保存 libc.so 的符号表
        if(null != s_SaveLicSym){
            KdUtils.string2File(s_SaveLicSym, dirpath + File.separator + "libc.so_sym.json");
        }



        ZipUtils.fileToZip(dirpath, zippath, false);
        KdUtils.recursiveDelete(tmpdirFile);

        return true;
    }

    public static String readDumpjson(String filepath, String name){
        File file = new File(filepath);
        if(file.isDirectory())
        {
            InputStream is = null;
            try {
                is = new FileInputStream(filepath + File.separator + name);
                String jsonTxt = IOUtils.toString(is, "UTF-8");
                return jsonTxt;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        else{
            byte[] bytes = KdUtils.readZipFile(filepath, name);
            if(null != bytes){
                return new String(bytes);
            }
            else{
                return null;
            }
        }


    }
    public static byte[] readDumpBin(String filepath, String name){
        byte[] retbytes = null;
        File file = new File(filepath);
        if(file.isDirectory())
        {
            File content_file_f = new File(filepath + File.separator + name);
            if (content_file_f.exists()) {
                try {
                    InputStream content_file_is = new FileInputStream(filepath + File.separator + name);
                    retbytes = IOUtils.toByteArray(content_file_is);
                    content_file_is.close();
                } catch (Exception e) {
                    System.out.println("error:" + filepath + File.separator + name);
                    e.printStackTrace();
                }
            }
        }
        else{
            retbytes = KdUtils.readZipFile(filepath, name);
        }
        return retbytes;
    }
    private static int UNICORN_PAGE_SIZE = 0x1000;
    private static long align_page_down(long x){
        return x & ~(UNICORN_PAGE_SIZE - 1);
    }
    private static long align_page_up(long x){
        return (x + UNICORN_PAGE_SIZE - 1) & ~(UNICORN_PAGE_SIZE - 1);
    }

    private static boolean map_segment(long address, long size, int perms, String name, Map<String, UnidbgPointer> midamem, Emulator emulator){
        boolean bret = false;
        long mem_start = address;
        long mem_end = address + size;
        long mem_start_aligned = align_page_down(mem_start);
        long mem_end_aligned = align_page_up(mem_end);

        if (mem_start_aligned < mem_end_aligned){
            try{
                emulator.getBackend().mem_map(mem_start_aligned, mem_end_aligned - mem_start_aligned, perms);
                bret = true;
            }
            catch (Exception e){
                e.printStackTrace();
            }

            ///需要添加到这边ida中才能看到段;
            midamem.put(name ,UnidbgPointer.pointer(emulator, mem_start_aligned).setSize(mem_end_aligned - mem_start_aligned).setPerms(perms));
        }
        return bret;
    }

    ///设置地址白名单, 调试的时候非常方便使用;
    private static boolean isAddrRange(long startAddr, long endAddr, List<Long> addr_list){
        if(null == addr_list){
            return false;
        }
        for(Long value :addr_list ){
            if((value >= startAddr) &&(value < endAddr)){
                return true;
            }
        }
        return  false;
    }
}
