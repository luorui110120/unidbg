package com.utils;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.*;

public class SmileUtils {
    //// 获取当前jar包的所在路径;
    public static String getJarDirPath()
    {
        String path = SmileUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        if(System.getProperty("os.name").contains("dows"))
        {
            path = path.substring(1,path.length());
        }
        if(path.contains("jar"))
        {
            // System.out.println("jar = " + path);
            path = path.substring(0,path.lastIndexOf("."));
            return path.substring(0,path.lastIndexOf("/"));
        }

        // System.out.println(path);
        // path.replace("target/classes/", "");
        return path.replace("/target/test-classes/", "");
    }
    public static List<String> readZipAllName(String zippath) {
        List<String> retlist = null;
        File file = new File(zippath);
        try {
            retlist = new ArrayList<String>();
            ZipInputStream zipInput = null ;	// 定义压缩输入流
            ZipEntry entry = null ;	// 每一个压缩实体
            zipInput = new ZipInputStream(new FileInputStream(file)) ;	// 实例化ZIpInputStream
            while((entry = zipInput.getNextEntry())!=null){	// 得到一个压缩实体
                //System.out.println("解压缩" + entry.getName() + "文件。") ;
                retlist.add(entry.getName());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return retlist;
    }
    public static byte[] readZipFile(String zippath, String name){
        byte[] retbytes = null;
        File file=new File(zippath);
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);
            ZipEntry zipEntry = zipFile.getEntry(name);
            InputStream inputStream = zipFile.getInputStream(zipEntry);
            retbytes = IOUtils.toByteArray(inputStream);
            inputStream.close();
            zipFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retbytes;
    }

    public static int checkCompressedSize(byte[] buffer) {
        Deflater compresser =  new Deflater();
        byte[] compressedBuffer = new byte[buffer.length + 0x20];
        // in fact the buffer could be of any positive size
        compresser.setInput(buffer);
        compresser.finish();
        int currentPos = 0;
        currentPos = compresser.deflate(compressedBuffer);
        return currentPos;
    }
    public static int checkDecompressedSize(byte[] inbytes) {
        int resultLength = -1;
        Deflater compresser = new Deflater();
        // 解压
        Inflater decompresser = new Inflater();
        decompresser.setInput(inbytes, 0, inbytes.length);
        byte[] result = new byte[inbytes.length * 3];
        try {
            resultLength = decompresser.inflate(result);
        } catch (DataFormatException e) {
            e.printStackTrace();
        }
        decompresser.end();
        return resultLength;
    }


    public static byte[] bytes2ZlibBytes(byte[] inbytes){
        Deflater compresser = new Deflater();
        int size = checkCompressedSize(inbytes);
        compresser.setInput(inbytes);
        compresser.finish();
        byte[] retbytes = new byte[size];
        int compressedDataLength = compresser.deflate(retbytes);
        return retbytes;
    }

    public static byte[] zlibBytes2Bytes(byte[] inbytes){
        return zlibBytes2Bytes(inbytes, 0);
    }
    public static byte[] zlibBytes2Bytes(byte[] inbytes, int size){

        Deflater compresser = new Deflater();
        // 解压
        if(size == 0){
            size = checkDecompressedSize(inbytes);
            if(size == -1){
                size = inbytes.length * 4;
            }
        }
        Inflater decompresser = new Inflater();
        decompresser.setInput(inbytes, 0, inbytes.length);
        try {
            byte[] retbytes = new byte[size];
            int resultLength = decompresser.inflate(retbytes);
            if(resultLength != size){
                return subByte(retbytes, 0, resultLength);
            }
            else{
                return retbytes;
            }

        } catch (DataFormatException e) {
            e.printStackTrace();
        }
        decompresser.end();
        return null;
    }
    /**
     * 截取byte数组   不改变原数组
     * @param b 原数组
     * @param off 偏差值（索引）
     * @param length 长度
     * @return 截取后的数组
     */
    public static byte[] subByte(byte[] b,int off,int length){
        byte[] b1 = new byte[length];
        System.arraycopy(b, off, b1, 0, length);
        return b1;
    }

    /**
     * 合并byte[]数组 （不改变原数组）
     * @param byte_1
     * @param byte_2
     * @return 合并后的数组
     */
    public static byte[] byteMerger(byte[] byte_1, byte[] byte_2){
        byte[] byte_3 = new byte[byte_1.length+byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
    public static byte[] getFileToBytes(String filePath) {
        File file = new File(filePath);
        long fileSize = file.length();
        if (fileSize > Integer.MAX_VALUE) {
            System.out.println("file too big...");
            return null;
        }
        FileInputStream fi = null;
        byte[] buffer = new byte[(int) fileSize];
        try {
            fi = new FileInputStream(file);
            int offset = 0;
            int numRead = 0;
            while (offset < buffer.length
                    && (numRead = fi.read(buffer, offset, buffer.length - offset)) >= 0) {
                offset += numRead;
            }
            // 确保所有数据均被读取
            if (offset != buffer.length) {
                return null;
            }
            fi.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }

    public static void logdebug(Object str) {
        Boolean a = false;
        if (a) {
            System.out.println(str);
        }
    }
    public static ByteBuffer byte2Byffer(byte[] byteArray) {


        //初始化一个和byte长度一样的buffer
        ByteBuffer buffer= ByteBuffer.allocate(byteArray.length);
        // 数组放到buffer中
        buffer.put(byteArray);
        //重置 limit 和postion 值 否则 buffer 读取数据不对
        buffer.flip();
        return buffer;
    }
    public static String bytes2String(byte[] byteArray){
        return new String(byteArray);
    }
    public static byte[] string2Bytes(String instr){
        return instr.getBytes();
    }


    //byte 数组与 long 的相互转换
    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(0, x);
        return buffer.array();
    }
    public static long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.put(bytes, 0, bytes.length);
        buffer.flip();//need flip
        return buffer.getLong();
    }


    //将16进制字符串转为 long
    public static long StringHex2Long(String instr){
        String newinstr = instr.replace("0x", "");
        return new BigInteger(newinstr, 16).longValue();
    }
    public static String long2StringHex(long inlong, int bit){
        BigInteger bigInt = new BigInteger(1, longToBytes(inlong));
        // 参数16表示16进制
        String result = bigInt.toString(16);
        if(bit == 32){
            // 不足32位高位补零
            while(result.length() < 8) {
                result = "0" + result;
            }
        }
        if(bit == 64){
            // 不足32位高位补零
            while(result.length() < 16) {
                result = "0" + result;
            }
        }
        return "0x" + result;
    }
    public static String long2StringHex(long inlong){
        return long2StringHex(inlong, 0);
    }

    public static boolean string2File(String instr, String filepath){
        return bytes2File(instr.getBytes(), filepath);
    }
    public static boolean bytes2File(byte[] inbytes, String filepath){
        try {
            FileOutputStream outputStream  = new FileOutputStream(new File(filepath));
            outputStream.write(inbytes);
            outputStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    public static byte[] file2byte(String filepath){
        byte[] bytes = null;
        try
        {
            File file = new File(filepath);
            FileInputStream fis = new FileInputStream(file);
            bytes = new byte[(int) file.length()];
            fis.read(bytes);
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bytes;
    }
    public static String geetMD5(byte[] inbytes) {
        if(inbytes == null || inbytes.length == 0) {
            return null;
        }
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(inbytes);
            byte[] byteArray = md5.digest();

            BigInteger bigInt = new BigInteger(1, byteArray);
            // 参数16表示16进制
            String result = bigInt.toString(16);
            // 不足32位高位补零
            while(result.length() < 32) {
                result = "0" + result;
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Create a new temporary directory. Use something like
     * {@link #recursiveDelete(File)} to clean this directory up since it isn't
     * deleted automatically
     * @return  the new directory
     * @throws IOException if there is an error creating the temporary directory
     */
    public static File createTempDir() throws IOException
    {
        final File sysTempDir = new File(System.getProperty("java.io.tmpdir"));
        File newTempDir;
        final int maxAttempts = 9;
        int attemptCount = 0;
        do
        {
            attemptCount++;
            if(attemptCount > maxAttempts)
            {
                throw new IOException(
                        "The highly improbable has occurred! Failed to " +
                                "create a unique temporary directory after " +
                                maxAttempts + " attempts.");
            }
            String dirName = UUID.randomUUID().toString();
            newTempDir = new File(sysTempDir, dirName);
        } while(newTempDir.exists());

        if(newTempDir.mkdirs())
        {
            return newTempDir;
        }
        else
        {
            throw new IOException(
                    "Failed to create temp dir named " +
                            newTempDir.getAbsolutePath());
        }
    }

    /**
     * Recursively delete file or directory
     * @param fileOrDir
     *          the file or dir to delete
     * @return
     *          true iff all files are successfully deleted
     */
    public static boolean recursiveDelete(File fileOrDir)
    {
        if(fileOrDir.isDirectory())
        {
            // recursively delete contents
            for(File innerFile: fileOrDir.listFiles())
            {
                if(!SmileUtils.recursiveDelete(innerFile))
                {
                    return false;
                }
            }
        }

        return fileOrDir.delete();
    }
    ////判断是在idea环境还是jar包
    public static <T> boolean isStartupFromJar() {
        String protocol = SmileUtils.class.getResource("").getProtocol();
        return Objects.equals(protocol, "jar");
    }
}
