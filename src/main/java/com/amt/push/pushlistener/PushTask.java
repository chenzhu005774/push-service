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
package com.amt.push.pushlistener;

import com.amt.push.beans.ClientStatMachine;
import com.amt.push.beans.PushMessage;
import com.amt.push.consts.Constant;
import com.amt.push.handler.NodeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * 　　* @Description: 消息处理任务，读取消息，处理读取出来的缓冲区数据
 * 　　* @author ckx
 * 　　* @date 2018/9/28 18:14
 */
public class PushTask implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(PushTask.class);

    private NIOPushListener listener;
    private SocketChannel channel;
    private SelectionKey key;
    private long lastActive;
    private boolean isCancel = false;

    private boolean writePending = false;
    private int maxContentLength;
    private byte[] bufferArray;
    private ByteBuffer buffer;

    public PushTask(NIOPushListener listener, SocketChannel channel) {
        this.listener = listener;
        this.channel = channel;
        maxContentLength = Constant.PUSH_MSG_MAX_CONTENT_LEN;
        bufferArray = new byte[Constant.PUSH_MSG_HEADER_LEN + maxContentLength];
        buffer = ByteBuffer.wrap(bufferArray);
        buffer.limit(Constant.PUSH_MSG_HEADER_LEN);
        lastActive = System.currentTimeMillis();
    }

    public void setKey(SelectionKey key) {
        this.key = key;
    }

    private void cancelKey(final SelectionKey key) {

        Runnable r = new Runnable() {
            public void run() {
                listener.cancelKey(key);
            }
        };
        listener.addEvent(r);
    }

    private void registerForWrite(final SelectionKey key, final boolean needWrite) {
        if (key == null || key.isValid() == false) {
            return;
        }

        if (needWrite == true) {
            if ((key.interestOps() & SelectionKey.OP_WRITE) > 0) {
                return;
            }
        } else {
            if ((key.interestOps() & SelectionKey.OP_WRITE) == 0) {
                return;
            }
        }

        Runnable r = new Runnable() {
            public void run() {
                if (key == null || !key.isValid()) {
                    return;
                }
                key.selector().wakeup();
                if (needWrite == true) {
                    key.interestOps(key.interestOps() & (~SelectionKey.OP_READ) | SelectionKey.OP_WRITE);
                } else {
                    key.interestOps(key.interestOps() & (~SelectionKey.OP_WRITE) | SelectionKey.OP_READ);
                }
            }
        };
        listener.addEvent(r);//添加线程节点事件
        try {
            key.selector().wakeup();
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        }
    }

    @Override
    public synchronized void run() {
        if (listener == null || channel == null) {
            return;
        }

        if (key == null) {
            return;
        }
        if (isCancel) {
            return;
        }
        try {
            if (!writePending) {

                if (key.isReadable()) {
                    readReq();
                } else {
                    // do nothing
                }
            } else {//has package

                // try send pkg and place hasPkg=false
                //
                //register write ops if not enough buffer
                //if(key.isWritable()){
                writeRes();
                //}
            }
        } catch (Exception e) {
            cancelKey(key);
            isCancel = true;
        } catch (Throwable t) {
            cancelKey(key);
            isCancel = true;
        }

        key = null;

    }

    /**
     * 　　* @Description: 读取消息
     * 　　* @author ckx
     * 　　* @date 2018/9/27 17:59
     */
    private void readReq() throws Exception {
        if (this.writePending) {
            return;
        }

        if (channel.read(buffer) < 0) {
            throw new Exception("end of stream");
        }
        if (!this.calcWritePending()) {
            return;
        } else {
            byte res = 0;
            try {
                processReq();
            } catch (Exception e) {
                res = 1;
            } catch (Throwable t) {
                res = -1;
            }

            buffer.clear();
            buffer.limit(1);
            buffer.put(res);
            buffer.flip();

            registerForWrite(key, true);

        }


        lastActive = System.currentTimeMillis();
    }

    private void writeRes() throws Exception {
        if (buffer.hasRemaining()) {
            channel.write(buffer);
        } else {
            buffer.clear();
            buffer.limit(Constant.PUSH_MSG_HEADER_LEN);
            this.writePending = false;
            registerForWrite(key, false);
        }
        lastActive = System.currentTimeMillis();
    }

    public long getLastActive() {
        return lastActive;
    }

    public boolean isWritePending() {
        return writePending;
    }

    /**
     * 　　* @Description: 校验缓冲区是否能使用
     * 　　* @author ckx
     * 　　* @date 2018/9/27 18:01
     */
    private synchronized boolean calcWritePending() throws Exception {
        if (!this.writePending) {
            if (buffer.position() < Constant.PUSH_MSG_HEADER_LEN) {
                this.writePending = false;
            } else {
                int bodyLen = (int) ByteBuffer.wrap(bufferArray, Constant.PUSH_MSG_HEADER_LEN - 2, 2).getChar();
                if (bodyLen > maxContentLength) {
                    throw new IllegalArgumentException("content length " + bodyLen + " larger than max " + maxContentLength);
                }
                if (bodyLen == 0) {
                    this.writePending = true;
                } else {
                    if (buffer.limit() != Constant.PUSH_MSG_HEADER_LEN + bodyLen) {
                        buffer.limit(Constant.PUSH_MSG_HEADER_LEN + bodyLen);
                    } else {
                        if (buffer.position() == Constant.PUSH_MSG_HEADER_LEN + bodyLen) {
                            this.writePending = true;
                        }
                    }
                }
            }
        } else {//this.writePending == true
            if (buffer.hasRemaining()) {//缓冲区是否还有可用数据
                this.writePending = true;
            } else {
                this.writePending = false;
            }
        }

        return this.writePending;
    }

    /**
     * 　　* @Description: 处理读取出来的缓冲区数据，处理手机端发送过来的数据
     *                     投屏iptv账号获取机顶盒信息，如果没有创建新的机顶盒信息
     * 　　* @author ckx
     * 　　* @date 2018/9/27 18:02
     */
    private void processReq() throws Exception {
        //check and put data into nodeStat
        buffer.flip();
        byte[] data = new byte[buffer.limit()];
        System.arraycopy(bufferArray, 0, data, 0, buffer.limit());
        buffer.clear();
        //this.writePending = false;//important
        PushMessage pm = new PushMessage(data);


        //初始化NodeStatus，终端机信息
        NodeStatus nodeStat = NodeStatus.getInstance();
        String uuid = pm.getUuidHexString();


        /**
         * * @Description: 如果该iptv账号在注册中心没有查询到对应的机顶盒和投屏服务器信息，则不处理。
         * *               2018/10/15 16:59 update 之前是在注册中心 没有终端机信息，创建新的终端机信息，添加至NodeStatus，重新注册
         * * @author ckx
         */
        ClientStatMachine csm = nodeStat.getClientStat(uuid);//获取终端状态机信息
        if (csm == null) {
            logger.info("iptv账号:" + uuid + " 注册中心没有机顶盒与投屏服务器的绑定信息");
//			csm = ClientStatMachine.newByPushReq(pm);
//			if(csm == null){
//				throw new Exception("can not new state machine");
//			}
//			nodeStat.putClientStat(uuid, csm);
        } else {//将该缓冲区消息放入Sender队列中
            logger.info("iptv账号:" + uuid + "to 机顶盒ip:"+csm.getLastAddr().toString());
            try {
                csm.onPushMessage(pm);
            } catch (Exception e) {
            }
        }

    }

}
