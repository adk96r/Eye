package adk.giteye;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * Created by Adu on 12/10/2017.
 */

public class PersonDialog extends DialogFragment {

    private static final String TAG = "Checks";
    private Context context;
    private Person person;
    private PersonInfoView personInfoView;
    private Dialog dialog;

    public PersonDialog() {

    }

    public void setData(Context context, Person person, PersonInfoView personInfoView) {
        this.context = context;
        this.person = person;
        this.personInfoView = personInfoView;
        personInfoView.allowDialogs(false);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View view = inflater.inflate(R.layout.person_info_dialog, null);

        Log.d(TAG, person.getSem() + " ");
        // Set the person's data.
        ((TextView) view.findViewById(R.id.person_dialog_rollno))
                .setText(String.valueOf(person.getRollNo()));
        ((TextView) view.findViewById(R.id.person_dialog_sem))
                .setText(String.valueOf(person.getSem()));
        ((TextView) view.findViewById(R.id.person_dialog_branch))
                .setText(person.getBranch());
        ((TextView) view.findViewById(R.id.person_dialog_att))
                .setText(String.valueOf(person.getAttendance()));
        view.findViewById(R.id.person_dialog_dismiss_fab)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.cancel();
                    }
                });

        builder.setView(view);
        this.dialog = builder.create();
        return this.dialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        personInfoView.allowDialogs(true);
    }
}
