package com.snappfood.model;
import com.snappfood.controller.GenerallController;
import com.snappfood.model.ConfirmStatus;

public class Seller extends User
{
    private String brandName;
    private String brandDescription;
    private ConfirmStatus status = ConfirmStatus.PENDING;

    public Seller(String name, String phoneNumber, String email, String password, String address, byte[] profileImage, BankInfo bankInfo) {
        super(name, phoneNumber, email, password, Role.SELLER, address, GenerallController.toBase64(profileImage), bankInfo);
        this.status = ConfirmStatus.PENDING;
    }

    public Seller() {
        super();
        this.setRole(Role.SELLER);
    }


    public ConfirmStatus getStatus() {
        return status;
    }

    public void setStatus(ConfirmStatus status) {
        this.status = status;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getBrandDescription() {
        return brandDescription;
    }

    public void setBrandDescription(String brandDescription) {
        this.brandDescription = brandDescription;
    }
}