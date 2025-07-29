package com.snappfood.model;

import com.snappfood.controller.GenerallController;

public class courier extends User
{
    String vehicleType;
    String licenseNumber;
    private ConfirmStatus status = ConfirmStatus.PENDING;
    private CourierStatus courierStatus;

    public courier() {}

    public courier(String name, String phoneNumber, String email, String password, String address, String vehicleType, String licenseNumber, byte[] profileImage, BankInfo bankInfo) {
        super(name, phoneNumber, email, password, Role.COURIER, address, GenerallController.toBase64(profileImage), bankInfo);
        this.vehicleType = vehicleType;
        this.licenseNumber = licenseNumber;
        this.status = ConfirmStatus.PENDING;
        this.courierStatus = CourierStatus.AVAILABLE;
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

    public CourierStatus getCourierStatus() {
        return courierStatus;
    }

    public void setCourierStatus(CourierStatus courierStatus) {
        this.courierStatus = courierStatus;
    }
}