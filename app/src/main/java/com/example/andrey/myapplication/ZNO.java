package com.example.andrey.myapplication;

import java.util.Date;

class ZNO {
    String datestamp;
    String SDCIINFO;
    String SDESPPID;
    String SDCLASSIF;
    String SDSTATUS;
    String SDCIADDR;
    String SDTASKID;
    String SDSERVICE;
    String SDINFO;
    String SDCREATED;
    String SDDEADLINE;

    @Override
    public int hashCode(){
        return SDTASKID.hashCode();
    }

    @Override
    public boolean equals (Object obj){
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ZNO other = (ZNO) obj;
        if (!SDTASKID.equals(other.SDTASKID))
            return false;
        if (!SDCIADDR.equals(other.SDCIADDR))
            return false;
        if (!SDCIINFO.equals(other.SDCIINFO))
            return false;
        if (!SDCLASSIF.equals(other.SDCLASSIF))
            return false;
        if (!SDCREATED.equals(other.SDCREATED))
            return false;
        if (!SDDEADLINE.equals(other.SDDEADLINE))
            return false;
        if (!SDESPPID.equals(other.SDESPPID))
            return false;
        if (!SDINFO.equals(other.SDINFO))
            return false;
        if (!SDSERVICE.equals(other.SDSERVICE))
            return false;
        if (!SDSTATUS.equals(other.SDSTATUS))
            return false;
        return true;
    }

    @Override
    public String toString(){
        return datestamp+":"+SDTASKID;
    }
}
