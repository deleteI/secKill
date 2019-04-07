package com.nnngu.Test;

import com.nnngu.dao.cache.RedisDao;
import com.nnngu.entity.Seckill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

@Component
public class Mmain {

    @Autowired
     private static RedisDao redisDao;
    TestEnun test;
    public Mmain(TestEnun test){
        this.test=test;
    }
    public void dep(){
        switch (test){
            case ONE:
                System.out.println("sss");
        }
    }
    public static void main(String[] args) throws IOException {
        URL url = new URL("https://www.bilibili.com/video/av37672360/?p=238");
        InputStream stream = url.openStream();
        BufferedReader bf = new BufferedReader(new InputStreamReader(stream));
        String s=null;
        while(null!=(s=bf.readLine())){
            System.out.println(s);
        }
    }
}
