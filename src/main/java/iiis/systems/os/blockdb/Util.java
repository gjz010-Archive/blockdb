package iiis.systems.os.blockdb;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
public class Util {
    public static JSONObject readJsonFile(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        return new JSONObject(content);
    }
    public static boolean CRASH_TEST=true;
    public static double CRASH_RATE=0.1;
    private static Random r=new Random();
    public static void testfail(){
        testfail(CRASH_RATE);
    }
    public static void testfail(double rate){
        if(CRASH_TEST){
            if(r.nextDouble()>=rate) return; 
            System.out.println("Mock critical error raised!");
            System.exit(1);
        }
    }
}
