package uk.co.o2.android.handshake.common.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by hostova1 on 10/07/2014.
 */
public class Contact implements Parcelable {

    public String firstName;
    public String familyName;
    public String phoneNumber;
    public String emailAddress;


    public Contact(String firstName, String familyName, String phoneNumber, String emailAddress) {
        this.firstName = firstName;
        this.familyName = familyName;
        this.phoneNumber = phoneNumber;
        this.emailAddress = emailAddress;
    }

    public static final Parcelable.Creator<Contact> CREATOR
            = new Parcelable.Creator<Contact>() {
        public Contact createFromParcel(Parcel in) {
            return new Contact(in);
        }

        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[]{firstName, familyName, phoneNumber, emailAddress});

    }

    private Contact(Parcel in) {
        String[] arrray = new String[4];
        in.readStringArray(arrray);
        firstName = arrray[0];
        familyName = arrray[1];
        phoneNumber = arrray[2];
        emailAddress = arrray[3];

    }

    public String toString() {
        return String.format("FirstName: %s\n Surname: %s\n phoneNumber: %s\n emailAddress: %s", firstName, familyName, phoneNumber, emailAddress);
    }
}
