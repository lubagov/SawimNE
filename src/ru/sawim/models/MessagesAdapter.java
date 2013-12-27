package ru.sawim.models;

import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import protocol.Protocol;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.Scheme;
import ru.sawim.text.TextLinkClickListener;
import ru.sawim.widget.chat.MessageItemView;
import sawim.chat.Chat;
import sawim.chat.MessData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 04.04.13
 * Time: 21:33
 * To change this template use File | Settings | File Templates.
 */
public class MessagesAdapter extends BaseAdapter {

    private List<MessData> items = new ArrayList<MessData>();
    private Protocol currentProtocol;
    private String currentContact;

    public static boolean isRepaint;
    private boolean isMultiQuote = false;
    private int position = -1;

    public void init(Chat chat) {
        currentProtocol = chat.getProtocol();
        currentContact = chat.getContact().getUserId();
        refreshList(chat.getMessData());
    }

    public void refreshList(List<MessData> list) {
        items.clear();
        for (int i = 0; i < list.size(); ++i) {
            items.add(list.get(i));
        }
        notifyDataSetChanged();
    }

    public boolean isMultiQuote() {
        return isMultiQuote;
    }

    public void setMultiQuote(boolean multiShot) {
        isMultiQuote = multiShot;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public MessData getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public void onTimeStampShow(final View view) {
        ((MessageItemView)view).titleItemView.setTimeStampVisible(true);
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                ((MessageItemView)view).titleItemView.setTimeStampVisible(false);
                notifyDataSetChanged();
            }
        }, 1000);
        notifyDataSetChanged();
    }

    @Override
    public View getView(int index, View row, ViewGroup viewGroup) {
        final MessData mData = items.get(index);
        if (mData.messView == null || isRepaint)
            mData.messView = new MessageItemView(General.currentActivity, !(mData.isMe() || mData.isPresence()));

        MessageItemView item = mData.messView;
        CharSequence parsedText = mData.getText();
        String nick = mData.getNick();
        boolean incoming = mData.isIncoming();

        item.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        item.msgText.setOnTextLinkClickListener(new TextLinkClickListener(currentProtocol, currentContact));
        item.msgText.setTypeface(Typeface.DEFAULT);
        item.setBackgroundColor(0);
        if (mData.isMarked() && isMultiQuote) {
            item.msgText.setTypeface(Typeface.DEFAULT_BOLD);
            item.setBackgroundColor(Scheme.getColor(Scheme.THEME_MARKED_BACKGROUND));
        }

        if (mData.isMe() || mData.isPresence()) {
            item.msgText.setTextSize(General.getFontSize() - 2);
            if (mData.isMe()) {
                item.msgText.setText("* " + nick + " " + parsedText);
                item.msgText.setTextColor(Scheme.getColor(incoming ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG));
            } else {
                item.msgText.setText(nick + parsedText);
                item.msgText.setTextColor(Scheme.getColor(Scheme.THEME_CHAT_INMSG));
            }
        } else {
            item.setBackgroundResource(incoming ?
                    (Scheme.isBlack() ? R.drawable.msg_in_dark : R.drawable.msg_in)
                    : (Scheme.isBlack() ? R.drawable.msg_out_dark : R.drawable.msg_out));
            float displayDensity = General.getInstance().getDisplayDensity();
            if (incoming) {
                item.setPadding((int)(19 * displayDensity), (int)(7 * displayDensity), (int)(9 * displayDensity), (int)(9 * displayDensity));
            } else {
                item.setPadding((int)(11 * displayDensity), (int)(7 * displayDensity), (int)(18 * displayDensity), (int)(9 * displayDensity));
            }
            /*if (mData.iconIndex != Message.ICON_NONE) {
                Icon icon = Message.msgIcons.iconAt(mData.iconIndex);
                if (icon != null) {
                    item.titleItemView.setMsgImage(icon.getImage());
                }
            }*/

            item.titleItemView.setNick(Scheme.getColor(incoming ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG),
                    General.getFontSize(), Typeface.DEFAULT_BOLD, nick);

            item.titleItemView.setMsgTime(Scheme.getColor(incoming ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG),
                    General.getFontSize() * 2 / 3, Typeface.DEFAULT, mData.strTime);

            item.msgText.setTextSize(General.getFontSize());
            item.msgText.setTextColor(Scheme.getColor(mData.getMessColor()));
            item.msgText.setLinkTextColor(0xff35B6E5);
            item.msgText.setText(parsedText);
        }
        item.setShowDivider(Scheme.getColor(Scheme.THEME_TEXT), position == index && index > 0 && position != getCount());
        item.titleItemView.repaint();
        //item.msgText.repaint();
        return item;
    }
}