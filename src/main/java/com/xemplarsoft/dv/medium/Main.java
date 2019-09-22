package com.xemplarsoft.dv.medium;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.xemplarsoft.Vars;
import com.xemplarsoft.dv.medium.market.MarketHandler;
import com.xemplarsoft.dv.medium.server.DVBlockchainCrawler;
import com.xemplarsoft.dv.medium.server.DVClientHandler;
import com.xemplarsoft.dv.medium.server.DVClientListener;
import com.xemplarsoft.dv.medium.server.DVServerHandler;
import com.xemplarsoft.libs.crypto.server.CommunicationException;
import com.xemplarsoft.libs.crypto.server.CryptocoinException;
import com.xemplarsoft.libs.crypto.server.domain.Entity;
import com.xemplarsoft.libs.crypto.server.domain.UTXO;
import com.xemplarsoft.libs.crypto.server.domain.UTXOverview;
import com.xemplarsoft.libs.crypto.server.link.CryptoLinkRPC;
import com.xemplarsoft.libs.crypto.server.link.CryptoLinkRPCImpl;
import com.xemplarsoft.libs.util.Base64;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main implements DVClientListener {
    public CryptoLinkRPC link;
    public DVServerHandler handler;
    public MarketHandler market;
    private Main() throws CommunicationException, CryptocoinException{
        link = new CryptoLinkRPCImpl("127.0.0.1", 32369, Vars.rpcuser, Vars.rpcpass);
        DVClientHandler.db = new DBHandler();
        DVClientHandler.crawler = new DVBlockchainCrawler(link, DVClientHandler.db);
        DVClientHandler.crawler.start();

        handler = new DVServerHandler(this);
        market = new MarketHandler(handler);
        handler.start();
        market.start();
    }

    public void dwEventHappened(String data) {}
    public void dataReceived(long UID, String data) {
        String[] dat = data.trim().split(" ");
        DVBlockchainCrawler crawler = DVClientHandler.crawler;
        try {
            if(dat.length == 1) {
                switch (data) {
                    case "getinfo":
                        handler.writeToClient(UID, "Info#" + Base64.encode(serialize(link.getInfo(), false).getBytes()));
                        break;
                    case "getblockcount":
                        handler.writeToClient(UID, "" + link.getBlockCount());
                        break;
                    case "listunspent": {
                        String ret = "unspent";
                        ArrayList<String> addrs = crawler.getAddresses(UID);
                        List<UTXO> utxos = link.listUnspent(0, Integer.MAX_VALUE, addrs);
                        for (UTXO utxo : utxos) {
                            if (utxo != null) ret += " " + (Base64.encode(serialize(utxo, false)));
                        }

                        handler.writeToClient(UID, ret);
                        break;
                    }
                }
            } else {
                switch (dat[0]){
                    case "getReceivedByAddress":{
                        handler.writeToClient(UID, "" + link.getReceivedByAddress(dat[1]));
                        break;
                    }

                    case "importprivkey":{
                        System.out.println("INFO: Client " + UID + " added new address.");
                        link.importPrivKey(dat[1], "" + UID, false);
                        break;
                    }

                    case "pay":{
                        String address = dat[1];
                        BigDecimal amt = new BigDecimal(dat[2]);
                        BigDecimal total = amt.add(new BigDecimal("0.00001"));

                        if(dat.length == 3) {
                            BigDecimal balance = crawler.getBalance(UID);
                            if (balance.compareTo(total) >= 0) {
                                //Double checks can't hurt
                                String txid = link.sendToAddress(address, amt);
                                if (txid != null) {
                                    BigDecimal left = new BigDecimal(total.toPlainString());
                                    ArrayList<String> addresses = crawler.getAddresses(UID);
                                    int index = 0;
                                    while (left.compareTo(new BigDecimal("0.0")) > 0) {
                                        BigDecimal balCur = crawler.getBalance(addresses.get(index));
                                        if (left.compareTo(balCur) <= 0) {
                                            crawler.subBalance(addresses.get(index), left);
                                            left = new BigDecimal("0.0");
                                        } else {
                                            crawler.subBalance(addresses.get(index), balCur);
                                            total = total.subtract(balCur);
                                        }
                                        index++;
                                    }
                                    handler.writeToClient(UID, "pay successful " + txid);
                                } else {
                                    handler.writeToClient(UID, "pay failed");
                                }
                            } else {
                                handler.writeToClient(UID, "pay failed");
                            }
                        } else if(dat.length == 5){
                            String narration = dat[3];
                            String from = dat[4];

                            BigDecimal balance = crawler.getBalance(from);
                            if (balance.compareTo(total) >= 0) {
                                crawler.subBalance(from, total);
                                String txid = link.sendToAddress(address, amt, narration, false);
                                handler.writeToClient(UID, "pay successful " + txid);
                            } else {
                                handler.writeToClient(UID, "pay failed");
                            }
                        }
                        break;
                    }

                    case "listunspent":{
                        String ret = "unspent";
                        ArrayList<String> addrs = new ArrayList<>();
                        for(int i = 1; i < dat.length; i++){
                            addrs.add(dat[i]);
                        }
                        List<UTXO> utxos = link.listUnspent(0, Integer.MAX_VALUE, addrs);
                        for (UTXO utxo : utxos) {
                            if (utxo != null) ret += " " + (Base64.encode(serialize(utxo, false)));
                        }

                        handler.writeToClient(UID, ret);
                        break;
                    }

                    case "dumpprivkey":{
                        String address = dat[1];
                        if(crawler.ownsAddress(UID, address)){
                            String wif = link.dumpPrivKey(address);
                            handler.writeToClient(UID, "dumpprivkey " + wif);
                        } else {
                            handler.writeToClient(UID, "dumpprivkey failed");
                        }

                        break;
                    }

                    case "createrawtx": {
                        String to = dat[1];
                        String from = dat[2];
                        BigDecimal amt = new BigDecimal(dat[3]);
                        BigDecimal change = new BigDecimal(dat[4]);
                        String[] serialized = dat[5].split(":");
                        ArrayList<UTXOverview> outs = new ArrayList<>();

                        Class c;
                        try {
                            c = Class.forName("com.xemplarsoft.libs.crypto.server.domain.UTXOverview");
                        } catch (Exception e){
                            e.printStackTrace();
                            break;
                        }
                        for(String s : serialized){
                            outs.add((UTXOverview) deserialize(new String(Base64.decode(s)), c));
                        }
                        Map<String, BigDecimal> pays = new HashMap<>();
                        pays.put(to, amt);
                        pays.put(from, change);

                        String raw = link.createRawTransaction(outs, pays);
                        handler.writeToClient(UID, "rawtx " + from + " " + raw);
                        String serial = Base64.encode(serialize(link.decodeRawTransaction(raw), false));
                        handler.writeToClient(UID, "decoded " + serial);
                        break;
                    }

                    case "listunspentfortx": {
                        String ret = "unspentfortx";
                        ArrayList<String> addrs = new ArrayList<>();
                        for(int i = 1; i < dat.length; i++){
                            addrs.add(dat[i]);
                        }
                        List<UTXO> utxos = link.listUnspent(0, Integer.MAX_VALUE, addrs);
                        for (UTXO utxo : utxos) {
                            if (utxo != null) ret += " " + (Base64.encode(serialize(utxo, false)));
                        }

                        handler.writeToClient(UID, ret);
                        break;
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static String serialize(Object obj, boolean pretty) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

            if (pretty) {
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
            }

            return mapper.writeValueAsString(obj);
        } catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }

    public static Entity deserialize(String str, Class<? extends Entity> clazz) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            Entity obj = mapper.readValue(str, clazz);

            return obj;
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args){
        if(args.length == 2){
            Vars.rpcuser = args[0];
            Vars.rpcpass = args[1];
        }
        try {
            new Main();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static boolean isUserAGoat(){
        return false;
    }
}
