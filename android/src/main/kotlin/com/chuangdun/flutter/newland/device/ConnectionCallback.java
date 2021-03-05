package com.chuangdun.flutter.newland.device;

/**
 * 设备连接管理回调
 *
 * @author nick created on 2017/9/20
 */

public interface ConnectionCallback {
    /**
     * 开始连接时调用
     */
    void onConnecting();

    /**
     * 连接完成时调用
     */
    void onConnected();

    /**
     * 断开连接时调用
     */
    void onDisconnected();

    /**
     * 发生错误时调用
     *
     * @param error 错误信息
     */
    void onError(String error);
}
