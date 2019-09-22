package com.xemplarsoft.dv.medium.server;

import com.xemplarsoft.dv.medium.CryptoHandler;

import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class DVServerHandler implements Runnable {
    protected volatile ArrayList<DVClientHandler> clients;
    protected volatile boolean isRunning = false;
    protected DVClientListener listener;
    protected ServerSocket server;
    protected Thread t, t2;

    protected static volatile BigDecimal usd, btc;

    public DVServerHandler(DVClientListener listener){
        this.t = new Thread(this::run);
        this.t2 = new Thread(this::run2);
        this.clients = new ArrayList<>();
        this.listener = listener;
        usd = new BigDecimal("0.0");
        btc = new BigDecimal("0.0");
        DVClientHandler.crawler.setServerHandler(this);
    }

    public synchronized void start(){
        isRunning = true;
        t.start();
        t2.start();
        System.out.println("INFO: Server Starting");
    }

    public synchronized void stop(){
        isRunning = false;
    }

    public synchronized void setPrices(BigDecimal usdn, BigDecimal btcn){
        usd = usdn;
        btc = btcn;

        writeToAllENC("market usd " + usd.toPlainString());
        writeToAllENC("market btc " + btc.toPlainString());
    }

    public void run() {
        Socket socket = null;
        try {
            server = new ServerSocket(24516);
        } catch (Exception e){
            e.printStackTrace();
        }

        while (isRunning){
            try {
                System.out.println("INFO: Waiting for Client");
                socket = server.accept();
                System.out.println("INFO: Client Found, IP: " + socket.getInetAddress().getHostAddress());
            } catch (Exception e){
                e.printStackTrace();
            }
            DVClientHandler handler = new DVClientHandler(socket, listener);
            handler.start();
            clients.add(handler);
            System.out.println("INFO: Client Handler Started");
        }
        try {
            t.join();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void run2(){
        while(isRunning){
            DVClientHandler delete = null;
            for(int i = 0; i < clients.size(); i++){
                if(clients.get(i) == null) continue;
                if(clients.get(i).delete){
                    delete = clients.get(i);
                    break;
                }
            }

            if(delete != null){
                delete.stop();
                clients.remove(delete);
            }

            writeToAll("AWK");
            try{
                Thread.sleep(3000);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void writeToClient(long UID, String message){
        for(DVClientHandler cli : clients){
            if(cli.UID == UID){
                try {
                    cli.encryptAndWrite(message);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    public void writeToAllENC(String message) {
        for(int i = 0; i < clients.size(); i++) {
            try {
                clients.get(i).write(CryptoHandler.encryptMessage(message, clients.get(i).sync));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void writeToAll(String message) {
        for(int i = 0; i < clients.size(); i++) {
            try {
                clients.get(i).write(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
