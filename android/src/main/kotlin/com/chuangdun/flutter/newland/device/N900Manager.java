package com.chuangdun.flutter.newland.device;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.newland.me.ConnUtils;
import com.newland.me.DeviceManager;
import com.newland.mtype.ConnectionCloseEvent;
import com.newland.mtype.Device;
import com.newland.mtype.ExModuleType;
import com.newland.mtype.ModuleType;
import com.newland.mtype.event.DeviceEventListener;
import com.newland.mtype.module.common.cardreader.CardReader;
import com.newland.mtype.module.common.emv.EmvModule;
import com.newland.mtype.module.common.iccard.ICCardModule;
import com.newland.mtype.module.common.light.IndicatorLight;
import com.newland.mtype.module.common.pin.K21Pininput;
import com.newland.mtype.module.common.printer.Printer;
import com.newland.mtype.module.common.rfcard.RFCardModule;
import com.newland.mtype.module.common.scanner.BarcodeScanner;
import com.newland.mtype.module.common.scanner.BarcodeScannerManager;
import com.newland.mtype.module.common.security.SecurityModule;
import com.newland.mtype.module.common.serialport.SerialModule;
import com.newland.mtype.module.common.storage.Storage;
import com.newland.mtype.module.common.swiper.K21Swiper;
import com.newland.mtypex.nseries.NSConnV100ConnParams;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * N900设备管理器
 *
 * @author nick  created on 2017/9/21.
 */
public class N900Manager extends AbstractManager {
    private static final String TAG = "N900Manager";
    private final Context context;
    private final Handler mainHandler;
    private final ConnectionCallback callback;
    private final DeviceEventListener<ConnectionCloseEvent> closeListener = new DeviceEventListener<ConnectionCloseEvent>() {
        boolean isClosedHasError = false;

        @Override
        public void onEvent(ConnectionCloseEvent closeEvent, Handler handler) {
            if (closeEvent.isSuccess()) {
                Log.i(TAG, "onEvent: 设备连接正常关闭.");
                isClosedHasError = false;
            }
            if (closeEvent.isFailed()) {
                Log.w(TAG, "onEvent: 设备连接异常关闭.");
                isClosedHasError = true;

            }
            mainHandler.post(() -> {
                if (isClosedHasError) {
                    callback.onError("设备异常断开!");
                } else {
                    callback.onDisconnected();
                }
            });
        }

        @Override
        public Handler getUIHandler() {
            return mainHandler;
        }
    };
    private DeviceManager mDeviceManager;
    private ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("n900-pool-%d").build();
    private ExecutorService singleThreadPool = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(1024), factory);

    public N900Manager(Context context, ConnectionCallback callback) {
        this.context = context;
        this.callback = callback;
        this.mainHandler = new Handler();
    }

    @Override
    public void connect() {
        callback.onConnecting();
        mDeviceManager = ConnUtils.getDeviceManager();
        NSConnV100ConnParams params = new NSConnV100ConnParams();
        mDeviceManager.init(context, AvailableDrivers.K21_DRIVER, params, closeListener);
        Log.i(TAG, "connect: 连接管理器初始化成功.");
        singleThreadPool.execute(() -> {
            try {
                mDeviceManager.connect();
                mainHandler.post(() -> {
                    Log.i(TAG, "connect: 设备已连接.");
                    callback.onConnected();
                });
            } catch (Exception e) {
                Log.e(TAG, "连接异常,请检查设备或重新连接.", e);
                mainHandler.post(() -> callback.onError("连接异常,请检查设备或重新连接."));
            }
        });
    }

    @Override
    public void disconnect() {
        singleThreadPool.execute(() -> {
            try {
                Preconditions.checkNotNull(mDeviceManager);
                mDeviceManager.disconnect();
                mDeviceManager = null;
                Log.i(TAG, "disconnect: 设备已正常断开.");
                mainHandler.post(callback::onDisconnected);
            } catch (Exception e) {
                Log.e(TAG, "disconnect: 设备断开异常.", e);
            }
        });
        singleThreadPool.shutdown();
    }

    @Override
    public Device getCurrentDevice() {
        return mDeviceManager.getDevice();
    }

    @Override
    public boolean isAlive() {
        return (mDeviceManager != null && mDeviceManager.getDevice().isAlive());
    }

    @Override
    public CardReader getCardReaderModule() {
        return (CardReader) mDeviceManager.getDevice().getStandardModule(ModuleType.COMMON_CARDREADER);
    }

    @Override
    public EmvModule getEmvModule() {
        return (EmvModule) mDeviceManager.getDevice().getExModule("EMV_INNERLEVEL2");
    }

    @Override
    public ICCardModule getICCardModule() {
        return (ICCardModule) mDeviceManager.getDevice().getStandardModule(ModuleType.COMMON_ICCARDREADER);
    }

    @Override
    public IndicatorLight getIndicatorModule() {
        return (IndicatorLight) mDeviceManager.getDevice().getStandardModule(ModuleType.COMMON_INDICATOR_LIGHT);
    }

    @Override
    public K21Pininput getK21PinInputModule() {
        return (K21Pininput) mDeviceManager.getDevice().getStandardModule(ModuleType.COMMON_PININPUT);
    }

    @Override
    public Printer getPrinterModule() {
        Printer printer = (Printer) mDeviceManager.getDevice().getStandardModule(ModuleType.COMMON_PRINTER);
        printer.init();
        return printer;
    }

    @Override
    public RFCardModule getRFCardModule() {
        return (RFCardModule) mDeviceManager.getDevice().getStandardModule(ModuleType.COMMON_RFCARDREADER);
    }

    @Override
    public BarcodeScanner getBarcodeScanner() {
        BarcodeScannerManager barcodeScannerManager = (BarcodeScannerManager) mDeviceManager.getDevice().getStandardModule(ModuleType.COMMON_BARCODESCANNER);
        return barcodeScannerManager.getDefault();
    }

    @Override
    public SecurityModule getSecurityModule() {
        return (SecurityModule) mDeviceManager.getDevice().getStandardModule(ModuleType.COMMON_SECURITY);
    }

    @Override
    public Storage getStorageModule() {
        return (Storage) mDeviceManager.getDevice().getStandardModule(ModuleType.COMMON_STORAGE);
    }

    @Override
    public K21Swiper getK21SwiperModule() {
        return (K21Swiper) mDeviceManager.getDevice().getStandardModule(ModuleType.COMMON_SWIPER);
    }

    @Override
    public SerialModule getUsbSerialModule() {
        return (SerialModule) mDeviceManager.getDevice().getExModule(ExModuleType.USBSERIAL);
    }
}
