package iiis.systems.os.blockdb;

import java.util.HashMap;
import java.util.HashSet;
import static iiis.systems.os.blockdb.Util.testfail;
public class DatabaseEngine {
    private static DatabaseEngine instance = null;
    private LogManager logs=null;
    public static DatabaseEngine getInstance() {
        return instance;
    }

    public static void setup(String dataDir) {
        instance = new DatabaseEngine(dataDir);
    }

    private final HashMap<String, Integer> balances = new HashMap<>();
    private String dataDir;

    DatabaseEngine(String dataDir) {
        this.dataDir = dataDir;
        logs=new LogManager(dataDir);
        logs.initialize(balances);
    }

    private int getOrZero(String userId) {
        synchronized(balances){
            if (balances.containsKey(userId)) {
                return balances.get(userId);
            } else {
                return 0;
            }
        }
    }


    public int get(String userId) {
        synchronized(balances){
            return getOrZero(userId);
        }
        
    }

    public boolean put(String userId, int value) {
        synchronized(balances){
            if(value<0) return false;
            if(!logs.handlePut(userId, value)) return false;
            balances.put(userId, value);
            return true;
        }
    }

    public boolean deposit(String userId, int value) {
        synchronized(balances){
            if(value<0) return false;
            if(!logs.handleDeposit(userId, value)) return false;
            int balance = getOrZero(userId);
            balances.put(userId, balance + value);
            return true;
        }
    }

    public boolean withdraw(String userId, int value) {
        synchronized(balances){
            if(value<0) return false;
            int balance=getOrZero(userId);
            if(balance<value) return false;
            if(!logs.handleWithdraw(userId, value)) return false;
            balances.put(userId, balance - value);
            return true;
        }
    }

    public boolean transfer(String fromId, String toId, int value) {
        synchronized(balances){
            if(value<0) return false;
            int fromBalance = getOrZero(fromId);
            int toBalance = getOrZero(toId);
            if(fromBalance<value) return false;
            if(!logs.handleTransfer(fromId, toId, value)) return false;
            balances.put(fromId, fromBalance - value);
            balances.put(toId, toBalance + value);
            return true;
        }
    }

    public int getLogLength() {
        testfail();
        return logs.getLogLength();
    }
}
