/*
 *Copyright 2014 DDPush
 *Author: AndyKwok(in English) GuoZhengzhu(in Chinese)
 *Email: ddpush@126.com
 *

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/
package com.amt.push.consts;


import com.amt.push.util.PropertyUtil;

/**
 * 　　* @Description: 常量
 * 　　* @author ckx
 * 　　* @date 2018/9/28 17:22
 */
public class Constant {

    // TODO: 2018/9/28 ckx 放入配置文件中
    public static final int CLIENT_MESSAGE_MIN_LENGTH = 21;//客户端传输数据最小长度
    public static final int CLIENT_MESSAGE_MAX_LENGTH = 29;//客户端传输数据最大长度
    public static final int SERVER_MESSAGE_MIN_LENGTH = 5;//终端机传输数据最小长度
    public static final int PUSH_MSG_HEADER_LEN = 21;//客户端传输数据头部最小长度
    public static int VERSION_NUM = 1;//版本号
    public static String PUSH_NODE_NAME = PropertyUtil.getProperty("PUSH_NODE_NAME");

    //#==========================消息转发服务器端相关配置=============================
    public static int PUSH_MSG_MAX_CONTENT_LEN = PropertyUtil.getPropertyInt("PUSH_MSG_MAX_CONTENT_LEN");
    public static int PUSH_LISTENER_PORT = PropertyUtil.getPropertyInt("PUSH_LISTENER_PORT");
    public static int PUSH_LISTENER_SOCKET_TIMEOUT = PropertyUtil.getPropertyInt("PUSH_LISTENER_SOCKET_TIMEOUT");
    public static int PUSH_LISTENER_MIN_THREAD = PropertyUtil.getPropertyInt("PUSH_LISTENER_MIN_THREAD");
    public static int PUSH_LISTENER_MAX_THREAD = PropertyUtil.getPropertyInt("PUSH_LISTENER_MAX_THREAD");

    //#==========================机顶盒端相关配置=============================
    public static String ACK_HEARTBEAT_POLICY = PropertyUtil.getProperty("ACK_HEARTBEAT_POLICY");
    public static int CLIENT_UDP_PORT = PropertyUtil.getPropertyInt("CLIENT_UDP_PORT");
    public static int CLIENT_UDP_BUFFER_RECEIVE = PropertyUtil.getPropertyInt("CLIENT_UDP_BUFFER_RECEIVE");
    public static int CLIENT_UDP_BUFFER_SEND = PropertyUtil.getPropertyInt("CLIENT_UDP_BUFFER_SEND");
    public static int CLIENT_UDP_WORKER_THREAD = PropertyUtil.getPropertyInt("CLIENT_UDP_WORKER_THREAD");
    public static int CLEANER_DEFAULT_EXPIRED_SECONDS = PropertyUtil.getPropertyInt("CLEANER_DEFAULT_EXPIRED_SECONDS");


    //#==========================redis相关配置=============================
    public static String REDIS_HOST = PropertyUtil.getProperty("REDIS_HOST");
    public static String REDIS_PASS = PropertyUtil.getProperty("REDIS_PASS");
    public static int REDIS_PORT = PropertyUtil.getPropertyInt("REDIS_PORT");
    public static int REDIS_MACHINE_DATABASE = PropertyUtil.getPropertyInt("REDIS_MACHINE_DATABASE");
    public static int REDIS_SERVER_DATABASE = PropertyUtil.getPropertyInt("REDIS_SERVER_DATABASE");
    public static String REDIS_TYPE = PropertyUtil.getProperty("REDIS_TYPE");
}
