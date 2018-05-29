/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package iiis.systems.os.blockdb;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.*;
import static iiis.systems.os.blockdb.Util.testfail;
/**
 * Use file move to make sure atomic file operation.
 * @author Guo Jingzhe
 */

public class AtomicFileHelper {
    private static boolean atomicWriteEnabled=false;
    private static boolean atomicWriteDisabled=true;
    public  static boolean selfCheck(){
        if(atomicWriteDisabled){
            System.err.println("Warning: Atomic file writing force disabled.");
        }else
        try{
            Path source=Files.createTempFile("AtomicSource",null);
            Path target=Files.createTempFile("AtomicTarget", null);
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(source);
            Files.deleteIfExists(target);
            atomicWriteEnabled=true;
            System.out.println("Atomic file write enabled.");
        }catch(Exception ex){
            System.err.println("Warning: Atomic file writing is not supported on this OS!");
            System.err.println("Using general move for atomic writing.");
        }
        return atomicWriteEnabled;
    }
    public static void atomicWrite(String target, String data) throws Exception{
        Path source=Files.createTempFile("_logtmp",".json");
        com.google.common.io.Files.write(data, source.toFile(), Charset.forName("UTF-8"));
        testfail();
        if(atomicWriteEnabled){
            Files.move(source, new File(target).toPath(), StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }else{
            Files.move(source, new File(target).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        
    }
}
