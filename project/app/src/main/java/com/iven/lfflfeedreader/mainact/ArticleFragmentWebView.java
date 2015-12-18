package com.iven.lfflfeedreader.mainact;

import com.iven.lfflfeedreader.R;
import com.iven.lfflfeedreader.domparser.RSSFeed;
import com.iven.lfflfeedreader.utils.Preferences;
import com.iven.lfflfeedreader.utils.ScrollAwareFABBehavior;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.widget.TextView;

public class ArticleFragmentWebView extends Fragment {

	private int fPos;
	RSSFeed fFeed;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        //Initialize the feed (i.e. get all the data)
        fFeed = (RSSFeed) getArguments().getSerializable("feed");
		fPos = getArguments().getInt("pos");
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

        //apply activity's theme if dark theme is enabled
        Preferences.applyTheme(getActivity());

        //get the chosen article's text size from preferences
        float size = Preferences.resolveTextSizeResId(getContext());

       //set the view
		View view = inflater
				.inflate(R.layout.article_fragment_wb, container, false);

        //initialize the fab button
        final FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.back);

        //initialize continue reading and share TextViews used for immersive mode
        final TextView txt_continue_reading_wb = (TextView) view.findViewById(R.id.txt_continue_wb);

        final TextView txt_share_wb = (TextView) view.findViewById(R.id.txt_share_wb);

        final AppCompatActivity activity = (AppCompatActivity) getActivity();

        //initialize the scrollview
        final NestedScrollView scroll = (NestedScrollView) view.findViewById(R.id.sv_wb);

        //initialize the webview
        final WebView wb = (WebView) view.findViewById(R.id.wb);

        //remove fab button from the view if api < 19, i.e KitKat
        if (Build.VERSION.SDK_INT < 19){
            fab.setVisibility(View.INVISIBLE);
        }

        //only for api >=19, i.e KitKat
        //if immersive mode is enabled show a fab button dynamically to provide back navigation
        //since toolbar is now hidden in article activity

        if (Build.VERSION.SDK_INT >= 19){
            if (Preferences.immersiveEnabled(getActivity())) {

                //this the method to handle fab click to provide back navigation
                View.OnClickListener listener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.onBackPressed();
                    }
                };

                //set fab's on click listener
                fab.setOnClickListener(listener);

                //set fab's behavior
                //initialize coordinator layout
                final CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) fab.getLayoutParams();
                p.setBehavior(new ScrollAwareFABBehavior());
                fab.setLayoutParams(p);

            } else {

                //set fab button not visible if immersive mode is disabled
                fab.setVisibility(View.INVISIBLE);

            }
        }

        //set continue reading and share TextViews text dynamically

        //continue reading
        SpannableString str_read = new SpannableString(getResources().getString(R.string.continue_read));

        str_read.setSpan(new UnderlineSpan(), 0, str_read.length(),
                Spanned.SPAN_PARAGRAPH);

        txt_continue_reading_wb.setText(str_read);

        //share
        SpannableString str_share = new SpannableString(getResources().getString(R.string.share));

        str_share.setSpan(new UnderlineSpan(), 0, str_share.length(),
                Spanned.SPAN_PARAGRAPH);

        txt_share_wb.setText(str_share);

        //this the method to handle the continue reading TextView click on immersive mode
        View.OnClickListener listener_forward = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wb.loadUrl(fFeed.getItem(fPos).getLink());
                txt_continue_reading_wb.setTextColor(ContextCompat.getColor(getContext(), R.color.primary));

            }
        };

        //this the method to handle the share TextView click on immersive mode
        View.OnClickListener listener_share = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                share();
                txt_share_wb.setTextColor(ContextCompat.getColor(getContext(), R.color.primary));

            }
        };

        //set continue reading/share TextViews listeners
        txt_continue_reading_wb.setOnClickListener(listener_forward);

        txt_share_wb.setOnClickListener(listener_share);

        //initialize the dynamic items (the title, subtitle)
        //final because we need to access theme within the class from webview client method
		final TextView title_wb = (TextView) view.findViewById(R.id.titlewb);
		final TextView subtitle_wb = (TextView) view.findViewById(R.id.subtitlewb);

        //title
        title_wb.setText(fFeed.getItem(fPos).getTitle());

        //add date to subtitle
        subtitle_wb.setText(fFeed.getItem(fPos).getDate());

        //set the articles text size from preferences
        //little explanation about setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        // TypedValue.COMPLEX_UNIT_SP = the text unit, in this case SP
        // size = the text size from preferences
        title_wb.setTextSize(TypedValue.COMPLEX_UNIT_SP, size + 4);
        subtitle_wb.setTextSize(TypedValue.COMPLEX_UNIT_SP, size - 5);

        //set smooth scroll enabled
		scroll.setSmoothScrollingEnabled(true);

        //initialize the webview settings
		final WebSettings ws = wb.getSettings();

        //set default encoding to utf-8 to avoid malformed text
		ws.setDefaultTextEncodingName("utf-8");

        //set bg transparent because we will apply the bg using the activity's theme
		wb.setBackgroundColor(Color.TRANSPARENT);

        //control the layout of html
        //for more info http://developer.android.com/reference/android/webkit/WebSettings.LayoutAlgorithm.html
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
			ws.setLayoutAlgorithm(LayoutAlgorithm.NORMAL);
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			ws.setLayoutAlgorithm(LayoutAlgorithm.TEXT_AUTOSIZING);
		}

        //parse the articles text using jsoup and replace some items since this is a simple textview
        //and continue reading is not clickable
        //so we are going to replace with an empty text
		String base = fFeed.getItem(fPos).getDescription().replace("Continua a leggere...", "");

        //this is the text that will be loaded inside the html
        String baseformat = base.replace("Continue reading...", "");


        //here we handle the html where the articles will be loaded
		String html = "<!DOCTYPE html>";

        //the articles html
		html += baseformat;

        //Set different text color on light and dark themes
        //and justify the text
        //dark theme
        if (Preferences.darkThemeEnabled(getContext())) {
		html += "<body  text=\"#b1b1b1\" align=\"justify\";></body>";
        } else {

            //light theme
            html += "<body  text=\"#4e4e4e\" align=\"justify\";></body>";
        }
		html += "</html>";
		wb.loadData(html, "text/html; charset=utf-8;", "utf-8");
        wb.setBackgroundColor(Color.TRANSPARENT);

        //this is a needed transformation (from float to int) to set the text size on webview
        int size_wb = Math.round(size);
        ws.setDefaultFontSize(size_wb);

        //override this method to load the url inside the in-app webview
        wb.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView webview, String url) {
                webview.loadUrl(url);
                title_wb.setVisibility(View.INVISIBLE);
                subtitle_wb.setVisibility(View.INVISIBLE);
                txt_continue_reading_wb.setVisibility(View.INVISIBLE);
                txt_share_wb.setVisibility(View.INVISIBLE);
                return true;
            }
        });

        //enable JavaScript if the preference is enabled
        if (Preferences.JSEnabled(getContext())) {
            ws.setJavaScriptEnabled(true);
        }
		return view;
    }

    public void share() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, fFeed.getItem(fPos).getLink());
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share) + " '" + fFeed.getItem(fPos).getTitle() + "'"));
    }
}
