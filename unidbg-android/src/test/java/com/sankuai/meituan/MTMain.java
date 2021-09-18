package com.sankuai.meituan;

import java.util.ArrayList;

public class MTMain {
    public static void main(String[] args){
        Nativeimpl hc = new Nativeimpl();
        ////先初始化
        hc.main111();
        //hc.main203();
        System.out.println(hc.main203("9b69f861-e054-4bc4-9daf-d36ae205ed3e", "GET /aggroup/homepage/display __r0ysue", 2));
    }
}
