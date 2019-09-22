package com.xemplarsoft.dv.test;

import com.xemplarsoft.dv.test.data.Address;
import com.xemplarsoft.dv.test.data.AddressManager;
import com.xemplarsoft.dv.test.tx.RawTransactionBuilder;
import com.xemplarsoft.libs.crypto.server.domain.UTXO;
import com.xemplarsoft.libs.crypto.server.domain.UTXOverview;
import com.xemplarsoft.libs.util.Base64;
import com.xemplarsoft.libs.util.Sha256;
import org.iq80.leveldb.Options;

import java.io.File;
import java.math.BigDecimal;
import java.security.interfaces.ECPrivateKey;
import java.util.ArrayList;
import java.util.Set;

import static com.xemplarsoft.Vars.deserialize;
import static com.xemplarsoft.dv.test.tx.RawTransactionBuilder.TX_FEE;
import static com.xemplarsoft.libs.crypto.KeyManager.convertPrivToWIF;
import static com.xemplarsoft.libs.crypto.KeyManager.convertWIFtoECPrivateKey;
import static com.xemplarsoft.libs.crypto.KeySet.adjustTo64;
import static com.xemplarsoft.libs.util.DataUtil.bytes2hex;

public class TestMain implements DVClientListener {
    private static final String AES = "jsHjwrDrUnMlG6y+5qdsqVzntp+trHOPF5GXLui+5f0=";
    private static final long UID = 5375409349650691407L;

    protected AddressManager addressManager;
    protected DVClient cli;
    protected DBWrapper db;

    protected BigDecimal usd, btc;
    protected long block;

    public TestMain(){
        addressManager = new AddressManager();

        String folder = System.getProperty("user.home") + "/dvault";
        File f = new File(folder);
        if(!f.exists()) f.mkdir();

        try {
            Options op = new Options();
            op.createIfMissing(true);

            db = new DBWrapper(f.getCanonicalPath(), op);
        } catch (Exception e){
            e.printStackTrace();
            return;
        }

        cli = new DVClient(this, AES, UID);
        cli.start();
        cli.getAddresses();
    }

    public void dwEventHappened(String data) {
        String[] tad = data.split(" ");
        boolean updateUI = false;
        switch (tad[0]){
            case "balance":{
                String address = tad[2];
                BigDecimal amount = new BigDecimal(tad[3]);
                if(tad[1].equals("received")){
                    if(!addressManager.hasAddress(address)){
                        cli.getAddresses();
                    } else {
                        BigDecimal bal = addressManager.getBalance(address);
                        addressManager.setBalance(address, bal.add(amount));
                        System.out.println(address + " received " + amount.toPlainString() + " D");
                        addressManager.addTX(address, tad[4]);
                    }
                } else if(tad[1].equals("staked")){
                    System.out.println(address + " staked " + amount.toPlainString() + " D");
                    BigDecimal bal = addressManager.getBalance(address);
                    addressManager.setBalance(address, bal.add(amount));
                    addressManager.addTX(address, tad[4]);
                } else if(tad[1].equals("sent")){
                    System.out.println(address + " sent " + amount.toPlainString() + " D");
                    BigDecimal bal = addressManager.getBalance(address);
                    addressManager.setBalance(address, bal.subtract(amount));
                } else if(tad[1].equals("total")){
                    if(address.equals("wallet")){
                        System.out.println("Your total balance is " + amount.toPlainString() + " D");
                    } else {
                        System.out.println(address + " has a total balance of " + amount.toPlainString() + " D");
                        addressManager.setBalance(address, amount);
                    }
                }
                updateUI = true;
                break;
            }
            case "addresses":{
                updateUI = true;
                String[] addresses = tad[1].split(":");
                for(String s : addresses){
                    if(!addressManager.hasAddress(s)){
                        addressManager.addAddress(s, new BigDecimal("-1.0"));
                    }
                }
                break;
            }

            case "address": {
                updateUI = true;
                String address = tad[1];
                if (!addressManager.hasAddress(address)) {
                    addressManager.addAddress(address, new BigDecimal("-1.0"));
                }
                break;
            }
            case "label": {
                updateUI = true;
                String address = tad[1];
                String label = tad[2];

                addressManager.setLabel(address, label);
                break;
            }

            case "addydata": {
                updateUI = true;
                String address = tad[1];
                BigDecimal balance = new BigDecimal(tad[2]);
                String label = tad[3];
                String[] txs = tad[4].split(":");

                if(!addressManager.hasAddress(address)){
                    addressManager.addAddress(new Address(address, label, balance, txs));
                } else {
                    addressManager.setLabel(address, label);
                    addressManager.setBalance(address, balance);
                    addressManager.setTXs(address, txs);
                    addressManager.clean(address);
                }
                break;
            }

            case "txs":{
                updateUI = true;
                String address = tad[1];
                String[] txs = tad[2].split(":");

                addressManager.setTXs(address, txs);
                break;
            }

            case "bh":{
                setBlockHeight(Integer.parseInt(tad[1]));
                break;
            }

            case "market":{
                if(tad[1].equals("usd")){
                    usd = new BigDecimal(tad[2]);
                } else if(tad[1].equals("btc")){
                    btc = new BigDecimal(tad[2]);
                }

                savePrices();
                break;
            }

            case "key":{
                String address = tad[1];
                cli.getAddyData(address);
                break;
            }

            case "pay":{
                updateUI = true;
                if(tad[1].equals("successful")){
                    addressManager.dirtyAll();
                }
                break;
            }

            case "unspent":{
                ArrayList<UTXO> serialized = new ArrayList<>();
                BigDecimal total = new BigDecimal(0);
                Class c;
                try {
                    c = Class.forName("com.xemplarsoft.libs.crypto.server.domain.UTXO");
                } catch (Exception e){
                    e.printStackTrace();
                    break;
                }

                if(tad[1].equals("for")){
                    String addr = tad[2];
                    for (int i = 3; i < tad.length; i++) {
                        String dat = new String(Base64.decode(tad[i]));

                        UTXO tx = (UTXO) deserialize(dat, c);
                        total = total.add(tx.getAmount());
                        serialized.add(tx);
                    }
                } else {
                    for (int i = 1; i < tad.length; i++) {
                        String dat = new String(Base64.decode(tad[i]));

                        UTXO tx = (UTXO) deserialize(dat, c);
                        total = total.add(tx.getAmount());
                        serialized.add(tx);
                    }
                }

                System.out.println("Total : " + total.toPlainString());
                break;
            }

            case "dumpprivkey":{
                System.out.println("WIF: " + tad[1]);
                break;
            }

            case "unspentfortx":{
                ArrayList<UTXO> serialized = new ArrayList<>();
                BigDecimal total = new BigDecimal(0);
                Class c;
                try {
                    c = Class.forName("com.xemplarsoft.libs.crypto.server.domain.UTXO");
                } catch (Exception e){
                    e.printStackTrace();
                    makePayment(null);
                    break;
                }

                for (int i = 1; i < tad.length; i++) {
                    String dat = new String(Base64.decode(tad[i]));

                    UTXO tx = (UTXO) deserialize(dat, c);
                    total = total.add(tx.getAmount());
                    serialized.add(tx);
                }

                makePayment(serialized);
                break;
            }

            case "rawtx":{
                String address = tad[1];
                String hex = tad[2];

                System.out.println("RAW TX: " + hex);

                String WIF = db.getString(address + ":WIF");
                //ECPrivateKey priv = KeyManager.convertWIFtoECPrivateKey(WIF);

                break;
            }

            case "decoded":{
                String dat = new String(Base64.decode(tad[1]));
                Class c;
                try {
                    c = Class.forName("com.xemplarsoft.libs.crypto.server.domain.RawTransactionOverview");
                } catch (Exception e){
                    e.printStackTrace();
                    break;
                }
                //RawTransactionOverview over = (RawTransactionOverview) deserialize(dat, c);
                break;
            }
        }

        if(updateUI){
            //TODO Update UI
        }

        Set<String> addresses = addressManager.getAddressSet();
        for(String s : addresses){
            if(addressManager.isDirty(s)){
                addressManager.clean(s);
                cli.getAddyData(s);
                break;
            }
        }
    }

