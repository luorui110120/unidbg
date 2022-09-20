package com.github.unidbg.utils;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class ZipUtils {

//    public static void main(String[] args) throws Exception {
//        File sourceFile = new File("/Users/smali/tmp/t103/savedump");
//        fileToZip(sourceFile, "/Users/smali/tmp/t103/savedump.zip");
//    }
    /**
     * sourceFile一定要是文件夹
     * 默认会在同目录下生成zip文件
     *
     * @param sourceFilePath
     * @throws Exception
     */
    public static void fileToZip(String sourceFilePath, String outZipPath){
        fileToZip(sourceFilePath, outZipPath, true);
    }
    public static void fileToZip(String sourceFilePath, String outZipPath, boolean bSaveDirStructure) {

        try {
            fileToZip(new File(sourceFilePath), outZipPath, bSaveDirStructure);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void fileToZip(File sourceFile, String outZipPath) throws Exception{
        fileToZip(sourceFile, outZipPath, true);
    }
    /**
     * sourceFile一定要是文件夹
     * 默认会在同目录下生成zip文件
     *
     * @param sourceFile
     * @throws Exception
     */
    public static void fileToZip(File sourceFile, String outZipPath, boolean bSaveDirStructure) throws Exception {

        if (!sourceFile.exists()) {
            throw new RuntimeException("不存在");
        }
        if (!sourceFile.isDirectory()) {
            throw new RuntimeException("不是文件夹");
        }
        //zip文件生成位置
        File zipFile = new File(outZipPath);
        FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
        fileToZip(zos, sourceFile, "", bSaveDirStructure);
        zos.close();
        fos.close();
    }


    private static void fileToZip(ZipOutputStream zos, File sourceFile, String path, boolean bSaveDirStructure) throws Exception {

        //System.out.println(sourceFile.getAbsolutePath());

        //如果是文件夹只创建zip实体即可，如果是文件，创建zip实体后还要读取文件内容并写入
        if (sourceFile.isDirectory()) {
            path = path + sourceFile.getName() + "/";
            if(bSaveDirStructure){
                ZipEntry zipEntry = new ZipEntry(path);
                zos.putNextEntry(zipEntry);
            }
            for (File file : sourceFile.listFiles()) {
                fileToZip(zos, file, path, bSaveDirStructure);
            }
        } else {
            //创建ZIP实体，并添加进压缩包
            String name = "";
            if(bSaveDirStructure){
                name = path + sourceFile.getName();
            }
            else{
                name = sourceFile.getName();
            }
            ZipEntry zipEntry = new ZipEntry(name);
            zos.putNextEntry(zipEntry);
            byte[] bufs = new byte[1024 * 10];
            //读取待压缩的文件并写进压缩包里
            FileInputStream fis = new FileInputStream(sourceFile);
            BufferedInputStream bis = new BufferedInputStream(fis, 1024 * 10);
            int read = 0;
            while ((read = bis.read(bufs, 0, 1024 * 10)) != -1) {
                zos.write(bufs, 0, read);
            }
            bis.close();
            fis.close();
        }
    }
}
