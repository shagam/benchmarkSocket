/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package benchmarksocket;


import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author eli
 */
public class BenchmarkSocket implements Runnable {
    static final int PORT_NUM_BASE = 20000; 
    static int s_threadCnt;
    static int s_delay;
    static boolean s_verbose;
    static boolean s_help;
    static int s_portNum;
    int m_portNum; 
    static int s_processNum;
    
    static Thread [] s_thread;
    static long s_startTimeMili = System.currentTimeMillis();

    static ObjectOutputStream [] s_outputStreamArray;
    
    int m_loops = 0;
    int m_id;
   
    
    public BenchmarkSocket (int id) {
        m_id = id;
    }    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
    
        // get args
        s_processNum = Args.getInteger ("process", args, "ip port wrap (for process communication)");
        
        s_threadCnt = Args.getInteger("threads", args, "number of concurrent threads");
        if (s_threadCnt == Integer.MAX_VALUE)
            s_threadCnt = 16; // Runtime.getRuntime().availableProcessors();
        
//        if (s_processNum != Integer.MAX_VALUE)
//            assert s_threadCnt == 1;
        else if (s_threadCnt < 2 || s_threadCnt > 1000) {
            System.err.format("\nerr threadcount out of limits =%d", s_threadCnt);
            System.exit(0);            
        }

        s_thread = new Thread [s_threadCnt];

        s_delay = Args.getInteger("delay", args, "duration of test");
        if (s_delay == Integer.MAX_VALUE)
            s_delay = 5000;

        s_verbose = Args.getBool("verbose", args, "print debug info");

        s_portNum = Args.getInteger ("port", args, "ip port base (thread 0 portnum)");
        if (s_portNum == Integer.MAX_VALUE)
            s_portNum = PORT_NUM_BASE;

        s_help = Args.getBool("help", args, "print args info");        
        Args.showAndVerify (s_help);
        

       // create threads
       int threadCnt;
       if (s_processNum == Integer.MAX_VALUE)
           threadCnt = s_threadCnt;
       else
           threadCnt = 1;
        for (int n = 0; n < threadCnt; n++) {        
            Runnable runable = new BenchmarkSocket (n);               
            s_thread[n] = new Thread (runable);
            s_thread[n].start();
        }        
        
        if (s_processNum != Integer.MAX_VALUE)
            protectedSleep(2000);
        
        // client code      
        s_outputStreamArray = new ObjectOutputStream [s_threadCnt];
        for (int n = 0; n < threadCnt; n++) {
            try {                
                int nextThread;
                if (s_processNum == Integer.MAX_VALUE)
                    nextThread = (n + 1) % s_threadCnt;
                else
                    nextThread = (s_processNum + 1) % s_threadCnt;
                
                if (s_verbose)                 
                System.err.format("\nthread=%d before connect port=%d ", n, s_portNum + nextThread); 
                                
                Socket outSocket = new Socket("localhost", s_portNum + nextThread); 
                    
                s_outputStreamArray[n] = new ObjectOutputStream(outSocket.getOutputStream());
                if (s_verbose)                
                    System.err.format("\nthread=%d connected port=%d ", n, s_portNum + nextThread);                
            }
            catch (IOException e) {
                e.printStackTrace();
                System.err.format("\nerr thread=%d connect fail ", n);
                System.exit(0);              
            }            
        }

        // first thread need to start the chain        
        if (s_processNum != Integer.MAX_VALUE)
            protectedSleep(2000);
        Frame frame = new Frame(false);
        try {
            s_outputStreamArray[0].writeUnshared (frame);
        }
        catch (IOException e) {
            e.printStackTrace();            
        }
        
        // wait for threads finish
        for (int i = 0; i < s_threadCnt; i++){
            try {
                s_thread[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }        
    }
        
    public void run () {
        
        // server code
        
        ServerSocket serverSocket = null;
        ObjectInputStream  serverInputStream = null;
        int portNum = -1;
        try {
            if (s_processNum == Integer.MAX_VALUE)
                portNum = s_portNum + m_id;
            else
                portNum = s_portNum + s_processNum;
            if (s_verbose)
                System.err.format("\nthread=%d before accept port=%d ", m_id, portNum); 
            serverSocket = new ServerSocket (portNum);
         
            Socket clientSocket = serverSocket.accept();            
            serverInputStream = new ObjectInputStream (clientSocket.getInputStream());
            if (s_verbose)            
                System.err.format("\nthread=%d after accept ", m_id);
        }
        catch (IOException e) {
            e.printStackTrace();
            System.err.format("\nerr thread=%d server fail ", m_id);
            System.exit(1);              
        }
        protectedSleep(1000);            
        // ping pong chain loop
        for (long loops = 0;  ; loops++) {
            if ((loops & 0xf) == 0) {
                if (System.currentTimeMillis() - s_startTimeMili > s_delay) {
                    String txt = String.format("\nexit, thread=%d port=%d loops=%d  ", m_id, portNum, loops);
                    error (txt);
                    sendFrame (null, true);
                    try {
                        s_outputStreamArray[m_id].close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                    protectedSleep(20);
                    System.exit(0);
                }
            }
            
            try {
                Frame frame = (Frame) serverInputStream.readUnshared();
                if (frame.getExit()) {
                    String txt = String.format("\nexit, thread=%d port=%d loops=%d  from other\n", m_id, s_portNum, loops);
                    error (txt);           
                    System.exit(0);
                }
                sendFrame (frame, false);              
                //s_outputStreamArray[m_id].writeUnshared (frame);                
            }
            catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
                System.err.format( "\n" + e.getClass().getSimpleName() + " exit thread=%d port=%d loops=%d \n", m_id, s_portNum, loops);
                System.exit(1);                
            }                       
        }

    }

    int count = 0;    
    void sendFrame (Frame frame, boolean exit) {

        if (frame == null)
            frame = new Frame(exit);
        try {
            count ++;
            assert s_outputStreamArray[m_id] != null : " id=" + m_id + " process=" + s_processNum;
            s_outputStreamArray[m_id].writeUnshared (frame);
            if ((count & 0xf) == 0)            
            s_outputStreamArray[m_id].reset();
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }          
    }    

    public static void protectedSleep (long delay) {
        try {
           Thread.sleep (delay);
        } catch (Exception e) {
           e.printStackTrace();
           return;
        }
    }

    synchronized void error (String txt) {
        System.err.format( "\n" + txt);
    }
}


class Frame implements Serializable {
    private static final long serialVersionUID = 1L;    
    boolean m_exit;

    public Frame (boolean value) {
        m_exit = value;
    }
    
    public boolean getExit () {
        return m_exit;
    }
   
}

