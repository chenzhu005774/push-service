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

package com.amt.push.handler;

import com.amt.push.beans.ClientMessage;
import com.amt.push.beans.ClientStatMachine;
import com.amt.push.handler.statusstore.MetaStore;
import com.amt.push.handler.statusstore.impl.RedisMetaStore;

/**
 * 　　* @Description: 终端机节点处理
 * 　　* @author ckx
 * 　　* @date 2018/9/28 15:49
 */
public class NodeStatus {

    private static NodeStatus global;
    private static MetaStore nodeStat;


    /**
     * 　　* @Description: 根据配置选择不同的存储方式
     * 　　* @author ckx
     * 　　* @date 2018/9/28 18:03
     */
    private NodeStatus() {
        nodeStat = RedisMetaStore.create();//redis
    }

    public static NodeStatus getInstance() {
        if (global == null) {
            synchronized (NodeStatus.class) {
                if (global == null) {//need to check again!!
                    global = new NodeStatus();
//                    System.out.println("try load node stat file...");
//                    global.tryLoadFile();
                }
            }
        }
        return global;
    }


    public ClientStatMachine getClientStat(String key) {
        return nodeStat.get(key);
    }


    public void putClientStat(String key, ClientStatMachine value) {
        nodeStat.put(key, value);
    }


    public void upToDate(String uuid, ClientStatMachine csm) {
        nodeStat.upToDate(uuid, csm);
    }

    public ClientStatMachine getClientStat(String uuid, ClientMessage m) {
        return nodeStat.get(uuid, m);
    }
}
