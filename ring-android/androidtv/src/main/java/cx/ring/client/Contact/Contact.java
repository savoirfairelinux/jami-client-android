package cx.ring.client.Contact;

/**
 * Created by mschmit on 26/05/17.
 */

public class Contact {
    private static final String TAG = Contact.class.getSimpleName();

    static final long serialVersionUID = 727566175075960653L;
    private long id;
    private String name;
    private String address;
    private byte[] photo;

    public Contact() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String title) {
        this.name = title;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String studio) {
        this.address = studio;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    @Override
    public String toString() {
        return "Contact{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
