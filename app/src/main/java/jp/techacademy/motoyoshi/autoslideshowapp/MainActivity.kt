package jp.techacademy.motoyoshi.autoslideshowapp

import android.app.AlertDialog
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import jp.techacademy.motoyoshi.autoslideshowapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val PERMISSIONS_REQUEST_CODE = 100

    // APIレベルによって許可が必要なパーミッションを切り替える
    private val readImagesPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) android.Manifest.permission.READ_MEDIA_IMAGES
        else android.Manifest.permission.READ_EXTERNAL_STORAGE

    //URIをリストとして宣言
    private var imageUris: MutableList<Uri> = mutableListOf()
    private var currentImageIndex = 0 //初期値0

    //スライドショーの状態を宣言
    private var isSlideshowActive = false //初期はスライドショーは実行しない
    private val handler = Handler(Looper.getMainLooper())

    //Runnableのrunメソッドの設定
    private val changeImageRunnable = object : Runnable {
        override fun run() {
            if (isSlideshowActive) {
                moveToNextImage()
                handler.postDelayed(this, 2000) // 2秒後に再度実行
            }
        }
    }

    //=================================================
    // 起動時の設定
    //=================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // パーミッションの許可状態を確認する
        if (checkSelfPermission(readImagesPermission) == PackageManager.PERMISSION_GRANTED) {
            // 許可されている
            getContentsInfo()
        } else {
            // 許可されていないので許可ダイアログを表示する
            requestPermissions(
                arrayOf(readImagesPermission),
                PERMISSIONS_REQUEST_CODE
            )
        }

        //各ボタンへの処理の割り振り
        binding.button1.setOnClickListener {
            moveToPreviousImage()//戻る処理
        }

        binding.button2.setOnClickListener {
            toggleSlideshow() // スライドショーの処理
        }

        binding.button3.setOnClickListener {
            moveToNextImage()//進む処理
        }
    }



    //=================================================
    //戻る、進むの処理
    //=================================================

    //==============
    //戻る処理
    //==============
    private fun moveToPreviousImage() {
        if (imageUris.isNotEmpty()) {
            // インデックスを更新（リストの最初に達したら最後に戻る）
            currentImageIndex = if (currentImageIndex - 1 < 0) imageUris.size - 1 else currentImageIndex - 1
            // 画像を更新
            updateImage()
        }
    }
    //==============
    //進む処理
    //==============
    private fun moveToNextImage() {
        if (imageUris.isNotEmpty()) {
            // インデックスを更新（リストの最後に達したら最初に戻る）
            currentImageIndex = (currentImageIndex + 1) % imageUris.size
            // 画像を更新
            updateImage()
        }
    }



    //=================================================
    // スライドショーの処理
    //=================================================
    //==============
    // ボタン操作
    //==============
    private fun toggleSlideshow() {
        // 条件はボタンの表示内容
        if (binding.button2.text == "停止") {
            stopSlideshow() //スライドショーをとめる
            binding.button2.text = "再生"//表示を「再生」に変更
            //進,戻るを操作可能に
            binding.button1.isEnabled = true
            binding.button3.isEnabled = true
        } else {
            startSlideshow() //スライドショーをはじめる
            binding.button2.text = "停止"//表示を「停止」に変更
            //進,戻るを操作不可に
            binding.button1.isEnabled = false
            binding.button3.isEnabled = false
        }
    }

    //==============
    // スライドショーをはじめる
    //==============
    private fun startSlideshow() {
        isSlideshowActive = true
        handler.postDelayed(changeImageRunnable,2000)
    }

    //==============
    // スライドショーをとめる
    //==============
    private fun stopSlideshow() {
        isSlideshowActive = false
        handler.removeCallbacks(changeImageRunnable)
    }



    //=================================================
    // パーミッション
    //=================================================
    //==============
    // パーミッション許可表示
    //==============
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getContentsInfo()
                }else{
                    showAlertDialog()
                }
        }
    }

    //==============
    // パーミッションが不許可の場合のダイアログ設定
    //==============
    private fun showAlertDialog() {
        // AlertDialog.Builderクラスを使ってAlertDialogの準備をする
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("パーミッションが必要")
        alertDialogBuilder.setMessage("許可しないと画像を取得できません。このアプリが本体に保存している写真にアクセスしてもよろしいですか？")

        alertDialogBuilder.setNegativeButton("Cancel") { dialog, which ->
            //否定ボタン時の処理=>なし
        }

        // 肯定ボタンに表示される文字列、押したときのリスナーを設定する
        alertDialogBuilder.setPositiveButton("OK"){dialog, which ->
            // 許可されていないので許可ダイアログを表示する
            requestPermissions(
                arrayOf(readImagesPermission),
                PERMISSIONS_REQUEST_CODE
            )
        }
        // AlertDialogを作成して表示する
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }


    //=================================================
    // URI情報の取得
    //=================================================
    private fun getContentsInfo() {
        // 画像の情報を取得する
        val resolver = contentResolver
        val cursor = resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // データの種類
            null, // 項目（null = 全項目）
            null, // フィルタ条件（null = フィルタなし）
            null, // フィルタ用パラメータ
            null // ソート (nullソートなし）
        )

        imageUris.clear()
        while (cursor?.moveToNext() == true) {
            val fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
            val id = cursor.getLong(fieldIndex)
            val imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            imageUris.add(imageUri)
        }
        cursor?.close()

        if (imageUris.isNotEmpty()) {
            binding.imageView.setImageURI(imageUris[0])
        }
    }



    //=================================================
    //現在取得しているURI情報に基づいた画面を表示させる
    //=================================================
    private fun updateImage() {
        binding.imageView.setImageURI(imageUris[currentImageIndex])
    }
}
