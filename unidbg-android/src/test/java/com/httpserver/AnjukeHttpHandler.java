package com.httpserver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.anjuke.mobile.sign.SignUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class AnjukeHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) {
        try {
            StringBuilder responseText = new StringBuilder();

            responseText.append(new String("Request type:".getBytes("utf-8"),"utf-8")).append(httpExchange.getRequestMethod()).append("<br/>");
            //responseText.append("Request args:").append(getRequestParam(httpExchange)).append("<br/>");
            responseText.append("Request uri:").append(httpExchange.getRequestURI().toString()).append("<br/>");

            responseText.append("Request head:").append("123");
            String strbody = getRequestParam(httpExchange);
            System.out.println("body:"+strbody);
            Map<String,String> mapargs = AnalysisArgs(strbody, httpExchange.getRequestMethod());
            String anjukeSig = SignUtil.getAnjukeSig(mapargs.get("p1"), mapargs.get("p2"), mapargs.get("p3"));
            Map<String,String> retResponse = new HashMap<String, String>();
            retResponse.put("sig",anjukeSig);
            handleResponse(httpExchange, retResponse);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    /**
     * 获取请求参数
     * @param httpExchange
     * @return
     * @throws Exception
     */
    private String getRequestParam(HttpExchange httpExchange) throws Exception {
        String paramStr = "";

        if (httpExchange.getRequestMethod().equals("GET")) {
            //GET请求读queryString
            paramStr = httpExchange.getRequestURI().getQuery();
        } else {
            //非GET请求读请求体
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpExchange.getRequestBody(), "utf-8"));
            StringBuilder requestBodyContent = new StringBuilder();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                requestBodyContent.append(line);
            }
            paramStr = requestBodyContent.toString();
        }

        return paramStr;
    }
    public Map<String,String> AnalysisArgs(String instr,String type){
        HashMap<String, String> retMap = new HashMap();
        if(type.equals("GET")) {

            String[] args = instr.split("&");
            for (String arg : args) {
                String[] mapsStr = arg.split("=");
                if (mapsStr.length > 1) {
                    retMap.put(mapsStr[0], mapsStr[1]);
                }
            }
        }else{

            JSONObject map1 = JSON.parseObject(instr);
            Map<String, Object> itemMap = JSONObject.toJavaObject(map1, Map.class);
            for(String key : itemMap.keySet()){
                retMap.put(key, itemMap.get(key).toString());
            }
        }
        return retMap;
    }

    /**
     * 处理响应
     * @param httpExchange
     * @param responsetext
     * @throws Exception
     */
    private void handleResponse(HttpExchange httpExchange, Map responsetext) throws Exception {
        //生成html
        String json= JSON.toJSONString(responsetext);
        byte[] responseContentByte = json.getBytes("utf-8");

        //设置响应头，必须在sendResponseHeaders方法之前设置！
        //httpExchange.getResponseHeaders().add("Content-Type:", "text/html;charset=utf-8");
        httpExchange.getResponseHeaders().add("Content-Type:", "application/json");

        //设置响应码和响应体长度，必须在getResponseBody方法之前调用！
        httpExchange.sendResponseHeaders(200, responseContentByte.length);

        OutputStream out = httpExchange.getResponseBody();
        out.write(responseContentByte);
        out.flush();
        out.close();
    }
}
