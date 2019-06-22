package com.guillermonegrete.tts.importtext

import android.net.Uri
import android.os.Bundle
import android.text.TextPaint
import android.util.Xml
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.importtext.epub.Book
import com.guillermonegrete.tts.importtext.epub.NavPoint
import org.xmlpull.v1.XmlPullParser

class VisualizeTextActivity: AppCompatActivity() {

    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visualize_text)

        var book: Book? = null
        val text = if(SHOW_EPUB == intent.action) {
            book = readEpubFile()
            book.chapters.first()
        } else {
            intent?.extras?.getString(IMPORTED_TEXT) ?: "No text"
        }

        val currentPageLabel: TextView = findViewById(R.id.reader_current_page)

        viewPager = findViewById(R.id.text_reader_viewpager)
        viewPager.post{
            val pageTextPaint = createTextPaint()

            val pageSplitter =  createPageSplitter()
            pageSplitter.append(text)
            pageSplitter.split(pageTextPaint)
            val pages = pageSplitter.getPages()

            currentPageLabel.text = resources.getString(R.string.reader_current_page_label, 1, pages.size) // Example: 1 of 33
            viewPager.adapter = VisualizerAdapter(pages)
            viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){
                override fun onPageSelected(position: Int) {
                    val pageNumber = position + 1
                    currentPageLabel.text = resources.getString(R.string.reader_current_page_label, pageNumber, pages.size)
                }
            })
        }

        val showTOCBtn = findViewById<ImageButton>(R.id.show_toc_btn)
        val navPoints = book?.tableOfContents?.navPoints

        if(navPoints == null || navPoints.isEmpty()){
            showTOCBtn.visibility = View.GONE
        }else{
            showTOCBtn.setOnClickListener { showTableOfContents(navPoints) }
        }

    }

    private fun readEpubFile(): Book {
        val uri: Uri = intent.getParcelableExtra(EPUB_URI)
//        Toast.makeText(this, "Uri $uri", Toast.LENGTH_SHORT).show()

        val rootStream = contentResolver.openInputStream(uri)
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)

        val epubParser = EpubParser()

        return epubParser.parseBook(parser, rootStream)
//        visualizeText(book.chapters.first())
    }

    private fun createPageSplitter(): PageSplitter{
        val lineSpacingExtra = resources.getDimension(R.dimen.visualize_page_text_line_spacing_extra)
        val lineSpacingMultiplier = 1f
        val pageItemPadding = (80 * resources.displayMetrics.density + 0.5f).toInt() // Convert dp to px, 0.5 is for rounding to closest integer

        return PageSplitter(viewPager.width - pageItemPadding, viewPager.height - pageItemPadding, lineSpacingMultiplier, lineSpacingExtra)
    }

    private fun createTextPaint(): TextPaint{
        val textSize = resources.getDimension(R.dimen.text_size_large)
        val pageTextPaint = TextPaint()
        pageTextPaint.textSize = textSize
        return pageTextPaint
    }

    private fun showTableOfContents(navPoints: List<NavPoint>){

        val filePaths = navPoints.map { it.content }
        val titles = navPoints.map { it.navLabel }

        val adapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_item, titles)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Table of contents")
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setAdapter(adapter) { _, i ->
                val path = filePaths[i]
                Toast.makeText(this@VisualizeTextActivity, "Selected $path", Toast.LENGTH_SHORT).show()
            }
            .create()
        dialog.show()
    }

    companion object{
        const val IMPORTED_TEXT = "imported_text"
        const val EPUB_URI = "epub_uri"

        const val SHOW_EPUB = "epub"
    }
}