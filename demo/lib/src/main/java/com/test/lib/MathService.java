package com.test.lib;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;
import com.test.demo.IMath;

public class MathService extends Service {
    private final IBinder binder = new MathBinder();

    private class MathBinder extends IMath.Stub
    {
        @Override
        public int add(int a, int b) throws RemoteException {
            return a + b;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
