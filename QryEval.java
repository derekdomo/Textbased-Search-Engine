/**
 * The only entrance of the whole search engine
 * Created by XiangyuSun on 14-9-8.
 */
import Main.*;
public class QryEval {
    public static void main(String[] args) throws Exception{
        long startTime = System.currentTimeMillis();
        MainEval en = new MainEval();
        en.Entr(args);
        long estimatedTime = System.currentTimeMillis() - startTime;
        System.out.println("Elapsed "+estimatedTime/1000);
    }
}
