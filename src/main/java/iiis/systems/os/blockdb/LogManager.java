/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package iiis.systems.os.blockdb;
import java.util.*;
import com.google.common.io.*;
import java.io.File;
import java.nio.charset.Charset;
import org.json.JSONArray;
import org.json.JSONObject;
/**
 *
 * @author Guo Jingzhe
 */
public class LogManager {
    private String dataDir;
    public int logLength=0;
    public static final int maxLogLength=50;
    private TransactionLogs logs;
    public LogManager(String data){
        dataDir=data;
        logs=new TransactionLogs(dataDir);
    }
    
    public void initialize(HashMap<String, Integer> map){
        if(new File(dataDir+"/latest.json").exists()){
            logs.readData(map);
        }else{
            System.out.println("latest.json not detected, Performing a clean start.");
            logs.cleanStart();
        }
        
    }

    public synchronized boolean handlePut(String userId, int value){
        return logs.addLog(Transaction.newBuilder().setType(Transaction.Types.PUT).setUserID(userId).setValue(value).build());
    }
    public synchronized boolean handleWithdraw(String userId, int value){
        return logs.addLog(Transaction.newBuilder().setType(Transaction.Types.WITHDRAW).setUserID(userId).setValue(value).build());

    }
    public synchronized boolean handleDeposit(String userId, int value){
        return logs.addLog(Transaction.newBuilder().setType(Transaction.Types.DEPOSIT).setUserID(userId).setValue(value).build());
    }
    public synchronized boolean handleTransfer(String fromId, String toId, int value){
        return logs.addLog(Transaction.newBuilder().setType(Transaction.Types.TRANSFER).setFromID(fromId).setToID(toId).setValue(value).build());
    }
    //Writing everything in memory to disk.
    public synchronized void flush() throws Exception{
        logs.flush();
    }
    
    public synchronized int getLogLength(){
        return logs.logLength;
    }
    public class TransactionLogs{
        public int logLength;
        public int lastBlockID;
        private final String logPath;
        public LinkedList<Transaction> cache;
        private void crash(String reason) throws Exception{
            System.err.println("Crash while loading transaction logs: "+reason);
            throw new Exception();
        }
        private void cleanStart(){
            try{
                logLength=0;
                lastBlockID=0;
                flush();
            }catch(Exception ex){
                System.err.println("Clean start failed for some strange reasons.");
                ex.printStackTrace();
                System.exit(1);
            }
        }
        private void applyTransaction(HashMap<String, Integer> map, Transaction t) throws Exception{
            if(t.getType()==Transaction.Types.PUT){
                map.put(t.getUserID(), t.getValue());
            }else if(t.getType()==Transaction.Types.DEPOSIT){
                int v=0;
                if(map.containsKey(t.getUserID())) v=map.get(t.getUserID());
                map.put(t.getUserID(), v+t.getValue());
            }else if(t.getType()==Transaction.Types.WITHDRAW){
                int v=0;
                if(map.containsKey(t.getUserID())) v=map.get(t.getUserID());
                if(v<t.getValue()) crash("Bad withdraw cause account money drop below zero.");
                map.put(t.getUserID(), v-t.getValue());
            }else if(t.getType()==Transaction.Types.TRANSFER){
                int v1=0;
                int v2=0;
                if(map.containsKey(t.getFromID())) v1=map.get(t.getFromID());
                if(map.containsKey(t.getToID())) v2=map.get(t.getToID());
                if(v1<t.getValue()) crash("Bad transfer cause account money drop below zero.");
                map.put(t.getFromID(), v1-t.getValue());
                map.put(t.getToID(), v2+t.getValue());
            }else{
                crash("Bad Type: "+t.getType().toString());
            }
        }
        public TransactionLogs(String logPath){
            this.logPath=logPath;
            cache=new LinkedList<>();
        }
        public void readData(HashMap<String, Integer> data){
            try{
                String logs_content=Files.toString(new File(logPath+"/latest.json"), Charset.forName("UTF-8"));
                JSONObject obj=new JSONObject(logs_content);
                logLength=obj.getInt("LogLength");
                if(logLength>=maxLogLength){
                    crash("Too long temporary log!");
                }
                lastBlockID=obj.getInt("LastBlock");
                Iterator cache_iter=obj.getJSONArray("CachedTransactions").iterator();
                while(cache_iter.hasNext()){
                    String code=((String)cache_iter.next());
                    byte buf[]=BaseEncoding.base64().decode(code);
                    Transaction t=Transaction.parseFrom(buf);
                    cache.push(t);
                }
                for(int i=1;i<lastBlockID;i++){
                    String block_content=Files.toString(new File(logPath+"/"+i+".json"), Charset.forName("UTF-8"));
                    JSONObject block=new JSONObject(block_content);
                    if(block.getInt("BlockID")!=i) crash("BlockID and Block File name Mismatch!");
                    if(block.getJSONArray("Transactions").length()!=maxLogLength){
                        crash("Block too short!");
                    }
                    Iterator iter=block.getJSONArray("Transactions").iterator();
                    while(iter.hasNext()){
                        JSONObject t=(JSONObject)iter.next();
                        Transaction.Builder b=Transaction.newBuilder();
                        b.setType(Transaction.Types.valueOf(t.getString("Type")));
                        if(t.getString("Type").equals("TRANSFER")){
                            b.setFromID(t.getString("FromID"));
                            b.setToID(t.getString("ToID"));
                        }else{
                            b.setUserID(t.getString("UserID"));
                        }
                        b.setValue(t.getInt("Value"));
                        applyTransaction(data, b.build());
                    }
                }
                for (Transaction t:cache) {
                    applyTransaction(data, t);
                }
            }catch(Exception ex){
                ex.printStackTrace();
                System.exit(1);
            }
        }
        public synchronized void flush() throws Exception{
            if(logLength==maxLogLength){
                lastBlockID++;
                logLength=0;
                JSONObject block=new JSONObject();
                block.put("BlockID", lastBlockID);
                block.put("PrevHash","00000000");
                block.put("Nonce","00000000");
                JSONArray transactions=new JSONArray();
                for(Transaction t:cache){
                    JSONObject tj=new JSONObject();
                    tj.put("Type", t.getType().toString());
                    if(t.getType()==Transaction.Types.TRANSFER){
                        tj.put("FromID", t.getFromID());
                        tj.put("ToID", t.getToID());
                        
                    }else{
                        tj.put("UserID", t.getUserID());
                    }
                    tj.put("Value", t.getValue());
                    transactions.put(tj);
                }
                cache.clear();
                block.put("Transactions", transactions);
                String block_json=block.toString();
                Files.write(block_json, new File(logPath+"/"+lastBlockID+".json"), Charset.forName("UTF-8"));
            }
            JSONObject latest=new JSONObject();
            latest.put("LogLength", logLength);
            latest.put("LastBlock", lastBlockID);
            JSONArray transactions=new JSONArray();
            for(Transaction t: cache){
                transactions.put(BaseEncoding.base64().encode(t.toByteArray()));
            }
            latest.put("CachedTransactions", transactions);
            AtomicFileHelper.atomicWrite(logPath+"/latest.json", latest.toString());
        }
        public synchronized boolean addLog(Transaction t){
            cache.add(t);
            logLength++;
            try{
                flush();
                return true;
            }catch(Exception ex){
                System.err.println("Log Add Error!");
                return false;
            }
        }
    }
}
