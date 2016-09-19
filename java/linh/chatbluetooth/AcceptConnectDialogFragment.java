package linh.chatbluetooth;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

/**
 * Created by sev_user on 8/9/2016.
 */
public class AcceptConnectDialogFragment extends DialogFragment {
    private iDialog iDialog;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        iDialog = (iDialog)activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getActivity().getResources().getString(R.string.connect_request));
        String deviceName = getArguments().getString("device");
        builder.setMessage(deviceName + getActivity().getResources().getString(R.string.connect_request_question));
        builder.setPositiveButton(getActivity().getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                iDialog.sendAcceptMessage();
            }
        });
        builder.setNegativeButton(getActivity().getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                iDialog.refuseConnect();
            }
        });
        builder.setCancelable(false);
        return builder.create();
    }

    public interface iDialog{
        void sendAcceptMessage();
        void refuseConnect();
    }
}