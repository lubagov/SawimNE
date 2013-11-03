package ru.sawim.view;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.*;
import android.widget.*;
import protocol.Protocol;
import ru.sawim.General;
import sawim.Options;
import sawim.roster.Roster;
import sawim.comm.StringConvertor;
import protocol.Profile;
import protocol.StatusInfo;
import ru.sawim.R;
import ru.sawim.models.AccountsAdapter;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 16.05.13
 * Time: 19:58
 * To change this template use File | Settings | File Templates.
 */
public class AccountsListView extends Fragment {

    public static final String TAG = "AccountsListView";
    private AccountsAdapter accountsListAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.accounts_manager, container, false);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        General.returnFromAcc = true;
        ListView accountsList = (ListView) getActivity().findViewById(R.id.AccountsList);
        accountsListAdapter = new AccountsAdapter(getActivity());
        getActivity().setTitle(getString(R.string.options_account));
        accountsList.setCacheColorHint(0x00000000);
        accountsList.setAdapter(accountsListAdapter);
        accountsList.setOnCreateContextMenuListener(this);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(Menu.FIRST, R.id.menu_edit, 0, R.string.edit);
        menu.add(Menu.FIRST, R.id.lift_up, 0, R.string.lift_up);
        menu.add(Menu.FIRST, R.id.put_down, 0, R.string.put_down);
        menu.add(Menu.FIRST, R.id.menu_delete, 0, R.string.delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Profile account = accountsListAdapter.getItem(info.position);
        final int accountID = (int) accountsListAdapter.getItemId(info.position);
        final String itemName = account.userId;
        final int protocolType = account.protocolType;
        int num = info.position;

        switch (item.getItemId()) {
            case R.id.menu_edit:
                new LoginDialog(protocolType, accountID, true).show(getActivity().getSupportFragmentManager(), "login");
                return true;

            case R.id.lift_up:
                if ((0 != num) && (num < Options.getAccountCount())) {
                    Profile up = Options.getAccount(num);
                    Profile down = Options.getAccount(num - 1);
                    Options.setAccount(num - 1, up);
                    Options.setAccount(num, down);
                    Roster.getInstance().setCurrentProtocol();
                    update();
                }
                return true;

            case R.id.put_down:
                if (num < Options.getAccountCount() - 1) {
                    Profile up = Options.getAccount(num);
                    Profile down = Options.getAccount(num + 1);
                    Options.setAccount(num, down);
                    Options.setAccount(num + 1, up);
                    Roster.getInstance().setCurrentProtocol();
                    update();
                }
                return true;

            case R.id.menu_delete:
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setCancelable(true);
                builder.setMessage(getString(R.string.acc_delete_confirm) + " " + itemName + "?")
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        Protocol p = Roster.getInstance().getProtocol(Options.getAccount(accountID));
                                        android.accounts.Account acc = new android.accounts.Account(Options.getId(accountID), getString(R.string.app_name));
                                        AccountManager am = AccountManager.get(getActivity());
                                        am.removeAccount(acc, null, null);
                                        if (p != null)
                                            p.setStatus(StatusInfo.STATUS_OFFLINE, "");
                                        Options.delAccount(accountID);
                                        Roster.getInstance().setCurrentProtocol();
                                        Options.safeSave();
                                        update();
                                    }
                                })
                        .setNegativeButton(android.R.string.no, null)
                        .create().show();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    private void addAccountAuthenticator(String id) {
        Account account = new Account(id, getString(R.string.app_name));
        AccountManager am = AccountManager.get(getActivity());
        boolean accountCreated = am.addAccountExplicitly(account, null, null);
        Bundle extras = getActivity().getIntent().getExtras();
        if (extras != null && accountCreated) {
            AccountAuthenticatorResponse response = extras.getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
            Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, id);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.app_name));
            if (response != null)
                response.onResult(result);
        }
    }

    public void addAccount(int num, Profile acc) {
        addAccountAuthenticator(acc.userId);
        Options.setAccount(num, acc);
        Roster.getInstance().setCurrentProtocol();
        update();
    }

    public void update() {
        General.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                accountsListAdapter.notifyDataSetChanged();
            }
        });
    }

    public void addAccount() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(true);
        builder.setTitle(R.string.acc_sel_protocol);
        builder.setItems(Profile.protocolNames, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {
                new LoginDialog(Profile.protocolTypes[item], -1, false).show(getActivity().getSupportFragmentManager(), "login");
            }
        });
        builder.create().show();
    }

     class LoginDialog extends DialogFragment {
        private int type;
        public int id;
        private boolean isEdit;

        public LoginDialog(final int type, final int id, final boolean isEdit) {
            this.type = type;
            this.id = id;
            this.isEdit = isEdit;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View dialogLogin = inflater.inflate(R.layout.login, container, false);
            final TextView loginText = (TextView) dialogLogin.findViewById(R.id.acc_login_text);
            final EditText editLogin = (EditText) dialogLogin.findViewById(R.id.edit_login);
            final TextView serverText = (TextView) dialogLogin.findViewById(R.id.acc_server_text);
            final EditText editServer = (EditText) dialogLogin.findViewById(R.id.edit_server);
            final EditText editNick = (EditText) dialogLogin.findViewById(R.id.edit_nick);
            final EditText editPass = (EditText) dialogLogin.findViewById(R.id.edit_password);
            int protocolIndex = 0;
            final boolean isJabber = type == Profile.PROTOCOL_JABBER;
            for (int i = 0; i < Profile.protocolTypes.length; ++i) {
                if (type == Profile.protocolTypes[i]) {
                    protocolIndex = i;
                    break;
                }
            }
            loginText.setText(Profile.protocolIds[protocolIndex]);
            if (isJabber) {
                serverText.setVisibility(TextView.VISIBLE);
                editServer.setVisibility(EditText.VISIBLE);
            } else {
                serverText.setVisibility(TextView.GONE);
                editServer.setVisibility(EditText.GONE);
            }
            if (isEdit) {
                final Profile account = Options.getAccount(id);
                getDialog().setTitle(getText(R.string.acc_edit));
                if (isJabber) {
                    editLogin.setText(account.userId.substring(0, account.userId.indexOf('@')));
                    editServer.setText(account.userId.substring(account.userId.indexOf('@') + 1));
                } else {
                    editLogin.setText(account.userId);
                }
                editPass.setText(account.password);
                editNick.setText(account.nick);
            } else {
                getDialog().setTitle(getText(R.string.acc_add));
                if (isJabber) {
                    editServer.setText(General.DEFAULT_SERVER);
                }
            }
            if (type == Profile.PROTOCOL_ICQ) {
                editLogin.setInputType(InputType.TYPE_CLASS_NUMBER);
            } else {
                editLogin.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            }
            Button buttonOk = (Button) dialogLogin.findViewById(R.id.ButtonOK);
            final int finalProtocolIndex = protocolIndex;
            buttonOk.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    String login = editLogin.getText().toString();
                    String server = editServer.getText().toString();
                    String password = editPass.getText().toString();
                    String nick = editNick.getText().toString();
                    Profile account = new Profile();
                    if (1 < Profile.protocolTypes.length) {
                        account.protocolType = Profile.protocolTypes[finalProtocolIndex];
                    }
                    if (isJabber) {
                        if (login.indexOf('@') + 1 > 0) //isServer
                            account.userId = login;
                        else
                            account.userId = login + "@" + server;
                    } else
                        account.userId = login;
                    if (StringConvertor.isEmpty(account.userId)) {
                        return;
                    }
                    account.password = password;
                    account.nick = nick;

                    int editAccountNum = Options.getAccountIndex(account);
                    if (Options.getAccountCount() <= editAccountNum) {
                        account.isActive = true;
                    } else {
                        account.isActive = Options.getAccount(editAccountNum).isActive;
                    }

                    if (isEdit) {
                        addAccount(id, account);
                    } else {
                        account.isActive = true;
                        addAccount(Options.getAccountCount() + 1, account);
                    }
                    dismiss();
                }
            });
            return dialogLogin;
        }
     }
}
