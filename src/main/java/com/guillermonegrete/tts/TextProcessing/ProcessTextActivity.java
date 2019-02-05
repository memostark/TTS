/*
    Ejemplo tomado: https://github.com/Azure-Samples/Cognitive-Speech-TTS/tree/master/Android
    referencias: https://medium.com/google-developers/custom-text-selection-actions-with-action-process-text-191f792d2999
    Actividad flotante: https://stackoverflow.com/questions/33853311/how-to-create-a-floating-touchable-activity-that-still-allows-to-touch-native-co
                        http://www.androidmethlab.com/2015/09/transparent-floating-window-in-front-of.html
*/

package com.guillermonegrete.tts.TextProcessing;


import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.material.tabs.TabLayout;
import com.guillermonegrete.tts.CustomTTS.CustomTTS;
import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.SavedWords.SaveWordDialogFragment;
import com.guillermonegrete.tts.Services.ScreenTextService;
import com.guillermonegrete.tts.Main.SettingsFragment;
import com.guillermonegrete.tts.TextProcessing.domain.model.WikiItem;
import com.guillermonegrete.tts.TextProcessing.domain.model.WiktionaryItem;
import com.guillermonegrete.tts.TextProcessing.domain.model.WiktionaryLangHeader;
import com.guillermonegrete.tts.ThreadExecutor;
import com.guillermonegrete.tts.data.source.DictionaryRepository;
import com.guillermonegrete.tts.data.source.WordRepository;
import com.guillermonegrete.tts.data.source.local.WordLocalDataSource;
import com.guillermonegrete.tts.data.source.remote.MSTranslatorSource;
import com.guillermonegrete.tts.data.source.remote.WiktionarySource;
import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.db.WordsDAO;
import com.guillermonegrete.tts.db.WordsDatabase;
import com.guillermonegrete.tts.threading.MainThreadImpl;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.guillermonegrete.tts.SavedWords.SaveWordDialogFragment.TAG_DIALOG_UPDATE_WORD;


public class ProcessTextActivity extends FragmentActivity implements CustomTTS.CustomTTSListener, ProcessTextContract.View {
    private CustomTTS tts;
    private WiktionaryAdapter mAdapter;

    private String TAG = this.getClass().getSimpleName();
    public static final String LONGPRESS_SERVICE_NOSHOW = "startServiceLong";
    public static final String LONGPRESS_SERVICE = "showServiceg";

    private boolean mIsSentence;
    private boolean mInsideDatabase;
    private boolean mWikiRequestDone;
    private boolean mInsideWikitionary;
    private boolean mLanguageDetected;

    private boolean mAutoTTS;

    private String mTranslation;
    private String mSelectedText;


    private WordsDAO mWordsDAO;

    private Words mFoundWords;

    private ProcessTextContract.Presenter presenter;

