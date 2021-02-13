import java.util.*;
import java.util.concurrent.*; 
import java.io.*;

public class KeyValueStore {
    
    // 6 hours, 40 minutes and 0 seconds is timelimit for each key to expire.
    // epoch time in millis
    // pick up value from store eager loading
    private static final Long EXPIRE_TIMELIMIT = 24000000L;
    private static final String EXPIRED_KEY_VALUE_STORE = "expired-keys-store.txt";
    
    private Double avgTotalValue = 0.0;
    private Double avgTotalKeys = 0.0;
    
    // ConcurrentHashMap for thread safety
    // key, value pair
    private Map<String, Integer> keyValueMap = new ConcurrentHashMap<>();
    
    // key, key-expiry-epoch-time in millis pair
    private Map<String, Long> keyValidityMap = new ConcurrentHashMap<>();

    private static KeyValueStore ourInstance = new KeyValueStore();

    private KeyValueStore(){}

    public static KeyValueStore getInstance() {
        return ourInstance;
    }

    public void put(String key, Integer value, Long timestamp) {
        // updateKeyMap and make them thread-safe
        keyValueMap.put(key, value);
        keyValidityMap.put(key, timestamp+EXPIRE_TIMELIMIT);

        // updateAverage
        updateAverage(key, value, true);
    }
    
    public Integer get(String key) {
        // refreshKeyMap
        refreshKeyMap();
        // if key exists return value, otherwise null
        if(keyValueMap.containsKey(key)){
            return keyValueMap.get(key);
        }
        return null;
    }
    
    public double average() {
        if (avgTotalKeys == 0.0)
            return 0.0;
        return (avgTotalValue/avgTotalKeys);
    }
    
    //----------------------- PRIVATE METHODS ------------------------//

    private void updateAverage(String key, boolean addKey){
        Integer value = keyValueMap.get(key);
        updateAverage(key, value, addKey);
    }
    
    private void updateAverage(String key, Integer value, boolean addKey){
        Double defaultMultiplier = 1.0;
        if(addKey){
            defaultMultiplier = -1.0;
        }
        
        avgTotalValue = avgTotalValue + (value*defaultMultiplier);
        avgTotalKeys = avgTotalKeys + (1*defaultMultiplier);
    }
    
    private void writeExpiredKeyToStore(String key, Integer value){
        try {
            // currently append only to a local existing file.
            String str = key + "->" + String.valueOf(value);
            BufferedWriter writer = new BufferedWriter(new FileWriter(EXPIRED_KEY_VALUE_STORE, true));
            writer.write(str);
            writer.newLine();

            writer.close();
        } catch(Exception e) {
            // do logging if required
            System.out.println("Something went wrong.");
            e.printStackTrace(); 
        }
    }
    
    private void expireKey(String key){
        Integer value = keyValueMap.get(key);

        // updateKeyMap and make them thread-safe
        keyValueMap.remove(key);
        keyValidityMap.remove(key);

        // updateAverage
        updateAverage(key, false);

        // writeExpiredKeyToStore
        writeExpiredKeyToStore(key, value);
    }
    
    private void refreshKeyMap(){
        Long currentTimestamp = System.currentTimeMillis();
        // check each key for their expiry, expireKey
        // this can be optimised by keeping keys-values in a sorted order of timestamps.
        for (Map.Entry<String,Long> entry : keyValidityMap.entrySet()){
            String key = entry.getKey();
            if(currentTimestamp > entry.getValue()){
                expireKey(key);
            }
        }
    }  

    // -------------------- MAIN FOR TESTING ----------------------- //

    public static void main(String[] args) {
        KeyValueStore store = getInstance();
        store.put("a", 1, System.currentTimeMillis());
        store.put("b", 2, System.currentTimeMillis());
        Integer value1 = store.get("a");
        System.out.println(value1);
        Integer value2 = store.get("b");
        System.out.println(value2);
        double avg = store.average();
        System.out.println(avg);
    }
}