package org.amoradi.syncopoli;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.content.Intent;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import android.app.Dialog;

import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;


public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "Syncopoli";

    public final static String KEY_SERVER_ADDRESS = "pref_key_server_address"; // String
    public final static String KEY_PROTOCOL = "pref_key_protocol"; // String
    public final static String KEY_RSYNC_USERNAME = "pref_key_username"; // String
    public final static String KEY_RSYNC_OPTIONS = "pref_key_options"; // String
    public final static String KEY_PRIVATE_KEY = "pref_key_private_key"; // String
    public final static String KEY_PORT = "pref_key_port"; // int
    public final static String KEY_FREQUENCY = "pref_key_frequency"; // int
    public final static String KEY_RSYNC_PASSWORD = "pref_key_rsync_password"; // String
    public final static String KEY_SSH_PASSWORD = "pref_key_ssh_password"; // String
    public final static String KEY_WIFI_ONLY = "pref_key_wifi_only"; // boolean
    public final static String KEY_WIFI_NAME = "pref_key_wifi_name"; // String
    public final static String KEY_VERIFY_HOST = "pref_key_verify_host"; // String
    public final static String KEY_CLEAR_HOSTS = "pref_key_clear_hosts"; // String
    public final static String KEY_AS_ROOT = "pref_key_as_root"; // boolean
    public final static String KEY_VERSION_CODE = "pref_key_version_code";
    public final static String KEY_SSH_KEYGEN = "pref_key_gen_SSH";
    public final static String KEY_SSH_EXPORT = "pref_key_export_SSH";
    public final static String KEY_SSH_IMPORT = "pref_key_import_SSH";

    private final static int PICK_SSH_KEY_REQUEST = 1;

	private final static int DEFAULT_RSYNC_PORT = 873;
	private final static int DEFAULT_SSH_PORT = 22;

    public final static String[] KEYS = {
        KEY_SERVER_ADDRESS,
        KEY_PROTOCOL,
        KEY_RSYNC_USERNAME,
        KEY_RSYNC_PASSWORD,
        KEY_RSYNC_OPTIONS,
        KEY_PRIVATE_KEY,
        KEY_PORT,
        KEY_FREQUENCY,
        KEY_SSH_PASSWORD,
        KEY_WIFI_ONLY,
        KEY_WIFI_NAME,
        KEY_AS_ROOT
    };

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_WIFI_ONLY) || key.equals(KEY_AS_ROOT)) {
            return;
        }

		/*
		 * if user is changing the protocol and leaves the port as default, then we change
		 * the port to match the default for the protocol selected. Else, if the user has
		 * changed the port to a custom one, then leave it alone since User Knows Best (TM)
		 */
		
		if (key.equals(KEY_PROTOCOL)) {
			SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
			int port = Integer.parseInt(prefs.getString(KEY_PORT, "22"));

			int newport = -1;

			if (prefs.getString(KEY_PROTOCOL, "SSH").equals("SSH") && port == DEFAULT_RSYNC_PORT) {
				newport = DEFAULT_SSH_PORT;
			} else if (prefs.getString(KEY_PROTOCOL, "SSH").equals("Rsync") && port == DEFAULT_SSH_PORT) {
				newport = DEFAULT_RSYNC_PORT;
			}

			if (newport > 0) {
				getPreferenceScreen()
					.getSharedPreferences()
					.edit()
					.putString(KEY_PORT, Integer.toString(newport))
					.apply();
			}
		}

        if (key.equals(KEY_FREQUENCY)) {
            ((BackupActivity)getActivity()).setupSyncAccount();
        }

        /*
		 * hide passwords from preference screen
		 */
		if (key.equals(KEY_SSH_PASSWORD) || key.equals(KEY_RSYNC_PASSWORD)) {
			SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
            Preference p = findPreference(key);
			if (prefs.getString(key, "").length() > 0) {
				p.setSummary("******");
			} else {
                p.setSummary("");
            }
		} else {
            Preference pref = findPreference(key);
            if (pref != null) {
                String summary = sharedPreferences.getString(key, "Not set");
                pref.setSummary(summary);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_SSH_KEY_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Uri returnUri = data.getData();

                Log.e(TAG, returnUri.toString());
                InputStream is;
                int size;

                try {
                    is = getContext().getContentResolver().openInputStream(returnUri);
                    size = is.available();
                } catch (IOException e) {
                    Toast.makeText(getContext(), getText(R.string.pref_toast_import_key_failed),  Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to open SSH key file for import");
                    return;
                }

                // too large files are implausible...
                if (size > 1000) {
                    Toast.makeText(getContext(), getText(R.string.pref_toast_import_key_failed),  Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to import SSH key: File to long.");
                }

                SSHManager sshman = new SSHManager(getContext());
                try {
                    sshman.writeKeyFromInput(is);
                    is.close();
                } catch (IOException e) {
                    Toast.makeText(getContext(), getText(R.string.pref_toast_import_key_failed),  Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to copy SSH key file into private storage");
                }

                if (sshman.getPubKey() == null) {
                    Toast.makeText(getContext(), getText(R.string.pref_toast_import_key_failed),  Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to import SSH key: Corrupt file.");
                    sshman.deleteKey();
                    Preference p = findPreference(KEY_SSH_EXPORT);
                    if (p != null) {
                        p.setEnabled(false);
                        p.setShouldDisableView(true);
                    }
                }
            }
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        if (getArguments() != null) {
            String key = getArguments().getString("rootKey");
            setPreferencesFromResource(R.xml.pref_general, key);
        } else {
            setPreferencesFromResource(R.xml.pref_general, rootKey);
        }
        setHasOptionsMenu(true);

        initializeSummaries();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        Preference verifyButton = findPreference(KEY_VERIFY_HOST);
        if (verifyButton != null) {
            verifyButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new GetHostFingerprintTask(getActivity().getWindow().getContext()).execute();
                    return true;
                }
            });
        }

        Preference clearButton = findPreference(KEY_CLEAR_HOSTS);
        if (clearButton != null) {
            clearButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new ClearHostsTask(getActivity().getWindow().getContext()).execute();
                    return true;
                }
            });
        }

        Preference genSshButton = findPreference(KEY_SSH_KEYGEN);
        if (genSshButton != null) {
            genSshButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    GenerateKeyDialog dialog = new GenerateKeyDialog();
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    dialog.show(ft, "dialog");
                    return false;
                }
            });
        }

        Preference importSshButton = findPreference(KEY_SSH_IMPORT);
        if (importSshButton != null) {
            importSshButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (new SSHManager(getContext()).isKeyPresent()) {
                        DialogFragment dialog = new KeyExistDialog();
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        dialog.show(ft, "keyExistDialog");
                    } else {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("*/*");
                        //Intent j = Intent.createChooser(intent, "Choose an application to open with:");
                        startActivityForResult(intent, PICK_SSH_KEY_REQUEST);
                    }
                    return false;
                }
            });
        }

        Preference shareKeyButton = findPreference(KEY_SSH_EXPORT);
        if (shareKeyButton != null) {
            SSHManager sshman = new SSHManager(getContext());
            if (!sshman.isKeyPresent()) {
                shareKeyButton.setEnabled(false);
                shareKeyButton.setShouldDisableView(true);
            }
            shareKeyButton.setOnPreferenceClickListener( new Preference.OnPreferenceClickListener() {
                @Override
                public  boolean onPreferenceClick(Preference preference) {
                    String pubKey = new SSHManager(getContext()).getPubKey();
                    if (pubKey != null) {
                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, pubKey);
                        sendIntent.setType("text/plain");
                        startActivity(sendIntent);
                    } else {
                        Toast.makeText(getContext(), "Failed to get public key", Toast.LENGTH_LONG).show();
                    }
                    return false;
                }
            });
        }

    }

    @Override
    public void onNavigateToScreen(PreferenceScreen preferenceScreen){
        SettingsFragment applicationPreferencesFragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString("rootKey", preferenceScreen.getKey());
        applicationPreferencesFragment.setArguments(args);
        getFragmentManager()
                .beginTransaction()
                .replace(getId(), applicationPreferencesFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.menu_settings).setVisible(false);
        menu.findItem(R.id.action_done).setVisible(false);
        menu.findItem(R.id.action_refresh).setVisible(false);
        menu.findItem(R.id.action_run).setVisible(false);
    }

    private void initializeSummaries() {
        String[] keys = {KEY_SERVER_ADDRESS, KEY_PROTOCOL, KEY_RSYNC_USERNAME,
		KEY_RSYNC_OPTIONS, KEY_PRIVATE_KEY, KEY_PORT, KEY_FREQUENCY};
        SharedPreferences sp = getPreferenceScreen().getSharedPreferences();

        for (String key : keys) {
            onSharedPreferenceChanged(sp, key);
        }
    }

	static private class AcceptHostFingerprintTask extends AsyncTask<Void, Void, Boolean> {
		private Context mContext;
		private SSHManager sshman;
		private String fingerprint;

		AcceptHostFingerprintTask(Context ctx, String fp) {
		    mContext = ctx;
		    sshman = new SSHManager(mContext);
            fingerprint = fp;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
		    return sshman.acceptHostKeyFingerprint(fingerprint);
        }

        @Override
        protected void onPostExecute(Boolean result) {
		    if (result) {
		        Log.i(TAG, "Remote host fingerprint accepted");
            } else {
                Log.e(TAG, "Could not accept remote host fingerprint");
                Toast.makeText(mContext, "Could not accept remote host fingerprint, please see logcat for details", Toast.LENGTH_LONG).show();
            }
        }
	}

	private class ClearHostsTask extends AsyncTask<Void, String, Boolean> {
        private Context mContext;
        private SSHManager sshman;

        ClearHostsTask(Context ctx) {
            mContext = ctx;
            sshman = new SSHManager(mContext);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return sshman.clearAcceptedHostKeyFingerprints();
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            if (result) {
                Toast.makeText(mContext, "Cleared all verified hosts", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, "Failed to clear hosts", Toast.LENGTH_SHORT).show();
            }
        }
    }

    static public class VerifyFingerprintDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String fp = getArguments().getString("fp");
            final Context ctx = getActivity();

            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            new AcceptHostFingerprintTask(ctx, fp).execute();
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(ctx);//, R.style.AppTheme);
            builder.setMessage("Does the following fingerprint match the host?\n" + fp);
            builder.setPositiveButton("Yes", dialogClickListener);
            builder.setNegativeButton("No", dialogClickListener);
            return builder.create();
        }
    }

    private class GetHostFingerprintTask extends AsyncTask<Void, String, String> {
        private Context mContext;
        private SSHManager sshman;

        GetHostFingerprintTask(Context ctx) {
            mContext = ctx;
            sshman = new SSHManager(mContext);
        }

        @Override
        protected String doInBackground(Void... params) {
            return sshman.getRemoteHostFingerprint();
        }

        @Override
        protected void onPostExecute(final String result) {
            if (result == null) {
                Toast.makeText(mContext, "Failed to verify host.", Toast.LENGTH_SHORT).show();
                return;
            }
            VerifyFingerprintDialog dialog = new VerifyFingerprintDialog();
            Bundle args = new Bundle();
            args.putString("fp", result);
            dialog.setArguments(args);

            FragmentTransaction tr = getFragmentManager().beginTransaction();
            dialog.show(tr, "dialog");
        }
    }

    static public class GenerateKeyDialog extends DialogFragment {

        private void startKeyGenTask(int keyLen) {
            GenKeyTaskFragment taskFragment = GenKeyTaskFragment.newInstance(keyLen);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(taskFragment, "KEYGEN-TASK").commit();
        }

        private Dialog makeKeyChooseDialog() {
            final Context ctx = getActivity();
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            SSHManager sshman = new SSHManager(ctx);
                            int pos = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                            int keyLen = new int[]{2048, 4096}[pos];
                            if (sshman.isKeyPresent()) {
                                GenerateKeyDialog dg = new GenerateKeyDialog();
                                Bundle args = new Bundle();
                                args.putInt("keylen", keyLen);
                                dg.setArguments(args);
                                FragmentTransaction ft = getFragmentManager().beginTransaction();
                                dg.show(ft, "key-exist-dialog");

                            } else {
                                startKeyGenTask(keyLen);
                            }
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(ctx)
                    .setTitle(getString(R.string.pref_dialog_key_length))
                    .setSingleChoiceItems(R.array.pref_entries_sshkeygen, 0, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // nothing to be done here
                            return;
                        }
                    })
                    .setPositiveButton(getString(R.string.pref_dialog_gen_ssh_key), dialogClickListener)
                    .setNegativeButton(getString(R.string.dialog_cancel), dialogClickListener);
            return builder.create();
        }

        private Dialog makeKeyExistDialog(final int keyLen) {
            final Context ctx = getActivity();
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            startKeyGenTask(keyLen);
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(ctx)
                    .setTitle(getString(R.string.pref_dialog_key_replace))
                    .setMessage(getString(R.string.pref_dialog_key_replace_msg))
                    .setPositiveButton(getString(R.string.pref_dialog_gen_ssh_key), dialogClickListener)
                    .setNegativeButton(getString(R.string.dialog_cancel), dialogClickListener);
            return builder.create();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Dialog dg;
            Bundle bundle = getArguments();
            if (bundle == null) {
                dg = makeKeyChooseDialog();
            } else {
                dg = makeKeyExistDialog(bundle.getInt("keylen"));
            }

            return dg;
        }
    }

    static public class GenKeyTaskFragment extends Fragment {
        /* This retained, non-UI fragment is used, to make the async task, that handles the key
         * generation (i.e. running dropbearkey) survive configuration changes (i.e. screen rotation).
         */

        private GenerateKeyTask mTask;
        private AlertDialog mDlg;
        private Context mCtx;

        public static GenKeyTaskFragment newInstance(int keyLen) {
            GenKeyTaskFragment f = new GenKeyTaskFragment();

            Bundle args = new Bundle();
            args.putInt("keylen", keyLen);
            f.setArguments(args);

            return f;
        }

        /**
         * This method will only be called once when the retained
         * Fragment is first created.
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Bundle bundle = getArguments();
            final int keyLen = bundle.getInt("keylen");

            // Retain this fragment across configuration changes.
            setRetainInstance(true);

            // Create and execute the background task.
            mTask = new GenerateKeyTask();
            mTask.execute(keyLen);

            // create the dialog
            mDlg = createDialog();
        }

        @Override
        public void onAttach(Context ctx) {
            super.onAttach(ctx);
            if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED) {
                // after configuration change: recreate dialog if task is not yet finished
                mDlg = createDialog();
            }
            mCtx = ctx;
        }

        @Override
        public void onResume() {
            super.onResume();
            if (mTask != null && mTask.getStatus() == AsyncTask.Status.FINISHED) {
                /* If the task finished after onInstanceStateSaved (i.e. if app is paused), we can
                 * not remove this fragment directly. So we must do it the app is resumed.
                 */
                getFragmentManager().beginTransaction().remove(this).commit();
            }
        }

        private AlertDialog createDialog() {
            /* It is more easy to recreate the dialog after a configuration change (i.e. screen
             * rotation), than wrapping the dialog in an own Fragment and manage the DialogFragment
             * removal from out of this Fragment.
             */
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setCancelable(false)
                    .setView(R.layout.progress_dialog);
                    //.setTitle("Generating SSH Key");
            return builder.show();
        }

        @Override
        public void onDetach() {
            super.onDetach();
            if (mDlg != null) {
                mDlg.dismiss();
                mDlg = null;
            }
            mCtx = null;
        }

        private class GenerateKeyTask extends AsyncTask<Integer, Void, Boolean> {
            @Override
            protected Boolean doInBackground(Integer... ints) {
                SSHManager sshMan = new SSHManager(getActivity());
                return sshMan.generateKey(ints[0]);
            }

            @Override
            protected void onPostExecute(Boolean res) {
                if (mDlg != null) {
                    mDlg.dismiss();
                }

                if (!isStateSaved()) {
                    /* Remove this fragment: We can do this only before onInstanceStadeSaved()
                     * Otherwise The this is is done in GenKeyTaskFragment.onResume()
                     */
                    getFragmentManager().beginTransaction().remove(GenKeyTaskFragment.this).commit();
                }

                // enable/disable export key button
                Fragment f = getFragmentManager().findFragmentById(R.id.content_container);
                if (f instanceof SettingsFragment) {
                    Preference p = ((SettingsFragment) f).findPreference(KEY_SSH_EXPORT);
                    if (p != null) {
                        p.setEnabled(!res);
                        p.setShouldDisableView(res);
                    }
                }

                if (!res) {
                    Toast.makeText(getActivity(),getString(R.string.pref_toast_key_success), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getActivity(),getString(R.string.pref_toast_key_failed), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Generation failed!");
                }
            }
        }
    }

    static public class KeyExistDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context ctx = getActivity();
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("*/*");
                            SettingsFragment pref = (SettingsFragment)getFragmentManager().
                                    findFragmentByTag(BackupActivity.FRAGMENT_PREFERENCES_TAG);
                            if (pref != null) {
                                pref.startActivityForResult(intent, PICK_SSH_KEY_REQUEST);
                            }
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(ctx)
                    .setTitle(getString(R.string.pref_dialog_key_replace))
                    .setMessage(getString(R.string.pref_dialog_key_replace_msg))
                    .setPositiveButton(getString(R.string.pref_dialog_import_ssh_key), dialogClickListener)
                    .setNegativeButton(getString(R.string.dialog_cancel), dialogClickListener);
            return builder.create();
        }
    }
}