    //Payment Methods
    private String to, from, narration;
    private BigDecimal amt, total;
    public void makePayment(String to, BigDecimal amount, String narration, String from){
        this.to = to;
        this.from = from;
        this.amt = amount;
        this.total = amount.add(TX_FEE);
        this.narration = narration;

        cli.createRawTXList(from);
    }
    private void makePayment(ArrayList<UTXO> utxos){
        if(utxos == null){
            this.to = null;
            this.from = null;
            this.amt = null;
            this.total = null;
            this.narration = null;
            return;
        }

        ArrayList<UTXO> required = new ArrayList<>();
        ArrayList<UTXOverview> requiredOverview = new ArrayList<>();
        BigDecimal soFar = new BigDecimal(0);
        boolean needsMore = true;
        int index = 0;

        while(needsMore){
            UTXO current = utxos.get(index);
            soFar = soFar.add(current.getAmount());
            required.add(current);
            requiredOverview.add(current);

            if(soFar.compareTo(total) >= 0){
                needsMore = false;
            }
            index++;

            if(index >= utxos.size()) needsMore = false;
        }

        String WIF = "QP5bV1BrQmy74wxmEEUb1DRMzU2cfUbpopyf1sL9HhSqtbWCaV2B";
        ECPrivateKey priv = convertWIFtoECPrivateKey(WIF);
        System.out.println("WIF: " + convertPrivToWIF(adjustTo64(priv.getS().toString(16)).toUpperCase()));

        RawTransactionBuilder raw = new RawTransactionBuilder(required, from, to, amt);
        System.out.println("RAWTXBUILDER unsigned: " + raw.buildUnsignedRawTX());
        System.out.println("RAWTXBUILDER txid: " + bytes2hex(Sha256.getHash(Sha256.getHash(raw.buildUnsignedRawTX()))));
        try {
            System.out.println("RAWTXBUILDER signed: " + raw.signTX(priv));
        } catch (Exception e){
            e.printStackTrace();
        }
        this.to = null;
        this.amt = null;
        this.total = null;
        this.narration = null;
    }

    //END

    public void savePrices(){
        db.putString("usd", usd.toPlainString());
    }

    public void setBlockHeight(int block){
        this.block = block;
    }

    public static void main(String[] args){
        new TestMain();
    }
}
