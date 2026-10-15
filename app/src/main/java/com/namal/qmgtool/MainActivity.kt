package com.namal.qmgtool

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MainActivity : Activity() {

    private val BG = Color.parseColor("#0F1020")
    private val CARD = Color.parseColor("#1A1B30")
    private val CARD2 = Color.parseColor("#24264A")
    private val STROKE = Color.parseColor("#33365E")
    private val ACCENT = Color.parseColor("#7C5CFF")
    private val ACCENT2 = Color.parseColor("#22D3EE")
    private val MUTED = Color.parseColor("#9AA0B8")
    private val GOOD = Color.parseColor("#4ADE80")

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private var videoUri: Uri? = null

    private lateinit var preview: ImageView
    private lateinit var info: TextView
    private lateinit var status: TextView
    private lateinit var goBtn: Button
    private lateinit var bar: ProgressBar
    private lateinit var fpsIn: EditText
    private lateinit var wIn: EditText
    private lateinit var hIn: EditText
    private lateinit var secIn: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BG
        window.navigationBarColor = BG

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(BG)
        root.setPadding(dp(18), dp(28), dp(18), dp(28))

        root.addView(text("QMG Frames", 30f, Color.WHITE, true))
        val sub = text("Video එකක් boot animation frames බවට හරවමු", 14f, MUTED, false)
        sub.setPadding(0, dp(4), 0, 0)
        root.addView(sub)

        // Card 1: video
        val c1 = card()
        c1.addView(text("1 · VIDEO", 12f, ACCENT2, true))

        preview = ImageView(this)
        preview.scaleType = ImageView.ScaleType.CENTER_CROP
        preview.background = rounded(CARD2, 14, STROKE)
        preview.clipToOutline = true
        val plp = LinearLayout.LayoutParams(MATCH, dp(200))
        plp.topMargin = dp(10)
        preview.layoutParams = plp
        c1.addView(preview)

        info = text("තවම video එකක් තෝරලා නෑ", 13f, MUTED, false)
        info.setPadding(0, dp(10), 0, dp(10))
        c1.addView(info)

        val pickBtn = button("Video තෝරන්න", false)
        pickBtn.setOnClickListener { pickVideo() }
        c1.addView(pickBtn, LinearLayout.LayoutParams(MATCH, dp(48)))
        root.addView(c1)

        // Card 2: settings
        val c2 = card()
        c2.addView(text("2 · SETTINGS", 12f, ACCENT2, true))

        fpsIn = numField("30")
        secIn = numField("4")
        wIn = numField("720")
        hIn = numField("1600")

        val presets = LinearLayout(this)
        presets.orientation = LinearLayout.HORIZONTAL
        addPreset(presets, "720×1600", 720, 1600)
        addPreset(presets, "1080×2340", 1080, 2340)
        addPreset(presets, "1080×2408", 1080, 2408)
        val prlp = LinearLayout.LayoutParams(MATCH, WRAP)
        prlp.topMargin = dp(12)
        presets.layoutParams = prlp
        c2.addView(presets)

        c2.addView(row(labeled("FPS", fpsIn), labeled("උපරිම තත්පර", secIn)))
        c2.addView(row(labeled("Width", wIn), labeled("Height", hIn)))
        root.addView(c2)

        // Card 3: action
        val c3 = card()
        c3.addView(text("3 · EXPORT", 12f, ACCENT2, true))

        goBtn = button("Frames හදන්න", true)
        goBtn.setOnClickListener { startExport() }
        val glp = LinearLayout.LayoutParams(MATCH, dp(52))
        glp.topMargin = dp(10)
        c3.addView(goBtn, glp)

        bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        bar.progressTintList = ColorStateList.valueOf(ACCENT)
        bar.max = 100
        bar.progress = 0
        val blp = LinearLayout.LayoutParams(MATCH, dp(8))
        blp.topMargin = dp(14)
        c3.addView(bar, blp)

        status = text("සූදානම්", 13f, MUTED, false)
        status.setPadding(0, dp(10), 0, 0)
        c3.addView(status)
        root.addView(c3)

        val note = text(
            "සටහන: QMG encoder එක තාම නෑ. මේකෙන් හැදෙන bootup001.png... zip එක " +
                "ඉස්සරහට encoder එකට හෝ Samsung conversion එකට පාවිච්චි කරන්න.",
            12f, MUTED, false
        )
        note.setPadding(dp(4), dp(16), dp(4), 0)
        root.addView(note)

        val sv = ScrollView(this)
        sv.setBackgroundColor(BG)
        sv.addView(root)
        setContentView(sv)
    }

    // ---------- UI helpers ----------

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int, stroke: Int = 0): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dp(radius).toFloat()
        if (stroke != 0) g.setStroke(dp(1), stroke)
        return g
    }

    private fun card(): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.background = rounded(CARD, 20, STROKE)
        c.setPadding(dp(16), dp(16), dp(16), dp(16))
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.topMargin = dp(16)
        c.layoutParams = lp
        return c
    }

    private fun text(s: String, sizeSp: Float, color: Int, bold: Boolean): TextView {
        val t = TextView(this)
        t.text = s
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        t.setTextColor(color)
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD)
        return t
    }

    private fun button(label: String, filled: Boolean): Button {
        val b = Button(this)
        b.text = label
        b.isAllCaps = false
        b.setTextColor(Color.WHITE)
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        b.background = if (filled) rounded(ACCENT, 14) else rounded(CARD2, 14, STROKE)
        b.stateListAnimator = null
        return b
    }

    private fun numField(def: String): EditText {
        val e = EditText(this)
        e.setText(def)
        e.inputType = InputType.TYPE_CLASS_NUMBER
        e.setTextColor(Color.WHITE)
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        e.background = rounded(CARD2, 12, STROKE)
        e.setPadding(dp(12), dp(10), dp(12), dp(10))
        e.gravity = Gravity.CENTER
        return e
    }

    private fun labeled(label: String, e: EditText): LinearLayout {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.addView(text(label, 12f, MUTED, false))
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.topMargin = dp(4)
        box.addView(e, lp)
        return box
    }

    private fun row(a: View, b: View): LinearLayout {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        val lp1 = LinearLayout.LayoutParams(0, WRAP, 1f)
        lp1.rightMargin = dp(6)
        val lp2 = LinearLayout.LayoutParams(0, WRAP, 1f)
        lp2.leftMargin = dp(6)
        r.addView(a, lp1)
        r.addView(b, lp2)
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.topMargin = dp(12)
        r.layoutParams = lp
        return r
    }

    private fun addPreset(parent: LinearLayout, label: String, w: Int, h: Int) {
        val b = button(label, false)
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        b.setOnClickListener {
            wIn.setText(w.toString())
            hIn.setText(h.toString())
        }
        val lp = LinearLayout.LayoutParams(0, dp(40), 1f)
        lp.rightMargin = dp(4)
        lp.leftMargin = dp(4)
        parent.addView(b, lp)
    }

    private fun setStatus(s: String, color: Int = MUTED) {
        runOnUiThread {
            status.text = s
            status.setTextColor(color)
        }
    }

    // ---------- Video picking ----------

    private fun pickVideo() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "video/*"
        startActivityForResult(i, 1)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            val u = data?.data
            if (u != null) {
                videoUri = u
                loadPreview(u)
            }
        }
    }

    private fun loadPreview(uri: Uri) {
        info.text = "පූරණය වෙනවා..."
        Thread {
            try {
                val m = MediaMetadataRetriever()
                m.setDataSource(this, uri)
                val d = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val vw = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH) ?: "?"
                val vh = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT) ?: "?"
                val bmp = m.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                m.release()
                val secs = String.format("%.1f", d / 1000.0)
                runOnUiThread {
                    preview.setImageBitmap(bmp)
                    info.text = vw + "×" + vh + "  ·  " + secs + " තත්පර"
                    info.setTextColor(Color.WHITE)
                }
            } catch (e: Exception) {
                runOnUiThread { info.text = "Preview error: " + e }
            }
        }.start()
    }

    // ---------- Export ----------

    private fun startExport() {
        val uri = videoUri
        if (uri == null) {
            setStatus("මුලින් video එකක් තෝරන්න", Color.parseColor("#FBBF24"))
            return
        }
        val fps = (fpsIn.text.toString().toIntOrNull() ?: 30).coerceIn(1, 60)
        val w = (wIn.text.toString().toIntOrNull() ?: 720).coerceIn(16, 4096)
        val h = (hIn.text.toString().toIntOrNull() ?: 1600).coerceIn(16, 4096)
        val secs = (secIn.text.toString().toIntOrNull() ?: 4).coerceIn(1, 30)

        goBtn.isEnabled = false
        goBtn.alpha = 0.5f
        bar.progress = 0

        Thread {
            var mmr: MediaMetadataRetriever? = null
            try {
                mmr = MediaMetadataRetriever()
                mmr.setDataSource(this, uri)
                val durMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val useMs = minOf(durMs, secs * 1000L)
                val total = (useMs * fps / 1000).toInt().coerceAtLeast(1)
                runOnUiThread { bar.max = total }

                val name = "qmg_frames_" + System.currentTimeMillis() + ".zip"
                val values = ContentValues()
                values.put(MediaStore.Downloads.DISPLAY_NAME, name)
                values.put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                val outUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (outUri == null) {
                    setStatus("Download folder එකට ලියන්න බැරි වුණා", Color.parseColor("#F87171"))
                    return@Thread
                }
                val os = contentResolver.openOutputStream(outUri)
                if (os == null) {
                    setStatus("Output stream එක open වුණේ නෑ", Color.parseColor("#F87171"))
                    return@Thread
                }

                var written = 0
                ZipOutputStream(os).use { zip ->
                    zip.setLevel(1)
                    for (i in 0 until total) {
                        val tUs = i * 1_000_000L / fps
                        val src = mmr.getFrameAtTime(tUs, MediaMetadataRetriever.OPTION_CLOSEST) ?: continue
                        val bmp = fitCrop(src, w, h)
                        written++
                        zip.putNextEntry(ZipEntry(String.format("bootup%03d.png", written)))
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, zip)
                        zip.closeEntry()
                        bmp.recycle()
                        val n = written
                        runOnUiThread { bar.progress = n }
                        setStatus("Frame " + n + " / " + total)
                    }
                }
                setStatus("ඉවරයි ✅  frames " + written + " ක්\nDownload/" + name, GOOD)
            } catch (e: Exception) {
                setStatus("Error: " + e, Color.parseColor("#F87171"))
            } finally {
                try {
                    mmr?.release()
                } catch (e: Exception) {
                }
                runOnUiThread {
                    goBtn.isEnabled = true
                    goBtn.alpha = 1f
                }
            }
        }.start()
    }

    private fun fitCrop(src: Bitmap, w: Int, h: Int): Bitmap {
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val sr = src.width.toFloat() / src.height
        val dr = w.toFloat() / h
        val rect: Rect
        if (sr > dr) {
            val nw = (src.height * dr).toInt()
            rect = Rect((src.width - nw) / 2, 0, (src.width + nw) / 2, src.height)
        } else {
            val nh = (src.width / dr).toInt()
            rect = Rect(0, (src.height - nh) / 2, src.width, (src.height + nh) / 2)
        }
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        Canvas(out).drawBitmap(src, rect, Rect(0, 0, w, h), paint)
        src.recycle()
        return out
    }
}
