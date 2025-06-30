package com.snappfood.model;

public class courier extends User
{
    // list of deliveries and their info
    String vehicleType;
    String licenseNumber;
    private ConfirmStatus status = ConfirmStatus.PENDING;

    public courier(String name, String phoneNumber, String email, String password, String address, String vehicleType, String licenseNumber, byte[] profileImage, BankInfo bankInfo) {
        super(name, phoneNumber, email, password, Role.COURIER, address, profileImage, bankInfo);
        this.vehicleType = vehicleType;
        this.licenseNumber = licenseNumber;
        this.status = ConfirmStatus.PENDING;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public ConfirmStatus getStatus() {
        return status;
    }

    public void setStatus(ConfirmStatus status) {
        this.status = status;
    }

    // methods to add:delivering, update delivery status,
}