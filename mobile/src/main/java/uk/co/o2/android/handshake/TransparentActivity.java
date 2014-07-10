package uk.co.o2.android.handshake;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import uk.co.o2.android.handshake.common.model.Contact;
import uk.co.o2.android.handshake.common.utils.Constants;


public class TransparentActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        Contact contact = intent.getParcelableExtra(Constants.Extras.CONTACT);
        if (contact == null) {
            finish();
            return;
        }

        Bundle bundle = new Bundle(1);
        bundle.putParcelable(Constants.Extras.CONTACT, contact);
        DialogFragment fragment = new ContactPopupFragment();
        fragment.setArguments(bundle);
        fragment.show(getFragmentManager(), "Contact_Popup");
    }

    public static class ContactPopupFragment extends DialogFragment {
        private Contact mContact;

        public ContactPopupFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mContact = getArguments().getParcelable(Constants.Extras.CONTACT);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Dialog dialog = new Dialog(getActivity(), R.style.O2Dialog);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.contact_dialog, null, false);
            ((TextView) view.findViewById(R.id.firstName_TextView)).setText(getString(R.string.family_name_is, mContact.firstName));
            ((TextView) view.findViewById(R.id.familyName_TextView)).setText(getString(R.string.family_name_is, mContact.familyName));
            ((TextView) view.findViewById(R.id.phoneNumber_TextView)).setText(getString(R.string.phone_number_is, mContact.phoneNumber));
            ((TextView) view.findViewById(R.id.email_TextView)).setText(getString(R.string.email_address_is, mContact.emailAddress));
            dialog.setContentView(view);
            dialog.getWindow().getAttributes().windowAnimations = R.style.Scale_Dialog_Animation;
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            return dialog;
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            getActivity().finish();
        }
    }
}
