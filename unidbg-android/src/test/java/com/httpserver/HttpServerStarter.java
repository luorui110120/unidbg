package com.httpserver;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class HttpServerStarter {
    public static void main(String[] args) throws IOException {
        //创建一个HttpServer实例，并绑定到指定的IP地址和端口号
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(8181), 0);

        //创建一个HttpContext，将路径为/myserver请求映射到MyHttpHandler处理器
        /// get  请求的例子  http://127.0.0.1:8181/myserver?aaa=1&bbb=2
        httpServer.createContext("/myserver", new MyHttpHandler());
        /// post 请求的例子 http://127.0.0.1:8181/anjuke   post 内容  {"p1":"123","p2":"456","p3":"789"}
        httpServer.createContext("/anjuke", new AnjukeHttpHandler());

        //设置服务器的线程池对象
        httpServer.setExecutor(Executors.newFixedThreadPool(10));

        //启动服务器
        httpServer.start();
    }
}