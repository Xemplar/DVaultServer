package com.xemplarsoft.dv.test.data;

import static com.xemplarsoft.libs.util.DataUtil.bytes2hex;
import static com.xemplarsoft.libs.util.DataUtil.hex2bytes;

public class TX {
    protected final byte[] txid;

    public TX(String txid){
        this.txid = hex2bytes(txid);
    }

    public byte[] getRawTxid(){
        return txid;
    }

    public String getTxid(){
        return bytes2hex(txid);
    }
}
