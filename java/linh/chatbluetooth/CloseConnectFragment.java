package linh.chatbluetooth;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

/**
 * Created by sev_user on 8/10/2016.
 */
public class CloseConnectFragment extends DialogFragment {
    private ICloseFragment iCloseFragment;
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        iCloseFragment = (ICloseFragment)activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getActivity().getResources().getString(R.string.close_connect_question));
        builder.setPositiveButton(getActivity().getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                iCloseFragment.disconnect();
            }
        });
        builder.setNegativeButton(getActivity().getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        return builder.create();
    }

    public interface ICloseFragment{
        void disconnect();
    }
}