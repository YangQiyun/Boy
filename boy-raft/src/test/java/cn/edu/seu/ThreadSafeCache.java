package cn.edu.seu;

public class ThreadSafeCache {

    static class sim{
        int ok = 0;
    }

    private sim result = new sim();

    private sim getResult(){
        return result;
    }

    private void setResult(int r){
        result.ok = r;
    }

    private void setResult(sim s){
        result = s;
    }

    public static void main(String[] args){
        ThreadSafeCache safeCache = new ThreadSafeCache();


        for (int i = 0; i < 8; i++) {
            new Thread(()->{
                int x = 0;
                while (safeCache.getResult().ok<100){
                    if(x==Integer.MAX_VALUE){
                        System.out.println(safeCache.getResult().ok);
                        break;
                    }
                    x++;
                }
                System.out.println(x);
            }).start();
        }

        sim s = new sim();
        s.ok = 200;
        safeCache.setResult(s);
    }
}
