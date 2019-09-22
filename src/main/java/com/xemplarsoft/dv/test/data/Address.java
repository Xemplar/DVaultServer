package com.xemplarsoft.dv.test.data;

import java.math.BigDecimal;
import java.util.ArrayList;

public class Address {
    protected final String address;
    protected String label;
    protected BigDecimal balance;
    protected boolean dirty = true;
    protected ArrayList<TX> txs;

    public Address(String address){
        this.address = address;
        this.balance = new BigDecimal("0.0");
        this.txs = new ArrayList<>();
    }

    public Address(String address, BigDecimal balance){
        this.address = address;
        this.balance = balance;
        this.txs = new ArrayList<>();
    }

    public Address(String address, String label, BigDecimal balance){
        this.address = address;
        this.balance = balance;
        this.label = label;
        this.txs = new ArrayList<>();
    }

    public Address(String address, String label, BigDecimal balance, String[] txs){
        this.address = address;
        this.balance = balance;
        this.label = label;
        this.txs = new ArrayList<>();
        for(String s : txs) this.txs.add(new TX(s));
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clean(){
        dirty = false;
    }

    public String getAddress() {
        return address;
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void setTXs(String[] txs){
        this.txs.clear();
        for(String s : txs) this.txs.add(new TX(s));
    }

    public void addTX(String tx){
        this.txs.add(new TX(tx));
    }

    public ArrayList<TX> getTXsList(){
        return txs;
    }

    public TX[] getTXs(){
        TX[] ret = new TX[txs.size()];
        ret = txs.toArray(ret);

        return ret;
    }

    public int getTXCount(){
        return txs.size();
    }
}
