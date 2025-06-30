package com.snappfood.model;

import com.mysql.cj.protocol.x.SyncFlushDeflaterOutputStream;
import com.snappfood.controller.UserController;
import com.snappfood.exception.*;
import com.snappfood.model.Role;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import java.sql.SQLException;
import java.util.Map;

public class User
{
    private String fullName;
    private String phone;
    private String email; //optional
    private String password;
    private String address; //optional
    private int id;
    private byte[] profileImage;
    private String profileImagePath; // Path to profile image on user's device. this feild will not be uploaded to database and it's temporary.
    private Role role;
    private BankInfo bankInfo;
    //BankInfo, for seller, customer, and com.snappfood.model.courier

    public User(){}

    public User (String name, String phoneNumber, String email,
                 String password, Role role, String address , byte[] profileImage, BankInfo bankInfo) // <-- Change here
    {
        this.fullName = name;
        this.phone = phoneNumber;
        this.email = email;
        this.password = password;
        this.role = role;
        this.address = address;
        this.profileImage = profileImage; // <-- And here
        this.bankInfo = bankInfo;
    }
    public int getId(){
        return id;
    }
    public void setId(int id){
        this.id = id;
    }
    public String getName(){
        return fullName;
    }
    public void setName(String name){
        this.fullName = name;
    }
    public String getPhone(){
        return phone;
    }
    public void setPhone(String phoneNumber){
        this.phone = phoneNumber;
    }
    public String getEmail(){
        return email;
    }
    public void setEmail(String email){
        this.email = email;
    }
    public String getPassword(){
        return password;
    }
    public void setPassword(String password){
        this.password = password;
    }
    public Role getRole(){
        return role;
    }
    public void setRole(Role role){
        this.role = role;
    }
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public byte[] getProfileImage() { // <-- Change here
        return profileImage;
    }

    public void setProfileImage(byte[] profileImage) { // <-- And here
        this.profileImage = profileImage;
    }
    public BankInfo getBankInfo() {
        return bankInfo;
    }
    public void setBankInfo(BankInfo bankInfo) {
        this.bankInfo = bankInfo;
    }
    public String getProfileImagePath() {
        return profileImagePath;
    }
    public void setProfileImagePath(String profileImagePath) {
        this.profileImagePath = profileImagePath;
    }
    private static byte[] readImageToBytes(String imagePath) throws IOException {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return null;
        }
        return Files.readAllBytes(Paths.get(imagePath));
    }

    static public void signup(String name, String phone, String email, String password, String role, String address,
                              String imagePath, String bankName, String accountNumber) {

        byte[] profileImage = null;
        try {
            if (imagePath != null && !imagePath.trim().isEmpty()) {
                profileImage = Files.readAllBytes(Paths.get(imagePath));
                String mimeType = Files.probeContentType(Paths.get(imagePath));
                List<String> allowedMimeTypes = Arrays.asList("image/jpeg", "image/png", "image/gif");
                if (mimeType == null || !allowedMimeTypes.contains(mimeType)) {
                    throw new UnsupportedMediaTypeException("Unsupported media type");
                }
            }
        } catch (UnsupportedMediaTypeException e) {
            System.err.println("error: " + e.getMessage());
            return;
        }
        catch (IOException e) {
            System.err.println("Error reading image file: " + e.getMessage());
            return;
        }

        BankInfo bankInfo = new BankInfo(bankName, accountNumber);
        Role enumRole = null;
        for (Role r : Role.values()) {
            if (r.getValue().equalsIgnoreCase(role)) {
                enumRole = r;
                break;
            }
        }

        User newUser = new User(
                name,
                phone,
                email,
                password,
                enumRole,
                address,
                profileImage,
                bankInfo
        );

        UserController controller = new UserController();
        try {
            // The controller will now receive a pre-validated User object
            Map<String, Object> result = controller.handleSignup(newUser);
            System.out.println("Status Code: " + result.get("status"));
            System.out.println("Message: " + result.get("message"));
            System.out.println("User ID: " + result.get("user_id"));
            System.out.println("Token: " + result.get("token"));
        } catch (InvalidInputException | DuplicatePhoneNumberException | ForbiddenException | ResourceNotFoundException | TooManyRequestsException | InternalServerErrorException e) {
            System.err.println("Signup Failed. Error: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("A critical database error occurred: " + e.getMessage());
        }
    }

    static public void logIn()
    {

    }

    //methods to add: log in, managing profile(edit, view, delete)

}