
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class) 
@SuiteClasses( { 

})
public class allTest {
  public static void main(String[] args) {
    JUnitCore.main(allTest.class.getName());
  }
}