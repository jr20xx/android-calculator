package cu.lt.joe.calculator.models;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

public class SavedState implements Parcelable
{
    public static final Creator<SavedState> CREATOR = new Creator<>()
    {
        @Override
        public SavedState createFromParcel(Parcel in)
        {
            return new SavedState(in);
        }

        @Override
        public SavedState[] newArray(int size)
        {
            return new SavedState[size];
        }
    };
    private final String savedScreenContent, savedResult;
    private final boolean savedSolvedStatus;

    public SavedState(String savedScreenContent, String savedResult, boolean savedSolvedStatus)
    {
        this.savedScreenContent = savedScreenContent;
        this.savedResult = savedResult;
        this.savedSolvedStatus = savedSolvedStatus;
    }

    protected SavedState(@NonNull Parcel in)
    {
        savedScreenContent = in.readString();
        savedResult = in.readString();
        savedSolvedStatus = in.readInt() != 0;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i)
    {
        parcel.writeString(savedScreenContent);
        parcel.writeString(savedResult);
        parcel.writeInt(savedSolvedStatus ? 1 : 0);
    }

    public String getSavedScreenContent()
    {
        return savedScreenContent;
    }

    public boolean getSavedSolvedStatus()
    {
        return savedSolvedStatus;
    }

    public String getSavedResult()
    {
        return savedResult;
    }
}