package com.snappfood.model;
import com.snappfood.model.ConfirmStatus;

public class Seller extends User
{
    // list of resturants and their info
    private ConfirmStatus status = ConfirmStatus.PENDING;

    public Seller(String name, String phoneNumber, String email, String password, String address, byte[] profileImage, BankInfo bankInfo) {
        super(name, phoneNumber, email, password, Role.SELLER, address, profileImage, bankInfo);
        this.status = ConfirmStatus.PENDING;
    }

    public ConfirmStatus getStatus() {
        return status;
    }

    public void setStatus(ConfirmStatus status) {
        this.status = status;
    }

    //methods to add: managing restaurants, adding new restaurants, updating restaurant information, deleting restaurant
}