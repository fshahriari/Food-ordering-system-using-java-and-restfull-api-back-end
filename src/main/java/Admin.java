import java.util.HashMap;

public class Admin extends User
{
    static HashMap<String, User> usres;
    //an static list of pending orders
    //an static report of sales statics
    public Admin (String name, String phoneNumber, String email, String password, String address) {
        super(name, phoneNumber, email, password, address);
    }

    // methods to add: confirming or rejecting restaurant requests, confirming orders
    // confirm or removing users, (static) view overall system statics
    // (static) checking order problems
}
