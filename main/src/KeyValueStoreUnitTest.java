import org.junit.*;

public class KeyValueStoreUnitTest {

	@Test
   	public void getKeyValueTest() {
   	    // may use mockito for complex cases.
   	    KeyValueStore store = KeyValueStore.getInstance();
            store.put("a", 1, System.currentTimeMillis());
      	    store.put("b", 2, System.currentTimeMillis());
      	    assertEquals(1,store.get("a"));  
      	    assertEquals(2,store.get("b"));
      	    assertEquals(1.5,store.average()); 
   	}
}
