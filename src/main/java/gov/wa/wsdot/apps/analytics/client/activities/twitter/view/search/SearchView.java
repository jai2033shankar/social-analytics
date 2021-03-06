/*
 * Copyright (c) 2016 Washington State Department of Transportation
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package gov.wa.wsdot.apps.analytics.client.activities.twitter.view.search;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.jsonp.client.JsonpRequestBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.binder.EventBinder;
import com.google.web.bindery.event.shared.binder.EventHandler;
import gov.wa.wsdot.apps.analytics.client.ClientFactory;
import gov.wa.wsdot.apps.analytics.client.activities.events.SearchEvent;
import gov.wa.wsdot.apps.analytics.client.activities.twitter.view.tweet.TweetView;
import gov.wa.wsdot.apps.analytics.client.resources.Resources;
import gov.wa.wsdot.apps.analytics.shared.Mention;
import gov.wa.wsdot.apps.analytics.shared.Words;
import gov.wa.wsdot.apps.analytics.util.Consts;
import gwt.material.design.client.base.SearchObject;
import gwt.material.design.client.constants.IconType;
import gwt.material.design.client.ui.*;

import java.util.ArrayList;
import java.util.List;

public class SearchView extends Composite{

    interface MyEventBinder extends EventBinder<SearchView> {}
    private final MyEventBinder eventBinder = GWT.create(MyEventBinder.class);

    private static SearchViewUiBinder uiBinder = GWT
            .create(SearchViewUiBinder.class);

    interface SearchViewUiBinder extends
            UiBinder<Widget, SearchView> {
    }

    @UiField
    MaterialTextBox tweetSearch;

    @UiField
    MaterialIcon clearSearch;

    @UiField
    static
    MaterialPreLoader searchLoader;

    @UiField
    static
    HTMLPanel searchList;

    @UiField
    static
    MaterialButton moreSearchBtn;

    @UiField
    static
    MaterialButton backToSearchTopBtn;

    @UiField
    static
    MaterialLink advSearchLink;

    @UiField(provided = true)
    static
    AdvSearchView advSearch;

    @UiField
    static
    MaterialLink exportLink;

    final Resources res;

    private static final String JSON_URL_SUGGESTION = Consts.HOST_URL + "/search/suggest/";
    private static int pageNum = 1;
    private static String searchText = "";
    private static String url = "";

    private static ClientFactory clientFactory;

    public SearchView(ClientFactory clientFactory) {

        advSearch = new AdvSearchView(clientFactory);
        res = GWT.create(Resources.class);
        res.css().ensureInjected();
        eventBinder.bindEventHandlers(this, clientFactory.getEventBus());
        initWidget(uiBinder.createAndBindUi(this));
        this.clientFactory = clientFactory;
    }


    @UiHandler("advSearchLink")
    void onAdvSearch(ClickEvent e){
        advSearch.open();
    }

    @UiHandler("tweetSearch")
    void onSearch(ValueChangeEvent<String> e){
        SearchEvent searchEvent = new SearchEvent(tweetSearch.getValue());
        clientFactory.getEventBus().fireEvent(searchEvent);
    }

    @UiHandler("tweetSearch")
    void onKeyUp(KeyUpEvent e){
        //getSuggestions(tweetSearch.getValue());
    }

    @UiHandler("clearSearch")
    void onClear(ClickEvent e){
        tweetSearch.clear();
    }

    @UiHandler("exportLink")
    void onExport(ClickEvent e){
        Window.open(url, "_blank", "");
    }

    @UiHandler("moreSearchBtn")
    public void onMore(ClickEvent e){

        int nextPage = pageNum + 1;

        searchLoader.setVisible(true);

        JsonpRequestBuilder jsonp = new JsonpRequestBuilder();
        // Set timeout for 30 seconds (30000 milliseconds)
        jsonp.setTimeout(30000);
        jsonp.requestObject(url + nextPage, new AsyncCallback<Mention>() {

            @Override
            public void onFailure(Throwable caught) {
                Window.alert("Failure: " + caught.getMessage());
                searchLoader.setVisible(false);
            }

            @Override
            public void onSuccess(Mention mention) {
                if (mention.getMentions() != null) {
                    pageNum++;
                    updateSearch(mention.getMentions());
                    searchLoader.setVisible(false);
                }
            }
        });
    }

    @UiHandler("backToSearchTopBtn")
    public void onBackToTop(ClickEvent e){
        Window.scrollTo(0,0);
    }

    @EventHandler
    void onSearch(SearchEvent e) {
        pageNum = 1;
        backToSearchTopBtn.setVisible(false);
        searchText = e.getSearchText();
        searchList.clear();
        moreSearchBtn.setVisible(false);
        exportLink.setVisible(false);

        tweetSearch.setText(e.getSearchText());

        String url = getUrl(e);

        searchLoader.setVisible(true);

        JsonpRequestBuilder jsonp = new JsonpRequestBuilder();
        // Set timeout for 30 seconds (30000 milliseconds)
        jsonp.setTimeout(30000);
        jsonp.requestObject(url + pageNum, new AsyncCallback<Mention>() {

            @Override
            public void onFailure(Throwable caught) {
                Window.alert("Failure: " + caught.getMessage());
                searchLoader.setVisible(false);
            }

            @Override
            public void onSuccess(Mention mention) {
                if (mention.getMentions() != null) {
                    updateSearch(mention.getMentions());
                    searchLoader.setVisible(false);
                }
            }
        });
    }

    /**
     * Constructs a url for searching tweets
     *
     * the url has the form:
     *
     *  HOST_URL/search/:text/:screenName/:collection/:fromYear/:fromMonth/:fromDay/:toYear/:toMonth/:toDay/:page
     *
     * @param e
     * @return
     */
    private static String getUrl(SearchEvent e){

        searchText = e.getSearchText();

        searchText = URL.encodePathSegment(searchText);

        url = Consts.HOST_URL + "/search/" + searchText + "/";

        url = url + e.getAccount() + "/" + ((e.getSearchType() == 2) ? "mentions" : "statuses");

        url = url + "/" + e.getMediaOnly();

        if (e.getStartDate() != null && e.getEndDate() != null){
            url = url + e.getStartDate() + e.getEndDate() + "/";
        }else if (e.getEndDate() != null){
            url = url + "/0/0/0" + e.getEndDate() + "/";
        }else if (e.getStartDate() != null){
            url = url + e.getStartDate() + "/0/0/0/";
        }else{
            url = url + "/0/0/0/0/0/0/";
        }

        return url;
    }

    public static void updateSearch(JsArray<Mention> asArrayOfMentionData) {

        int j = asArrayOfMentionData.length();

        if (j > 24){
            backToSearchTopBtn.setVisible(true);
        }

        DateTimeFormat dateTimeFormat = DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        DateTimeFormat dateTimeFormat2 = DateTimeFormat.getFormat("MMMM dd, yyyy h:mm:ss a");

        String urlPattern = "(https?:\\/\\/[-a-zA-Z0-9._~:\\/?#@!$&\'()*+,;=%]+)";
        String atPattern = "@+([_a-zA-Z0-9-]+)";
        String hashPattern = "#+([_a-zA-Z0-9-]+)";
        String text;
        String updatedText;
        String screenName;
        String mediaUrl;

        for (int i = 0; i < j; i++) {

            screenName = (asArrayOfMentionData.get(i).getFromUser() != null) ?
                    asArrayOfMentionData.get(i).getFromUser() :
                    asArrayOfMentionData.get(i).getUser().getScreenName();

            TweetView tweet;

            text = asArrayOfMentionData.get(i).getText();
            updatedText = text.replaceAll(urlPattern, "<a href=\"$1\" target=\"_blank\">$1</a>");
            updatedText = updatedText.replaceAll(atPattern, "<a href=\"http://twitter.com/#!/$1\" target=\"_blank\">@$1</a>");
            updatedText = updatedText.replaceAll(hashPattern, "<a href=\"http://twitter.com/#!/search?q=%23$1\" target=\"_blank\">#$1</a>");

            String createdAt = dateTimeFormat2.format(dateTimeFormat.parse(asArrayOfMentionData.get(i).getCreatedAt()));

            String link = "http://twitter.com/#!/" + screenName + "/status/" + asArrayOfMentionData.get(i).getIdStr();

            mediaUrl = null;
            try {
                for (int k = 0; k < asArrayOfMentionData.get(i).getEntities().getMedia().length(); k++) {
                    mediaUrl =  asArrayOfMentionData.get(i).getEntities().getMedia().get(k).getMediaUrl();
                }
            } catch (Exception e) {} // Image preview is nice, but if it fails...oh well.

            String id = asArrayOfMentionData.get(i).getIdStr();

            if (asArrayOfMentionData.get(i).getSentiment().equals("positive")) {
                tweet = new TweetView(id, screenName, updatedText, createdAt, link, mediaUrl, IconType.SENTIMENT_SATISFIED);
            } else if (asArrayOfMentionData.get(i).getSentiment().equals("negative")) {
                tweet = new TweetView(id, screenName, updatedText, createdAt, link, mediaUrl, IconType.SENTIMENT_DISSATISFIED);
            } else {
                tweet = new TweetView(id, screenName, updatedText, createdAt, link, mediaUrl, IconType.SENTIMENT_NEUTRAL);
            }
            searchList.add(tweet);
        }

        if (j > 0){
            exportLink.setVisible(true);
        }

        if (j < 25){
            moreSearchBtn.setVisible(false);
        } else {
            moreSearchBtn.setVisible(true);
        }
    }

    public void getSuggestions(String searchText) {
        String url = JSON_URL_SUGGESTION;
        String searchString = SafeHtmlUtils.htmlEscape(searchText.trim().replace("'", ""));

        // Append the name of the callback function to the JSON URL.
        url += searchString;
        url = URL.encode(url);
        JsonpRequestBuilder jsonp = new JsonpRequestBuilder();
        // Set timeout for 30 seconds (30000 milliseconds)
        jsonp.setTimeout(30000);
        jsonp.requestObject(url, new AsyncCallback<Words>() {

            @Override
            public void onFailure(Throwable caught) {
                // Just fail silently here.
            }

            @Override
            public void onSuccess(Words words) {
                if (words.getWords() != null) {

                    List<SearchObject> searchHints = new ArrayList<SearchObject>();

                    for (int i = 0; i < words.getWords().length(); i++){
                        SearchObject search = new SearchObject();
                        search.setKeyword(words.getWords().get(i));
                        searchHints.add(search);
                    }

                    updateSuggestions(searchHints);
                }
            }
        });
    }

    public void updateSuggestions(List<SearchObject> suggestions){

    }
}

