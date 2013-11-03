package ru.sawim.view;

import DrawControls.icons.Icon;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import protocol.Contact;
import protocol.ContactMenu;
import protocol.Protocol;
import protocol.jabber.*;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.activities.SawimActivity;
import ru.sawim.models.ChatsSpinnerAdapter;
import ru.sawim.models.MessagesAdapter;
import ru.sawim.view.menu.MyMenu;
import ru.sawim.widget.LabelView;
import ru.sawim.widget.MyListView;
import ru.sawim.widget.Util;
import sawim.Clipboard;
import sawim.Options;
import sawim.chat.Chat;
import sawim.chat.ChatHistory;
import sawim.chat.MessData;
import ru.sawim.Scheme;
import sawim.roster.Roster;
import sawim.util.JLocale;

import java.util.Hashtable;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 24.01.13
 * Time: 20:30
 * To change this template use File | Settings | File Templates.
 */
public class ChatView extends SawimFragment implements Roster.OnUpdateChat {

    public static final String TAG = "ChatView";
    private Chat chat;
    private Protocol protocol;
    private Contact contact;
    private static Protocol lastProtocol;
    private static Contact lastContact;
    private boolean sendByEnter;
    private static boolean isTablet;

    private ImageButton usersImage;
    private ImageButton chatsImage;
    private ImageButton menuButton;
    private ImageButton smileButton;
    private ImageButton sendButton;
    private EditText messageEditor;
    private MyListView nickList;
    private MyListView chatListView;
    private ChatLabelView labelView;
    private ChatsSpinnerAdapter chatsSpinnerAdapter;
    private ChatListsView chatListsView;
    private ChatInputBarView chatInputBarView;
    private ChatBarView chatBarLayout;
    private ChatViewRoot chat_viewLayout;
    private MucUsersView mucUsersView;
    private MessagesAdapter adapter;

    private static Hashtable<String, State> chatHash = new Hashtable<String, State>();
    private static ViewsState viewsState;

