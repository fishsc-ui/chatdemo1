package com.test.demo

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.ArrayMap
import android.util.Log
import android.view.ContextThemeWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import dalvik.system.DexClassLoader
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method


class MainActivity : ComponentActivity() {
    var TAG : String = "mahiro"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContent {
//            DemoTheme {
//                // A surface container using the 'background' color from the theme
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    Greeting("Android")
//                }
//            }
//        }

//        setContent{
//            LoginScreen()
//        }
        setContent{
            Text("")
        }

        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        requestPermissions(permissions, 0)

        var allow = true
        var iterator = permissions.iterator()
        while (iterator.hasNext())
        {
            if (checkSelfPermission(iterator.next()) == PackageManager.PERMISSION_DENIED)
            {
                allow = false
            }
        }
//
//        var mathService: IMath? = null
//        val connection: ServiceConnection = object : ServiceConnection {
//            override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
//                mathService = IMath.Stub.asInterface(iBinder)
//
//                // 调用服务中的方法
//                val result = mathService!!.add(1, 2)
//                Log.d(TAG, "result: $result")
//            }
//
//            override fun onServiceDisconnected(componentName: ComponentName) {
//                mathService = null
//            }
//        }
//        // 绑定服务
//        val intent = Intent(this, MathService::class.java)
//        bindService(intent, connection, BIND_AUTO_CREATE)
//        return



        if (allow && Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT){
            startDexActivityWithClassLoader(
                this,
                copyFile(
                    this,
                    listOf(
                        "classes",
                        "classes2",
                        "classes3")
                )
            )
        }
    }

    fun repleaceResource(context: Context)
    {
        var mDexPath = "/data/data/com.test.demo/files/app-debug.apk"
        try {
            var assets: AssetManager = AssetManager::class.java.newInstance()
            val addAssetPath = assets.javaClass.getMethod(
                "addAssetPath",
                String::class.java
            )
            var res = addAssetPath.invoke(assets, mDexPath)
            Log.d("mahiro", "res:" + res)

            val superRes = super.getResources()
            var mResources = Resources(
                assets, superRes.displayMetrics,
                superRes.configuration
            )

            Log.d("mahiro", "CLASS:" + context.javaClass)
            var field = ContextThemeWrapper::class.java.getDeclaredField("mResources");
            field.setAccessible(true);
            field.set(context, mResources);
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

    }

    fun copyFile(context: Context, dexFiles: List<String>): ArrayList<String> {
        var buffer = ByteArray(1024)
        var dexList: ArrayList<String> = ArrayList()

        try {
            dexFiles.forEach { dexName ->
                var istream = context.assets.open(dexName)
                var dexFile = File(filesDir, dexName)
                var ostream = FileOutputStream(dexFile)
                dexList.add(dexFile.absolutePath)

                var readlen : Int = 0
                while (true)
                {
                    readlen = istream.read(buffer)
                    if (readlen == -1) break

                    ostream.write(buffer, 0, readlen)
                }

                istream.close()
                ostream.close()
            }
        } catch (e: Exception)
        {
            Log.e(TAG, "$e")
        }


        return dexList
    }

    fun replaceClassLoader(classLoader: ClassLoader)
    {
        try{
            var class_ActivityThread: Class<*> = Class.forName("android.app.ActivityThread")
            var method_currentActivityThread: Method = class_ActivityThread.getDeclaredMethod("currentActivityThread")
            var object_sCurrentActivityThread = method_currentActivityThread.invoke(null)
            Log.d(TAG, "$object_sCurrentActivityThread")

//        final ArrayMap<String, WeakReference<LoadedApk>> mPackages = new ArrayMap<>();
            var field_mPackages: Field =  class_ActivityThread.getDeclaredField("mPackages")
            field_mPackages.isAccessible = true
            var object_mPackages: ArrayMap<*, *> = field_mPackages.get(object_sCurrentActivityThread) as ArrayMap<*, *>
            Log.d(TAG, "$object_mPackages")

            // 获取 LoadedApk 实例对象
            var weakReference = object_mPackages.get(packageName) as WeakReference<*>
            val object_LoadedApk = weakReference.get()
            Log.d(TAG, "$object_LoadedApk")

            var class_LoadedApk = Class.forName("android.app.LoadedApk")
            var field_LoadedApk = class_LoadedApk.getDeclaredField("mClassLoader")
            field_LoadedApk.isAccessible = true
            Log.d(TAG, "ClassLoader old ${field_LoadedApk.get(object_LoadedApk)}")
            field_LoadedApk.set(object_LoadedApk, classLoader)
            Log.d(TAG, "ClassLoader new ${field_LoadedApk.get(object_LoadedApk)}")
        }catch (e: Exception)
        {
            Log.e(TAG, "$e")
        }
    }

    /*
    @param Context
    @param List<String>
    * */
    fun startDexActivityWithClassLoader(context: Context, dexPaths: List<String>)
    {
        var mDexPath:String = dexPaths.joinToString(separator = ":")
        repleaceResource(this)
        // 存放优化后的 dex 文件目录
        var optPath = context.getDir("opt_dex", Context.MODE_PRIVATE)
        // 依赖库目录，用于存放 so 文件
        var libPath = context.getDir("lib_path", Context.MODE_PRIVATE)

        // 实例化自定义的 类加载器
        var dexClassLoader = DexClassLoader(
            mDexPath,
            optPath.absolutePath,
            libPath.absolutePath,
            context.classLoader
        )
        Log.d(TAG, "$dexClassLoader")
        replaceClassLoader(dexClassLoader)

        var cls: Class<*>? = null
        try {
            // 从dex中寻找目标activity类
            cls = dexClassLoader.loadClass("com.test.example.MainActivity2")
        } catch (e: ClassNotFoundException)
        {
            Log.e(TAG, "$e")
        }

        // 启动目标Activity
        if (cls != null)
        {
            try {
                context.startActivity(Intent(context, cls))
            } catch (e: ActivityNotFoundException)
            {
                Log.e(TAG, "$e")
            }
        }

    }
}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun LoginScreen() {
//    val context = LocalContext.current
//    var username by remember { mutableStateOf("") }
//    var showDialog by remember { mutableStateOf(false) }
//    var dialogTitle by remember { mutableStateOf("") }
//    var dialogMessage by remember { mutableStateOf("") }
//
//    Column(
//        modifier = Modifier
//            .padding(16.dp)
//            .fillMaxWidth()
//    ) {
//        OutlinedTextField(
//            value = username,
//            onValueChange = { username = it },
//            label = { Text("Username") },
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(bottom = 16.dp)
//        )
//        Button(
//            onClick = {
//                if (username == "zack") {
//                    dialogTitle = "Password Correct"
//                    dialogMessage = "Welcome, zack!"
//                } else {
//                    dialogTitle = "Password Incorrect"
//                    dialogMessage = "Please try again"
//                }
//                showDialog = true
//            },
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Text("Login")
//        }
//    }
//
//    if (showDialog)
//    {
//        AlertDialog(
//            onDismissRequest = { showDialog = false },
//            title = { Text(dialogTitle) },
//            text = { Text(text = dialogMessage) },
//            confirmButton = {
//                Button(
//                    onClick = { showDialog = false },
//                    modifier = Modifier.fillMaxWidth()
//                ){
//                    Text("ok")
//                }
//            }
//
//        )
//    }
//
//}
