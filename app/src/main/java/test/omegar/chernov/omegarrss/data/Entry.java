package test.omegar.chernov.omegarrss.data;


import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class Entry implements Parcelable {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {

        public Entry createFromParcel(Parcel parcel)
        {
            return new Entry(parcel);
        }

        public Entry[] newArray(int i)
        {
            return new Entry[i];
        }
    };

    private String title;
    private String abstractText;
    private Date date;
    private Date fetchDate;
    private String link;
    private String author;
    private String imageUrl;

    public Entry() {}

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getFetchDate() {
        return fetchDate;
    }

    public void setFetchDate(Date fetchDate) {
        this.fetchDate = fetchDate;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    Entry(Parcel in) {
        title = in.readString();
        abstractText = in.readString();
        date = new Date(in.readLong());
        fetchDate = new Date(in.readLong());
        link = in.readString();
        author = in.readString();
        imageUrl = in.readString();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(title);
        parcel.writeString(abstractText);
        parcel.writeLong(date.getTime());
        parcel.writeLong(fetchDate.getTime());
        parcel.writeString(link);
        parcel.writeString(author);
        parcel.writeString(imageUrl);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