    private ListView listView;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);


        if(tts == null) {
            tts = new CustomTTS(ProcessTextActivity.this);
            tts.setListener(this);
        }

        mSelectedText = getSelectedText();
        String[] splittedText = mSelectedText.split(" ");
        mIsSentence = splittedText.length > 1;
        mInsideWikitionary = true;
        mWikiRequestDone = false;
        mLanguageDetected = false;
        mInsideDatabase = false;


        final Intent intentService = new Intent(this, ScreenTextService.class);
        intentService.setAction(LONGPRESS_SERVICE_NOSHOW);
        startService(intentService);

        mAutoTTS = getAutoTTSPreference();

        /*if("WITH_FLAG".equals(getIntent().getAction())){
            mInsideDatabase = false;
            sendWiktionaryRequest(mSelectedText);
            tts.determineLanguage(mSelectedText);
            return;
        }

        if(mIsSentence){
            mWikiRequestDone = true;
            mInsideWikitionary = false;
            tts.determineLanguage(mSelectedText);
        }else{
            mFoundWords = searchInDatabase(mSelectedText);
            if(!mInsideDatabase) {
                sendWiktionaryRequest(mSelectedText);
                tts.determineLanguage(mSelectedText);
            } else {
                setBottomDialog();
                mAdapter = null;
                mTranslation = null;
                setWordLayout(mSelectedText, mFoundWords);
                createSmallViewPager();

                tts.initializeTTS(mFoundWords.lang);
                if(mAutoTTS) tts.speak(mSelectedText);
            }
        }*/

        presenter = new ProcessTextPresenter(ThreadExecutor.getInstance(),
                MainThreadImpl.getInstance(),
                this,
                WordRepository.getInstance(MSTranslatorSource.getInstance(), WordLocalDataSource.getInstance(WordsDatabase.getDatabase(getApplicationContext()).wordsDAO())),
                DictionaryRepository.getInstance(WiktionarySource.getInstance()));
        presenter.getLayout(getSelectedText());

    }



    private String getSelectedText(){
        Intent intent = getIntent();
        final CharSequence selected_text = intent
                .getCharSequenceExtra("android.intent.extra.PROCESS_TEXT");
        return selected_text.toString();
    }

    private boolean getAutoTTSPreference(){
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getBoolean(SettingsFragment.PREF_AUTO_TEST_SWITCH, true);
    }

    private Words searchInDatabase(String selected_word){
        mWordsDAO = WordsDatabase.getDatabase(getApplicationContext()).wordsDAO();
        Words foundWords = mWordsDAO.findWord(selected_word);
        mInsideDatabase = foundWords != null;
        return foundWords;
    }

    private void setWordLayout(final String textString, final Words foundWords){
        setContentView(R.layout.activity_processtext);

        TextView mTextTTS = findViewById(R.id.text_tts);
        mTextTTS.setText(textString);

        if(foundWords != null) {
            ImageButton saveIcon = findViewById(R.id.save_icon);
            saveIcon.setImageResource(R.drawable.ic_bookmark_black_24dp);

            ImageButton editIcon = findViewById(R.id.edit_icon);
            editIcon.setVisibility(View.VISIBLE);
            editIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DialogFragment dialogFragment = SaveWordDialogFragment.newInstance(
                        foundWords.word,
                        foundWords.lang,
                        foundWords.definition);
                    dialogFragment.show(getSupportFragmentManager(), TAG_DIALOG_UPDATE_WORD);
                }
            });
        }

        findViewById(R.id.play_tts_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tts.speak(textString);
            }
        });

        findViewById(R.id.save_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    showSaveDialog(textString);

            }
        });


    }

    private void setSentenceLayout(String text){
        setContentView(R.layout.activity_process_sentence);
        TextView mTextTTS = findViewById(R.id.text_tts);
        mTextTTS.setText(text);
    }

    private void showSaveDialog(String word) {
        DialogFragment dialogFragment;
        dialogFragment = SaveWordDialogFragment.newInstance(word, tts.language, mTranslation);
        dialogFragment.show(getSupportFragmentManager(), "New word process");
    }

    private void showDeleteDialog(final String word){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to delete this word?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mWordsDAO.deleteWord(word);
                        ImageButton saveIcon = findViewById(R.id.save_icon);
                        saveIcon.setImageResource(R.drawable.ic_bookmark_border_black_24dp);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        builder.create().show();
    }

    @Override
    public void setWiktionaryLayout(List<WikiItem> items) {




        /*List<WikiItem> view_items = new ArrayList<>();
        for (WikiItem item: items){
            view_items.add(item);
        }
        Toast.makeText(this, "wiktionary", Toast.LENGTH_SHORT).show();*/

        Toast.makeText(this, "iniciado", Toast.LENGTH_SHORT).show();

        /*for (WikiItem def : items){
            if(def instanceof WiktionaryItem){
                WiktionaryItem item = (WiktionaryItem) def;
                System.out.println("---------Item type:");
                System.out.println(item.getSubHeaderText());
                System.out.println(item.getItemText());
            }else{
                WiktionaryLangHeader item = (WiktionaryLangHeader) def;
                System.out.println("---------Header type:");
                System.out.println(item.getLanguage());
            }
        }*/
        System.out.println(String.format("In UI thread, set wiki layout method, items size %s", String.valueOf(items.size())));
        setCenterDialog();
        setWordLayout(getSelectedText(), null);
        mAdapter = new WiktionaryAdapter(this, items);

        mFoundWords = null;

        ViewPager pager =  findViewById(R.id.process_view_pager);
        TabLayout tabLayout = findViewById(R.id.pager_menu_dots);
        tabLayout.setupWithViewPager(pager, true);
        pager.setAdapter(new MyPageAdapter(getSupportFragmentManager()));

    }

    @Override
    public void setSavedWordLayout(Words word) {
        setBottomDialog();
        Toast.makeText(this,"saved word", Toast.LENGTH_SHORT).show();
        setWordLayout(getSelectedText(), word);
    }

    @Override
    public void setTranslationLayout(Words word) {
        setBottomDialog();
        Toast.makeText(this,"translation", Toast.LENGTH_SHORT).show();
        setWordLayout(getSelectedText(), word);
    }

    @Override
    public void setSentenceLayout(Words word) {
        setBottomDialog();
        setContentView(R.layout.activity_process_sentence);
        TextView mTextTTS = findViewById(R.id.text_tts);
        mTextTTS.setText(word.definition);
    }

    @Override
    public void setExternalDictionary() {

    }

    @Override
    public void onClickBookmark() {

    }

    @Override
    public void onClickReproduce() {

    }

    @Override
    public void onClickEdit() {

    }

    @Override
    public void setPresenter(ProcessTextContract.Presenter presenter) {
        this.presenter = presenter;
    }

    public static class WiktionaryParser{
        String text;
        final static int TYPE_HEADER = 100;
        final static int TYPE_SUBHEADER = 101;
        final static int TYPE_TEXT = 102;

        public WiktionaryParser(String text){
            this.text = text;
        }

        public List<WiktionaryItem> parse(){
            List<String> LanguageSections = getLanguages(text);

            String[] separated;
            List<WiktionaryItem> items = new ArrayList<>();

            for (int i=1; i<LanguageSections.size(); i++){
                separated = LanguageSections.get(i).split("\n=== ");
                String lang = separated[0].split(" ")[0];

                items.add(new WiktionaryItem(lang, "Placeholder"));

                int j;
                //Log.i(TAG,"--------------"+lang+"---------------");
                for (j=1; j<separated.length;j++){
                    String[] subheaders = separated[j].split(" ===\n");
                    //Log.i(TAG,"----Subheader " + j +": "+subheaders[0]);
                    //Log.i(TAG,subheaders[1]);
                    items.add(new WiktionaryItem(subheaders[0], "Placeholder"));
                    String[] subsubheader = subheaders[1].split("\n==== ");
                    for(int k=0; k<subsubheader.length; k++){
                        items.add(new WiktionaryItem(subsubheader[k].replace("====\n",""), "Placeholder"));
                    }
                }
            }

            return items;
        }

        private List<String> getLanguages(String extract){
            String[] separated = extract.split("\n== ");
            List<String> langs = new ArrayList<>();
            Collections.addAll(langs, separated);
            return langs;
        }
    }


    public static List<String> getLanguages(String extract){
        String[] separated = extract.split("\n== ");
        List<String> langs = new ArrayList<>();
        for (int i=0; i<separated.length; i++){
            langs.add(separated[i]);
            //Log.i(TAG,(i+1)+".- "+separated[i]);
        }
        return langs;
    }

    @Override
    protected void onDestroy() {
        if(tts != null) tts.finishTTS();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void onLanguageDetected(String translation) {
        mTranslation = translation;
        mLanguageDetected = true;
        setLayout();
    }


    private void setLayout(){
        if(mLanguageDetected && mWikiRequestDone){

            setWindowParams();
            if(mIsSentence){

                TextView mTextTranslation = findViewById(R.id.text_translation);
                mTextTranslation.setText(mTranslation);
            } else if(mInsideWikitionary){
                if("WITH_FLAG".equals(getIntent().getAction())) mFoundWords = searchInDatabase(mSelectedText);
                setWordLayout(mSelectedText, mFoundWords);
                mFoundWords = null;
                ViewPager pager =  findViewById(R.id.process_view_pager);
                TabLayout tabLayout = findViewById(R.id.pager_menu_dots);
                tabLayout.setupWithViewPager(pager, true);
                pager.setAdapter(new MyPageAdapter(getSupportFragmentManager()));
            } else { // Single word not in wiktionary
                setWordLayout(mSelectedText, null);
                mAdapter = null;
                createSmallViewPager();
            }
            if(mAutoTTS) tts.speak(mSelectedText);
        }else{
            Log.d(TAG,"Not all data ready");
        }
    }

    public void setWindowParams() {
        if(mInsideWikitionary) setCenterDialog();
        else setBottomDialog();
    }

    private void setCenterDialog(){
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        WindowManager.LayoutParams wlp = getWindow().getAttributes();
        wlp.flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        getWindow().setAttributes(wlp);
    }

    private void setBottomDialog(){
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        WindowManager.LayoutParams wlp = getWindow().getAttributes();
        wlp.dimAmount = 0;
        wlp.flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        wlp.gravity = Gravity.BOTTOM;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setAttributes(wlp);
    }

    private void createSmallViewPager(){
        ViewPager pager =  findViewById(R.id.process_view_pager);
        ViewGroup.LayoutParams params = pager.getLayoutParams();
        params.height = 250;
        pager.setLayoutParams(params);
        TabLayout tabLayout = findViewById(R.id.pager_menu_dots);
        tabLayout.setupWithViewPager(pager, true);
        pager.setAdapter(new MyPageAdapter(getSupportFragmentManager()));
    }


    private class MyPageAdapter extends FragmentPagerAdapter{

        MyPageAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            switch (position){
                case 0: return DefinitionFragment.newInstance(mFoundWords, mTranslation, mAdapter);
                case 1: return ExternalLinksFragment.newInstance(mSelectedText);
                default: return DefinitionFragment.newInstance(mFoundWords, mTranslation, mAdapter);
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

    }
}

