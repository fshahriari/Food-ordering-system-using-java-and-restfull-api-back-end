package com.snappfood.model;

import com.google.gson.annotations.SerializedName;
import com.snappfood.controller.UserController;
import com.snappfood.exception.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class User {
    @SerializedName("full_name")
    private String fullName;

    private String phone;
    private String email;
    private String password;
    private String address;
    private int id;

    // This field maps to the 'profileImageBase64' in the YAML
    @SerializedName("profileImageBase64")
    private byte[] profileImage;

    // This field is for internal use and not part of the API JSON
    private String profileImagePath;

    private Role role;

    @SerializedName("bank_info")
    private BankInfo bankInfo;

    // Internal field, not part of the API JSON
    private int failedLoginAttempts;

    // Internal field, not part of the API JSON
    private Timestamp lockTime;

    public User() {
    }

    public User(String name, String phoneNumber, String email,
                String password, Role role, String address, byte[] profileImage, BankInfo bankInfo) {
        this.fullName = name;
        this.phone = phoneNumber;
        this.email = email;
        this.password = password;
        this.role = role;
        this.address = address;
        this.profileImage = profileImage;
        this.bankInfo = bankInfo;
    }

    // Getters and Setters for all fields

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return fullName;
    }

    public void setName(String name) {
        this.fullName = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phoneNumber) {
        this.phone = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        if (role != role.SELLER && role != role.CUSTOMER &&
            role != role.ADMIN && role != role.COURIER) {
            return Role.UNDEFIENED;
        }
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public byte[] getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(byte[] profileImage) {
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

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public Timestamp getLockTime() {
        return lockTime;
    }

    public void setLockTime(Timestamp lockTime) {
        this.lockTime = lockTime;
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
        } catch (IOException e) {
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
            Map<String, Object> result = controller.handleSignup(newUser);
            System.out.println("Status Code: " + result.get("status"));
            System.out.println("Message: " + result.get("message"));
            System.out.println("User ID: " + result.get("user_id"));
            System.out.println("Token: " + result.get("token"));
        } catch (InvalidInputException | DuplicatePhoneNumberException | ForbiddenException | ResourceNotFoundException | TooManyRequestsException | InternalServerErrorException | UnauthorizedException e) {
            System.err.println("Signup Failed. Error: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("A critical database error occurred: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static public void logIn(String id, String password) {
        UserController controller = new UserController();
        try {
            Map<String, Object> result = controller.handleLogin(id, password);
            System.out.println("Status Code: " + result.get("status"));
            System.out.println("Message: " + result.get("message"));
            if (result.containsKey("token")) {
                System.out.println("Token: " + result.get("token"));
                User user = (User) result.get("user");
                System.out.println("Welcome, " + user.getName() + "!");
            }
        } catch (InvalidInputException | UnauthorizedException | ForbiddenException | InternalServerErrorException |
                 ResourceNotFoundException | TooManyRequestsException e) {
            System.err.println("Login Failed. Error: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("A critical database error occurred: " + e.getMessage());
        }
        catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
        }
    }
}