    private static class ViewsState {
        private ImageButton usersImage;
        private ImageButton chatsImage;
        private ImageButton menuButton;
        private ImageButton smileButton;
        private ImageButton sendButton;
        private ChatLabelView labelView;
        private ChatBarView chatBarLayout;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        General.currentActivity = (FragmentActivity) activity;
        isTablet = activity.findViewById(R.id.fragment_container) == null;
        messageEditor = new EditText(activity);
        if (viewsState == null) {
            viewsState = new ViewsState();
            usersImage = viewsState.usersImage = new ImageButton(activity);
            labelView = viewsState.labelView = new ChatLabelView(activity);
            chatsImage = viewsState.chatsImage = new ImageButton(activity);

            menuButton = viewsState.menuButton = new ImageButton(activity);
            smileButton = viewsState.smileButton = new ImageButton(activity);
            sendButton = viewsState.sendButton = new ImageButton(activity);

            chatBarLayout = viewsState.chatBarLayout = new ChatBarView(activity);
        } else {
            usersImage = viewsState.usersImage;
            labelView = viewsState.labelView;
            chatsImage = viewsState.chatsImage;

            menuButton = viewsState.menuButton;
            smileButton = viewsState.smileButton;
            sendButton = viewsState.sendButton;

            chatBarLayout = viewsState.chatBarLayout;
        }
        chatListView = new MyListView(activity);
        nickList = new MyListView(activity);
        chatListsView = new ChatListsView(activity);
        chatInputBarView = new ChatInputBarView(activity);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            messageEditor.getBackground().setColorFilter(Scheme.getColor(Scheme.THEME_BACKGROUND), PorterDuff.Mode.MULTIPLY);
        }
    }

    private class ChatViewRoot extends LinearLayout {

        public ChatViewRoot(Context context) {
            super(context);
            setOrientation(LinearLayout.VERTICAL);
            setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            //addViewInLayout(chatBarLayout, 0, chatBarLayout.getLayoutParams());
            addViewInLayout(chatListsView, 0, chatListsView.getLayoutParams());
            View chatsImageDivider = Util.getDivider(context, false, Scheme.getColor(Scheme.THEME_CAP_BACKGROUND));
            addViewInLayout(chatsImageDivider, 1, chatsImageDivider.getLayoutParams());
            addViewInLayout(chatInputBarView, 2, chatInputBarView.getLayoutParams());
        }

        public void showHint() {
            hideHint();
            TextView hint = new TextView(getContext());
            hint.setTextColor(Scheme.getColor(Scheme.THEME_TEXT));
            hint.setTextSize(General.getFontSize());
            hint.setGravity(Gravity.CENTER);
            hint.setText(R.string.select_contact);
            addView(hint, 3);
        }

        public void hideHint() {
            if (getChildAt(3) != null) removeViewAt(3);
        }
    }

    private class ChatLabelView extends LinearLayout {

        ImageView imageView;
        LabelView textView;
        public ChatLabelView(Context context) {
            super(context);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            setOrientation(HORIZONTAL);
            layoutParams.weight = 1;
            setLayoutParams(layoutParams);

            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setGravity(Gravity.CENTER);

            imageView = new ImageView(context);
            imageView.setPadding(0, 0, 10, 0);
            linearLayout.addView(imageView);

            textView = new LabelView(context);
            linearLayout.addView(textView);

            addViewInLayout(linearLayout, 0, layoutParams);
        }

        public void updateTextView(String text) {
            textView.setTextColor(Color.WHITE);
            textView.setTextSize(General.getFontSize());
            textView.setText(text);
        }

        public void updateLabelIcon(Drawable drawable) {
            imageView.setImageDrawable(drawable);
        }
    }

    private class ChatBarView extends LinearLayout {

        public ChatBarView(Context context) {
            super(context);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            setOrientation(HORIZONTAL);
            setLayoutParams(layoutParams);

            LinearLayout.LayoutParams usersImageLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            usersImageLP.gravity = Gravity.CENTER_VERTICAL;
            usersImage.setMinimumWidth(76);
            addViewInLayout(usersImage, 0, usersImageLP);

            addViewInLayout(labelView, 1, labelView.getLayoutParams());

            LinearLayout.LayoutParams chatsImageLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            chatsImageLP.gravity = Gravity.CENTER_VERTICAL;
            chatsImage.setMinimumWidth(76);
            addViewInLayout(chatsImage, 2, chatsImageLP);
        }

        public void setVisibilityUsersImage(int visibility) {
            getChildAt(0).setVisibility(visibility);
        }

        public void setVisibilityChatsImage(int visibility) {
            getChildAt(2).setVisibility(visibility);
        }
    }

    private class ChatListsView extends LinearLayout {

        public ChatListsView(Context context) {
            super(context);
            rebuild();
        }

        public void rebuild() {
            removeAllViews();
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            setOrientation(HORIZONTAL);
            layoutParams.weight = 2;
            setLayoutParams(layoutParams);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (!isTablet && (getContext().getResources().getDisplayMetrics().densityDpi < 200 || android.os.Build.MODEL.equals("Digma iDx5")))
                lp.weight = 10;
            else
                lp.weight = 1;
            addViewInLayout(chatListView, 0, lp);

            lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (!isTablet && (getContext().getResources().getDisplayMetrics().densityDpi < 200 || android.os.Build.MODEL.equals("Digma iDx5")))
                lp.weight = 0;
            else if (isTablet)
                lp.weight = 3;
            else
                lp.weight = (float) 1.5;
            addViewInLayout(nickList, 1, lp);
        }
    }

    private class ChatInputBarView extends LinearLayout {

        public ChatInputBarView(Context context) {
            super(context);
            init();
        }

        private void init() {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            setOrientation(HORIZONTAL);
            setPadding(5, 5, 5, 5);
            setLayoutParams(layoutParams);

            LinearLayout.LayoutParams menuButtonLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            menuButton.setImageResource(android.R.drawable.ic_menu_sort_by_size);
            addViewInLayout(menuButton, 0, menuButtonLP);

            LinearLayout.LayoutParams smileButtonLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            smileButton.setImageResource(R.drawable.input_smile_button);
            addViewInLayout(smileButton, 1, smileButtonLP);

            LinearLayout.LayoutParams messageEditorLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            messageEditorLP.gravity = Gravity.CENTER | Gravity.LEFT;
            messageEditorLP.weight = (float) 0.87;
            addViewInLayout(messageEditor, 2, messageEditorLP);

            LinearLayout.LayoutParams sendButtonLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            sendButton.setImageResource(R.drawable.input_send_button);
            addViewInLayout(sendButton, 3, sendButtonLP);
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceStateLog) {
        isTablet = getActivity().findViewById(R.id.fragment_container) == null;
        /*if (isTablet) {
            RosterView.actionBar.setDisplayShowTitleEnabled(false);
            RosterView.actionBar.setDisplayShowHomeEnabled(false);
            RosterView.actionBar.setDisplayUseLogoEnabled(false);
            RosterView.actionBar.setDisplayHomeAsUpEnabled(false);
        }*/
        SawimActivity.actionBar.setNavigationMode(isTablet ? ActionBar.NAVIGATION_MODE_TABS : ActionBar.NAVIGATION_MODE_STANDARD);
        SawimActivity.actionBar.setDisplayShowCustomEnabled(true);
        SawimActivity.actionBar.setCustomView(chatBarLayout);
        updateChatIcon();

        if (chat_viewLayout == null)
            chat_viewLayout = new ChatViewRoot(getActivity());
        else
            ((ViewGroup)chat_viewLayout.getParent()).removeView(chat_viewLayout);
        int background = Scheme.getColor(Scheme.THEME_BACKGROUND);
        chat_viewLayout.setBackgroundColor(background);
        if (!isTablet) {
            chatBarLayout.setVisibilityUsersImage(ImageView.VISIBLE);
            usersImage.setBackgroundDrawable(new ColorDrawable(0));
            usersImage.setImageDrawable(General.usersIcon);
            usersImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (nickList == null) return;
                    Animation inAnimation = AnimationUtils.makeInAnimation(General.currentActivity, false);
                    Animation outAnimation = AnimationUtils.makeOutAnimation(General.currentActivity, true);
                    inAnimation.setDuration(200);
                    outAnimation.setDuration(200);
                    nickList.startAnimation(nickList.getVisibility() == View.VISIBLE
                            ? outAnimation : inAnimation);
                    nickList.setVisibility(nickList.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                }
            });
        } else
            chatBarLayout.setVisibilityUsersImage(View.GONE);
        chatsImage.setBackgroundDrawable(new ColorDrawable(0));
        chatsImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                forceGoToChat(ChatHistory.instance.getPreferredItem());
                RosterView rosterView = (RosterView) ChatView.this.getFragmentManager().findFragmentById(R.id.roster_fragment);
                if (rosterView != null)
                    rosterView.update();
            }
        });
        if (isTablet) {
            menuButton.setVisibility(ImageButton.VISIBLE);
            menuButton.setBackgroundColor(background);
            menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showMenu();
                }
            });
        } else
            menuButton.setVisibility(ImageButton.GONE);
        smileButton.setBackgroundColor(background);
        smileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard(view);
                new SmilesView().show(General.currentActivity.getSupportFragmentManager(), "show-smiles");
            }
        });
        messageEditor.setSingleLine(false);
        messageEditor.setMaxLines(4);
        messageEditor.setHorizontallyScrolling(false);
        messageEditor.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        messageEditor.setHint(R.string.hint_message);
        messageEditor.addTextChangedListener(textWatcher);
        messageEditor.setTextColor(Scheme.getColor(Scheme.THEME_TEXT));
        if (sendByEnter) {
            messageEditor.setImeOptions(EditorInfo.IME_ACTION_SEND);
            messageEditor.setOnEditorActionListener(enterListener);
        }
        sendByEnter = Options.getBoolean(Options.OPTION_SIMPLE_INPUT);
        if (sendByEnter) {
            sendButton.setVisibility(ImageButton.GONE);
        } else {
            sendButton.setVisibility(ImageButton.VISIBLE);
            sendButton.setBackgroundColor(background);
            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    send();
                    if (nickList.getVisibility() == View.VISIBLE && !isTablet)
                        nickList.setVisibility(View.GONE);
                }
            });
            sendButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    insert("/me ");
                    showKeyboard();
                    return true;
                }
            });
        }
        return chat_viewLayout;
    }

    @Override
    public void onStart() {
        super.onStart();
        General.currentActivity = getActivity();
        if (contact != null)
            openChat(protocol, contact);
        if (isTablet)
            if (lastContact == null)
                chat_viewLayout.showHint();
            else
                openChat(lastProtocol, lastContact);
    }

    @Override
    public void onPause() {
        super.onPause();
        pause(chat);
    }

    @Override
    public void onResume() {
        super.onResume();
        resume(chat);
    }

    public void pause(Chat chat) {
        if (chat == null) return;
        lastProtocol = protocol;
        lastContact = contact;
        initChat(protocol, contact);

        View item = chatListView.getChildAt(0);
        chat.scrollPosition = chatListView.getFirstVisiblePosition();
        chat.offset = (item == null) ? 0 : Math.abs(item.getBottom());
        chat.dividerPosition = chat.getMessData().size();
        chat.message = getText();

        chat.setVisibleChat(false);
        Roster.getInstance().setOnUpdateChat(null);
        chat.resetUnreadMessages();
        if (chat.empty()) ChatHistory.instance.unregisterChat(chat);
    }

    public void resume(Chat chat) {
        if (chat == null) return;
        chat.setVisibleChat(true);
        ChatHistory.instance.registerChat(chat);
        Roster.getInstance().setOnUpdateChat(this);
        chat.resetUnreadMessages();
        removeMessages(Options.getInt(Options.OPTION_MAX_MSG_COUNT));
        messageEditor.setText(chat.message);
        adapter.setPosition(chat.dividerPosition);
        if (chat.scrollPosition > 0) {
            chatListView.setSelectionFromTop(chat.scrollPosition + 2, chat.offset);
        } else {
            chatListView.setSelection(0);
        }
        updateChatIcon();
        updateList(contact);
    }

    private void removeMessages(final int limit) {
        if (chat.getMessData().size() < limit) return;
        if ((0 < limit) && (0 < chat.getMessData().size())) {
            while (limit < chat.getMessData().size()) {
                chat.scrollPosition--;
                chat.dividerPosition--;
                chat.getMessData().remove(0);
            }
        } else ChatHistory.instance.unregisterChat(chat);
    }

    public boolean hasBack() {
        if (nickList != null && !isTablet)
            if (nickList.getVisibility() == View.VISIBLE) {
                nickList.setVisibility(View.GONE);
                return false;
            }
        return true;
    }

    private void forceGoToChat(int position) {
        Chat current = ChatHistory.instance.chatAt(position);
        if (current == null) return;
        pause(chat);
        openChat(current.getProtocol(), current.getContact());
        resume(current);
    }

    public void initChat(Protocol p, Contact c) {
        protocol = p;
        contact = c;
    }

    public void openChat(Protocol p, Contact c) {
        chat_viewLayout.hideHint();
        initChat(p, c);
        chat = protocol.getChat(contact);

        initLabel();
        initList();
        initMucUsers();
    }

    public Chat getCurrentChat() {
        return chat;
    }

    private void initLabel() {
        chatsSpinnerAdapter = new ChatsSpinnerAdapter(getActivity());
        labelView.updateLabelIcon(chatsSpinnerAdapter.getImageChat(chat, false));
        labelView.updateTextView(contact.getName());
        labelView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DialogFragment() {
                    @Override
                    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                             Bundle savedInstanceState) {
                        getDialog().setCanceledOnTouchOutside(true);
                        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                        View v = inflater.inflate(R.layout.chats_dialog, container, false);
                        v.setBackgroundColor(Scheme.getInversColor(Scheme.THEME_CAP_BACKGROUND));
                        MyListView lv = (MyListView) v.findViewById(R.id.listView);
                        lv.setAdapter(chatsSpinnerAdapter);
                        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                Object o = parent.getAdapter().getItem(position);
                                if (o instanceof Chat) {
                                    Chat current = (Chat) o;
                                    pause(chat);
                                    openChat(current.getProtocol(), current.getContact());
                                    resume(current);
                                    dismiss();
                                    RosterView rosterView = (RosterView) ChatView.this.getFragmentManager().findFragmentById(R.id.roster_fragment);
                                    if (rosterView != null)
                                        rosterView.update();
                                }
                            }
                        });
                        return v;
                    }
                }.show(getFragmentManager().beginTransaction(), "force go to chat");
                chatsSpinnerAdapter.refreshList();
            }
        });
    }

    private void initList() {
        State chatState = chatHash.get(contact.getUserId());
        if (chatState == null) {
            chatState = new State();
            adapter = new MessagesAdapter();
            adapter.init(chat);
            chatState.adapter = adapter;
            chatHash.put(contact.getUserId(), chatState);
        } else {
            adapter = chatState.adapter;
        }
        chatListView.setAdapter(adapter);
        chatListView.setStackFromBottom(true);
        chatListView.setFastScrollEnabled(true);
        chatListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        chatListView.setOnCreateContextMenuListener(this);
        chatListView.setOnItemClickListener(new ChatClick());
        chatListView.setFocusable(true);
    }

    private void initMucUsers() {
        nickList.setVisibility(isTablet ? View.VISIBLE : View.GONE);
        nickList.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
        if (contact instanceof JabberServiceContact && contact.isConference()) {
            mucUsersView = new MucUsersView();
            mucUsersView.init(protocol, (JabberServiceContact) contact);
            mucUsersView.show(this, nickList);
            chatBarLayout.setVisibilityUsersImage(isTablet ? View.GONE : View.VISIBLE);
        } else {
            chatBarLayout.setVisibilityUsersImage(View.GONE);
            nickList.setVisibility(View.GONE);
        }
    }

    public void updateChatIcon() {
        Icon icMess = ChatHistory.instance.getUnreadMessageIcon();
        if (icMess == null) {
            chatBarLayout.setVisibilityChatsImage(View.GONE);
        } else {
            chatBarLayout.setVisibilityChatsImage(View.VISIBLE);
            chatsImage.setImageDrawable(icMess.getImage());
        }
    }

    @Override
    public void updateChat(final Contact contact) {
        General.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateChatIcon();
                updateList(contact);
            }
        });
    }

    private void updateList(Contact contact) {
        if (contact == this.contact) {
            if (chatsSpinnerAdapter != null)
                labelView.updateLabelIcon(chatsSpinnerAdapter.getImageChat(chat, false));
            if (adapter != null)
                adapter.refreshList(chat.getMessData());
        }
    }

    @Override
    public void updateMucList() {
        General.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (contact.isPresence() == (byte) 1) {
                    if (adapter != null)
                        adapter.refreshList(chat.getMessData());
                }
                if (mucUsersView != null)
                    mucUsersView.update();
            }
        });
    }

    private static class State {
        public MessagesAdapter adapter;
    }

    private class ChatClick implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            MessData mData = (MessData) adapterView.getAdapter().getItem(position);
            if (adapter.isMultiQuote()) {
                mData.setMarked(!mData.isMarked());
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < chat.getMessData().size(); ++i) {
                    MessData messData = chat.getMessageDataByIndex(i);
                    if (messData.isMarked()) {
                        String msg = messData.getText();
                        if (messData.isMe())
                            msg = "*" + messData.getNick() + " " + msg;
                        sb.append(Clipboard.serialize(false, messData.isIncoming(), messData.getNick() + " " + messData.strTime, msg));
                        sb.append("\n-----\n");
                    }
                }
                Clipboard.setClipBoardText(0 == sb.length() ? null : sb.toString());
                adapter.notifyDataSetChanged();
            } else {
                if (contact instanceof JabberServiceContact) {
                    JabberServiceContact jabberServiceContact = ((JabberServiceContact) contact);
                    if (jabberServiceContact.getContact(mData.getNick()) == null && !jabberServiceContact.getName().equals(mData.getNick())) {
                        Toast.makeText(General.currentActivity, getString(R.string.contact_walked), Toast.LENGTH_LONG).show();
                    }
                }
                setText(chat.onMessageSelected(mData));
                if (nickList.getVisibility() == View.VISIBLE && getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                    nickList.setVisibility(View.GONE);
            }
        }
    }

    public void showMenu() {
        if (chat == null) return;
        final MyMenu menu = new MyMenu(General.currentActivity);
        boolean accessible = chat.getWritable() && (contact.isSingleUserContact() || contact.isOnline());
        menu.add(getString(adapter.isMultiQuote() ?
                R.string.disable_multi_citation : R.string.include_multi_citation), ContactMenu.MENU_MULTI_CITATION);
        if (0 < chat.getAuthRequestCounter()) {
            menu.add(JLocale.getString("grant"), ContactMenu.USER_MENU_GRANT_AUTH);
            menu.add(JLocale.getString("deny"), ContactMenu.USER_MENU_DENY_AUTH);
        }
        if (!contact.isAuth()) {
            menu.add(JLocale.getString("requauth"), ContactMenu.USER_MENU_REQU_AUTH);
        }
        if (accessible) {
            if (sawim.modules.fs.FileSystem.isSupported()) {
                menu.add(JLocale.getString("ft_name"), ContactMenu.USER_MENU_FILE_TRANS);
            }
            menu.add(JLocale.getString("ft_cam"), ContactMenu.USER_MENU_CAM_TRANS);
        }
        menu.add(General.currentActivity.getResources().getString(R.string.user_statuses), ContactMenu.USER_MENU_STATUSES);
        if (!contact.isSingleUserContact() && contact.isOnline()) {
            menu.add(JLocale.getString("leave_chat"), ContactMenu.CONFERENCE_DISCONNECT);
        }
        menu.add(JLocale.getString("delete_chat"), ContactMenu.ACTION_CURRENT_DEL_CHAT);
        menu.add(JLocale.getString("all_contact_except_this"), ContactMenu.ACTION_DEL_ALL_CHATS_EXCEPT_CUR);
        menu.add(JLocale.getString("all_contacts"), ContactMenu.ACTION_DEL_ALL_CHATS);

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(General.currentActivity, R.style.AlertDialogCustom));
        builder.setTitle(contact.getName());
        builder.setCancelable(true);
        builder.setAdapter(menu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (menu.getItem(which).idItem) {
                    case ContactMenu.MENU_MULTI_CITATION:
                        if (adapter.isMultiQuote()) {
                            adapter.setMultiQuote(false);
                        } else {
                            adapter.setMultiQuote(true);
                            Toast.makeText(General.currentActivity, R.string.hint_multi_citation, Toast.LENGTH_LONG).show();
                        }
                        adapter.notifyDataSetChanged();
                        break;

                    case ContactMenu.ACTION_CURRENT_DEL_CHAT:
                        /*chat.removeMessagesAtCursor(chatListView.getFirstVisiblePosition() + 1);
                        if (0 < messData.size()) {
                            updateChat();
                        }*/
                        ChatHistory.instance.unregisterChat(chat);
                        if (General.currentActivity.findViewById(R.id.fragment_container) != null)
                            getFragmentManager().popBackStack();
                        else
                            chat_viewLayout.setVisibility(LinearLayout.GONE);
                        break;

                    case ContactMenu.ACTION_DEL_ALL_CHATS_EXCEPT_CUR:
                        ChatHistory.instance.removeAll(chat);
                        break;

                    case ContactMenu.ACTION_DEL_ALL_CHATS:
                        ChatHistory.instance.removeAll(null);
                        if (General.currentActivity.findViewById(R.id.fragment_container) != null)
                            getFragmentManager().popBackStack();
                        else
                            chat_viewLayout.setVisibility(LinearLayout.GONE);
                        break;

                    default:
                        new ContactMenu(protocol, contact).doAction(menu.getItem(which).idItem);
                }
            }
        });
        builder.create().show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, android.view.ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(Menu.FIRST, ContactMenu.MENU_COPY_TEXT, 0, android.R.string.copy);
        menu.add(Menu.FIRST, ContactMenu.ACTION_QUOTE, 0, JLocale.getString("quote"));
        if (contact instanceof JabberServiceContact && contact.isConference()) {
            menu.add(Menu.FIRST, ContactMenu.COMMAND_PRIVATE, 0, R.string.open_private);
            menu.add(Menu.FIRST, ContactMenu.COMMAND_INFO, 0, R.string.info);
            menu.add(Menu.FIRST, ContactMenu.COMMAND_STATUS, 0, R.string.user_statuses);
        }
        if (protocol instanceof Jabber) {
            menu.add(Menu.FIRST, ContactMenu.ACTION_TO_NOTES, 0, R.string.add_to_notes);
        }
        if (!Options.getBoolean(Options.OPTION_HISTORY) && chat.hasHistory()) {
            menu.add(Menu.FIRST, ContactMenu.ACTION_ADD_TO_HISTORY, 0, JLocale.getString("add_to_history"));
        }
        contact.addChatMenuItems(menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        MessData md = adapter.getItem(info.position);
        String nick = md.getNick();
        String msg = md.getText();
        switch (item.getItemId()) {
            case ContactMenu.MENU_COPY_TEXT:
                if (null == md) {
                    return false;
                }
                if (md.isMe()) {
                    msg = "*" + md.getNick() + " " + msg;
                }
                Clipboard.setClipBoardText(msg + "\n");
                break;

            case ContactMenu.ACTION_QUOTE:
                StringBuffer sb = new StringBuffer();
                if (md.isMe()) {
                    msg = "*" + md.getNick() + " " + msg;
                }
                sb.append(Clipboard.serialize(true, md.isIncoming(), md.getNick() + " " + md.strTime, msg));
                sb.append("\n-----\n");
                Clipboard.setClipBoardText(0 == sb.length() ? null : sb.toString());
                break;

            case ContactMenu.COMMAND_PRIVATE:
                String jid = Jid.realJidToSawimJid(contact.getUserId() + "/" + nick);
                JabberServiceContact c = (JabberServiceContact) protocol.getItemByUIN(jid);
                if (null == c) {
                    c = (JabberServiceContact) protocol.createTempContact(jid);
                    protocol.addTempContact(c);
                }
                pause(getCurrentChat());
                openChat(protocol, c);
                resume(getCurrentChat());
                break;
            case ContactMenu.COMMAND_INFO:
                protocol.showUserInfo(((JabberServiceContact) contact).getPrivateContact(nick));
                break;
            case ContactMenu.COMMAND_STATUS:
                protocol.showStatus(((JabberServiceContact) contact).getPrivateContact(nick));
                break;

            case ContactMenu.ACTION_ADD_TO_HISTORY:
                chat.addTextToHistory(md);
                break;

            case ContactMenu.ACTION_TO_NOTES:
                MirandaNotes notes = ((Jabber)protocol).getMirandaNotes();
                notes.showIt();
                MirandaNotes.Note note = notes.addEmptyNote();
                note.tags = md.getNick() + " " + md.strTime;
                note.text = md.getText();
                notes.showNoteEditor(note);
                break;
        }
        return super.onContextItemSelected(item);
    }

    private void showKeyboard(View view) {
        Configuration conf = Resources.getSystem().getConfiguration();
        if (conf.hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_NO) {
            InputMethodManager keyboard = (InputMethodManager) General.currentActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.showSoftInput(view, InputMethodManager.SHOW_FORCED);
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) General.currentActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void showKeyboard() {
        messageEditor.requestFocus();
        showKeyboard(messageEditor);
    }

    @Override
    public void pastText(final String text) {
        General.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                insert(" " + text + " ");
                showKeyboard();
            }
        });
    }

    private void send() {
        if (chat == null) return;
        hideKeyboard(messageEditor);
        chat.sendMessage(getText());
        resetText();
        adapter.setPosition(-1);
        updateList(contact);
        for (int i = 0; i < chat.getMessData().size(); ++i) {
            MessData messData = chat.getMessageDataByIndex(i);
            if (messData.isMarked()) {
                messData.setMarked(false);
            }
        }
    }

    private boolean canAdd(String what) {
        String text = getText();
        if (0 == text.length()) return false;
        // more then one comma
        if (text.indexOf(',') != text.lastIndexOf(',')) return true;
        // replace one post number to another
        if (what.startsWith("#") && !text.contains(" ")) return false;
        return true/*!text.endsWith(", ")*/;
    }

    private void resetText() {
        General.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageEditor.setText("");
            }
        });
    }

    private String getText() {
        return messageEditor.getText().toString();
    }

    private void setText(final String text) {
        General.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String t = null == text ? "" : text;
                if ((0 == t.length()) || !canAdd(t)) {
                    messageEditor.setText(t);
                    messageEditor.setSelection(t.length());
                } else {
                    insert(t);
                }
                showKeyboard();
            }
        });
    }

    private boolean hasText() {
        return 0 < messageEditor.getText().length();
    }

    public void insert(String text) {
        int start = messageEditor.getSelectionStart();
        int end = messageEditor.getSelectionEnd();
        messageEditor.getText().replace(Math.min(start, end), Math.max(start, end),
                text, 0, text.length());
    }

    private boolean isDone(int actionId) {
        return (EditorInfo.IME_NULL == actionId)
                || (EditorInfo.IME_ACTION_DONE == actionId)
                || (EditorInfo.IME_ACTION_SEND == actionId);
    }

    private boolean compose = false;
    private TextWatcher textWatcher = new TextWatcher() {
        private String previousText;
        private int lineCount = 0;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (sendByEnter) {
                previousText = s.toString();
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (sendByEnter && (start + count <= s.length()) && (1 == count)) {
                boolean enter = ('\n' == s.charAt(start));
                if (enter) {
                    messageEditor.setText(previousText);
                    messageEditor.setSelection(start);
                    send();
                }
            }
            if (lineCount != messageEditor.getLineCount()) {
                lineCount = messageEditor.getLineCount();
                messageEditor.requestLayout();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (protocol == null) return;
            int length = s.length();
            if (length > 0) {
                if (!compose) {
                    compose = true;
                    protocol.sendTypingNotify(contact, true);
                }
            } else {
                if (compose) {
                    compose = false;
                    protocol.sendTypingNotify(contact, false);
                }
            }
        }
    };

    private final TextView.OnEditorActionListener enterListener = new android.widget.TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(android.widget.TextView textView, int actionId, KeyEvent event) {
            if (isDone(actionId)) {
                if ((null == event) || (event.getAction() == KeyEvent.ACTION_DOWN)) {
                    send();
                    return true;
                }
            }
            return false;
        }
    };
}