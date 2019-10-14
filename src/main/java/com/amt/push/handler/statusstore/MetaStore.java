package com.amt.push.handler.statusstore;

import com.amt.push.beans.ClientMessage;
import com.amt.push.beans.ClientStatMachine;

/**
 *
 */
public interface MetaStore {
    /**
     * 添加元素.
     *
     * @param k 键
     * @param v 值
     */
    void put(String k, ClientStatMachine v);

    /**
     * 获取元素.
     *
     * @param k 键
     * @return 值
     */
    ClientStatMachine get(String k);

    /**
     * 获取元素.
     *
     * @param k 键
     * @return 值
     */
    ClientStatMachine get(String k, ClientMessage m);

    /**
     * 获取心跳的所有数量
     *
     * @return 数量
     */
    long size();

    /**
     * 获取当前节点心跳的所有数量
     *
     * @return 数量
     */
    long allSize();

    /**
     * 移除.
     *
     * @param k 键
     * @return 状态
     */
    boolean remove(String k);

    /**
     * 更新节点.
     *
     * @param uuid 客户端编号
     * @param csm  心跳数据.
     */
    void upToDate(String uuid, ClientStatMachine csm);

}
