package org.amoradi.syncopoli;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import static android.app.Activity.RESULT_OK;

import java.util.ArrayList;
import java.util.Arrays;

public class AddBackupItemFragment extends Fragment {
    IBackupHandler mHandler;
    BackupItem mBackup = null;
    int SOURCE_REQUEST_CODE = 1;
    int DESTINATION_REQUEST_CODE = 2;

    private TextInputEditText v_name;
    private TextInputEditText v_src;
    private TextInputEditText v_dst;
    private TextInputEditText v_opts;
    private TextInputLayout l_src;
    private TextInputLayout l_dst;

    @Override
    public void onAttach(Activity acc) {
        super.onAttach(acc);
        mHandler = (IBackupHandler) acc;
    }

    public void setBackupContent(BackupItem b) {
        mBackup = b;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_addbackupitem, container, false);

        v_name = (TextInputEditText) v.findViewById(R.id.addbackupitem_name);
        v_src = (TextInputEditText) v.findViewById(R.id.addbackupitem_source);
        v_dst = (TextInputEditText) v.findViewById(R.id.addbackupitem_destination);
        v_opts = (TextInputEditText) v.findViewById(R.id.addbackupitem_rsync_options);
        l_src = (TextInputLayout) v.findViewById(R.id.addbackupitem_source_layout);
        l_dst = (TextInputLayout) v.findViewById(R.id.addbackupitem_destination_layout);

        l_src.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, SOURCE_REQUEST_CODE);
            }
        });

        l_dst.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, DESTINATION_REQUEST_CODE);
            }
        });

        Spinner v_dir = (Spinner) v.findViewById(R.id.addbackupitem_direction);
        String[] items = getResources().getStringArray(R.array.addbackupitem_direction_entries);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        v_dir.setAdapter(adapter);

        if (mBackup == null) {
            return v;
        }

        if (mBackup.direction == BackupItem.Direction.OUTGOING) {
            v_dir.setSelection(1);
        } else if (mBackup.direction == BackupItem.Direction.LOCAL) {
            v_dir.setSelection(2);
        } else {
            v_dir.setSelection(0);
        }

        v_name.setText(mBackup.name);
        v_src.setText(TextUtils.join("\n", mBackup.sources.toArray()));
        v_dst.setText(mBackup.destination);
        v_opts.setText(mBackup.rsync_options);

        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            String path = getPathFromUri(uri);
            if (path == null) {
                // Fallback or error handling
                path = uri.getPath();
            }

            if (requestCode == SOURCE_REQUEST_CODE) {
                String currentText = v_src.getText().toString();
                if (!currentText.isEmpty() && !currentText.endsWith("\n")) {
                    v_src.append("\n");
                }
                v_src.append(path);
            } else if (requestCode == DESTINATION_REQUEST_CODE) {
                v_dst.setText(path);
            }
        }
    }

    private String getPathFromUri(Uri uri) {
        if (uri == null) return null;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT &&
            "com.android.externalstorage.documents".equals(uri.getAuthority())) {
            String docId = android.provider.DocumentsContract.getTreeDocumentId(uri);
            String[] split = docId.split(":");
            String type = split[0];
            String path = split.length > 1 ? split[1] : "";

            if ("primary".equalsIgnoreCase(type)) {
                return android.os.Environment.getExternalStorageDirectory() + "/" + path;
            } else {
                // Return generic path for non-primary volumes (like SD card UUID)
                // Assuming standard mount point for now as best effort
                 return "/storage/" + type + "/" + path;
            }
        }
        return uri.getPath();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.action_done).setVisible(true);
        menu.findItem(R.id.action_refresh).setVisible(false);
        menu.findItem(R.id.action_run).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_done) {
            BackupItem backupItem = new BackupItem();

            View v = getView();

            EditText t = (EditText) v.findViewById(R.id.addbackupitem_source);

            backupItem.sources = new ArrayList<String>();
            for (String s : t.getText().toString().split("\n")) {
                if (s.trim() != "") {
                    backupItem.sources.add(s.trim());
                }
            }

            t = (EditText) v.findViewById(R.id.addbackupitem_destination);
            backupItem.destination = t.getText().toString();

            t = (EditText) v.findViewById(R.id.addbackupitem_name);
            backupItem.name = t.getText().toString();

            t = (EditText) v.findViewById(R.id.addbackupitem_rsync_options);
            backupItem.rsync_options = t.getText().toString();

            Spinner s = (Spinner) v.findViewById(R.id.addbackupitem_direction);
            if (s.getSelectedItemPosition() == 0) {
                backupItem.direction = BackupItem.Direction.INCOMING;
            } else if (s.getSelectedItemPosition() == 1) {
                backupItem.direction = BackupItem.Direction.OUTGOING;
            } else {
                backupItem.direction = BackupItem.Direction.LOCAL;
            }

            if (mBackup == null) {
                mHandler.addBackup(backupItem);
            } else {
                mHandler.updateBackup(mBackup.name, backupItem);
            }
        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }
}
