package com.github.tvbox.osc.util;

public class HistoryHelper {
    private static Integer[] hisNumArray = {50,100,150,200,300,500};

    public static final String getHistoryNumName(int index){
        Integer value = getHisNum(index);
        return value + "条";
    }

    public static final int getHisNum(int index){
        Integer value = null;
        if(index>=0 && index < hisNumArray.length){
            value = hisNumArray[index];
        }else{
            value = hisNumArray[0];
        }
        return value;
    }
    public static final int getHistoryNumArraySize(){
        return hisNumArray.length;
    }
}
