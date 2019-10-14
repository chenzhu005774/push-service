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
package com.amt.push.process;

import com.amt.push.beans.ClientMessage;
import com.amt.push.beans.ClientStatMachine;
import com.amt.push.handler.NodeStatus;
import com.amt.push.udpconnector.UdpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 　　* @Description: 消息处理者，处理终端机发送的消息
 * 　　* @author ckx
 * 　　* @date 2018/9/28 18:12
 */
public class Messenger implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(Messenger.class);

    private UdpConnector connector;
    private NodeStatus nodeStat;
    private Thread hostThread;

    boolean started = false;
    boolean stoped = false;

    public Messenger(UdpConnector connector, NodeStatus nodeStat) {
        this.connector = connector;
        this.nodeStat = nodeStat;
    }

    @Override
    public void run() {
        this.started = true;

        while (!stoped) {
            try {
                procMessage();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
            }
        }

    }

    public void stop() {
        this.stoped = true;
    }

    /**
     * 　　* @Description: 读取终端机送的心跳消息
     * 　　* @author ckx
     * 　　* @date 2018/9/28 11:11
     */
    private void procMessage() throws Exception {
        //读取终端机客户端发送消息
        ClientMessage m = this.obtainMessage();
        if (m == null) {
            try {
                Thread.sleep(5);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            return;
        }
        //处理终端机发送的消息，如果是心跳包，不做回应
        this.deliverMessage(m);
    }

    /**
     * 　　* @Description: 处理终端机发送的消息，如果是心跳包，不做回应,更新节点最后时间
     * * @update 2019/1/8 删除逻辑判断 直接执行更新插入操作
     * 　　* @author ckx
     * 　　* @date 2018/9/28 17:05
     */
    private void deliverMessage(ClientMessage m) throws Exception {
        String uuid = m.getUuidHexString();
        ClientStatMachine csm = ClientStatMachine.newByClientTick(m);
//        logger.info("准备更新redis： from:" + csm.getLastAddr().toString() + " iptvAccount:" + m.getUuidHexString());
        //保存终端机信息、发送的信息（此处为心跳包）
        nodeStat.upToDate(uuid, csm);
        logger.info("更新redis成功： from:" + csm.getLastAddr().toString() + " iptvAccount:" + m.getUuidHexString());


        /**
         * * @Description: 正式环境不需要回应
         * * @author ckx
         * * @date 2018/12/25 16:28
         */
//        ArrayList<ServerMessage> smList = csm.onClientMessage(m);
//        if (smList == null) {
//            return;
//        }
//        for (int i = 0; i < smList.size(); i++) {
//            ServerMessage sm = smList.get(i);
//            if (sm.getSocketAddress() == null) continue;
//            this.connector.send(sm);
//        }

    }

    private ClientMessage obtainMessage() throws Exception {
        return connector.receive();
    }

    public void setHostThread(Thread t) {
        this.hostThread = t;
    }

    public Thread getHostThread() {
        return this.hostThread;
    }

}
