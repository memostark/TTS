package com.guillermonegrete.tts.importtext.visualize

import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.utils.dpToPixel
import java.text.BreakIterator
import java.util.*

class VisualizerAdapter(
    private val pages: List<CharSequence>,
    private val showTextDialog: (CharSequence) -> Unit,
    val viewModel: VisualizeTextViewModel
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var isExpanded = false
    var isSplit = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return when(viewType){
            R.layout.visualizer_split_page_item -> SplitPageViewHolder(layout)
            else -> PageViewHolder(isExpanded, layout)
        }
    }

    override fun getItemCount() = pages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder){
            is PageViewHolder -> holder.bind(pages[position])
            is SplitPageViewHolder -> holder.bind(pages[position], viewModel.translatedPages[position])
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        println("Binding view holder $position, payload: $payloads")
        if(payloads.isEmpty()){
            onBindViewHolder(holder, position)
        }else{
            if(holder is SplitPageViewHolder) holder.updateLayoutParams(payloads.first() as Boolean)
        }
    }

    override fun getItemViewType(position: Int): Int {
        if(isSplit) return R.layout.visualizer_split_page_item
        return R.layout.visualizer_page_item
    }

    open inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        protected val pageTextView: TextView = view.findViewById(R.id.page_text_view)

        private val actionModeCallback = object : ActionMode.Callback{

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                when(item.itemId){
                    R.id.show_process_text_activity -> {
                        if (pageTextView.isFocused) {
                            val selStart = pageTextView.selectionStart
                            val selEnd = pageTextView.selectionEnd

                            // We need to make sure start and end are within the text length
                            val min = 0.coerceAtLeast(selStart.coerceAtMost(selEnd))
                            val max = 0.coerceAtLeast(selStart.coerceAtLeast(selEnd))

                            val selectedText = pageTextView.text.subSequence(min, max)
                            showTextDialog(selectedText)
                        }

                        mode.finish()
                        return true
                    }
                    else -> return false
                }
            }

            override fun onCreateActionMode(mode: ActionMode, menu: Menu) = true

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                menu.clear()
                val inflater: MenuInflater = mode.menuInflater
                menu.add(Menu.NONE, android.R.id.copy, Menu.NONE, "Copy")
                inflater.inflate(R.menu.menu_context_text_visualizer, menu)
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode?) {}
        }

        init {
            pageTextView.customSelectionActionModeCallback = actionModeCallback
            // Color taken from member variable mHighlightColor from TextView class.
            pageTextView.highlightColor = 0x6633B5E5
            pageTextView.movementMethod = LinkMovementMethod.getInstance()
        }

        // Based on: https://stackoverflow.com/questions/8612652/select-a-word-on-a-tap-in-textview-edittext
        private fun setSpannables(view: TextView){
            val spans = view.text as SpannableString
            val iterator = BreakIterator.getWordInstance(Locale.US)
            iterator.setText(spans.toString())
            var start = iterator.first()
            var end = iterator.next()
            while (end != BreakIterator.DONE) {
                val possibleWord = spans.substring(start, end)
                if (Character.isLetterOrDigit(possibleWord.first())) {

                    val clickSpan = object: ImportedClickableSpan() {
                        override fun onClick(widget: View) {
                            super.onClick(widget)
                            showTextDialog(possibleWord)
                        }
                    }

                    spans.setSpan(
                        clickSpan, start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                start = end
                end = iterator.next()
            }
        }

        protected fun setPageText(text: CharSequence){
            pageTextView.movementMethod = LinkMovementMethod.getInstance()

            pageTextView.setText(text, TextView.BufferType.SPANNABLE)
            setSpannables(pageTextView)
        }

    }

    inner class PageViewHolder(isExpanded: Boolean, view: View): ViewHolder(view){

        init {
            if(isExpanded){
                TextViewCompat.setAutoSizeTextTypeWithDefaults(pageTextView, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
            }else{
                TextViewCompat.setAutoSizeTextTypeWithDefaults(pageTextView, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE)
            }
        }

        fun bind(text: CharSequence){
            setPageText(text)
        }
    }

    inner class SplitPageViewHolder(view: View): ViewHolder(view){
        private val bottomText: View = view.findViewById(R.id.page_bottom_text_view)

        private val noTranslation = itemView.context.getString(R.string.translation_not_found)

        private val pageMarginsSize = itemView.context.dpToPixel(40)

        private val hiddenParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, pageMarginsSize, 0f)
        private val halfShownParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.5f).apply {
            setMargins(0, 0, 0, pageMarginsSize)
        }

        fun bind(text: CharSequence, translatedText: CharSequence?){
            setPageText(text)
        }

        fun updateLayoutParams(splitPage: Boolean){
            bottomText.layoutParams = if(splitPage) halfShownParams else hiddenParams
        }

    }
}