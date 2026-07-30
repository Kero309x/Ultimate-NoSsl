import java.lang.reflect.Method;
public class TestRuntime {
    public static void main(String[] args) {
        for (Method m : Runtime.class.getDeclaredMethods()) {
            if (m.getName().contains("load")) {
                System.out.println(m);
            }
        }
    }
}
