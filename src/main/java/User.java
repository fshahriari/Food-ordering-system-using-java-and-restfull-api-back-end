public class User
{
    private String name;
    private final String phoneNumber;
    private final String email; //optional
    private final String password;
    private final String address; //optional
    public String id; // =phoneNumber;
    //BankInfo, for seller, buyer, and courier
    
    public User (String name, String phoneNumber, String email,
         String password, String address)
    {
        this.address = address;
		this.id = phoneNumber;
		this.phoneNumber = phoneNumber;
		this.password = password;
		this.email = email;
    }

	//methods to add: sign up, log in, managing profile(edit, view, delete)
}
