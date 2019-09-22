package com.xemplarsoft.dv.medium.server;

public interface DVClientListener {
    public void dataReceived(long UID, String data);
    public void dwEventHappened(String data);
}
