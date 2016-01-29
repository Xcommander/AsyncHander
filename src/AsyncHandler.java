import sun.nio.cs.ext.MS874;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Created by xulinchao on 2016/1/29.
 */
/*
*同步与异步

       通常同步意味着一个任务的某个处理过程会对多个线程在用串行化处理，而异步则意味着某个处理过程可以允许多个线程同时处理。

       异步通常代表着更好的性能，因为它很大程度上依赖于缓冲，是典型的使用空间换时间的做法，例如在计算机当中，高速缓存作为cpu和磁盘io之间的缓冲地带协调cpu高速计算能力和磁盘的低速读写能力。

volatile

       应用场景：检查一个应用执行关闭或中断状态。因为此关键字拒绝了虚拟对一个变量多次赋值时的优化从而保证了虚拟机一定会检查被该关键字修饰的变量的状态变化。

CountDownLatch

       应用场景：控制在一组线程操作执行完成之前当前线程一直处于等待。例如在主线程中执行await()方法阻塞主线程，在工作线程执行完逻辑后执行countDown()方法。

本文示例场景：

       1，从控制台发送消息到消息服务器(由一个队列模拟)。

       2，将消息队列写入到文件(对写文件的操作设置延时以模拟性能瓶颈)。

       3，消息服务器作为控制台和文件写入之间的缓冲区。



示例代码：

      注：往消息队列添加消息可以通过for循环一次性加入，本文为了便于观察文件和队列的变化而采用了控制台输入，实际写一行文件记录速度应该高于手速，所以本文示例中增加了线程sleep时间。

CountDownLatch类是一个同步计数器,构造时传入int参数,该参数就是计数器的初始值，每调用一次countDown()方法，
计数器减1,计数器大于0 时，await()方法会阻塞程序继续执行




java中的传值和传引用区别

1.对象就是传引用

2.原始类型就是传值

3.String类型因为没有提供自身修改的函数，每次操作都是新生成一个String对象，所以要特殊对待。可以认为是传值。
*
*
* */
public class AsyncHandler {
    /*控制线程的资源释放*/
    private CountDownLatch countDownLatch;
    /*处理完成标示*/
    private volatile boolean handfinish;
    /*消息写入标示*/
    private volatile boolean sendfinish;
    /*消息队列*/
    private BlockingDeque<String> queue;
    /*缓冲写入*/
    private BufferedWriter bufferedWriter;

    //初始化
    public AsyncHandler(CountDownLatch latch){
        this.countDownLatch=latch;
        queue=new LinkedBlockingDeque<String>();
        File file=new File("E:/hello.txt");
        try {
            bufferedWriter=new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void handle(){
        new Thread(){
            @Override
            public void run() {
                while (!handfinish){
                    try {
                        TimeUnit.SECONDS.sleep(3);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    String s=queue.peek();
                    if(s!=null){
                        queue.poll();
                        try {
                            bufferedWriter.write(s);
                            bufferedWriter.newLine();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if(queue.isEmpty()&&sendfinish){
                        countDownLatch.countDown();
                        handfinish=true;
                        //System.out.println("xlc2"+countDownLatch.toString());
                        break;
                    }
                }
            }
        }.start();
    }

//    public void setSendfinish(boolean sendfinish) {
//        this.sendfinish = sendfinish;
//    }
    public void sendFinish(){
        sendfinish=true;
    }
    //发送消息
    public void sendMsg(String Msg){
        if(!Msg.isEmpty()&&Msg!=null){
            queue.add(Msg);
        }
    }
    //释放资源
    public void release() throws IOException {
        System.out.println("释放资源中........");
        if(bufferedWriter!=null){
            bufferedWriter.close();
        }
        if(queue!=null){
            queue.clear();
            queue=null;
        }
    }
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch=new CountDownLatch(1);
        AsyncHandler handler=new AsyncHandler(latch);
//        System.out.println("xlc1"+latch.toString());
        handler.handle();
        //注意:全局变量可以不预先赋值，系统会自动扫描全局变量并且赋值，局部变量需要赋值

        Scanner scanner=new Scanner(System.in);
        while(true){
            String s=scanner.next();
            if("exit".equals(s)){
                handler.sendFinish();
                break;
            }
            handler.sendMsg(s);
        }
        latch.await();
        try {
            handler.release();
        } catch (IOException e) {
            e.printStackTrace();
        }
        scanner.close();

    }

}
